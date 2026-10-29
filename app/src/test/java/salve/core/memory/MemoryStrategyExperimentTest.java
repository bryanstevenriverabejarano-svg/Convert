package salve.core.memory;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import salve.core.autonomy.VerifiedStrategyPolicy;
import salve.data.db.RecuerdoDao;
import salve.data.db.RecuerdoEntity;

public class MemoryStrategyExperimentTest {
    private static String digest(int seed) {
        return String.format(java.util.Locale.ROOT, "%064x", seed);
    }
    private static Set<Integer> correct() {
        return new HashSet<>(Arrays.asList(1, 2, 3, 4));
    }
    private static final class FakeDao {
        int reads;
        boolean fail;
        boolean wrongCandidate;
        final RecuerdoDao dao = (RecuerdoDao) Proxy.newProxyInstance(
                RecuerdoDao.class.getClassLoader(), new Class<?>[]{RecuerdoDao.class},
                (proxy, method, args) -> {
                    if (!"buscarIndice".equals(method.getName()))
                        throw new UnsupportedOperationException(method.getName());
                    reads++;
                    if (fail) throw new IllegalStateException("never expose database errors");
                    String expression = (String) args[0];
                    List<RecuerdoEntity> out = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) {
                        RecuerdoEntity row = new RecuerdoEntity();
                        row.id = i;
                        row.frase = "orion lunar document " + i;
                        row.timestamp = i;
                        row.etiquetas = "[]";
                        out.add(row);
                    }
                    return out;
                });
    }
    private static MemorySearchService.Result run(FakeDao store,
                                                   MemorySearchService.Strategy strategy) {
        MemorySearchQuery q = MemorySearchQuery.parse("orion lunar");
        return new MemorySearchService(store.dao).searchWithRecovery(
                "orion lunar", q, true, 4, null, MemorySalience.When.ANY, strategy);
    }

    @Test public void laboratoryVariantReusesExactDaoAndNeverChangesDefaultSearch() {
        FakeDao store = new FakeDao();
        MemorySearchService service = new MemorySearchService(store.dao);
        MemorySearchQuery q = MemorySearchQuery.parse("orion lunar");
        MemorySearchService.Result standard = service.searchWithRecovery(
                "orion lunar", q, true, 4, null, MemorySalience.When.ANY);
        MemorySearchService.Result baseline = run(store, MemorySearchService.Strategy.BASELINE);
        MemorySearchService.Result fast = run(store, MemorySearchService.Strategy.EXACT_FAST_PATH);
        assertEquals(3, standard.indexReads);
        assertEquals(standard.indexReads, baseline.indexReads);
        assertEquals(1, fast.indexReads);
        assertEquals(4, fast.records.size());
        assertEquals(baseline.records.get(0).id, fast.records.get(0).id);
        assertFalse(fast.failed);
    }

    @Test public void threePairedVerifiedCasesMaySuggestButNotDeployCandidate() {
        VerifiedStrategyPolicy existing = new VerifiedStrategyPolicy();
        MemoryStrategyExperiment lab = new MemoryStrategyExperiment(existing);
        assertEquals(MemoryStrategyExperiment.BASELINE,
                lab.candidateForManualReview(MemoryStrategyExperiment.PERSONAL));
        for (int i = 1; i <= 3; i++) {
            String problem = digest(i), sameSnapshot = digest(200 + i);
            lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, problem, sameSnapshot,
                    MemoryStrategyExperiment.BASELINE,
                    run(new FakeDao(), MemorySearchService.Strategy.BASELINE), correct(), true);
            lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, problem, sameSnapshot,
                    MemoryStrategyExperiment.EXACT_FAST_PATH,
                    run(new FakeDao(), MemorySearchService.Strategy.EXACT_FAST_PATH), correct(), true);
        }
        assertEquals(3, lab.evidence(MemoryStrategyExperiment.PERSONAL).pairedProblems);
        assertEquals(MemoryStrategyExperiment.EXACT_FAST_PATH,
                lab.candidateForManualReview(MemoryStrategyExperiment.PERSONAL));
        // Ordinary production calls STILL use BASELINE, regardless of experiment state.
        assertEquals(3, run(new FakeDao(), MemorySearchService.Strategy.BASELINE).indexReads);
        assertEquals(MemoryStrategyExperiment.BASELINE,
                lab.candidateForManualReview(MemoryStrategyExperiment.GENERAL));
        lab.resetContext(MemoryStrategyExperiment.PERSONAL);
        assertEquals(MemoryStrategyExperiment.BASELINE,
                lab.candidateForManualReview(MemoryStrategyExperiment.PERSONAL));
        assertTrue(lab.evidence(MemoryStrategyExperiment.PERSONAL).pairedProblems == 0);
    }

    @Test public void unverifiedOrMismatchedGoldenLabelsCannotRecommendChanges() {
        MemoryStrategyExperiment lab = new MemoryStrategyExperiment(new VerifiedStrategyPolicy());
        MemorySearchService.Result candidate =
                run(new FakeDao(), MemorySearchService.Strategy.EXACT_FAST_PATH);
        try {
            lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, digest(1), digest(2),
                    MemoryStrategyExperiment.EXACT_FAST_PATH, candidate, correct(), false);
            fail("Model claims must never self-certify a memory result");
        } catch (IllegalArgumentException expected) { /* expected */ }
        for (int i = 1; i <= 3; i++) {
            lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, digest(i), digest(200 + i),
                    MemoryStrategyExperiment.BASELINE,
                    run(new FakeDao(), MemorySearchService.Strategy.BASELINE), correct(), true);
            // The evaluator deliberately marks an unsupported extra/missing source as failed.
            lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, digest(i), digest(200 + i),
                    MemoryStrategyExperiment.EXACT_FAST_PATH, candidate,
                    new HashSet<>(Arrays.asList(1, 2)), true);
        }
        assertEquals(MemoryStrategyExperiment.BASELINE,
                lab.candidateForManualReview(MemoryStrategyExperiment.PERSONAL));
    }

    @Test public void errorsDoNotBecomeSuccessAndReplayedProblemsAreNotExtraPairs() {
        MemoryStrategyExperiment lab = new MemoryStrategyExperiment(new VerifiedStrategyPolicy());
        FakeDao broken = new FakeDao(); broken.fail = true;
        MemorySearchService.Result failed = run(broken, MemorySearchService.Strategy.EXACT_FAST_PATH);
        assertTrue(failed.failed);
        lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, digest(1), digest(100),
                MemoryStrategyExperiment.EXACT_FAST_PATH, failed, new HashSet<>(), true);
        for (int i = 0; i < 5; i++) {
            lab.recordIndependentlyVerified(MemoryStrategyExperiment.PERSONAL, digest(1), digest(100),
                    MemoryStrategyExperiment.BASELINE,
                    run(new FakeDao(), MemorySearchService.Strategy.BASELINE), correct(), true);
        }
        assertEquals(1, lab.evidence(MemoryStrategyExperiment.PERSONAL).pairedProblems);
        assertEquals(MemoryStrategyExperiment.BASELINE,
                lab.candidateForManualReview(MemoryStrategyExperiment.PERSONAL));
    }

    @Test public void exactFastPathDoesNotSkipWhenEvidenceIsInsufficient() {
        FakeDao store = new FakeDao();
        MemorySearchService.Result outcome = new MemorySearchService(store.dao).searchWithRecovery(
                "orion", MemorySearchQuery.parse("orion"), true, 4, null,
                MemorySalience.When.ANY, MemorySearchService.Strategy.EXACT_FAST_PATH);
        assertEquals(2, outcome.indexReads);
        assertEquals(4, outcome.records.size());
    }
}
