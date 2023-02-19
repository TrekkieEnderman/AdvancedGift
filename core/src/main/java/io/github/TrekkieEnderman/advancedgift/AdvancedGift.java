package io.github.TrekkieEnderman.advancedgift;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.github.TrekkieEnderman.advancedgift.metrics.GiftCounter;
import io.github.TrekkieEnderman.advancedgift.nms.NMSInterface;
import io.github.TrekkieEnderman.advancedgift.nms.Reflect;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class AdvancedGift extends JavaPlugin {
    private final File configFile = new File(getDataFolder(),"config.yml"), playerInfoFile = new File(getDataFolder(), "playerinfo.json");
    private final FileConfiguration giftBlockData = new YamlConfiguration();
    private final Type uuidSetType = TypeToken.getParameterized(HashSet.class, UUID.class).getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(uuidSetType, new uuidSetJsonAdapter()).create();
    private Set<UUID> togglePlayers = new HashSet<>(), spyPlayers = new HashSet<>();
    private final Map<UUID, Set<UUID>> blockPlayers = new HashMap<>();
    private final HashMap<Integer, ArrayList<String>> worldList = new HashMap<>();
    String prefix, extLib;
    static NMSInterface nms;
    boolean canUseTooltips;
    boolean hasArtMap = false;
    private final GiftCounter giftCounter = new GiftCounter();

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
        final File oldBlockFile = new File(getDataFolder(), "giftblock.yml");
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
        if (!playerInfoFile.exists()) {
            getLogger().info(playerInfoFile.getName() + " not found. Creating a new one.");
            createPlayerInfo();
            getLogger().info("Looking for an older file, " + oldBlockFile.getName() + ".");
            if (oldBlockFile.exists()) {
                getLogger().info(oldBlockFile.getName() + " found. Migrating data from it to " + playerInfoFile.getName() + ".");
                try {
                    giftBlockData.load(oldBlockFile);
                    loadOldBlockList();
                    if (savePlayerInfo()) {
                        getLogger().info("Done. Removing the old file as it's no longer needed.");
                        oldBlockFile.delete();
                    }
                } catch (InvalidConfigurationException | IOException e) {
                    getLogger().log(Level.SEVERE, "Unable to load " + oldBlockFile.getName(), e);
                }
            } else {
                getLogger().info(oldBlockFile.getName() + " not found.");
            }
        }
        loadPlayerInfo();
    }

    private FileConfiguration getBlockData() {
        return this.giftBlockData;
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
        savePlayerInfo();
    }

    private void createPlayerInfo() {
        try (PrintWriter pw = new PrintWriter(playerInfoFile, StandardCharsets.UTF_8.name())) {
            pw.print("{");
            pw.print("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadOldBlockList() {
        if (this.getBlockData().isSet("UUIDs")) {
            togglePlayers = getBlockData().getStringList("UUIDs").stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
        }
    }

    private boolean loadPlayerInfo() {
        try (BufferedReader buffer = Files.newBufferedReader(playerInfoFile.toPath())) {
            //Check what happens if a list being loaded is empty (aka is null). May need to use isJsonNull() before loading it.
            JsonObject object = new JsonParser().parse(new JsonReader(buffer)).getAsJsonObject();
            String name;
            if (object.has(name = "ToggleList")) togglePlayers = gson.fromJson(object.get(name), uuidSetType);
            if (object.has(name = "SpyList")) spyPlayers = gson.fromJson(object.get(name), uuidSetType);
            if (object.has(name = "BlockList")) {
                JsonObject mapJsonObject = object.getAsJsonObject(name);
                for (Map.Entry<String, JsonElement> entry : mapJsonObject.entrySet()) {
                    blockPlayers.put(UUID.fromString(entry.getKey()), gson.fromJson(entry.getValue(), uuidSetType));
                }
            }
        } catch (JsonSyntaxException | JsonIOException | IOException e) {
            getLogger().log(Level.SEVERE, "Unable to load player info", e);
            return false;
        }
        getLogger().log(Level.INFO, "Player info loaded.");
        return true;
    }

    private boolean savePlayerInfo() {
        //Combines all lists into a single json object so gson will convert them to string in one go
        JsonObject object = new JsonObject();
        object.add("ToggleList", gson.toJsonTree(togglePlayers));
        object.add("SpyList", gson.toJsonTree(spyPlayers));
        JsonObject mapJsonObj = new JsonObject();
        for (Map.Entry<UUID, Set<UUID>> entry : blockPlayers.entrySet()) {
            mapJsonObj.add(entry.getKey().toString(), gson.toJsonTree(entry.getValue()));
        }
        object.add("BlockList", mapJsonObj);
        final String json = gson.toJson(object);
        try (BufferedWriter writer = Files.newBufferedWriter(playerInfoFile.toPath())) {
            writer.write(json);
            getLogger().info("Saving playerinfo.json.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to save player info", e);
        }
        return true;
    }

    //Bad. Just bad. Unfortunately this is not something I can easily fix without a major rewrite.
    void addUUID(UUID playerUUID, String var, UUID secondUUID) {
        if (playerUUID == null) return;
        switch (var) {
            case "tg":
                togglePlayers.add(playerUUID);
                break;
            case "spy":
                spyPlayers.add(playerUUID);
                break;
            case "block":
                if (!blockPlayers.containsKey(playerUUID)) {
                    Set<UUID> list = new HashSet<>();
                    list.add(secondUUID);
                    blockPlayers.put(playerUUID, list);
                } else blockPlayers.get(playerUUID).add(secondUUID);
                break;
        }
    }

    //Why did I do this way?
    boolean containsUUID(UUID playerUUID, String var, UUID secondUUID) {
        if (playerUUID == null) return false;
        boolean bool = false;
        switch (var) {
            case "tg":
                bool = togglePlayers.contains(playerUUID);
                break;
            case "spy":
                bool = spyPlayers.contains(playerUUID);
                break;
            case "block":
                if (blockPlayers.containsKey(playerUUID)) {
                    bool = blockPlayers.get(playerUUID).contains(secondUUID);
                }
                break;
        }
        return bool;
    }

    //Seriously, why?
    void removeUUID(UUID playerUUID, String var, UUID secondUUID) {
        if (playerUUID == null) return;
        switch (var) {
            case "tg":
                togglePlayers.remove(playerUUID);
                break;
            case "spy":
                spyPlayers.remove(playerUUID);
                break;
            case "block":
                blockPlayers.get(playerUUID).remove(secondUUID);
                if (blockPlayers.get(playerUUID).isEmpty()) blockPlayers.keySet().remove(playerUUID);
                break;
        }
    }

    Set<UUID> getBlockList(UUID playerUUID) {
        return blockPlayers.get(playerUUID);
    }

    boolean clearBlockList(UUID playerUUID) {
        if (blockPlayers.containsKey(playerUUID)) {
            blockPlayers.keySet().remove(playerUUID);
            return true;
        }
        return false;
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
                        if (!containsUUID(senderUUID, "spy", null)) {
                            addUUID(senderUUID, "spy", null);
                            s.sendMessage(spyEnabled);
                        } else {
                            removeUUID(senderUUID, "spy", null);
                            s.sendMessage(spyDisabled);
                        }
                    } else {
                        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("enable")) {
                            if (!containsUUID(senderUUID, "spy", null)) {
                                addUUID(senderUUID, "spy", null);
                                s.sendMessage(spyEnabled);
                            } else {
                                s.sendMessage(prefix + ChatColor.GRAY + "Gift Spy is already enabled.");
                            }
                        } else if (args[0].equalsIgnoreCase("off") || args [0].equalsIgnoreCase("disable")) {
                            if (containsUUID(senderUUID, "spy", null)) {
                                removeUUID(senderUUID, "spy", null);
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

    //Converts a set of UUIDs to/from json
    static class uuidSetJsonAdapter implements JsonSerializer<Set<UUID>>, JsonDeserializer<Set<UUID>> {
        @Override
        public Set<UUID> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            return StreamSupport.stream(jsonArray.spliterator(), false).map(JsonElement::getAsString).map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
        }

        @Override
        public JsonElement serialize(Set<UUID> uuids, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonArray jsonArray = new JsonArray();
            uuids.stream().map(UUID::toString).forEach(jsonArray::add);
            return jsonArray;
        }
    }
}