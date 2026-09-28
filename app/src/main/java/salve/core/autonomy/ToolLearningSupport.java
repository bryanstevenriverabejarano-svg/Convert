package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import salve.core.autonomy.VerifiedStrategyPolicy;

/** Package-local bridge: experimental selection cannot replace the exact tool verifier. */
final class ToolLearningSupport {
    static final int PROBE_OPERATIONS = 100_000;
    private ToolLearningSupport() {}

    static VerifiedStrategyPolicy policy(JsonObject state) {
        if (!state.has("learningPolicy")) return new VerifiedStrategyPolicy();
        JsonElement raw = state.get("learningPolicy");
        if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Invalid learning journal");
        return VerifiedStrategyPolicy.decode(raw.getAsString());
    }
    static boolean enabled(JsonObject state) {
        if (!state.has("experimentalLearning")) return true;
        JsonElement flag = state.get("experimentalLearning");
        if (!flag.isJsonPrimitive() || !flag.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Invalid learning flag");
        return flag.getAsBoolean();
    }
    static String context(String family, JsonObject input) {
        int size;
        switch (family) {
            case "route": case "dependencies": size = input.get("nodes").getAsInt(); break;
            case "knapsack": size = input.getAsJsonArray("items").size(); break;
            case "schedule": size = input.getAsJsonArray("jobs").size(); break;
            default: throw new IllegalArgumentException("Unknown learning family");
        }
        String context = family + (size <= 7 ? ":small" : ":large");
        if (family.equals("route")) {
            boolean negative = false;
            for (JsonElement e : input.getAsJsonArray("edges"))
                if (e.getAsJsonArray().get(2).getAsInt() < 0) negative = true;
            context += negative ? ":negative" : ":nonnegative";
        }
        return context;
    }
    static String fingerprint(JsonElement data) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(canonical(data).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(Character.forDigit((b >>> 4) & 15, 16))
                    .append(Character.forDigit(b & 15, 16));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
    private static String canonical(JsonElement data) {
        if (data == null || data.isJsonNull()) return "null";
        if (data.isJsonPrimitive()) return data.toString();
        StringBuilder out = new StringBuilder();
        if (data.isJsonArray()) {
            out.append('[');
            for (JsonElement item : data.getAsJsonArray()) out.append(canonical(item)).append(',');
            return out.append(']').toString();
        }
        List<String> keys = new ArrayList<>(data.getAsJsonObject().keySet());
        Collections.sort(keys);
        out.append('{');
        for (String key : keys) out.append(new com.google.gson.JsonPrimitive(key))
                .append(':').append(canonical(data.getAsJsonObject().get(key))).append(',');
        return out.append('}').toString();
    }
    static String trialId(JsonObject input, JsonArray replay) {
        JsonArray paired = new JsonArray(); paired.add(input);
        paired.add(replay == null ? new JsonArray() : replay);
        return fingerprint(paired);
    }
    static final class Trial {
        final JsonObject result;
        final boolean passed;
        final String feedback;
        final int replayChecks;
        Trial(JsonObject result, boolean passed, String feedback, int checks) {
            this.result = result; this.passed = passed; this.feedback = feedback; this.replayChecks = checks;
        }
    }
    /** Exactly the same evaluator is used for production candidates and shadow probes. */
    static Trial evaluate(JsonObject program, JsonObject input, JsonArray replay, OperationBudget budget) {
        ToolInterpreter.Execution execution = ToolInterpreter.run(program, input, budget);
        boolean passed = execution.verification.passed;
        String feedback = execution.verification.summary;
        int checks = 0;
        if (passed && replay != null) {
            for (JsonElement old : replay) {
                if (old.equals(input)) continue;
                ToolInterpreter.Execution regression = ToolInterpreter.run(program, old.getAsJsonObject(), budget);
                checks++;
                if (!regression.verification.passed) {
                    passed = false;
                    feedback = "El candidato falla una regresión previamente verificada";
                    break;
                }
            }
        }
        return new Trial(execution.result, passed, feedback, checks);
    }
}
