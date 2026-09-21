package salve.core;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

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
        assertTrue(json.contains("\"schemaVersion\": 3"));
        assertTrue(json.contains("\"proposalId\""));
        assertTrue(json.contains("\"targetPath\": \"app/src/main/java/salve/core/Example.java\""));
    }

    @Test
    public void proposalRequiresAllGatesBeforePullRequest() {
        assertTrue(proposal(true, true, true).isReadyForPullRequest());
        assertFalse(proposal(false, true, true).isReadyForPullRequest());
        assertFalse(proposal(true, false, true).isReadyForPullRequest());
        assertFalse(proposal(true, true, false).isReadyForPullRequest());
    }

    @Test public void sourceIdentityIsSerializedTogetherAndCannotBePartial() throws Exception {
        AutoImprovementProposalStore.Proposal bound = new AutoImprovementProposalStore.Proposal(1, "Example",
                "app/src/main/java/salve/core/Example.java", "issue", "diff --git a/x b/x", "tests",
                true, false, false, true, "a".repeat(40), "b".repeat(64));
        File directory = Files.createTempDirectory("salve-source-proposal").toFile();
        File output = new AutoImprovementProposalStore(directory).enqueue(bound);
        try {
            String json = Files.readString(output.toPath());
            assertTrue(json.contains("\"sourceRevision\": \"" + "a".repeat(40) + "\""));
            assertTrue(json.contains("\"sourceSha256\": \"" + "b".repeat(64) + "\""));
        } finally { output.delete(); directory.delete(); }
        assertThrows(IllegalArgumentException.class, () -> new AutoImprovementProposalStore.Proposal(1, "Example",
                "path", "issue", "diff", "tests", true, false, false, true, "a".repeat(40), null));
    }

    private AutoImprovementProposalStore.Proposal proposal(boolean sandboxPassed,
                                                            boolean ethicalReviewPassed,
                                                            boolean syntheticPassed) {
        return new AutoImprovementProposalStore.Proposal(
                1L,
                "salve.core.Example",
                "app/src/main/java/salve/core/Example.java",
                "Simplificar método",
                "diff --git a/app/src/main/java/salve/core/Example.java b/app/src/main/java/salve/core/Example.java\n",
                "@Test public void example() {}",
                syntheticPassed,
                true,
                sandboxPassed,
                ethicalReviewPassed
        );
    }
}
