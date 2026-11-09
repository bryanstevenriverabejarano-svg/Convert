package salve.core.selfpr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Linaje de los cambios que Salve ha propuesto sobre sí misma: qué quiso cambiar, dónde, sobre qué
 * revisión y cómo iba su examen en ese momento. Solo se añade (un JSON por línea), nunca se reescribe.
 *
 * Sirve para lo que «pasar las pruebas» no dice: si después de un cambio el examen empeora, ella lo ve
 * y lo cuenta, y Bryan puede revertir ese commit. No demuestra causa: entre dos exámenes cambian más cosas.
 */
public final class SelfLineage {
    private SelfLineage() { }

    public static JsonObject entry(long whenMillis, String origin, String goal, String path, String revision,
                                   String issueUrl, int examFailures) {
        JsonObject e = new JsonObject();
        e.addProperty("t", whenMillis);
        e.addProperty("origen", origin);
        e.addProperty("objetivo", goal == null ? "" : goal);
        e.addProperty("archivo", path);
        e.addProperty("revision", revision);
        e.addProperty("issue", issueUrl);
        e.addProperty("fallos_examen", examFailures);
        return e;
    }

    public static synchronized void append(File file, JsonObject entry) throws IOException {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new IOException("No pude crear " + dir.getName());
        Files.write(file.toPath(), (entry.toString() + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Las últimas entradas válidas, de la más antigua a la más reciente; una línea dañada se salta. */
    public static List<JsonObject> read(File file, int last) {
        List<JsonObject> out = new ArrayList<>();
        if (file == null || !file.isFile()) return out;
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) continue;
                try { out.add(JsonParser.parseString(line).getAsJsonObject()); }
                catch (RuntimeException damaged) { /* se salta */ }
            }
        } catch (IOException unreadable) { return out; }
        return out.size() > last ? new ArrayList<>(out.subList(out.size() - last, out.size())) : out;
    }

    /** Casos fallidos de un examen guardado; −1 si no hay examen o no trae casos. */
    public static int failedCases(JsonObject exam) {
        JsonArray cases = findCases(exam, 0);
        if (cases == null) return -1;
        int failed = 0;
        for (JsonElement e : cases) {
            if (!e.isJsonObject()) continue;
            JsonElement f = e.getAsJsonObject().get("failed");
            if (f != null && f.isJsonArray() && f.getAsJsonArray().size() > 0) failed++;
        }
        return failed;
    }

    private static JsonArray findCases(JsonObject node, int depth) {
        if (node == null || depth > 2) return null;
        JsonElement direct = node.get("cases");
        if (direct != null && direct.isJsonArray()) return direct.getAsJsonArray();
        for (Map.Entry<String, JsonElement> child : node.entrySet()) {
            if (!child.getValue().isJsonObject()) continue;
            JsonArray found = findCases(child.getValue().getAsJsonObject(), depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    public static String describe(List<JsonObject> entries, int currentFailures) {
        if (entries == null || entries.isEmpty()) return "";
        StringBuilder out = new StringBuilder("Linaje de mis cambios (los últimos " + entries.size() + "):");
        SimpleDateFormat day = new SimpleDateFormat("d MMM", new Locale("es"));
        for (JsonObject e : entries) {
            String path = text(e, "archivo");
            out.append("\n• ").append(day.format(new Date(e.has("t") ? e.get("t").getAsLong() : 0L))).append(" — ")
                    .append(path.substring(path.lastIndexOf('/') + 1));
            String goal = text(e, "objetivo");
            if (!goal.isEmpty()) out.append(": «").append(goal).append('»');
            out.append("examen".equals(text(e, "origen")) ? " (por mi examen)" : " (me lo pidió Bryan)");
        }
        JsonObject latest = entries.get(entries.size() - 1);
        int before = latest.has("fallos_examen") ? latest.get("fallos_examen").getAsInt() : -1;
        if (before >= 0 && currentFailures >= 0) {
            out.append("\nCuando propuse el último, mi examen tenía ").append(before).append(" casos fallidos; el más reciente tiene ")
                    .append(currentFailures).append('.');
            if (currentFailures > before) out.append(" Ha empeorado: puede no ser por ese cambio, pero conviene repetir el examen "
                    + "y, si sigue peor, revertir ese commit.");
            else if (currentFailures < before) out.append(" Ha mejorado.");
            else out.append(" Sin cambio medible (si no he repetido el examen desde entonces, es el mismo).");
        }
        return out.toString();
    }

    private static String text(JsonObject e, String key) {
        JsonElement v = e.get(key);
        return v == null || !v.isJsonPrimitive() ? "" : v.getAsString();
    }
}
