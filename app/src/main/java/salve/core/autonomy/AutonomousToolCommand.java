package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/** Local command routing and strict data parsing, independent of Android and inference. */
public final class AutonomousToolCommand {
    public static final int MAX_INPUT_BYTES = 64 * 1024;
    public enum Action { STATUS, CATALOG, PAUSE, RESUME, ROLLBACK, SOLVE }
    public final Action action;
    public final String payload;

    private AutonomousToolCommand(Action action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    public static AutonomousToolCommand parse(String input) {
        if (input == null) return null;
        String value = input.trim();
        int colon = value.indexOf(':');
        if (colon >= 0 && colon <= 40 && normalized(value.substring(0, colon)).equals("resuelve reto"))
            return new AutonomousToolCommand(Action.SOLVE, value.substring(colon + 1).trim());
        // Oversized/malformed challenge commands must stay local, including the missing colon.
        String prefix = normalized(value.substring(0, Math.min(value.length(), 40)));
        if (prefix.equals("resuelve reto") || prefix.startsWith("resuelve reto "))
            return new AutonomousToolCommand(Action.SOLVE, "");
        if (value.length() > 160) return null;
        String command = normalized(value).replaceFirst("^[¿¡]+", "")
                .replaceFirst("[?!.]+$", "").trim();
        switch (command) {
            case "laboratorio autonomo":
            case "estado del laboratorio":
            case "que herramientas has aprendido": return new AutonomousToolCommand(Action.STATUS, "");
            case "catalogo de herramientas": return new AutonomousToolCommand(Action.CATALOG, "");
            case "pausa el laboratorio": return new AutonomousToolCommand(Action.PAUSE, "");
            case "reanuda el laboratorio": return new AutonomousToolCommand(Action.RESUME, "");
            default:
                if (command.equals("revierte herramienta") || command.startsWith("revierte herramienta "))
                    return new AutonomousToolCommand(Action.ROLLBACK,
                            command.substring("revierte herramienta".length()).trim());
                return null;
        }
    }

    public static boolean isFamily(String family) {
        return "route".equals(family) || "knapsack".equals(family)
                || "schedule".equals(family) || "dependencies".equals(family);
    }

    /** A narrow invitation to structure an explicit discrete problem, never general self-modification. */
    public static boolean offersToolFor(String input) {
        return !offeredFamilyFor(input).isEmpty();
    }

    public static String offeredFamilyFor(String input) {
        if (input == null || input.length() > MAX_INPUT_BYTES) return "";
        String text = normalized(input);
        if (!text.matches("(?s).*\\d.*")) return "";
        boolean route = text.contains("ruta mas corta") || text.contains("ruta minima")
                || text.contains("camino mas corto") || text.contains("camino minimo")
                || text.contains("shortest path");
        boolean knapsack = text.contains("knapsack") || text.contains("mochila")
                || (text.contains("capacidad") && text.contains("peso") && text.contains("valor"));
        boolean schedule = text.contains("intervalos");
        boolean dependencies = text.contains("dependencias") || text.contains("orden topologico");
        if ((route ? 1 : 0) + (knapsack ? 1 : 0) + (schedule ? 1 : 0) + (dependencies ? 1 : 0) != 1)
            return "";
        return route ? "route" : knapsack ? "knapsack" : schedule ? "schedule" : "dependencies";
    }

    /** Exact JSON only: no extraction from prose, no duplicate fields, comments or trailing data. */
    public static JsonObject parseObject(String json) {
        if (json == null || json.length() > MAX_INPUT_BYTES
                || json.getBytes(StandardCharsets.UTF_8).length > MAX_INPUT_BYTES)
            throw new IllegalArgumentException("El reto supera el límite de 64 KiB.");
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement element = readElement(reader, 0, new int[] {0});
            if (!element.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("Se requiere un único objeto JSON.");
            return element.getAsJsonObject();
        } catch (IOException | NumberFormatException invalid) {
            throw new IllegalArgumentException("Reto JSON inválido, duplicado o demasiado complejo.", invalid);
        }
    }

    private static JsonElement readElement(JsonReader reader, int depth, int[] count) throws IOException {
        if (depth > 32 || ++count[0] > 4096) throw new IOException("JSON demasiado complejo.");
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw new IOException("Campo JSON duplicado.");
                    object.add(name, readElement(reader, depth + 1, count));
                }
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) array.add(readElement(reader, depth + 1, count));
                reader.endArray();
                return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER:
                String number = reader.nextString();
                if (number.length() > 128) throw new IOException("Número demasiado largo.");
                return new JsonPrimitive(new BigDecimal(number));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw new IOException("Valor JSON inválido.");
        }
    }

    private static String normalized(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
