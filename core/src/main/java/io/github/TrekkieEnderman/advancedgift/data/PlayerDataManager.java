package io.github.TrekkieEnderman.advancedgift.data;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class PlayerDataManager {

    protected final AdvancedGift plugin;
    protected final File playerInfoFile;
    protected Set<UUID> togglePlayers = new HashSet<>(), spyPlayers = new HashSet<>();
    protected Map<UUID, Set<UUID>> blockPlayers = new HashMap<>();

    public PlayerDataManager(AdvancedGift plugin) {
        this.plugin = plugin;
        playerInfoFile = new File(plugin.getDataFolder(), "playerinfo.json");
    }

    public final void load() {
        if (!playerInfoFile.exists()) {
            plugin.getLogger().info(playerInfoFile.getName() + " not found. Creating a new one.");
            createPlayerInfo();
            findOldBlockList();
        }
        try {
            loadPlayerInfo();
            plugin.getLogger().info(playerInfoFile.getName() + " loaded.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load " + playerInfoFile.getName(), e);
        }
    }

    public final void save() {
        try {
            savePlayerInfo();
            plugin.getLogger().info("Saving " + playerInfoFile.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to save " + playerInfoFile.getName(), e);
        }
    }

    // There probably is a nicer way to do this, but this works for what I need
    private void findOldBlockList() {
        final File oldBlockFile = new File(plugin.getDataFolder(), "giftblock.yml");
        plugin.getLogger().info("Looking for an older file, " + oldBlockFile.getName() + ".");
        if (oldBlockFile.exists()) {
            plugin.getLogger().info(oldBlockFile.getName() + " found. Migrating data from it to " + playerInfoFile.getName() + ".");
            try {
                loadOldBlockList(oldBlockFile);
                try {
                    savePlayerInfo();
                    plugin.getLogger().info("Done. Removing the old file as it's no longer needed.");
                    oldBlockFile.delete();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Unable to write to " + playerInfoFile.getName() + ". The old file will remain for a retry next time.", e);
                }
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getLogger().log(Level.SEVERE, "Unable to load " + oldBlockFile.getName(), e);
            }
        } else {
            plugin.getLogger().info(oldBlockFile.getName() + " not found.");
        }
    }

    private void loadOldBlockList(File file) throws IOException, InvalidConfigurationException {
        final FileConfiguration giftBlockData = new YamlConfiguration();
        giftBlockData.load(file);
        if (giftBlockData.isSet("UUIDs")) {
            togglePlayers = giftBlockData.getStringList("UUIDs").stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
        }
    }

    private void createPlayerInfo() {
        try (PrintWriter pw = new PrintWriter(playerInfoFile, StandardCharsets.UTF_8.name())) {
            pw.print("{");
            pw.print("}");
        } catch (IOException ignored) {
        }
    }

    protected abstract void loadPlayerInfo() throws JsonSyntaxException, JsonIOException, IOException;

    protected abstract void savePlayerInfo() throws IOException;

    //Bad. Just bad. Unfortunately this is not something I can easily fix without a major rewrite.
    public final void addUUID(UUID playerUUID, String var, UUID secondUUID) {
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
    public final boolean containsUUID(UUID playerUUID, String var, UUID secondUUID) {
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
    public final void removeUUID(UUID playerUUID, String var, UUID secondUUID) {
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

    public Set<UUID> getBlockList(UUID playerUUID) {
        return blockPlayers.get(playerUUID);
    }

    public boolean clearBlockList(UUID playerUUID) {
        if (blockPlayers.containsKey(playerUUID)) {
            blockPlayers.keySet().remove(playerUUID);
            return true;
        }
        return false;
    }
}
