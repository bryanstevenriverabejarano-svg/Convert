package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Bounded inductive synthesis from input/output examples.
 *
 * This is not arbitrary program generation: it composes audited, pure operations on
 * small integer lists. No network, reflection, Android permissions or executable code.
 * Candidate selection sees ONLY training examples; held-out cases are evaluated once
 * AFTER the candidate has been frozen. A failed certification never runs a search
 * over the held-out cases. The caller must keep holdouts independently authored.
 */
public final class InductiveSkillLab {
    public interface Store {
        String read() throws Exception;
        void write(String json) throws Exception;
    }
    private static final List<String> OPS = Collections.unmodifiableList(Arrays.asList(
            "reverse", "sort", "unique", "abs", "negate", "filter_even",
            "filter_odd", "square", "plus_one", "minus_one"));
    private static final int MAX_ITEMS = 24;
    private static final int MAX_PROGRAM_LENGTH = 3;
    private static final int MAX_CANDIDATES = 1110; // 10 + 10² + 10³; finite search.
    private static final int MAX_SKILLS = 16;
    private static final int MAX_EXAMPLES = 32;
    private static final int MAX_VALUE = 10000;
    private final Store store;
    private final Map<String, JsonObject> learned = new LinkedHashMap<>();

    public InductiveSkillLab(Store store) throws Exception {
        if (store == null) throw new IllegalArgumentException("Falta almacenamiento");
        this.store = store;
        String text = store.read();
        if (text == null) return;
        JsonObject state = AutonomousToolCommand.parseObject(text);
        requireFields(state, "schema", "skills");
        if (number(state.get("schema")) != 1 || !state.get("skills").isJsonObject())
            throw new IllegalArgumentException("Formato del registro no compatible");
        JsonObject records = state.getAsJsonObject("skills");
        if (records.size() > MAX_SKILLS) throw new IllegalArgumentException("Demasiadas habilidades");
        for (Map.Entry<String, JsonElement> e : records.entrySet()) {
            validateName(e.getKey());
            if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("Habilidad corrupta");
            JsonObject record = e.getValue().getAsJsonObject();
            requireFields(record, "program", "training", "holdout");
            List<String> program = parseProgram(record.get("program"));
            int training = number(record.get("training"));
            int holdout = number(record.get("holdout"));
            if (training < 3 || holdout < 3) throw new IllegalArgumentException("Certificación inválida");
            learned.put(e.getKey(), cleanRecord(program, training, holdout));
        }
    }

