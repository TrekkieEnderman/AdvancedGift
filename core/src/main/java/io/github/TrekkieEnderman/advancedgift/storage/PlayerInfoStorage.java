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

package io.github.TrekkieEnderman.advancedgift.storage;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.player.PlayerData;
import io.github.TrekkieEnderman.advancedgift.storage.loader.ConfigurateLoader;
import io.github.TrekkieEnderman.advancedgift.storage.loader.JsonLoader;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads and saves data from/to the "playerinfo.json" file.
 */
public class PlayerInfoStorage implements PlayerDataLoader {
    private final AdvancedGift plugin;
    private final Path path;
    private final ConfigurateLoader loader;
    private Set<UUID> togglePlayers = new HashSet<>();
    private Set<UUID> spyPlayers = new HashSet<>();
    private Map<UUID, Set<UUID>> blockPlayers = new HashMap<>();

    public PlayerInfoStorage(AdvancedGift plugin) {
        this.plugin = plugin;
        path = plugin.getDataPath().resolve("playerinfo.json");
        loader = new JsonLoader();
    }

    private ConfigurationNode loadFile() throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return loader.getLoader(path).load();
    }

    private void saveFile(ConfigurationNode data) throws IOException {
        loader.getLoader(path).save(data);
    }

    @Override
    public void init() {
        try {
            ConfigurationNode data = loadFile();
            if (data == null) {
                return;
            }
            List<UUID> list = data.node("ToggleList").getList(UUID.class);
            togglePlayers = list == null ? new HashSet<>() : new HashSet<>(list);
            list = data.node("SpyList").getList(UUID.class);
            spyPlayers = list == null ? new HashSet<>() : new HashSet<>(list);
            Map<UUID, Set<UUID>> map = new HashMap<>();
            ConfigurationNode blockNode = data.node("BlockList");
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : blockNode.childrenMap().entrySet()) {
                UUID blocker = UUID.fromString((String) entry.getKey());
                ConfigurationNode node = entry.getValue();
                list = node.getList(UUID.class);
                Set<UUID> blocked = list == null ? new HashSet<>() : new HashSet<>(list);;
                map.put(blocker, blocked);
            }
            blockPlayers = map;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load data", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            ConfigurationNode data = BasicConfigurationNode.root();
            data.node("ToggleList").set(togglePlayers);
            data.node("SpyList").set(spyPlayers);
            data.node("BlockList").set(blockPlayers);
            saveFile(data);
            togglePlayers.clear();
            spyPlayers.clear();
            blockPlayers.clear();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to save data", e);
        }
    }

    @Override
    public PlayerData loadPlayer(UUID uuid) {
        return new PlayerData(!togglePlayers.contains(uuid),
                spyPlayers.contains(uuid),
                blockPlayers.getOrDefault(uuid, new HashSet<>()));
    }

    @Override
    public void savePlayer(UUID uuid, PlayerData data) {
        if (data.isGiftEnabled()) {
            togglePlayers.remove(uuid);
        } else {
            togglePlayers.add(uuid);
        }

        if (data.isSpy()) {
            spyPlayers.add(uuid);
        } else {
            spyPlayers.remove(uuid);
        }

        if (data.getBlockList().isEmpty()) {
            blockPlayers.remove(uuid);
        } else {
            blockPlayers.put(uuid, data.getBlockList());
        }
    }
}
