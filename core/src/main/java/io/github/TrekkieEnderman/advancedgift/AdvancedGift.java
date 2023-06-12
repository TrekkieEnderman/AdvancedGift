package io.github.TrekkieEnderman.advancedgift;

import io.github.TrekkieEnderman.advancedgift.data.LegacyDataManager;
import io.github.TrekkieEnderman.advancedgift.data.PlayerDataManager;
import io.github.TrekkieEnderman.advancedgift.data.StandardDataManager;
import io.github.TrekkieEnderman.advancedgift.metrics.GiftCounter;
import io.github.TrekkieEnderman.advancedgift.nms.NMSInterface;
import io.github.TrekkieEnderman.advancedgift.nms.Reflect;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class AdvancedGift extends JavaPlugin {
    private final File configFile = new File(getDataFolder(),"config.yml");
    private final HashMap<Integer, ArrayList<String>> worldList = new HashMap<>();
    String prefix, extLib;
    static NMSInterface nms;
    boolean canUseTooltips;
    boolean hasArtMap = false;
    private final GiftCounter giftCounter = new GiftCounter();
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        ServerVersion.init();
        getLogger().info("===================================================");
        getLogger().info("Checking server version  ------------------");
        getLogger().info("NMS Version used: " + ServerVersion.getNMSVersion());
        getLogger().info("");
        try {
            final Class<?> classy = Class.forName("io.github.TrekkieEnderman.advancedgift.nms." + ServerVersion.getNMSVersion().toUpperCase());
            if (NMSInterface.class.isAssignableFrom(classy)) {
                nms = (NMSInterface) classy.getConstructor().newInstance();
            }
        } catch (final Exception ignored) {
            //Attempt to use the reflection class only if the server is newer than 1.19.
            if (ServerVersion.getMinorVersion() > 19) {
                try {
                    nms = new Reflect(ServerVersion.getNMSVersion());
                } catch (Throwable ex) {
                    getLogger().log(Level.WARNING, "Couldn't set up reflection for item text hover over", ex);
                    nms = null;
                }
            }
        }
        if (nms != null) {
            getLogger().info("This version is supported!");
            getLogger().info("");
            canUseTooltips = true;
        } else {
            getLogger().warning("Warning!");
            getLogger().warning("This plugin doesn't have support for this version!");
            getLogger().warning("In order to maintain compatibility with this server,");
            getLogger().warning("Item text hoverover has been disabled in this plugin!");
            getLogger().warning("Check for updates at www.spigotmc.org/resources/advancedgift.46458/");
            getLogger().warning("");
            getLogger().warning("If this plugin still breaks, please contact TrekkieEnderman immediately.");
            canUseTooltips = false;
            getLogger().info("");
        }

        getLogger().info("Loading files  --------------------");
        loadFiles();
        getLogger().info("");

        getLogger().info("Searching for a material library  -----------------");
        if (Bukkit.getPluginManager().getPlugin("LangUtils") != null) {
            getLogger().info("Language Utils found. This library will be used.");
            extLib = "LangUtils";
        } else {
            getLogger().info("No supported material library found.");
            getLogger().info("Spigot's material enum will be used instead.");
            extLib = "none"; //If you're wondering why this isn't left null instead, I don't know!
        }
        this.getCommand("gift").setExecutor(new CommandGift(this));
        this.getCommand("togglegift").setExecutor(new CommandGiftToggle(this));
        CommandGiftBlock commandGiftBlock = new CommandGiftBlock(this);
        this.getCommand("giftblock").setExecutor(commandGiftBlock);
        this.getCommand("giftblocklist").setExecutor(commandGiftBlock);
        getLogger().info("===================================================");
        if (Bukkit.getPluginManager().getPlugin("ArtMap") != null) hasArtMap = true;
        startMetrics();
    }

    private void loadFiles() {
        if(!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        if (!configFile.exists()) {
            getLogger().info("Config not found. Creating a new one.");
            saveDefaultConfig();
            loadConfigFile();
        } else {
            loadConfigFile();
            if (!this.getConfig().isSet("restrict-interworld-gift")) {
                getLogger().warning("Outdated config! Some newer options are missing!");
                configFile.renameTo(new File(getDataFolder(), "outdated_config.yml"));
                saveDefaultConfig();
                loadConfigFile();
                getLogger().info("Regenerating the default config.");
                getLogger().info("The old config has been renamed to \"outdated_config.yml\".");
                getLogger().info("The data from outdated_config.yml can be copied and pasted to the new config.yml.");
                getLogger().info("Make sure not to overwrite the new options added in this update.");
                getLogger().info("");
            }
        }
        if (ServerVersion.getMinorVersion() > 11) playerDataManager = new StandardDataManager(this);
        else playerDataManager = new LegacyDataManager(this);
        this.getPlayerDataManager().load();
    }

    FileConfiguration getConfigFile() {
        return getConfig();
    }

    private boolean loadConfigFile() {
        getLogger().log(Level.INFO, "Config loaded.");
        reloadConfig();
        loadWorldGroupList();
        prefix = ChatColor.translateAlternateColorCodes('&', this.getConfigFile().getString("prefix") + " ");
        return true;
    }

    @Override
    public void onDisable() {
        this.getPlayerDataManager().save();
    }

    private void loadWorldGroupList() {
        int key = 1;
        for (String world : getConfig().getStringList("world-group-list")) {
            String[] array = world.split(", ");
            ArrayList<String> list = new ArrayList<>(Arrays.asList(array));
            worldList.put(key, list);
            key += 1; //Previously "key = key + 1". If world restriction breaks, revert to this.
        }
    }

    int getPlayerWorldGroup(Player player) {
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

    public GiftCounter getGiftCounter() {
        return giftCounter;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("agreload")) {
            if (!(sender instanceof Player)) {
                if (loadConfigFile()) getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Reloaded the config.");
                else getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "Failed to reload the config.");
            } else {
                if (sender.hasPermission("advancedgift.reload")) {
                    if (loadConfigFile()) sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded the config.");
                    else sender.sendMessage(prefix + ChatColor.RED + "Failed to reload the config. Check the console for errors.");
                } else sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this command!");
            }
        }
        if (cmd.getName().equalsIgnoreCase("giftspy")) {
            if (!(sender instanceof Player)) sender.sendMessage("This command can only be run by a player.");
            else {
                if (sender.hasPermission("advancedgift.gift.spy")) {
                    String usage = ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/giftspy " + ChatColor.GRAY + "<on/off>";
                    Player s = (Player) sender;
                    UUID senderUUID = s.getUniqueId();
                    String spyEnabled = prefix + ChatColor.GREEN + "Gift Spy enabled.";
                    String spyDisabled = prefix + ChatColor.RED + "Gift Spy disabled.";
                    if (args.length == 0) {
                        if (!getPlayerDataManager().containsUUID(senderUUID, "spy", null)) {
                            getPlayerDataManager().addUUID(senderUUID, "spy", null);
                            s.sendMessage(spyEnabled);
                        } else {
                            getPlayerDataManager().removeUUID(senderUUID, "spy", null);
                            s.sendMessage(spyDisabled);
                        }
                    } else {
                        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("enable")) {
                            if (!getPlayerDataManager().containsUUID(senderUUID, "spy", null)) {
                                getPlayerDataManager().addUUID(senderUUID, "spy", null);
                                s.sendMessage(spyEnabled);
                            } else {
                                s.sendMessage(prefix + ChatColor.GRAY + "Gift Spy is already enabled.");
                            }
                        } else if (args[0].equalsIgnoreCase("off") || args [0].equalsIgnoreCase("disable")) {
                            if (getPlayerDataManager().containsUUID(senderUUID, "spy", null)) {
                                getPlayerDataManager().removeUUID(senderUUID, "spy", null);
                                s.sendMessage(spyDisabled);
                            } else {
                                s.sendMessage(prefix + ChatColor.GRAY + "Gift Spy is already disabled.");
                            }
                        } else {
                            s.sendMessage(prefix + ChatColor.RED + "Cannot understand " + args[0] + "!");
                            s.sendMessage(usage);
                        }
                    }
                } else {
                    sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this!");
                }
            }
        }
        return true;
    }
}