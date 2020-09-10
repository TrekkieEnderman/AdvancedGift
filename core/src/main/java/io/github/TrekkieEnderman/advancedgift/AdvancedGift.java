package io.github.TrekkieEnderman.advancedgift;

import io.github.TrekkieEnderman.advancedgift.command.CommandHandler;
import io.github.TrekkieEnderman.advancedgift.nms.NMSInterface;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

public class AdvancedGift extends JavaPlugin {
    private File configFile, playerInfoFile;
    private FileConfiguration giftBlockData, configData;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ArrayList<String> togglePlayers;
    private HashMap<String, ArrayList<String>> blockPlayers = new HashMap<>();
    public ArrayList<String> spyPlayers;
    private HashMap<Integer, ArrayList<String>> worldList = new HashMap<>();
    public String prefix, extLib;
    public static NMSInterface nms;
    public boolean canUseTooltips;
    public boolean isBefore1_9 = false;
    public boolean isBefore1_11 = false;
    public boolean isBefore1_13 = false;
    public boolean hasArtMap = false;

    @Override
    public void onEnable() {
        getLogger().info("===================================================");
        getLogger().info("Checking server version  ------------------");
        String packageName = this.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        getLogger().info("Version: " + version);
        if (version.startsWith("v1_8")) isBefore1_9 = true;
        if (isBefore1_9 || version.startsWith("v1_9") || version.equalsIgnoreCase("v1_10_R1")) isBefore1_11 = true;
        if (isBefore1_11 || version.equalsIgnoreCase("v1_11_R1") || version.equalsIgnoreCase("v1_12_R1")) isBefore1_13 = true;
        try {
            final Class<?> classy = Class.forName("io.github.TrekkieEnderman.advancedgift.nms." + version.toUpperCase());
            if (NMSInterface.class.isAssignableFrom(classy)) {
                nms = (NMSInterface) classy.getConstructor().newInstance();
                getLogger().info("Found NMS support for this server version!");
                getLogger().info("");
                canUseTooltips = true;
            }
        } catch (final Exception e) {
            getLogger().warning("ERROR! NMS support for this server version not found!");
            getLogger().warning("Are you using a newer server version? Check for AdvancedGift updates at:");
            getLogger().warning("www.spigotmc.org/resources/advancedgift.46458/");
            getLogger().warning("In meantime, some features will be disabled for better compatibility.");
            getLogger().warning("However other parts of AdvancedGift may will break!");
            canUseTooltips = false;
            getLogger().info("");
        }

        getLogger().info("Loading files  --------------------");
        loadFiles();
        getLogger().info("");

        getLogger().info("Searching for a material lib  -----------------");
        if (Bukkit.getPluginManager().getPlugin("LangUtils") != null) {
            getLogger().info("Language Utils found. Using it.");
            extLib = "LangUtils";
        } else {
            getLogger().info("No supported material lib found.");
            getLogger().info("Using Spigot's material enum instead.");
            extLib = "none";
        }
        CommandHandler commandHandler = new CommandHandler(this);
        this.getCommand("gift").setExecutor(commandHandler);
        this.getCommand("togglegift").setExecutor(commandHandler);
        this.getCommand("giftblock").setExecutor(commandHandler);
        this.getCommand("giftblocklist").setExecutor(commandHandler);
        this.getCommand("agreload").setExecutor(commandHandler);
        this.getCommand("giftspy").setExecutor(commandHandler);
        //todo register tab completer for the commands
        getLogger().info("===================================================");
        if (Bukkit.getPluginManager().getPlugin("ArtMap") != null) hasArtMap = true;
    }