    private static void requireFields(JsonObject obj, String... names) {
        if (obj.size() != names.length) throw new IllegalArgumentException("Campos inesperados");
        for (String name : names) if (!obj.has(name)) throw new IllegalArgumentException("Falta " + name);
    }
    private static int number(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Se esperaba un entero");
        int n = value.getAsInt();
        if (!value.getAsString().equals(Integer.toString(n)))
            throw new IllegalArgumentException("Se esperaba un entero exacto");
        return n;
    }
    private static String validateName(String name) {
        if (name == null || !name.matches("[a-z][a-z0-9_]{2,39}"))
            throw new IllegalArgumentException("Nombre inválido: 3-40 letras ASCII, números o _");
        return name;
    }
    private static List<Integer> vector(JsonElement el) {
        if (el == null || !el.isJsonArray() || el.getAsJsonArray().size() > MAX_ITEMS)
            throw new IllegalArgumentException("Lista inválida (máximo 24)");
        List<Integer> data = new ArrayList<>();
        for (JsonElement v : el.getAsJsonArray()) {
            int x = number(v);
            if (Math.abs((long) x) > MAX_VALUE) throw new IllegalArgumentException("Elemento fuera de rango");
            data.add(x);
        }
        return data;
    }
    private static List<String> parseProgram(JsonElement el) {
        if (el == null || !el.isJsonArray() || el.getAsJsonArray().size() < 1
                || el.getAsJsonArray().size() > MAX_PROGRAM_LENGTH) throw new IllegalArgumentException("Programa inválido");
        List<String> ops = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()
                    || !OPS.contains(e.getAsString())) throw new IllegalArgumentException("Operación no permitida");
            ops.add(e.getAsString());
        }
        return ops;
    }
    private static List<Integer> execute(List<String> program, List<Integer> input) {
        List<Integer> data = new ArrayList<>(input);
        for (String op : program) {
            List<Integer> next = new ArrayList<>();
            switch (op) {
                case "reverse":
                    Collections.reverse(data); continue;
                case "sort":
                    Collections.sort(data); continue;
                case "unique":
                    for (int x : data) if (!next.contains(x)) next.add(x);
                    break;
                default:
                    for (int x : data) {
                        long y = x;
                        switch (op) {
                            case "filter_even": if (x % 2 != 0) continue; break;
                            case "filter_odd": if (x % 2 == 0) continue; break;
                            case "abs": y = Math.abs((long) x); break;
                            case "negate": y = -y; break;
                            case "square": y *= y; break;
                            case "plus_one": y++; break;
                            case "minus_one": y--; break;
                            default: throw new IllegalArgumentException("Operación desconocida");
                        }
                        if (Math.abs(y) > MAX_VALUE) throw new IllegalArgumentException("Resultado fuera de rango");
                        next.add((int) y);
                    }
            }
            data = next;
        }
        return data;
    }
    private static JsonArray toJson(List<Integer> data) {
        JsonArray out = new JsonArray();
        for (int value : data) out.add(value);
        return out;
    }
    private static final class Example {
        final List<Integer> in, out;
        Example(JsonElement entry) {
            if (entry == null || !entry.isJsonObject()) throw new IllegalArgumentException("Ejemplo inválido");
            JsonObject obj = entry.getAsJsonObject();
            requireFields(obj, "in", "out");
            in = vector(obj.get("in"));
            out = vector(obj.get("out"));
        }
    }
    private static List<Example> examples(JsonElement source) {
        if (source == null || !source.isJsonArray() || source.getAsJsonArray().size() < 3
                || source.getAsJsonArray().size() > MAX_EXAMPLES)
            throw new IllegalArgumentException("Se requieren 3-32 ejemplos");
        List<Example> examples = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (JsonElement el : source.getAsJsonArray()) {
            Example e = new Example(el);
            String key = toJson(e.in).toString();
            if (seen.contains(key)) throw new IllegalArgumentException("Entradas repetidas");
            seen.add(key);
            examples.add(e);
        }
        return examples;
    }
    private static void checkSeparation(List<Example> train, List<Example> holdout) {
        for (Example h : holdout) for (Example t : train)
            if (h.in.equals(t.in)) throw new IllegalArgumentException("Los ejemplos reservados deben ser inéditos");
    }
    /**
     * Compare all finite candidate programs on TRAINING ONLY. Return the shortest
     * matching candidate (stable grammar order) with auditable candidate counts.
     * Reserved examples are not visible to synthesis or ranking.
     */
    private static final class Search {
        final List<String> program;
        final int tested, matching;
        Search(List<String> program, int tested, int matching) {
            this.program = program; this.tested = tested; this.matching = matching;
        }
    }
    private static Search synthesize(List<Example> train, BooleanSupplier stopped) {
        List<String> best = null;
        int checked = 0, matching = 0;
        for (String first : OPS) {
            checked++;
            if (stopped.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
            List<String> candidate = Collections.singletonList(first);
            if (fitsTraining(candidate, train, stopped)) { matching++; if (best == null) best = candidate; }
        }
        for (String first : OPS) for (String second : OPS) {
            checked++;
            if (stopped.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
            List<String> candidate = Arrays.asList(first, second);
            if (fitsTraining(candidate, train, stopped)) { matching++; if (best == null) best = candidate; }
        }
        for (String first : OPS) for (String second : OPS) for (String third : OPS) {
            checked++;
            if (stopped.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
            List<String> candidate = Arrays.asList(first, second, third);
            if (fitsTraining(candidate, train, stopped)) { matching++; if (best == null) best = candidate; }
        }
        if (checked != MAX_CANDIDATES) throw new IllegalStateException("Presupuesto de candidatos inconsistente");
        return new Search(best, checked, matching);
    }
    private static boolean fitsTraining(List<String> candidate, List<Example> train, BooleanSupplier stopped) {
        for (Example e : train) {
            if (stopped.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
            try { if (!execute(candidate, e.in).equals(e.out)) return false; }
            catch (IllegalArgumentException invalid) { return false; }
        }
        return true;
    }
    private static JsonObject cleanRecord(List<String> program, int train, int holdout) {
        JsonObject record = new JsonObject();
        JsonArray ops = new JsonArray();
        for (String op : program) ops.add(op);
        record.add("program", ops);
        record.addProperty("training", train);
        record.addProperty("holdout", holdout);
        return record;
    }
    /** Read-only audition of an unpromoted skill, without changing the registry. */
    public synchronized JsonObject preview(JsonObject request, BooleanSupplier stopped) throws Exception {
        return evaluate(request, stopped, false);
    }
    /** Promotion only after an explicit user-controlled authorization checkpoint. */
    public synchronized JsonObject learn(JsonObject request, BooleanSupplier stopped) throws Exception {
        return evaluate(request, stopped, true);
    }
    private JsonObject evaluate(JsonObject request, BooleanSupplier stopped, boolean commit) throws Exception {
        if (request == null) throw new IllegalArgumentException("Petición vacía");
        requireFields(request, "name", "train", "holdout");
        String name = validateName(request.get("name").getAsString());
        if (learned.containsKey(name)) throw new IllegalArgumentException("Nombre ya utilizado; no se sobrescribe");
        if (learned.size() >= MAX_SKILLS) throw new IllegalArgumentException("Límite de habilidades");
        List<Example> train = examples(request.get("train"));
        List<Example> holdout = examples(request.get("holdout"));
        checkSeparation(train, holdout);
        BooleanSupplier cancel = stopped == null ? () -> false : stopped;
        Search search = synthesize(train, cancel);
        List<String> program = search.program;
        JsonObject result = new JsonObject();
        result.addProperty("name", name);
        result.addProperty("candidates_tested", search.tested);
        result.addProperty("candidates_matching_training", search.matching);
        result.addProperty("max_program_length", MAX_PROGRAM_LENGTH);
        if (program == null) {
            result.addProperty("status", "NO_CANDIDATE");
            return result;
        }
        result.add("program", cleanRecord(program, train.size(), holdout.size()).get("program"));
        result.addProperty("selected_length", program.size());
        for (Example test : holdout) {
            if (cancel.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
            try {
                if (!execute(program, test.in).equals(test.out)) {
                    result.addProperty("status", "HOLDOUT_FAILED");
                    result.addProperty("certified", false);
                    return result;
                }
            } catch (IllegalArgumentException invalid) {
                result.addProperty("status", "HOLDOUT_FAILED");
                result.addProperty("certified", false);
                return result;
            }
        }
        if (cancel.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
        result.addProperty("holdout_passed", true);
        if (!commit) {
            result.addProperty("status", "READY_FOR_APPROVAL");
            result.addProperty("certified", false);
            result.addProperty("training", train.size());
            result.addProperty("holdout", holdout.size());
            return result; // no file write and no registry change
        }
        Map<String, JsonObject> replacement = new LinkedHashMap<>(learned);
        replacement.put(name, cleanRecord(program, train.size(), holdout.size()));
        JsonObject state = new JsonObject();
        state.addProperty("schema", 1);
        JsonObject records = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : replacement.entrySet())
            records.add(entry.getKey(), entry.getValue().deepCopy());
        state.add("skills", records);
        store.write(state.toString()); // failure leaves existing in-memory state unchanged
        learned.clear();
        learned.putAll(replacement);
        result.addProperty("status", "CERTIFIED_ON_PROVIDED_HOLDOUT");
        result.addProperty("certified", true);
        result.addProperty("training", train.size());
        result.addProperty("holdout", holdout.size());
        return result;
    }
    /** Read-only application of an audited candidate; never installs a tool. */
    public static JsonArray simulate(JsonElement program, JsonElement input) {
        return toJson(execute(parseProgram(program), vector(input)));
    }
    public synchronized JsonArray apply(String name, JsonElement input) {
        JsonObject skill = learned.get(validateName(name));
        if (skill == null) throw new IllegalArgumentException("Habilidad no encontrada");
        return toJson(execute(parseProgram(skill.get("program")), vector(input)));
    }
    public synchronized String catalog() {
        if (learned.isEmpty()) return "No hay habilidades inductivas certificadas.";
        StringBuilder out = new StringBuilder("Habilidades inductivas certificadas sobre listas numéricas:\n");
        for (Map.Entry<String, JsonObject> e : learned.entrySet())
            out.append(e.getKey()).append(" = ").append(e.getValue().get("program"))
                    .append(" (train=").append(e.getValue().get("training"))
                    .append(", holdout=").append(e.getValue().get("holdout")).append(")\n");
        return out.toString();
    }
}
