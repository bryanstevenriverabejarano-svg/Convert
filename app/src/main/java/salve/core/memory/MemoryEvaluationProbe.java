package salve.core.memory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Opt-in local measurement adapter for the existing ConversationMemoryGrounding
 * route. Never starts a search, retains history, stores a key, or writes a log.
 * Its returned observation is deliberately NOT a verified test receipt: human-
 * labeled gold answers and cited sources must be supplied independently before
 * using scripts/salve_eval_gate.py.
 */
public final class MemoryEvaluationProbe {
    private MemoryEvaluationProbe() { }

    public static final class Observation {
        public final String status;
        public final long durationMs;
        public final List<String> retrieved;
        private Observation(String status, long durationMs, List<String> retrieved) {
            this.status = status;
            this.durationMs = Math.max(0L, durationMs);
            this.retrieved = Collections.unmodifiableList(new ArrayList<>(retrieved));
        }
        /** No user text, source IDs, memory content, labels or raw key. */
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("status", status);
            json.addProperty("duration_ms", durationMs);
            JsonArray ids = new JsonArray();
            for (String id : retrieved) ids.add(id);
            json.add("retrieved", ids);
            json.addProperty("human_verified", false);
            json.addProperty("requires_independent_gold_and_citations", true);
            return json;
        }
    }

    /**
     * Call only after an explicitly authorized, local evaluation retrieval.
     * The caller must verify private-memory access BEFORE invoking retrieval.
     * Use the SAME random 32+ byte test-only key for both frozen revisions.
     * Never export the key, user prompts or raw Room identifiers.
     */
    public static Observation capture(ConversationMemoryGrounding.Result result,
                                      long elapsedNanos, byte[] localSecret) {
        if (localSecret == null || localSecret.length < 32)
            throw new IllegalArgumentException("Local evaluation HMAC key must contain 32+ bytes");
        if (result == null)
            return new Observation("ERROR", elapsedNanos / 1_000_000L, Collections.emptyList());

        String status;
        switch (result.getStatus()) {
            case FOUND: status = result.emittedMemoryIdsForEvaluation().isEmpty() ? "EMPTY" : "OK"; break;
            case EMPTY: status = "EMPTY"; break;
            case PARTIAL: status = "PARTIAL"; break;
            default: status = "ERROR"; break;
        }

        // Missing local records while pCloud is still restoring is UNKNOWN,
        // never an established empty personal history. Do not alter the
        // application's regular conversational status for this measurement.
        if (result.cloudIncompleteForEvaluation() && !"ERROR".equals(status))
            status = "PARTIAL";
        // Graph nodes and incomplete reads never count as episodic evidence.
        if (!"OK".equals(status))
            return new Observation(status, elapsedNanos / 1_000_000L, Collections.emptyList());

        Set<String> ids = new LinkedHashSet<>();
        for (Integer id : result.emittedMemoryIdsForEvaluation()) {
            if (id == null || id <= 0) continue;
            ids.add(hmacId(id, localSecret));
            if (ids.size() >= 4) break;
        }
        return new Observation(status, elapsedNanos / 1_000_000L, new ArrayList<>(ids));
    }

    private static String hmacId(int id, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(("salve-memory-evaluation-v1:recuerdos:" + id)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                int b = digest[i] & 0xff;
                hex.append(Character.forDigit(b >>> 4, 16))
                        .append(Character.forDigit(b & 0x0f, 16));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Local memory evaluation unavailable");
        }
    }
}
