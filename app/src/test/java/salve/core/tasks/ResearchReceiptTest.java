package salve.core.tasks;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResearchReceiptTest {
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
