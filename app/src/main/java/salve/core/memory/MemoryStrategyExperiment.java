package salve.core.memory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import salve.core.autonomy.VerifiedStrategyPolicy;
import salve.data.db.RecuerdoEntity;

/**
 * Laboratory-only adapter around Salve's EXISTING verified strategy learner.
 * No production call path, stored chat, auto-approval or policy duplication.
 *
 * Independently labeled cases and opaque SHA-256 fingerprints are required.
 * The policy remembers only context, trial/problem digests, pass and FTS reads.
 * It cannot infer truth from retrieved prose or a model's self-assessment.
 */
public final class MemoryStrategyExperiment {
    public static final String BASELINE = "BASELINE";
    public static final String EXACT_FAST_PATH = "EXACT_FAST_PATH";
    public static final String PERSONAL = "memory:personal";
    public static final String GENERAL = "memory:general";
    public static final List<String> CATALOG =
            Collections.unmodifiableList(Arrays.asList(BASELINE, EXACT_FAST_PATH));

    private final VerifiedStrategyPolicy policy;
    public MemoryStrategyExperiment(VerifiedStrategyPolicy existingPolicy) {
        if (existingPolicy == null) throw new IllegalArgumentException("Existing policy required");
        policy = existingPolicy;
    }

    /**
     * Gold labels must be made independently of both search implementations,
     * in a frozen local test database. Refuses missing labels or any failed
     * indexed read. Raw episode IDs are used transiently and NEVER persisted.
     */
    public void recordIndependentlyVerified(String context, String problemDigest,
            String trialDigest, String strategy, MemorySearchService.Result result,
            Set<Integer> independentlyLabeledGold, boolean independentlyVerified) {
        if (!independentlyVerified || result == null || independentlyLabeledGold == null)
            throw new IllegalArgumentException("Independent gold labels and verification required");
        if (!PERSONAL.equals(context) && !GENERAL.equals(context))
            throw new IllegalArgumentException("Unknown evaluation context");
        if (!CATALOG.contains(strategy) || independentlyLabeledGold.size() > 4
                || independentlyLabeledGold.contains(null))
            throw new IllegalArgumentException("Invalid bounded experiment");
        Set<Integer> actual = new HashSet<>();
        for (RecuerdoEntity row : result.records) {
            if (row == null || row.id <= 0) throw new IllegalArgumentException("Invalid memory evidence");
            actual.add(row.id);
        }
        // Strict equality deliberately rejects unsupported extra sources.
        boolean passed = !result.failed && result.records.size() <= 4
                && actual.size() == result.records.size()
                && actual.equals(independentlyLabeledGold);
        policy.observe(context, problemDigest, trialDigest, strategy, passed, result.indexReads);
    }

    /**
     * Recommendation is NOT activation: even a positive result needs the
     * frozen 30-case salve_eval_gate.py comparison, owner review, device
     * measurements and explicit deployment change. Default remains baseline.
     */
    public String candidateForManualReview(String context) {
        if (!PERSONAL.equals(context) && !GENERAL.equals(context))
            throw new IllegalArgumentException("Unknown evaluation context");
        return policy.compare(context, BASELINE, EXACT_FAST_PATH).eligible
                ? EXACT_FAST_PATH : BASELINE;
    }

    public VerifiedStrategyPolicy.Evidence evidence(String context) {
        if (!PERSONAL.equals(context) && !GENERAL.equals(context))
            throw new IllegalArgumentException("Unknown evaluation context");
        return policy.compare(context, BASELINE, EXACT_FAST_PATH);
    }

    /** Disable the experimental preference without editing user memories. */
    public void resetContext(String context) {
        if (!PERSONAL.equals(context) && !GENERAL.equals(context))
            throw new IllegalArgumentException("Unknown evaluation context");
        policy.forgetFamily(context);
    }
}