    private void loadFiles() {
        try {
            if(!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            configFile = new File(getDataFolder(), "config.yml");
            File giftBlockFile = new File(getDataFolder(), "giftblock.yml");
            playerInfoFile = new File(getDataFolder(), "playerinfo.json");
            configData = new YamlConfiguration();
            giftBlockData = new YamlConfiguration();
            if (!configFile.exists()) {
                getLogger().info("config.yml not found. Creating a new one.");
                saveDefaultConfig();
                loadConfig();
            } else {
                getLogger().info("config.yml found.");
                loadConfig();
                getLogger().info("Verifying that config.yml is up to date.");
                if (!this.getConfigFile().isSet("restrict-interworld-gift")) {
                    getLogger().warning("Outdated config.yml! Some newer options are missing!");
                    configFile.renameTo(new File(getDataFolder(), "outdated_config.yml"));
                    saveDefaultConfig();
                    loadConfig();
                    getLogger().info("Regenerating the default config.yml.");
                    getLogger().info("The old config has been renamed to \"outdated_config.yml\".");
                    getLogger().info("The data from outdated_config.yml can be copied and pasted to the new config.yml.");
                    getLogger().info("Make sure not to overwrite the new options added in this update.");
                    getLogger().info("");
                } else {
                    getLogger().info("config.yml is up to date.");
                }
            }
            if (!playerInfoFile.exists()) {
                getLogger().info("playerinfo.json not found. Looking for older file, giftblock.yml.");
                if (giftBlockFile.exists()) {
                    getLogger().info("giftblock.yml found. Initiating data migration!");
                    giftBlockData.load(giftBlockFile);
                    loadBlockList();
                    getLogger().info("Creating playerinfo.json...");
                    createPlayerInfo();
                    getLogger().info("Done. Transferring data from giftblock.yml to playerinfo.json...");
                    savePlayerInfo();
                    getLogger().info("Done. Removing giftblock.yml as it's no longer used or needed.");
                    giftBlockFile.delete();
                } else {
                    getLogger().info("giftblock.yml not found.");
                    getLogger().info("Creating playerinfo.json.");
                    createPlayerInfo();
                }

            } else {
                getLogger().info("playerinfo.json found.");
            }
            loadPlayerInfo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileConfiguration getBlockData() {
        return this.giftBlockData;
    }

    public FileConfiguration getConfigFile() {
        return this.configData;
    }

    @Override
    public void onDisable() {
        savePlayerInfo();
        getLogger().info("Saving playerinfo.yml.");
    }

    private void createPlayerInfo() {
        PrintWriter pw;
        try {
            pw = new PrintWriter(playerInfoFile, "UTF-8");
            pw.print("{");
            pw.print("}");
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBlockList() {
        if (!this.getBlockData().isSet("UUIDs")) {
            togglePlayers = new ArrayList<>();
        } else {
            togglePlayers = (ArrayList<String>)getBlockData().getStringList("UUIDs");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlayerInfo() {
        try {
            Map<String, Object> map = gson.fromJson(new FileReader(playerInfoFile), HashMap.class); //new HashMap<String, Object>().getClass()
            if (!map.containsKey("ToggleList")) {
                togglePlayers = new ArrayList<>();
            } else {
                togglePlayers = (ArrayList<String>)map.get("ToggleList");
            }
            if (!map.containsKey("SpyList")) {
                spyPlayers = new ArrayList<>();
            } else {
                spyPlayers = (ArrayList<String>)map.get("SpyList");
            }
            if (!map.containsKey("BlockList")) {
                blockPlayers = new HashMap<>();
            } else {
                LinkedTreeMap<String, Object> blockMap = (LinkedTreeMap<String, Object>)map.get("BlockList");
                for (String uuid : blockMap.keySet()) {
                    ArrayList<String> blockList = (ArrayList<String>)blockMap.get(uuid);
                    blockPlayers.put(uuid, blockList);
                }

            }
        } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void savePlayerInfo() {
        Map<String, Object> map = new HashMap<>();
        map.put("ToggleList", togglePlayers);
        map.put("SpyList", spyPlayers);
        map.put("BlockList", blockPlayers);
        final String json = gson.toJson(map);
        try {
            FileWriter fw = new FileWriter(playerInfoFile);
            fw.write(json);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUUID(UUID playerUUID, String var, String secondUUID) {
        String uuid = playerUUID.toString();
        switch (var) {
            case "tg":
                togglePlayers.add(uuid);
                break;
            case "spy":
                spyPlayers.add(uuid);
                break;
            case "block":
                if (!blockPlayers.containsKey(uuid)) {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(secondUUID);
                    blockPlayers.put(uuid, list);
                } else blockPlayers.get(uuid).add(secondUUID);
                break;
        }
    }

    public boolean containsUUID(UUID playerUUID, String var, String secondUUID) {
        boolean bool = false;
        String uuid = playerUUID.toString();
        switch (var) {
            case "tg":
                bool = togglePlayers.contains(uuid);
                break;
            case "spy":
                bool = spyPlayers.contains(uuid);
                break;
            case "block":
                if (blockPlayers.containsKey(uuid)) {
                    bool = blockPlayers.get(uuid).contains(secondUUID);
                }
                break;
        }
        return bool;
    }

    public void removeUUID(UUID playerUUID, String var, String secondUUID) {
        String uuid = playerUUID.toString();
        switch (var) {
            case "tg":
                togglePlayers.remove(uuid);
                break;
            case "spy":
                spyPlayers.remove(uuid);
                break;
            case "block":
                blockPlayers.get(uuid).remove(secondUUID);
                if (blockPlayers.get(uuid).isEmpty()) blockPlayers.keySet().remove(uuid);
                break;
        }
    }

    public String getBlockList(UUID playerUUID) {
        ArrayList<String> blockList = blockPlayers.get(playerUUID.toString());
        String playerBlockList = "";
        if (blockList != null) {
            for (String uuidString : blockList) {
                String player = Bukkit.getOfflinePlayer(UUID.fromString(uuidString)).getName();
                playerBlockList = (playerBlockList.isEmpty() ? "" : playerBlockList + " ") + player;
            }
        }
        return playerBlockList;
    }

    public boolean clearBlockList(UUID playerUUID) {
        String uuid = playerUUID.toString();
        if (blockPlayers.containsKey(uuid)) {
            blockPlayers.keySet().remove(uuid);
            return true;
        }
        return false;
    }

    private void loadWorldGroupList() {
        int key = 1;
        for (String world : getConfigFile().getStringList("world-group-list")) {
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

    public boolean loadConfig() {
        try {
            configData.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return false;
        }
        loadWorldGroupList();
        prefix = ChatColor.translateAlternateColorCodes('&', this.getConfigFile().getString("prefix") + " ");
        return true;
    }
}