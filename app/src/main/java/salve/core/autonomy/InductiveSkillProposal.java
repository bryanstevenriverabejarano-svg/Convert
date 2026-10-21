package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

/**
 * Narrow need -> example generation -> train-only synthesis -> independent
 * benchmark pipeline. No LLM-selected permissions or model-generated labels.
 */
public final class InductiveSkillProposal {
    public enum Kind { SORT_UNIQUE_ODDS, SORT_UNIQUE_EVENS }
    private static final int[][] TRAIN = {
            {7,3,7,4,1,3,4}, {-1,2,-3,7,-1,7,2}, {9,8,3,9,2,-5,3,8},
            {7,7,-7,4,0,1}, {6,4,3,-9,3,0,1}, {-11,2,-11,5,5,3,3}
    };
    private static final int[][] HOLDOUT = {
            {101,-21,101,2,-5,-5,11}, {8,6,4,2,-2,0},
            {-1,7,7,5,-3,1}, {5,-5,9,1,9,4,2},
            {11,11,12,12,-4,-2,-4}, {0,2,4,7,9,9,-6,-6}
    };
    public static final int BENCHMARK_CASES = 96;
    private InductiveSkillProposal() { }

    public static Kind recognize(String need) {
        if (need == null) return null;
        String folded = java.text.Normalizer.normalize(need.toLowerCase(java.util.Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").trim();
        if (folded.equals("ordenar impares sin repetir")) return Kind.SORT_UNIQUE_ODDS;
        if (folded.equals("ordenar pares sin repetir")) return Kind.SORT_UNIQUE_EVENS;
        return null;
    }
    public static String skillName(Kind kind) {
        return kind == Kind.SORT_UNIQUE_ODDS ? "auto_odd_sorted" : "auto_even_sorted";
    }
    private static JsonArray vector(int[] data) {
        JsonArray array = new JsonArray();
        for (int value : data) array.add(value);
        return array;
    }
    private static JsonArray expected(Kind kind, int[] data) {
        TreeSet<Integer> sorted = new TreeSet<>();
        for (int value : data) {
            if ((value % 2 == 0) == (kind == Kind.SORT_UNIQUE_EVENS)) sorted.add(value);
        }
        JsonArray result = new JsonArray();
        for (int value : sorted) result.add(value);
        return result;
    }
    private static JsonObject example(Kind kind, int[] data) {
        JsonObject obj = new JsonObject();
        obj.add("in", vector(data));
        obj.add("out", expected(kind, data));
        return obj;
    }
    public static JsonObject request(Kind kind) {
        if (kind == null) throw new IllegalArgumentException("Necesidad no admitida.");
        JsonObject request = new JsonObject();
        request.addProperty("name", skillName(kind));
        JsonArray train = new JsonArray(), holdout = new JsonArray();
        for (int[] data : TRAIN) train.add(example(kind, data));
        for (int[] data : HOLDOUT) holdout.add(example(kind, data));
        request.add("train", train);
        request.add("holdout", holdout);
        return request;
    }
    public static final class Evaluation {
        public final String skillName;
        public final JsonArray program;
        public final int candidateSuccesses, baselineSuccesses, total;
        public final int candidatesChecked;
        private Evaluation(String name, JsonArray program, int passed, int baseline, int tested) {
            this.skillName = name; this.program = program.deepCopy();
            candidateSuccesses = passed; baselineSuccesses = baseline;
            total = BENCHMARK_CASES; candidatesChecked = tested;
        }
        public boolean promotable() {
            return candidateSuccesses == total && candidateSuccesses >= baselineSuccesses + 10;
        }
    }
    public static Evaluation evaluate(InductiveSkillLab lab, Kind kind, BooleanSupplier stopped) throws Exception {
        if (lab == null || kind == null) throw new IllegalArgumentException("Laboratorio o objetivo ausente.");
        BooleanSupplier cancel = stopped == null ? () -> false : stopped;
        JsonObject result = lab.preview(request(kind), cancel);
        if (!"READY_FOR_APPROVAL".equals(result.get("status").getAsString()))
            throw new IllegalArgumentException("No pasó el examen reservado; no hay propuesta.");
        JsonArray program = result.getAsJsonArray("program");
        int correct = 0, baseline = 0;
        Random novel = new Random(kind == Kind.SORT_UNIQUE_ODDS ? 0x51A1E7 : 0x51A1E8);
        for (int i = 0; i < BENCHMARK_CASES; i++) {
            if (cancel.getAsBoolean()) throw new IllegalStateException("Aprendizaje cancelado");
            int[] numbers = new int[5 + novel.nextInt(14)];
            for (int j = 0; j < numbers.length; j++) numbers[j] = novel.nextInt(201) - 100;
            if (numbers.length > 7) numbers[numbers.length - 1] = numbers[0];
            JsonArray expected = expected(kind, numbers);
            if (InductiveSkillLab.simulate(program, vector(numbers)).equals(expected)) correct++;
            int[] naive = Arrays.copyOf(numbers, numbers.length);
            Arrays.sort(naive);
            if (vector(naive).equals(expected)) baseline++;
        }
        return new Evaluation(skillName(kind), program, correct, baseline,
                result.get("candidates_tested").getAsInt());
    }

    /** Execute the same frozen candidate after consent; never tune on benchmark. */
    public static JsonObject promote(InductiveSkillLab lab, Kind kind, JsonArray frozen,
                                    BooleanSupplier stopped) throws Exception {
        Evaluation check = evaluate(lab, kind, stopped);
        if (!check.promotable() || !check.program.equals(frozen))
            throw new IllegalArgumentException("La candidata cambió o no supera la línea base.");
        JsonObject persisted = lab.learn(request(kind), stopped);
        if (!persisted.get("certified").getAsBoolean()
                || !persisted.getAsJsonArray("program").equals(frozen))
            throw new IllegalStateException("La certificación guardada no coincide.");
        return persisted;
    }
}
