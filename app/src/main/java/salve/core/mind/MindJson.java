package salve.core.mind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura acotada de la salida JSON de un modelo local.
 *
 * <p>Los modelos pequeños suelen envolver el objeto en Markdown o añadir una frase.
 * Se extrae únicamente el primer objeto equilibrado; cualquier campo inesperado
 * se ignora y los obligatorios se validan con límites de longitud.</p>
 */
final class MindJson {
    private MindJson() {}

    static JsonObject firstObject(String raw, int maxChars) {
        if (raw == null) throw new IllegalArgumentException("Salida vacía.");
        int start = raw.indexOf('{');
        if (start < 0) throw new IllegalArgumentException("La salida no contiene JSON.");
        int depth = 0;
        boolean inString = false, escaped = false;
        for (int i = start; i < raw.length(); i++) {
            if (i - start > maxChars) throw new IllegalArgumentException("JSON demasiado grande.");
            char c = raw.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) {
                JsonElement parsed = JsonParser.parseString(raw.substring(start, i + 1));
                if (!parsed.isJsonObject()) throw new IllegalArgumentException("Se esperaba un objeto.");
                return parsed.getAsJsonObject();
            }
        }
        throw new IllegalArgumentException("JSON incompleto.");
    }

    static String text(JsonObject object, String key, int max, boolean required) {
        JsonElement value = object == null ? null : object.get(key);
        if (value == null || value.isJsonNull()) {
            if (required) throw new IllegalArgumentException("Falta " + key + ".");
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException(key + " debe ser texto.");
        String text = clean(value.getAsString());
        if (text.isEmpty()) {
            if (required) throw new IllegalArgumentException(key + " vacío.");
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max).trim();
    }

    static double number(JsonObject object, String key, double fallback, double min, double max) {
        JsonElement value = object == null ? null : object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return fallback;
        double number = value.getAsDouble();
        if (Double.isNaN(number)) return fallback;
        return Math.max(min, Math.min(max, number));
    }

    static List<JsonObject> objects(JsonObject object, String key, int max) {
        List<JsonObject> out = new ArrayList<>();
        JsonElement value = object == null ? null : object.get(key);
        if (value == null || !value.isJsonArray()) return out;
        for (JsonElement item : value.getAsJsonArray()) {
            if (out.size() >= max) break;
            if (item != null && item.isJsonObject()) out.add(item.getAsJsonObject());
        }
        return out;
    }

    static List<String> strings(JsonObject object, String key, int maxItems, int maxChars) {
        List<String> out = new ArrayList<>();
        JsonElement value = object == null ? null : object.get(key);
        if (value == null || !value.isJsonArray()) return out;
        JsonArray array = value.getAsJsonArray();
        for (JsonElement item : array) {
            if (out.size() >= maxItems) break;
            if (item == null || !item.isJsonPrimitive()) continue;
            String text = clean(item.getAsString());
            if (!text.isEmpty() && text.length() <= maxChars && !out.contains(text)) out.add(text);
        }
        return out;
    }

    static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").trim();
    }
}
