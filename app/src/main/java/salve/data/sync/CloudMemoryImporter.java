package salve.data.sync;

import com.google.gson.*;
import java.util.*;
import salve.core.memory.MemoryProfileFact;
import salve.data.db.*;

/** Projects journal data into the same evidence store used by every conversation model.
 * Call inside a database transaction. No model calls, re-enqueueing or generated facts. */
public final class CloudMemoryImporter {
    private final RecuerdoDao memories;
    private final MemorySyncStateDao states;
    public CloudMemoryImporter(RecuerdoDao memories, MemorySyncStateDao states) {
        this.memories = memories; this.states = states;
    }

    public boolean ingest(String payload, long fallbackTime) {
        JsonObject event = JsonParser.parseString(payload).getAsJsonObject();
        String type = string(event, "type", "");
        long time = event.has("time_ms") ? event.get("time_ms").getAsLong() : fallbackTime;
        if (time <= 0 || time > System.currentTimeMillis() + 86400000L)
            throw new IllegalArgumentException("Fecha del recuerdo inválida");
        String text = string(event, "content", "").trim();
        String category = string(event, "category", "");
        if (type.equals("profile_delete")) {
            requireCategory(category);
            return profile(category, time, null);
        }
        boolean user = type.equals("user_message") || type.equals("voice_message");
        boolean memory = type.equals("memory") || type.equals("memoria_manual") || type.equals("memoria_auto");
        if (!user && !memory && !type.equals("profile")) return false;
        if (text.isEmpty() || text.length() > 64000) throw new IllegalArgumentException("Texto del recuerdo inválido");
        List<String> tags = tags(event);
        if (type.equals("profile")) requireCategory(category);
        else {
            // Older versions sent complete profile text in user_message, but only the category
            // in memoria_perfil. Never interpret the latter as a person's actual name.
            MemoryProfileFact fact = (user || tags.contains("hecho_usuario")) ? MemoryProfileFact.parse(text) : null;
            if (fact != null) category = fact.getCategory();
            for (String tag : tags) if (tag.startsWith("profile:")) category = tag.substring(8);
        }
        RecuerdoEntity record = new RecuerdoEntity();
        record.frase = text; record.timestamp = time; record.binario = "";
        record.emocion = string(event, "emotion", "neutral");
        record.intensidad = event.has("intensity") ? Math.max(0, Math.min(10, event.get("intensity").getAsInt())) : 5;
        tags.add("pcloud"); tags.add(type);
        if (!category.isEmpty()) {
            requireCategory(category);
            tags.add("profile:" + category); tags.add("hecho_usuario");
            record.etiquetas = new Gson().toJson(new LinkedHashSet<>(tags));
            return profile(category, time, record);
        }
        record.etiquetas = new Gson().toJson(new LinkedHashSet<>(tags));
        if (memories.countExact(time, text) > 0) return false;
        memories.insertRecuerdo(record);
        return true;
    }

    private boolean profile(String category, long time, RecuerdoEntity record) {
        String key = "profile:" + category;
        MemorySyncStateEntity state = states.get(key);
        RecuerdoEntity current = memories.ultimoPorEtiqueta(key);
        long latest = Math.max(state == null ? 0 : state.updatedAt, current == null ? 0 : current.timestamp);
        // A deletion wins a timestamp tie; old cloud snapshots cannot resurrect a forgotten fact.
        if (time < latest || (time == latest && (record != null || (state != null && state.deleted)))) return false;
        if (record == null) memories.eliminarPorEtiqueta(key);
        else memories.reemplazarPorEtiqueta(key, record);
        states.put(MemorySyncStateEntity.of(key, time, record == null));
        return true;
    }

    public static String serialize(RecuerdoEntity record) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "memory"); event.addProperty("content", record.frase);
        event.addProperty("emotion", record.emocion); event.addProperty("intensity", record.intensidad);
        event.addProperty("tags", record.etiquetas == null ? "[]" : record.etiquetas);
        event.addProperty("time_ms", record.timestamp);
        return event.toString();
    }
    private static List<String> tags(JsonObject event) {
        List<String> result = new ArrayList<>();
        if (!event.has("tags")) return result;
        JsonElement raw = event.get("tags");
        if (raw.isJsonPrimitive()) raw = JsonParser.parseString(raw.getAsString());
        for (JsonElement item : raw.getAsJsonArray()) {
            String tag = item.getAsString();
            if (tag.length() <= 120 && result.size() < 50) result.add(tag);
        }
        return result;
    }
    private static String string(JsonObject event, String key, String fallback) {
        return event.has(key) && !event.get(key).isJsonNull() ? event.get(key).getAsString() : fallback;
    }
    private static void requireCategory(String category) {
        if (!category.matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Categoría inválida");
    }
}
