/*
 * Copyright (c) 2025 TrekkieEnderman
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift;

import io.github.TrekkieEnderman.advancedgift.commands.concrete.*;
import io.github.TrekkieEnderman.advancedgift.data.LegacyDataManager;
import io.github.TrekkieEnderman.advancedgift.data.PlayerDataManager;
import io.github.TrekkieEnderman.advancedgift.data.StandardDataManager;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.listener.PlayerJoinListener;
import io.github.TrekkieEnderman.advancedgift.metrics.GiftCounter;
import io.github.TrekkieEnderman.advancedgift.nms.NMSInterface;
import io.github.TrekkieEnderman.advancedgift.nms.Reflect;
import io.github.TrekkieEnderman.advancedgift.locale.Translation;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class AdvancedGift extends JavaPlugin {
    private final File configFile = new File(getDataFolder(),"config.yml");
    private final HashMap<Integer, ArrayList<String>> worldList = new HashMap<>();
    @Getter
    private String prefix;
    @Getter
    private String extLib;
    @Getter
    private NMSInterface nms;
    @Getter
    private boolean textTooltipEnabled;
    private boolean hasArtMap = false;
    @Getter
    private final GiftCounter giftCounter = new GiftCounter();
    @Getter
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        ServerVersion.init();
        getLogger().info("===================================================");
        getLogger().info("Loading files  --------------------");
        loadFiles();
        getLogger().info("");

        getLogger().info("Checking server version  ------------------");
        if (getConfigFile().getBoolean("enable-tooltip")) {
            getLogger().info("NMS Version used: " + ServerVersion.getNMSVersion());
            getLogger().info("");
            try {
                final Class<?> classy = Class.forName("io.github.TrekkieEnderman.advancedgift.nms." + ServerVersion.getNMSVersion().toUpperCase());
                if (NMSInterface.class.isAssignableFrom(classy)) {
                    nms = (NMSInterface) classy.getConstructor().newInstance();
                }
            } catch (final Exception ignored) {
                // Attempt to use the reflection class only if the server is 1.20.5 or newer.
                if (ServerVersion.getMinorVersion() == 20 && ServerVersion.getRevisionVersion() >= 5 || ServerVersion.getMinorVersion() >= 21) {
                    try {
                        nms = new Reflect(ServerVersion.getNMSVersion());
                    } catch (Throwable ex) {
                        getLogger().log(Level.WARNING, "Couldn't set up reflection for hover over text tooltip", ex);
                        nms = null;
                    }
                }
            }
            if (nms != null) {
                getLogger().info("This version is supported!");
                textTooltipEnabled = true;
            } else {
                getLogger().warning("Warning!");
                getLogger().warning("This plugin doesn't have support for this version!");
                getLogger().warning("In order to maintain compatibility with this server,");
                getLogger().warning("Item text hover over has been disabled in this plugin!");
                getLogger().warning("Check for updates at www.spigotmc.org/resources/advancedgift.46458/");
                getLogger().warning("");
                getLogger().warning("If this plugin still breaks, please contact TrekkieEnderman immediately.");
                textTooltipEnabled = false;
            }
        } else {
            getLogger().info("No version-dependent features in use. Skipping this step.");
            textTooltipEnabled = false;
        }
        getLogger().info("");

        getLogger().info("Searching for a material library  -----------------");
        if (Bukkit.getPluginManager().getPlugin("LangUtils") != null) {
            getLogger().info("Language Utils found. This library will be used.");
            extLib = "LangUtils";
        } else {
            getLogger().info("No supported material library found.");
            getLogger().info("Spigot's material enum will be used instead. Material names won't be translated.");
            extLib = "none"; //If you're wondering why this isn't left null instead, I don't know!
        }
        this.getCommand("gift").setExecutor(new CommandGift(this));
        this.getCommand("togglegift").setExecutor(new CommandGiftToggle(this));
        this.getCommand("giftblock").setExecutor(new CommandGiftBlock(this));
        this.getCommand("giftunblock").setExecutor(new CommandGiftUnblock(this));
        this.getCommand("giftblocklist").setExecutor(new CommandGiftBlockList(this));
        this.getCommand("agreload").setExecutor(new CommandReload(this));
        this.getCommand("giftspy").setExecutor(new CommandSpy(this));
        this.getCommand("agtranslate").setExecutor(new CommandTranslate(this));
        getLogger().info("===================================================");
        if (Bukkit.getPluginManager().getPlugin("ArtMap") != null) hasArtMap = true;
        startMetrics();
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    private void loadFiles() {
        if(!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        Translation.init(this);
        loadConfigFile();
        if (isConfigOutdated()) {
            getLogger().warning(Message.OUTDATED_CONFIG.translate());
        }

        if (ServerVersion.getMinorVersion() > 11) {
            playerDataManager = new StandardDataManager(this);
        } else {
            playerDataManager = new LegacyDataManager(this);
        }

        this.getPlayerDataManager().load();
    }

    public FileConfiguration getConfigFile() {
        return getConfig();
    }

    public boolean loadConfigFile() {
        // Moved config creation to here so the plugin doesn't run into issues when reloading it on command later
        if (!configFile.exists()) {
            getLogger().info(Message.CONFIG_NOT_FOUND.translate());
            saveDefaultConfig();
        }
        reloadConfig();
        Translation.updateLocale(getConfigFile().getString("locale"));
        loadWorldGroupList();
        prefix = ChatColor.translateAlternateColorCodes('&', this.getConfigFile().getString("prefix") + " ");
        getLogger().log(Level.INFO, Message.CONFIG_LOADED.translate());
        return true;
    }

    public boolean isConfigOutdated() {
        return !this.getConfig().isSet("locale");
    }

    @Override
    public void onDisable() {
        this.getPlayerDataManager().save();
    }

    private void loadWorldGroupList() {
        worldList.clear();
        int key = 0;
        for (String world : getConfig().getStringList("world-group-list")) {
            String[] array = world.split(", ");
            ArrayList<String> list = new ArrayList<>(Arrays.asList(array));
            worldList.put(key, list);
            key += 1;
        }
    }

    public int getPlayerWorldGroup(Player player) {
        //int playerWorldGroup = -1;
        for (int key : worldList.keySet()) {
            ArrayList<String> values = worldList.get(key);
            for (String w : values) {
                if (player.getWorld().getName().equalsIgnoreCase(w)) {
                    return key;
                }
            }
        }
        return -1;
    }

    private void startMetrics() {
        Metrics metrics = new Metrics(this, 13627);
        metrics.addCustomChart(new SingleLineChart("gifts_sent", giftCounter::collect));
    }

    public boolean hasArtMap() {
        return hasArtMap;
    }
}