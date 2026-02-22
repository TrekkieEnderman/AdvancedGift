/*
 * Copyright (c) 2025-2026 TrekkieEnderman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift;

import io.github.TrekkieEnderman.advancedgift.commands.concrete.*;
import io.github.TrekkieEnderman.advancedgift.data.PlayerDataManager;
import io.github.TrekkieEnderman.advancedgift.data.StandardDataManager;
import io.github.TrekkieEnderman.advancedgift.listener.PlayerJoinListener;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.locale.Translation;
import io.github.TrekkieEnderman.advancedgift.metrics.GiftCounter;
import io.github.TrekkieEnderman.advancedgift.util.ComponentUtils;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvancedGift extends JavaPlugin {
    private final File configFile = new File(getDataFolder(),"config.yml");
    private final Map<Integer, List<String>> worldGroups = new HashMap<>();
    private boolean hasArtMap = false;
    @Getter
    private final GiftCounter giftCounter = new GiftCounter();
    @Getter
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        loadFiles();
        this.getCommand("gift").setExecutor(new CommandGift(this));
        this.getCommand("togglegift").setExecutor(new CommandGiftToggle(this));
        this.getCommand("giftblock").setExecutor(new CommandGiftBlock(this));
        this.getCommand("giftunblock").setExecutor(new CommandGiftUnblock(this));
        this.getCommand("giftblocklist").setExecutor(new CommandGiftBlockList(this));
        this.getCommand("agreload").setExecutor(new CommandReload(this));
        this.getCommand("giftspy").setExecutor(new CommandSpy(this));
        this.getCommand("agtranslate").setExecutor(new CommandTranslate(this));
        hasArtMap = Bukkit.getPluginManager().isPluginEnabled("ArtMap");
        startMetrics();
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    private void loadFiles() {
        if(!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        Translation.init(this);
        loadConfigFile();
        if (isConfigOutdated()) {
            getLogger().warning(ComponentUtils.toPlainText(Message.OUTDATED_CONFIG.translate()));
        }
        playerDataManager = new StandardDataManager(this);
        this.getPlayerDataManager().load();
    }

    public FileConfiguration getConfigFile() {
        return getConfig();
    }

    public boolean loadConfigFile() {
        // Moved config creation to here so the plugin doesn't run into issues when reloading it on command later
        if (!configFile.exists()) {
            getLogger().info(ComponentUtils.toPlainText(Message.CONFIG_NOT_FOUND.translate()));
            saveDefaultConfig();
        }
        reloadConfig();
        Translation.updateLocale(getConfigFile().getString("locale"));
        loadWorldGroupList();
        Message.setPrefix(getConfigFile().getString("prefix"));
        getLogger().info(ComponentUtils.toPlainText(Message.CONFIG_LOADED.translate()));
        return true;
    }

    public boolean isConfigOutdated() {
        return getCurrentConfigVersion() != getDefaultConfigVersion();
    }

    public int getCurrentConfigVersion() {
        if (!getConfigFile().isSet("version")) return -1;
        return getConfigFile().getInt("version");
    }

    @SuppressWarnings("DataFlowIssue") // Let it throw NPE. Would only happen if the jar is packed incorrectly.
    public int getDefaultConfigVersion() {
        return getConfigFile().getDefaults().getInt("version");
    }

    @Override
    public void onDisable() {
        this.getPlayerDataManager().save();
    }

    @SuppressWarnings("DataFlowIssue")
    private void loadWorldGroupList() {
        final String worldGroupPath = "interworld-restriction.world-groups";
        if (!getConfigFile().isConfigurationSection(worldGroupPath)) {
            getLogger().warning("Unable to get world groups in the config file! Is it misconfigured?");
            return;
        }
        worldGroups.clear();
        final Set<String> groupKeys = getConfigFile().getConfigurationSection(worldGroupPath).getKeys(false);
        int index = 0;
        for (String groupKey : groupKeys) {
            worldGroups.put(index, getConfigFile().getStringList(worldGroupPath + "." + groupKey));
            index++;
        }
    }

    public int getPlayerWorldGroup(Player player) {
        for (int index : worldGroups.keySet()) {
            List<String> worlds = worldGroups.get(index);
            for (String world : worlds) {
                if (player.getWorld().getName().equalsIgnoreCase(world)) {
                    return index;
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