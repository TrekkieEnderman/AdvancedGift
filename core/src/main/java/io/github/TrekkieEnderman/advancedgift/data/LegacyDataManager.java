package io.github.TrekkieEnderman.advancedgift.data;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import io.github.TrekkieEnderman.advancedgift.AdvancedGift;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

// Uses the old way to load and save player data for pre 1.12 server versions, since parameterized TypeToken doesn't exist then.
public class LegacyDataManager extends PlayerDataManager {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public LegacyDataManager(AdvancedGift plugin) {
        super(plugin);
        plugin.getLogger().warning("This plugin is now using legacy methods to manage player data. " +
                "The likely reason for this is the server is on an outdated version (<1.12).");
        plugin.getLogger().warning("These methods are not optimized for reading and writing files, " +
                "and so may cause issues during startup and shutdown.");
        plugin.getLogger().warning("You will not receive support for these issues.");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadPlayerInfo() throws JsonSyntaxException, JsonIOException, IOException {
        try (BufferedReader buffer = Files.newBufferedReader(playerInfoFile.toPath())) {
            Map<String, Object> map = gson.fromJson(buffer, HashMap.class);
            String key;
            if (map.containsKey(key = "ToggleList")) {
                togglePlayers = convertToUuidSet((ArrayList<String>) map.get(key));
            }
            if (map.containsKey(key = "SpyList")) {
                spyPlayers = convertToUuidSet((ArrayList<String>) map.get(key));
            }
            if (map.containsKey(key = "BlockList")) {
                blockPlayers = new HashMap<>();
                Map<String, Object> nestedMap = (LinkedTreeMap<String, Object>) map.get(key);
                for (String s : nestedMap.keySet()) {
                    blockPlayers.put(UUID.fromString(s), convertToUuidSet((ArrayList<String>) nestedMap.get(s)));
                }
            }
        } catch (JsonSyntaxException | JsonIOException | IOException e) {
            throw e;
        }
    }

    @Override
    protected void savePlayerInfo() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("ToggleList", togglePlayers);
        map.put("SpyList", spyPlayers);
        map.put("BlockList", blockPlayers);
        final String json = gson.toJson(map);
        try (BufferedWriter writer = Files.newBufferedWriter(playerInfoFile.toPath())) {
            writer.write(json);
        } catch (IOException e) {
            throw e;
        }
    }

    private Set<UUID> convertToUuidSet(List<String> list) {
        return list.stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
    }
}
