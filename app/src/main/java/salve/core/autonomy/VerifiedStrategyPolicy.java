package salve.core.autonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Experimental, bounded online policy search. Only paired, independently verified
 * executions can change the preferred strategy. This is not neural training.
 * The caller owns verification, cancellation, durable transactions and permissions.
 */
public final class VerifiedStrategyPolicy {
    public static final int MAX_OBSERVATIONS = 96;
    public static final int MIN_PAIRED_PROBLEMS = 3;
    public static final int MAX_ENCODED_CHARS = 40_000;
    private final List<Observation> observations = new ArrayList<>();

    private static final class Observation {
        final String context, problem, trial, strategy;
        final boolean passed;
        final int operations;
        Observation(String context, String problem, String trial, String strategy,
                    boolean passed, int operations) {
            if (!identifier(context) || !identifier(strategy) || !digest(problem) || !digest(trial)
                    || operations < 0 || operations > 20_000_000)
                throw new IllegalArgumentException("Invalid experimental observation");
            this.context = context; this.problem = problem; this.trial = trial;
            this.strategy = strategy; this.passed = passed; this.operations = operations;
        }
        String key() { return context + ":" + problem + ":" + strategy; }
        String encode() { return context + "\t" + problem + "\t" + trial + "\t" + strategy
                + "\t" + (passed ? "1" : "0") + "\t" + operations; }
    }

    public static final class Evidence {
        public final int pairedProblems;
        public final long incumbentOperations, candidateOperations;
        public final boolean eligible;
        private Evidence(int pairs, long incumbent, long candidate, boolean eligible) {
            this.pairedProblems = pairs; this.incumbentOperations = incumbent;
            this.candidateOperations = candidate; this.eligible = eligible;
        }
    }

    private static boolean identifier(String s) {
        return s != null && s.matches("[A-Za-z0-9_:-]{1,80}");
    }
    private static boolean digest(String s) { return s != null && s.matches("[a-f0-9]{64}"); }

    /** Repeated identical problems replace observations; they do not inflate sample count. */
    public void observe(String context, String problem, String trial, String strategy,
                        boolean passed, int operations) {
        Observation fresh = new Observation(context, problem, trial, strategy, passed, operations);
        observations.removeIf(old -> old.key().equals(fresh.key()));
        observations.add(fresh);
        while (observations.size() > MAX_OBSERVATIONS) observations.remove(0);
    }

    /**
     * A candidate needs three distinct paired problems under the exact same replay
     * snapshot, no observed failures in this context, no per-problem cost regression,
     * and at least a 10% reduction in instrumented operations in aggregate.
     * This conservative gate is an engineering threshold, not a significance test.
     */
    public Evidence compare(String context, String incumbent, String candidate) {
        Map<String, Observation> baseline = new LinkedHashMap<>();
        for (Observation o : observations)
            if (o.context.equals(context) && o.strategy.equals(incumbent)) baseline.put(o.problem, o);
        int pairs = 0;
        long oldCost = 0, newCost = 0;
        boolean regression = false;
        for (Observation o : observations) {
            if (!o.context.equals(context) || !o.strategy.equals(candidate)) continue;
            if (!o.passed) regression = true;
            Observation old = baseline.get(o.problem);
            if (old == null || !old.passed || !o.passed || !old.trial.equals(o.trial)) continue;
            pairs++;
            oldCost += old.operations; newCost += o.operations;
            if (o.operations > old.operations) regression = true;
        }
        boolean eligible = !incumbent.equals(candidate) && !regression
                && pairs >= MIN_PAIRED_PROBLEMS && oldCost > 0 && newCost * 10 <= oldCost * 9;
        return new Evidence(pairs, oldCost, newCost, eligible);
    }

    /** Never introduces a strategy that was not in the caller's executable catalog. */
    public List<String> order(String context, List<String> baseline) {
        checkCatalog(context, baseline);
        List<String> ordered = new ArrayList<>(baseline);
        String incumbent = baseline.get(0), best = incumbent;
        double bestRatio = 1.0;
        for (String candidate : baseline) {
            Evidence evidence = compare(context, incumbent, candidate);
            if (!evidence.eligible) continue;
            double ratio = (double) evidence.candidateOperations / evidence.incumbentOperations;
            if (ratio < bestRatio) { best = candidate; bestRatio = ratio; }
        }
        ordered.remove(best); ordered.add(0, best);
        return Collections.unmodifiableList(ordered);
    }

    /** Select one counterfactual probe per solve, least-sampled first; do not repeat a retained strategy/problem pair. */
    public String probe(String context, String problem, List<String> catalog, String selected) {
        checkCatalog(context, catalog);
        if (!digest(problem) || !catalog.contains(selected)) throw new IllegalArgumentException("Invalid probe");
        String best = null;
        int minimum = Integer.MAX_VALUE;
        for (String candidate : catalog) {
            if (candidate.equals(selected)) continue;
            int count = 0, failures = 0;
            boolean already = false;
            for (Observation o : observations) {
                if (!o.context.equals(context) || !o.strategy.equals(candidate)) continue;
                count++;
                if (!o.passed) failures++;
                if (o.problem.equals(problem)) already = true;
            }
            // Stop repeatedly exploring a demonstrably unsuitable strategy in this context.
            if (already || failures >= 3) continue;
            if (count < minimum) { minimum = count; best = candidate; }
        }
        return best;
    }

    private static void checkCatalog(String context, List<String> catalog) {
        if (!identifier(context) || catalog == null || catalog.isEmpty() || catalog.size() > 4)
            throw new IllegalArgumentException("Invalid strategy catalog");
        Set<String> unique = new HashSet<>();
        for (String strategy : catalog)
            if (!identifier(strategy) || !unique.add(strategy)) throw new IllegalArgumentException("Invalid strategy");
    }

    public int size() { return observations.size(); }
    public VerifiedStrategyPolicy copy() { return decode(encode()); }
    public void forgetFamily(String family) {
        if (!identifier(family)) throw new IllegalArgumentException("Invalid family");
        observations.removeIf(o -> o.context.equals(family) || o.context.startsWith(family + ":"));
    }
    public String encode() {
        StringBuilder out = new StringBuilder("salve-strategy-policy/1\n");
        for (Observation o : observations) out.append(o.encode()).append('\n');
        return out.toString();
    }
    public static VerifiedStrategyPolicy decode(String raw) {
        if (raw == null || raw.length() > MAX_ENCODED_CHARS || !raw.endsWith("\n"))
            throw new IllegalArgumentException("Invalid policy state");
        String[] lines = raw.split("\n", -1);
        if (!lines[0].equals("salve-strategy-policy/1") || lines.length - 2 > MAX_OBSERVATIONS)
            throw new IllegalArgumentException("Unsupported policy state");
        VerifiedStrategyPolicy policy = new VerifiedStrategyPolicy();
        Set<String> unique = new HashSet<>();
        for (int i = 1; i < lines.length - 1; i++) {
            String[] f = lines[i].split("\t", -1);
            if (f.length != 6 || !(f[4].equals("0") || f[4].equals("1"))
                    || !f[5].matches("0|[1-9][0-9]{0,7}")) throw new IllegalArgumentException("Invalid observation");
            Observation o = new Observation(f[0], f[1], f[2], f[3], f[4].equals("1"), Integer.parseInt(f[5]));
            if (!unique.add(o.key())) throw new IllegalArgumentException("Duplicate observation");
            policy.observations.add(o);
        }
        return policy;
    }
}
