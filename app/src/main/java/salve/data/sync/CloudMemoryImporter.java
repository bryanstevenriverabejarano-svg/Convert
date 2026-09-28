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

    public boolean ingest(String payload, long fallbackTime) { return ingest(payload, fallbackTime, true); }

    public boolean ingest(String payload, long fallbackTime, boolean fromCloud) {
        JsonElement root = JsonParser.parseString(payload);
        if (!root.isJsonObject()) throw new IllegalArgumentException("Evento inválido");
        JsonObject event = root.getAsJsonObject();
        String type = string(event, "type", "");
        long time = number(event, "time_ms", fallbackTime);
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
        boolean legacy = type.equals("memoria_manual") || type.equals("memoria_auto");
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
        record.intensidad = (int) Math.max(0, Math.min(10, number(event, "intensity", 5)));
        // Validate every data field before cleanup, so malformed legacy input cannot
        // partially mutate a journal-repair transaction that skips invalid entries.
        if (!category.isEmpty()) requireCategory(category);
        if (legacy && memories.canonicalNear(time, text) > 0) return false;
        if (type.equals("memory")) memories.deleteLegacyCopies(time, text);
        tags.add(fromCloud ? "pcloud" : "diario_local"); tags.add(type);
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
        if (!event.has("tags") || event.get("tags").isJsonNull()) return result;
        JsonElement raw = event.get("tags");
        if (raw.isJsonPrimitive()) raw = JsonParser.parseString(raw.getAsString());
        if (!raw.isJsonArray()) throw new IllegalArgumentException("Etiquetas inválidas");
        for (JsonElement item : raw.getAsJsonArray()) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException("Etiqueta inválida");
            String tag = item.getAsString();
            if (tag.length() <= 120 && result.size() < 50) result.add(tag);
        }
        return result;
    }
    private static String string(JsonObject event, String key, String fallback) {
        if (!event.has(key) || event.get(key).isJsonNull()) return fallback;
        if (!event.get(key).isJsonPrimitive()) throw new IllegalArgumentException("Campo de texto inválido");
        return event.get(key).getAsString();
    }
    private static long number(JsonObject event, String key, long fallback) {
        if (!event.has(key)) return fallback;
        try { return event.get(key).getAsBigDecimal().longValueExact(); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("Campo numérico inválido", invalid); }
    }
    private static void requireCategory(String category) {
        if (!category.matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Categoría inválida");
    }
}
