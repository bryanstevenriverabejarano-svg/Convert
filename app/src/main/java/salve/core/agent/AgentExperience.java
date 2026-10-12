package salve.core.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import salve.core.memory.MemoryQueryTerms;

/** Finite operational evidence, never user facts, tool permissions or model-weight training. */
public final class AgentExperience {
    public static final int MAX_CONTEXT = 1400;
    private static final long MAX_AGE = 7L * 24 * 60 * 60_000;
    private static final Set<String> GENERIC = new HashSet<>(Arrays.asList(
            "consulta", "consultar", "busca", "buscar", "investiga", "investigar", "recuerda", "recordar",
            "revisa", "revisar", "resume", "resumir", "resumen", "compara", "comparar", "objetivo", "plan",
            "informacion", "datos", "fuentes", "https", "http", "www", "com", "org"));
    private AgentExperience() { }

    /** Recent, finished, related runs with the same remote configuration. Emits metadata only. */
    public static String contextFor(AgentRun current, List<AgentRun> history, long now) {
        // Code-enabled runs cannot consume personal-memory-derived context, including this channel.
        if (current.codeAllowed || history == null) return "";
        Set<String> query = terms(current.goal);
        if (query.isEmpty()) return "";
        List<AgentRun> candidates = new ArrayList<>();
        for (AgentRun previous : history) {
            if (previous == null || previous.id.equals(current.id) || previous.codeAllowed
                    || !previous.bridge.equals(current.bridge) || previous.updatedAt > now
                    || now - previous.updatedAt > MAX_AGE
                    || !Arrays.asList("SUCCEEDED", "PARTIAL", "BLOCKED").contains(previous.status)
                    || overlap(query, terms(previous.goal)) < Math.min(2, query.size())
                    || previous.observations.isEmpty()) continue;
            candidates.add(previous);
        }
        candidates.sort((a, b) -> {
            int score = Integer.compare(overlap(query, terms(b.goal)), overlap(query, terms(a.goal)));
            return score != 0 ? score : Long.compare(b.updatedAt, a.updatedAt);
        });
        JsonArray plans = new JsonArray();
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (AgentRun previous : candidates) {
            if (plans.size() >= 3) break;
            boolean included = false;
            Set<String> seen = new HashSet<>();
            for (AgentRun.Observation o : previous.observations) {
                int index = Arrays.asList("SUCCESS", "PARTIAL", "ERROR", "BLOCKED").indexOf(o.status);
                if (!AgentPolicy.TOOLS.contains(o.tool) || index < 0 || !seen.add(o.id)) continue;
                counts.computeIfAbsent(o.tool, ignored -> new int[4])[index]++;
                included = true;
            }
            if (included) plans.add(previous.id);
        }
        if (plans.isEmpty()) return "";
        JsonObject result = new JsonObject(); result.addProperty("version", 1); result.add("planes", plans);
        JsonArray tools = new JsonArray();
        for (Map.Entry<String, int[]> entry : counts.entrySet()) {
            JsonObject item = new JsonObject(); item.addProperty("tool", entry.getKey());
            int[] c = entry.getValue();
            item.addProperty("ok", c[0]); item.addProperty("parcial", c[1]); item.addProperty("error", c[2]); item.addProperty("bloqueo", c[3]);
            tools.add(item);
        }
        result.add("herramientas", tools);
        String json = result.toString();
        return json.length() <= MAX_CONTEXT ? json : "";
    }
    public static boolean valid(String context) {
        if (context == null || context.length() > MAX_CONTEXT) return false;
        if (context.isEmpty()) return true;
        try {
            JsonObject o = JsonParser.parseString(context).getAsJsonObject();
            if (o.size() != 3 || !integer(o.get("version"), 1, 1)) return false;
            JsonArray plans = o.getAsJsonArray("planes"), tools = o.getAsJsonArray("herramientas");
            if (plans.isEmpty() || plans.size() > 3 || tools.isEmpty() || tools.size() > AgentPolicy.TOOLS.size()) return false;
            Set<String> ids = new HashSet<>(), names = new HashSet<>();
            for (JsonElement id : plans) if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()
                    || !id.getAsString().matches("[a-zA-Z0-9-]{1,60}") || !ids.add(id.getAsString())) return false;
            for (JsonElement entry : tools) {
                JsonObject item = entry.getAsJsonObject();
                if (item.size() != 5 || !item.get("tool").getAsJsonPrimitive().isString()
                        || !AgentPolicy.TOOLS.contains(item.get("tool").getAsString()) || !names.add(item.get("tool").getAsString())) return false;
                for (String key : Arrays.asList("ok", "parcial", "error", "bloqueo")) if (!integer(item.get(key), 0, 24)) return false;
            }
            return true;
        } catch (RuntimeException invalid) { return false; }
    }
    private static boolean integer(JsonElement e, int min, int max) {
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()
                && e.getAsString().matches("0|[1-9][0-9]?") && e.getAsInt() >= min && e.getAsInt() <= max;
    }
    public static String describe(String context) {
        if (!valid(context) || context.isEmpty()) return "Sin experiencia previa pertinente conservada para este plan.";
        JsonObject data = JsonParser.parseString(context).getAsJsonObject();
        StringBuilder text = new StringBuilder("Experiencia disponible de ").append(data.getAsJsonArray("planes").size())
                .append(" planes relacionados de los siete días anteriores a este plan. Son resultados operativos, no una garantía de verdad.\n");
        for (JsonElement entry : data.getAsJsonArray("herramientas")) {
            JsonObject item = entry.getAsJsonObject();
            text.append(item.get("tool").getAsString()).append(": ").append(item.get("ok").getAsInt()).append(" respuestas, ")
                    .append(item.get("parcial").getAsInt()).append(" parciales, ").append(item.get("error").getAsInt())
                    .append(" errores, ").append(item.get("bloqueo").getAsInt()).append(" bloqueos.\n");
        }
        text.append("Planes de origen: ");
        for (JsonElement id : data.getAsJsonArray("planes")) text.append(id.getAsString(), 0, Math.min(8, id.getAsString().length())).append(' ');
        return text.toString();
    }
    /** Trims whole records, never JSON text or a tool's counters. */
    public static String excerpt(String context, int budget) {
        if (!valid(context) || context.isEmpty() || budget <= 0) return "";
        JsonObject data = JsonParser.parseString(context).getAsJsonObject();
        JsonArray tools = data.getAsJsonArray("herramientas");
        while (data.toString().length() > budget && tools.size() > 1) tools.remove(tools.size() - 1);
        String result = data.toString(); return result.length() <= budget ? result : "";
    }
    /** A hash avoids storing another plaintext copy of a private tool argument. */
    public static String fingerprint(String tool, String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest((tool + "\u0000" + input).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    /** Successful data can be reused; transient errors get one retry per explicit resume. */
    public static String repeatFeedback(AgentRun run, AgentRun.Pending call) {
        String key = fingerprint(call.tool, call.input); int errors = 0;
        for (AgentRun.Observation o : run.observations) {
            if (!key.equals(o.fingerprint)) continue;
            if (o.useful()) return "Esa llamada ya tiene la observación " + o.id + ". Usa su evidencia y sus límites o elige otra acción; no se ejecutó otra vez.";
            if (o.status.equals("BLOCKED")) return "Esa llamada ya fue bloqueada. Cambia de herramienta o explica el límite; repetirla no concede permisos.";
            if (o.status.equals("ERROR")) errors++;
        }
        return errors >= 2 ? "La misma llamada falló dos veces. Cambia de enfoque o responde con los límites de la evidencia disponible." : null;
    }
    private static Set<String> terms(String goal) {
        Set<String> result = new HashSet<>();
        for (String term : MemoryQueryTerms.extract(goal, 32)) {
            String folded = java.text.Normalizer.normalize(term, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            if (!GENERIC.contains(folded)) result.add(folded);
        }
        return result;
    }
    private static int overlap(Set<String> query, Set<String> candidate) {
        int score = 0; for (String term : query) if (candidate.contains(term)) score++; return score;
    }
}
