/*
 * Copyright (c) 2026 TrekkieEnderman
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

package io.github.TrekkieEnderman.advancedgift.player;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
    protected Set<UUID> togglePlayers = new HashSet<>();
    protected Set<UUID> spyPlayers = new HashSet<>();
    protected Map<UUID, Set<UUID>> blockPlayers = new HashMap<>();

    public PlayerDataManager(final AdvancedGift plugin) {
        this.plugin = plugin;
        playerInfoFile = new File(plugin.getDataFolder(), "playerinfo.json");
    }

    public PlayerData getData(Player pLayer) {
        return getData(pLayer.getUniqueId());
    }

    public PlayerData getData(UUID uuid) {
        return new PlayerData(this, uuid);
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
                    //noinspection ResultOfMethodCallIgnored
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

    private void loadOldBlockList(final File file) throws IOException, InvalidConfigurationException {
        final FileConfiguration giftBlockData = new YamlConfiguration();
        giftBlockData.load(file);
        if (giftBlockData.isSet("UUIDs")) {
            togglePlayers = giftBlockData.getStringList("UUIDs").stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
        }
    }

    private void createPlayerInfo() {
        try (PrintWriter pw = new PrintWriter(playerInfoFile, StandardCharsets.UTF_8)) {
            pw.print("{");
            pw.print("}");
        } catch (IOException ignored) {
        }
    }

    protected abstract void loadPlayerInfo() throws JsonSyntaxException, JsonIOException, IOException;

    protected abstract void savePlayerInfo() throws IOException;

    public boolean isSpy(final UUID uuid) {
        return spyPlayers.contains(uuid);
    }

    public void setSpy(final UUID uuid, boolean enabled) {
        if (enabled) {
            spyPlayers.add(uuid);
        } else {
            spyPlayers.remove(uuid);
        }
    }

    public boolean isGiftDisabled(final UUID uuid) {
        return togglePlayers.contains(uuid);
    }

    public void setGiftDisabled(final UUID uuid, boolean disabled) {
        if (disabled) {
            togglePlayers.add(uuid);
        } else {
            togglePlayers.remove(uuid);
        }
    }

    public boolean hasPlayerBlocked(final UUID blocker, final UUID blocked) {
        if (!blockPlayers.containsKey(blocker)) return false;
        return blockPlayers.get(blocker).contains(blocked);
    }

    public boolean blockPlayer(final UUID blocker, final UUID blocked) {
        Set<UUID> set = blockPlayers.computeIfAbsent(blocker, uuid -> new HashSet<>());
        return set.add(blocked);
    }

    public boolean unblockPlayer(final UUID blocker, final UUID blocked) {
        if (!blockPlayers.containsKey(blocker)) return false;
        Set<UUID> set = blockPlayers.get(blocker);
        boolean result = set.remove(blocked);
        if (set.isEmpty()) {
            blockPlayers.remove(blocker);
        }
        return result;
    }

    public Set<UUID> getBlockList(final UUID playerUUID) {
        return blockPlayers.get(playerUUID);
    }

    public boolean clearBlockList(final UUID playerUUID) {
        if (blockPlayers.containsKey(playerUUID)) {
            blockPlayers.remove(playerUUID);
            return true;
        }
        return false;
    }
}
