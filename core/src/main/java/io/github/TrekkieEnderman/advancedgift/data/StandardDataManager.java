package io.github.TrekkieEnderman.advancedgift.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.github.TrekkieEnderman.advancedgift.AdvancedGift;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StandardDataManager extends PlayerDataManager {

    private final Type uuidSetType = TypeToken.getParameterized(HashSet.class, UUID.class).getType(); // Issue here, method exists only since 1.12
    private final Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(uuidSetType, new uuidSetJsonAdapter()).create();

    public StandardDataManager(final AdvancedGift plugin) {
        super(plugin);
    }

    @Override
    protected void loadPlayerInfo() throws JsonSyntaxException, JsonIOException, IOException {
        // Still using try/catch so the BufferReader will still get closed regardless of whether the attempt succeeds or not
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
            throw e;
        }
    }

    @Override
    protected void savePlayerInfo() throws IOException {
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
        } catch (IOException e) {
            throw e;
        }
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
