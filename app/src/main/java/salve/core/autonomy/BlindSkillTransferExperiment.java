package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

/**
 * Evaluation harness for a skill the synthesizer has not been told by name.
 * Test authority owns the independent oracle; the learner only receives
 * demonstration pairs. Development probes may be retried. Final transfer
 * inputs and oracle labels are sampled only AFTER the program is frozen and
 * are never used for retrying or installing a skill.
 */
public final class BlindSkillTransferExperiment {
    public interface Oracle { JsonArray expected(JsonArray input); }
    public interface Sampler { JsonArray sample(Random random, boolean shifted); }

    public static final int MAX_RETRIES = 4;
    public static final int FINAL_CASES = 96;
    private static final int INITIAL_TRAIN = 6;
    private static final int EXTRA_TRAIN = 6;
    private static final int DEV_CASES = 6;

    private BlindSkillTransferExperiment() { }

    public static final class Result {
        public final String status;
        public final int attempts, developmentFailures, finalCorrect, baselineCorrect;
        public final JsonArray frozenProgram;
        private Result(String status, int attempts, int failed, int correct, int baseline,
                       JsonArray program) {
            this.status = status; this.attempts = attempts;
            developmentFailures = failed; finalCorrect = correct;
            baselineCorrect = baseline;
            frozenProgram = program == null ? null : program.deepCopy();
        }
        public JsonObject report() {
            JsonObject data = new JsonObject();
            data.addProperty("status", status);
            data.addProperty("attempts", attempts);
            data.addProperty("development_failures", developmentFailures);
            data.addProperty("final_correct", finalCorrect);
            data.addProperty("final_total", FINAL_CASES);
            data.addProperty("baseline_correct", baselineCorrect);
            data.addProperty("training_budget", INITIAL_TRAIN + EXTRA_TRAIN * (MAX_RETRIES - 1));
            if (frozenProgram != null) data.add("frozen_program", frozenProgram.deepCopy());
            data.addProperty("skill_installed", false);
            data.addProperty("final_exam_reused_for_training", false);
            return data;
        }
    }

    public static Result examine(Oracle oracle, Sampler sampler, long seed,
                                 BooleanSupplier stopped) throws Exception {
        if (oracle == null || sampler == null) throw new IllegalArgumentException("Falta oráculo o muestreador");
        BooleanSupplier cancel = stopped == null ? () -> false : stopped;
        // All learning is inside an isolated, ephemeral registry: never install by experiment.
        InductiveSkillLab lab = new InductiveSkillLab(new InductiveSkillLab.Store() {
            @Override public String read() { return null; }
            @Override public void write(String unused) { throw new IllegalStateException("El examen no instala habilidades"); }
        });
        Random learning = new Random(seed ^ 0x6A0F55L);
        Set<String> used = new HashSet<>();
        JsonArray train = new JsonArray();
        addExamples(train, INITIAL_TRAIN, false, learning, oracle, sampler, used, cancel);
        int failures = 0;
        JsonArray frozen = null;
        int attempts = 0;
        for (int trial = 1; trial <= MAX_RETRIES; trial++) {
            check(cancel);
            attempts = trial;
            JsonArray dev = new JsonArray();
            addExamples(dev, DEV_CASES, false, learning, oracle, sampler, used, cancel);
            JsonObject request = new JsonObject();
            request.addProperty("name", "blind_transfer_experiment");
            request.add("train", train.deepCopy());
            request.add("holdout", dev);
            JsonObject result = lab.preview(request, cancel);
            if ("READY_FOR_APPROVAL".equals(result.get("status").getAsString())) {
                frozen = result.getAsJsonArray("program").deepCopy();
                break;
            }
            failures++;
            if (trial < MAX_RETRIES) {
                // NEW demonstrations only; no development exam labels enter train.
                addExamples(train, EXTRA_TRAIN, false, learning, oracle, sampler, used, cancel);
            }
        }
        if (frozen == null) return new Result("DEVELOPMENT_FAILED", attempts, failures, 0, 0, null);

        // Final sealed distribution is selected only after candidate freeze.
        // Neither its inputs nor oracle's labels can enter another attempt.
        Random finalRandom = new Random(seed ^ 0x7F1A6F6D243B5L);
        int correct = 0, baseline = 0;
        for (int index = 0; index < FINAL_CASES; index++) {
            check(cancel);
            JsonArray input = uniqueSample(sampler, finalRandom, true, used);
            JsonArray truth = checkedOracle(oracle, input);
            if (truth.equals(input)) baseline++;
            try {
                if (InductiveSkillLab.simulate(frozen, input).equals(truth)) correct++;
            } catch (IllegalArgumentException failure) {
                // Failed application counts as an error; no guess or retry.
            }
        }
        String status = correct == FINAL_CASES && correct >= baseline + 12
                ? "TRANSFER_VERIFIED" : "TRANSFER_FAILED";
        return new Result(status, attempts, failures, correct, baseline, frozen);
    }

