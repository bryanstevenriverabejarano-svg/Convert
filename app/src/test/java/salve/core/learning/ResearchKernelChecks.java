package salve.core.learning;

import salve.core.autonomy.VerifiedStrategyPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import salve.core.memory.MemoryEvidenceRanker;

/** Dependency-free invariant checks; also called by JUnit in Android CI. */
public final class ResearchKernelChecks {
    private static int checks;
    private static String hash(int n) { return String.format(java.util.Locale.ROOT, "%064x", n); }
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void rejects(Runnable action) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected invalid state to be rejected");
    }
    private static VerifiedStrategyPolicy paired(int count, int oldCost, int newCost) {
        VerifiedStrategyPolicy p = new VerifiedStrategyPolicy();
        for (int i = 0; i < count; i++) {
            p.observe("route:small", hash(i), hash(100 + i), "OLD", true, oldCost);
            p.observe("route:small", hash(i), hash(100 + i), "NEW", true, newCost);
        }
        return p;
    }
    public static void main(String[] args) {
        checks = 0;
        List<String> catalog = Arrays.asList("OLD", "NEW");
        check(paired(2, 100, 50).order("route:small", catalog).equals(catalog), "Insufficient samples");
        VerifiedStrategyPolicy p = paired(3, 100, 90);
        check(p.order("route:small", catalog).get(0).equals("NEW"), "Paired gain must change ordering");
        check(p.compare("route:small", "OLD", "NEW").pairedProblems == 3, "Pair count");
        check(p.order("route:large", catalog).equals(catalog), "Context isolation");
        check(p.order("route:small", Arrays.asList("OLD", "OTHER")).get(0).equals("OLD"), "Catalog boundary");
        check(paired(3, 100, 91).order("route:small", catalog).equals(catalog), "Ten percent gate");
        check(paired(3, 0, 0).order("route:small", catalog).equals(catalog), "Zero cost is not a gain");
        p.observe("route:small", hash(9), hash(9), "NEW", false, 1);
        check(p.order("route:small", catalog).equals(catalog), "Any retained failure blocks promotion");
        p = paired(3, 100, 50);
        p.observe("route:small", hash(0), hash(100), "NEW", true, 101);
        check(p.order("route:small", catalog).equals(catalog), "Per-problem regression blocks mean gain");
        p = paired(3, 100, 50);
        p.observe("route:small", hash(0), hash(999), "NEW", true, 1);
        check(p.order("route:small", catalog).equals(catalog), "Unpaired replay snapshots");
        p = paired(1, 100, 10);
        for (int i = 0; i < 20; i++) p.observe("route:small", hash(0), hash(100), "NEW", true, 1);
        check(p.size() == 2 && !p.compare("route:small", "OLD", "NEW").eligible, "No repeated sample inflation");
        p = paired(3, 100, 50);
        check(VerifiedStrategyPolicy.decode(p.encode()).encode().equals(p.encode()), "Exact round trip");
        VerifiedStrategyPolicy copy = p.copy();
        p.forgetFamily("route");
        check(p.size() == 0 && copy.size() == 6, "Copy and rollback isolation");
        String encoded = copy.encode();
        rejects(() -> VerifiedStrategyPolicy.decode(encoded.substring(0, encoded.length() - 1)));
        rejects(() -> VerifiedStrategyPolicy.decode(encoded.replace("/1", "/2")));
        rejects(() -> VerifiedStrategyPolicy.decode(encoded + encoded.split("\n")[1] + "\n"));
        rejects(() -> VerifiedStrategyPolicy.decode(encoded.replace("\t100\n", "\t01\n")));
        rejects(() -> new VerifiedStrategyPolicy().observe("bad\nctx", hash(0), hash(0), "X", true, 1));
        rejects(() -> new VerifiedStrategyPolicy().observe("ctx", "short", hash(0), "X", true, 1));
        rejects(() -> new VerifiedStrategyPolicy().observe("ctx", hash(0), hash(0), "X", true, -1));
        rejects(() -> new VerifiedStrategyPolicy().order("ctx", Arrays.asList("A", "A")));
        p = new VerifiedStrategyPolicy();
        for (int i = 0; i < 140; i++) p.observe("route:small", hash(i), hash(i), "OLD", true, i);
        check(p.size() == VerifiedStrategyPolicy.MAX_OBSERVATIONS, "Bounded journal");
        check(p.encode().length() < VerifiedStrategyPolicy.MAX_ENCODED_CHARS, "Bounded encoding");
        check(VerifiedStrategyPolicy.decode(p.encode()).size() == p.size(), "Bounded restoration");
        p = new VerifiedStrategyPolicy();
        check("NEW".equals(p.probe("ctx", hash(1), catalog, "OLD")), "Probe alternative");
        p.observe("ctx", hash(1), hash(1), "NEW", true, 10);
        check(p.probe("ctx", hash(1), catalog, "OLD") == null, "No repeat probe");
        for (int i = 0; i < 3; i++) p.observe("ctx", hash(i + 10), hash(i + 10), "NEW", false, 10);
        check(p.probe("ctx", hash(20), catalog, "OLD") == null, "Stop unsuitable probes");
        p.observe("other:small", hash(0), hash(0), "OLD", true, 1);
        p.forgetFamily("ctx");
        check(p.size() == 1, "Rollback only affected family");

        List<MemoryEvidenceRanker.Candidate> memories = Arrays.asList(
            new MemoryEvidenceRanker.Candidate(1, "Salve conversación", 100),
            new MemoryEvidenceRanker.Candidate(2, "Salve saludo", 90),
            new MemoryEvidenceRanker.Candidate(3, "Salve avatar", 80),
            new MemoryEvidenceRanker.Candidate(4, "Salve memoria", 70),
            new MemoryEvidenceRanker.Candidate(5, "Salve pCloud copia verificada", 1));
        check(MemoryEvidenceRanker.rank(memories, Arrays.asList("Salve", "pCloud"), 4).get(0) == 5,
                "Full-query evidence outranks first keyword and recency");
        check(MemoryEvidenceRanker.rank(memories, Arrays.asList("Salve", "salve", "pCloud"), 1).get(0) == 5,
                "Duplicate terms do not inflate scores");
        check(MemoryEvidenceRanker.rank(memories, Collections.singletonList("missing"), 4).isEmpty(), "No invented hits");
        check(MemoryEvidenceRanker.rank(memories, Collections.singletonList("Salve"), 0).isEmpty(), "Zero budget");
        List<MemoryEvidenceRanker.Candidate> accents = Arrays.asList(
            new MemoryEvidenceRanker.Candidate(1, "Conciencia artificial", 1),
            new MemoryEvidenceRanker.Candidate(2, "Concienciacion y artificiales", 2),
            new MemoryEvidenceRanker.Candidate(3, "CONCIÉNCIA ARTIFICIAL", 3));
        check(MemoryEvidenceRanker.rank(accents, Arrays.asList("conciencia", "artificial"), 4).equals(Arrays.asList(3, 1)),
                "Case, diacritics and word boundaries");
        check(MemoryEvidenceRanker.rank(Arrays.asList(new MemoryEvidenceRanker.Candidate(1, "pCloud sí", 1),
                new MemoryEvidenceRanker.Candidate(2, "pCloud no", 2)), Collections.singletonList("pCloud"), 4)
                .equals(Arrays.asList(2, 1)), "Do not merge contradictory records");
        check(MemoryEvidenceRanker.rank(Arrays.asList(null, new MemoryEvidenceRanker.Candidate(1, null, 1)),
                Collections.singletonList("Salve"), 4).isEmpty(), "Missing text is not evidence");
        check(MemoryEvidenceRanker.rank(Arrays.asList(memories.get(0), memories.get(0)),
                Collections.singletonList("Salve"), 4).equals(Collections.singletonList(1)), "Unique source IDs");
        rejects(() -> MemoryEvidenceRanker.rank(Collections.nCopies(33, memories.get(0)), Collections.singletonList("x"), 1));
        rejects(() -> MemoryEvidenceRanker.rank(memories, Collections.singletonList("x"), -1));
        for (int n = 1; n <= 200; n++) {
            VerifiedStrategyPolicy generated = paired(3, n * 10, n * 9);
            check(generated.order("route:small", catalog).get(0).equals("NEW"), "Generated threshold case " + n);
            check(generated.order("isolated", catalog).equals(catalog), "Generated context isolation " + n);
            check(VerifiedStrategyPolicy.decode(generated.encode()).compare("route:small", "OLD", "NEW").eligible,
                    "Generated restart " + n);
        }
        System.out.println("Research kernel checks passed: " + checks + " assertions (synthetic invariants, not an LLM benchmark).");
    }
}
