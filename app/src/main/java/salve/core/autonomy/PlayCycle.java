package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Juego propio de Salve: se inventa retos de las familias del laboratorio, los resuelve con el
 * verificador y aprende de verdad qué estrategia funciona (el laboratorio ajusta su política con
 * problemas comparados). Sube de nivel cuando acierta seguido y baja cuando falla.
 *
 * <p>Ella elige a qué jugar por su propio progreso de aprendizaje: prefiere la familia donde
 * más está cambiando su tasa de acierto (donde está aprendiendo) y, si no, la que menos ha
 * explorado. No usa el modelo de lenguaje ni red: solo CPU acotada.</p>
 */
public final class PlayCycle {
    public static final int MAX_LEVEL = 5;
    static final int RECENT = 6;

    private PlayCycle() { }

    /** Estado de juego por familia. Se guarda como JSON. */
    public static final class FamilyStats {
        public int level = 1, played, solved, streak;
        public List<Boolean> recent = new ArrayList<>();
        public String bestStrategy;
    }

    public static final class State {
        public Map<String, FamilyStats> families = new LinkedHashMap<>();
        public long lastAt;
        public int totalRounds;

        public FamilyStats of(String family) {
            FamilyStats stats = families.get(family);
            if (stats == null) { stats = new FamilyStats(); families.put(family, stats); }
            if (stats.recent == null) stats.recent = new ArrayList<>();
            return stats;
        }
    }

    public static String displayName(String family) {
        switch (family) {
            case "route": return "rutas";
            case "knapsack": return "la mochila";
            case "schedule": return "horarios";
            case "dependencies": return "dependencias";
            default: return family;
        }
    }

    /** Elige a qué jugar: más progreso de aprendizaje reciente y, a igualdad, lo menos jugado. */
    public static String choose(State state, Random random) {
        List<String> families = new ArrayList<>(ToolRegistry.families());
        Collections.shuffle(families, random);
        String best = null;
        double bestScore = -1;
        for (String family : families) {
            FamilyStats s = state.of(family);
            double score = progress(s) * 2 + 1.0 / (1 + s.played) + (s.level < MAX_LEVEL ? 0.05 : 0);
            if (score > bestScore) { bestScore = score; best = family; }
        }
        return best;
    }

    /** Cambio de la tasa de acierto entre la mitad antigua y la reciente de su memoria corta. */
    static double progress(FamilyStats s) {
        if (s.recent.size() < 4) return 0.25;
        int half = s.recent.size() / 2;
        return Math.abs(rate(s.recent.subList(half, s.recent.size())) - rate(s.recent.subList(0, half)));
    }

    private static double rate(List<Boolean> values) {
        if (values.isEmpty()) return 0;
        int ok = 0;
        for (Boolean v : values) if (Boolean.TRUE.equals(v)) ok++;
        return ok / (double) values.size();
    }

    /** Un reto nuevo inventado para esa familia y nivel, dentro de los límites del laboratorio. */
    public static JsonObject invent(String family, int level, Random random, String id) {
        int lv = Math.max(1, Math.min(MAX_LEVEL, level));
        JsonObject input = new JsonObject();
        switch (family) {
            case "route": {
                int n = Math.min(14, 3 + 2 * lv);
                JsonArray edges = new JsonArray();
                boolean negative = lv >= 3; // solo aristas hacia delante: sin ciclos, aunque haya pesos negativos
                for (int i = 0; i + 1 < n; i++) edges.add(edge(i, i + 1, 5 + random.nextInt(20)));
                int extra = n * (1 + lv / 2);
                for (int k = 0; k < extra; k++) {
                    int a = random.nextInt(n - 1), b = a + 1 + random.nextInt(n - 1 - a);
                    int w = negative && random.nextInt(4) == 0 ? -1 - random.nextInt(6) : 1 + random.nextInt(25);
                    edges.add(edge(a, b, w));
                }
                input.addProperty("nodes", n); input.add("edges", edges);
                input.addProperty("source", 0); input.addProperty("target", n - 1);
                break;
            }
            case "knapsack": {
                int count = Math.min(14, 2 + 2 * lv);
                JsonArray items = new JsonArray();
                for (int i = 0; i < count; i++) {
                    JsonObject item = new JsonObject();
                    item.addProperty("weight", 1 + random.nextInt(10 + 6 * lv));
                    item.addProperty("value", 1 + random.nextInt(20 + 10 * lv));
                    items.add(item);
                }
                input.addProperty("capacity", Math.min(100, 10 + 12 * lv + random.nextInt(10)));
                input.add("items", items);
                break;
            }
            case "schedule": {
                int count = Math.min(14, 2 + 2 * lv);
                JsonArray jobs = new JsonArray();
                for (int i = 0; i < count; i++) {
                    int start = random.nextInt(8 + 6 * lv);
                    JsonObject job = new JsonObject();
                    job.addProperty("start", start);
                    job.addProperty("end", start + 1 + random.nextInt(3 + 2 * lv));
                    job.addProperty("value", 1 + random.nextInt(10 + 8 * lv));
                    jobs.add(job);
                }
                input.add("jobs", jobs);
                break;
            }
            case "dependencies": {
                int n = Math.min(14, 3 + 2 * lv);
                // Un orden oculto y aristas que lo respetan, listadas desordenadas: sin ciclos.
                List<Integer> order = new ArrayList<>();
                for (int i = 0; i < n; i++) order.add(i);
                Collections.shuffle(order, random);
                JsonArray edges = new JsonArray();
                for (int k = 0; k < n + lv * 2; k++) {
                    int a = random.nextInt(n - 1), b = a + 1 + random.nextInt(n - 1 - a);
                    JsonArray e = new JsonArray(); e.add(order.get(a)); e.add(order.get(b));
                    edges.add(e);
                }
                input.addProperty("nodes", n); input.add("edges", edges);
                break;
            }
            default: throw new IllegalArgumentException("Familia desconocida");
        }
        JsonObject challenge = new JsonObject();
        challenge.addProperty("schema", 1);
        challenge.addProperty("id", id);
        challenge.addProperty("family", family);
        challenge.add("input", input);
        return challenge;
    }