    private static void addExamples(JsonArray out, int count, boolean shifted, Random random,
                                    Oracle oracle, Sampler sampler, Set<String> used,
                                    BooleanSupplier cancel) {
        for (int i = 0; i < count; i++) {
            check(cancel);
            JsonArray input = uniqueSample(sampler, random, shifted, used);
            JsonObject example = new JsonObject();
            example.add("in", input);
            example.add("out", checkedOracle(oracle, input));
            out.add(example);
        }
    }
    private static JsonArray uniqueSample(Sampler sampler, Random random, boolean shifted, Set<String> used) {
        for (int i = 0; i < 512; i++) {
            JsonArray sample = sampler.sample(random, shifted);
            if (sample == null || sample.size() > 24) throw new IllegalArgumentException("Muestra inválida");
            String key = sample.toString();
            if (used.add(key)) return sample.deepCopy();
        }
        throw new IllegalArgumentException("No hay muestras inéditas suficientes");
    }
    private static JsonArray checkedOracle(Oracle oracle, JsonArray input) {
        JsonArray out = oracle.expected(input.deepCopy());
        if (out == null || out.size() > 24) throw new IllegalArgumentException("Resultado-oráculo inválido");
        return out.deepCopy();
    }
    private static void check(BooleanSupplier stop) {
        if (stop.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new IllegalStateException("Experimento cancelado");
    }

    /** Reproducible input structure, with a distinct shifted final distribution. */
    public static final Sampler NUMBER_LISTS = (random, shifted) -> {
        int length = shifted ? 12 + random.nextInt(13) : 4 + random.nextInt(6);
        JsonArray array = new JsonArray();
        for (int i = 0; i < length; i++) {
            int bound = shifted ? 71 : 25;
            array.add(random.nextInt(2 * bound + 1) - bound);
        }
        if (length > 6) array.set(length - 1, array.get(0).deepCopy());
        return array;
    };

    /** Independent Java oracle families, not the synthesize() DSL evaluator. */
    public enum Challenge { ABS_SORT_UNIQUE, SQUARE_SORT_UNIQUE,
        NEGATE_SORT_UNIQUE, EVEN_UNIQUE_REVERSE, ODD_SQUARE_SORT,
        PLUS_ONE_EVEN_SORT, MINUS_ONE_ODD_SORT }

    public static Oracle independentOracle(Challenge challenge) {
        if (challenge == null) throw new IllegalArgumentException("No hay reto");
        return data -> {
            List<Integer> in = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) in.add(data.get(i).getAsInt());
            List<Integer> transformed = new ArrayList<>();
            switch (challenge) {
                case ABS_SORT_UNIQUE:
                case SQUARE_SORT_UNIQUE:
                case NEGATE_SORT_UNIQUE: {
                    TreeSet<Integer> sorted = new TreeSet<>();
                    for (int n : in) sorted.add(challenge == Challenge.ABS_SORT_UNIQUE
                            ? Math.abs(n) : challenge == Challenge.SQUARE_SORT_UNIQUE
                            ? n * n : -n);
                    transformed.addAll(sorted);
                    break;
                }
                case EVEN_UNIQUE_REVERSE: {
                    Set<Integer> distinct = new java.util.LinkedHashSet<>();
                    for (int n : in) if (n % 2 == 0) distinct.add(n);
                    transformed.addAll(distinct);
                    java.util.Collections.reverse(transformed);
                    break;
                }
                case ODD_SQUARE_SORT:
                    for (int n : in) if (n % 2 != 0) transformed.add(n * n);
                    java.util.Collections.sort(transformed);
                    break;
                case PLUS_ONE_EVEN_SORT:
                    for (int n : in) if ((n + 1) % 2 == 0) transformed.add(n + 1);
                    java.util.Collections.sort(transformed);
                    break;
                case MINUS_ONE_ODD_SORT:
                    for (int n : in) if ((n - 1) % 2 != 0) transformed.add(n - 1);
                    java.util.Collections.sort(transformed);
                    break;
                default: throw new IllegalArgumentException("Reto desconocido");
            }
            JsonArray result = new JsonArray();
            for (int n : transformed) result.add(n);
            return result;
        };
    }

    /** The task is selected at runtime, after compilation and CI test authoring. */
    public static Result runNewChallenge(BooleanSupplier stopped) throws Exception {
        SecureRandom random = new SecureRandom();
        Challenge[] options = Challenge.values();
        Challenge chosen = options[random.nextInt(options.length)];
        return examine(independentOracle(chosen), NUMBER_LISTS, random.nextLong(), stopped);
    }
}
