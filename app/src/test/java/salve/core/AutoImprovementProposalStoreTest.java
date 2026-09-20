package salve.core;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutoImprovementProposalStoreTest {

    @Test
    public void enqueueWritesCompleteJsonAtomically() throws Exception {
        File outbox = Files.createTempDirectory("salve-proposals").toFile();
        AutoImprovementProposalStore store = new AutoImprovementProposalStore(outbox);
        AutoImprovementProposalStore.Proposal proposal = proposal(true, true, true);

        File saved = store.enqueue(proposal);

        assertTrue(saved.getName().endsWith(".json"));
        assertFalse(new File(saved.getAbsolutePath() + ".tmp").exists());
        String json = new String(Files.readAllBytes(saved.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"targetClass\": \"salve.core.Example\""));
        assertTrue(json.contains("\"patch\": \"class Example {}\""));
    }

    @Test
    public void proposalRequiresAllGatesBeforePullRequest() {
        assertTrue(proposal(true, true, true).isReadyForPullRequest());
        assertFalse(proposal(false, true, true).isReadyForPullRequest());
        assertFalse(proposal(true, false, true).isReadyForPullRequest());
        assertFalse(proposal(true, true, false).isReadyForPullRequest());
    }

    private AutoImprovementProposalStore.Proposal proposal(boolean sandboxPassed,
                                                            boolean ethicalReviewPassed,
                                                            boolean syntheticPassed) {
        return new AutoImprovementProposalStore.Proposal(
                1L,
                "salve.core.Example",
                "Simplificar método",
                "class Example {}",
                "@Test public void example() {}",
                syntheticPassed,
                true,
                sandboxPassed,
                ethicalReviewPassed
        );
    }
}
