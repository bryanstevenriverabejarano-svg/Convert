package salve.core.tasks;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResearchReceiptTest {
    @Test public void conversationalResultRetainsSourcesWithoutInventingAModelSynthesis() {
        salve.core.research.PublicResearchCoordinator coordinator = new salve.core.research.PublicResearchCoordinator(
                (query, stopped) -> new salve.core.WikipediaResearchClient.ResearchBatch(
                        salve.core.WikipediaResearchClient.Status.COMPLETE,
                        java.util.Collections.singletonList(new salve.core.WikipediaResearchClient.Page(
                                "https://example.org/topic", "Texto recuperado")), 0), null, 10500);
        ResearchReceipt receipt = ResearchReceipt.fromResult(coordinator.run("tema", () -> false), null);
        assertNotNull(receipt);
        assertEquals("SOURCES_ONLY", receipt.status);
        assertEquals("https://example.org/topic", receipt.sources.get(0).url);
        assertTrue(receipt.answer.contains("Texto recuperado"));
        assertTrue(receipt.providers.isEmpty());
        assertNull(ResearchReceipt.fromResult(coordinator.run("tema", () -> true), null));
    }

    @Test public void resultWithoutSourcesNeverCreatesAReceipt() {
        salve.core.research.PublicResearchCoordinator coordinator = new salve.core.research.PublicResearchCoordinator(
                (query, stopped) -> new salve.core.WikipediaResearchClient.ResearchBatch(
                        salve.core.WikipediaResearchClient.Status.EMPTY, java.util.Collections.emptyList(), 0), null, 10500);
        assertNull(ResearchReceipt.fromResult(coordinator.run("tema", () -> false), null));
    }
    public static ResearchReceipt evidence() {
        ResearchReceipt receipt = new ResearchReceipt(); receipt.status = "ANSWERED";
        receipt.answer = "Información recuperada [1].\n[1] https://example.org/topic";
        receipt.sources.add(new ResearchReceipt.Source("https://example.org/topic", "Extracto de la fuente"));
        receipt.providers.add("SYNTHESIZE: dolphin-3b · SUCCESS · 150 ms");
        return receipt;
    }
    @Test public void preservesEvidenceAndRealProviderThroughCheckpoint() {
        ResearchReceipt copy = ResearchReceipt.decode(evidence().encode());
        assertEquals("https://example.org/topic", copy.sources.get(0).url);
        assertTrue(copy.providers.get(0).contains("dolphin-3b"));
        assertEquals("SUCCEEDED", copy.completionStatus());
    }
    @Test public void noModelAndPartialEvidenceAreNotReportedAsFullSuccess() {
        ResearchReceipt receipt = evidence(); receipt.status = "SOURCES_ONLY";
        assertEquals("PARTIAL", ResearchReceipt.decode(receipt.encode()).completionStatus());
        receipt.status = "PARTIAL"; assertEquals("PARTIAL", receipt.completionStatus());
    }
    @Test public void refusesMissingEvidenceAndLocalDestinations() {
        ResearchReceipt receipt = evidence(); receipt.sources.clear();
        assertThrows(IllegalArgumentException.class, receipt::encode);
        ResearchReceipt local = evidence(); local.sources.get(0).url = "https://127.0.0.1/private";
        assertThrows(IllegalArgumentException.class, local::encode);
    }
    @Test public void rejectsFailureStatusAndOversizedCheckpoint() {
        ResearchReceipt receipt = evidence(); receipt.status = "MODEL_FAILED";
        assertThrows(IllegalArgumentException.class, receipt::encode);
        assertThrows(IllegalArgumentException.class, () -> ResearchReceipt.decode("x".repeat(64001)));
        assertThrows(IllegalArgumentException.class, () -> ResearchReceipt.decode("null"));
    }
}
