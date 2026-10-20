package salve.core.research;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.StringReader;

/** Small, strict data protocol: no markdown, duplicate keys, trailing text or executable actions. */
final class ResearchJson {
    private ResearchJson() { }
    static JsonObject object(String text) {
        if (text == null || text.length() > 6000) throw new IllegalArgumentException("JSON demasiado largo");
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = read(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT || !value.isJsonObject()) throw new IllegalArgumentException();
            return value.getAsJsonObject();
        } catch (Exception invalid) { throw new IllegalArgumentException("JSON de investigación inválido", invalid); }
    }
    private static JsonElement read(JsonReader reader, int depth) throws java.io.IOException {
        if (depth > 5) throw new IllegalArgumentException();
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (object.has(key) || object.size() >= 12) throw new IllegalArgumentException();
                    object.add(key, read(reader, depth + 1));
                }
                reader.endObject(); return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) {
                    if (array.size() >= 8) throw new IllegalArgumentException();
                    array.add(read(reader, depth + 1));
                }
                reader.endArray(); return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            default: throw new IllegalArgumentException();
        }
    }
    static String string(JsonObject object, String key, int max) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
        String text = value.getAsString().trim();
        if (text.length() > max) throw new IllegalArgumentException();
        return text;
    }
    static boolean bool(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException();
        return value.getAsBoolean();
    }
}