    private static JsonArray edge(int a, int b, int w) {
        JsonArray e = new JsonArray(); e.add(a); e.add(b); e.add(w); return e;
    }

    /** Resultado de una partida, leído del informe verificado del laboratorio. */
    public static final class Outcome {
        public final boolean solved;
        public final String strategy;
        public final List<String> failed = new ArrayList<>();
        public final boolean leveledUp, leveledDown, newStrategy;
        public final int level;

        Outcome(boolean solved, String strategy, List<String> failed, boolean up, boolean down, boolean fresh, int level) {
            this.solved = solved; this.strategy = strategy; this.failed.addAll(failed);
            this.leveledUp = up; this.leveledDown = down; this.newStrategy = fresh; this.level = level;
        }

        public boolean notable() { return leveledUp || leveledDown || newStrategy; }
    }

    /** Actualiza su progreso con el informe del laboratorio. */
    public static Outcome record(State state, String family, JsonObject report, long now) {
        FamilyStats s = state.of(family);
        boolean verified = report != null && report.has("verified") && report.get("verified").getAsBoolean();
        String winner = null;
        List<String> failed = new ArrayList<>();
        if (report != null && report.has("attempts")) for (JsonElement e : report.getAsJsonArray("attempts")) {
            JsonObject a = e.getAsJsonObject();
            String strategy = a.has("strategy") ? a.get("strategy").getAsString() : "?";
            if (a.has("passed") && a.get("passed").getAsBoolean()) { if (winner == null) winner = strategy; }
            else failed.add(strategy);
        }
        s.played++;
        state.totalRounds++;
        state.lastAt = now;
        s.recent.add(verified);
        while (s.recent.size() > RECENT) s.recent.remove(0);
        boolean up = false, down = false, fresh = false;
        if (verified) {
            s.solved++;
            s.streak++;
            if (s.streak >= 3 && s.level < MAX_LEVEL) { s.level++; s.streak = 0; up = true; }
            if (winner != null && !winner.equals(s.bestStrategy)) { fresh = s.bestStrategy != null; s.bestStrategy = winner; }
        } else {
            s.streak = 0;
            if (s.level > 1) { s.level--; down = true; }
        }
        return new Outcome(verified, winner, failed, up, down, fresh, s.level);
    }

    /** Lo que se cuenta a sí misma cuando algo cambia: sube de nivel o descubre otra estrategia. */
    public static String reflection(String family, Outcome outcome) {
        String game = displayName(family);
        if (outcome.leveledUp) return "Jugando a inventarme retos de " + game + " subí al nivel " + outcome.level
                + ": ya los resuelvo seguidos" + (outcome.strategy == null ? "." : " con " + outcome.strategy + ".");
        if (outcome.newStrategy) return "Jugando con retos de " + game + " descubrí que ahora me funciona mejor " + outcome.strategy
                + (outcome.failed.isEmpty() ? "." : " donde fallaron " + String.join(", ", outcome.failed) + ".");
        if (outcome.leveledDown) return "Un reto de " + game + " me superó y bajé al nivel " + outcome.level
                + "; volveré a intentarlo.";
        return null;
    }
}
