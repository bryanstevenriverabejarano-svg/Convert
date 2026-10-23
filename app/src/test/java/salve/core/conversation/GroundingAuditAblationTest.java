package salve.core.conversation;

import com.google.gson.JsonArray;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.research.PublicResearchCoordinator;

/** Deterministic off-device ablation: status discrimination, not LLM accuracy. */
public class GroundingAuditAblationTest {
    @Test public void v11DistinguishesEightEvidenceStatesThatLegacyDeliveryCouldNot() {
        PublicResearchCoordinator.Status[] cases = {
                PublicResearchCoordinator.Status.ANSWERED,
                PublicResearchCoordinator.Status.PARTIAL,
                PublicResearchCoordinator.Status.SOURCES_ONLY,
                PublicResearchCoordinator.Status.NO_SOURCES,
                PublicResearchCoordinator.Status.FETCH_FAILED,
                PublicResearchCoordinator.Status.INVALID_MODEL_REPLY,
                PublicResearchCoordinator.Status.MODEL_FAILED,
                PublicResearchCoordinator.Status.CANCELLED
        };
        int discriminated = 0;
        for (PublicResearchCoordinator.Status sourceState : cases) {
            int sources = sourceState == PublicResearchCoordinator.Status.NO_SOURCES
                    || sourceState == PublicResearchCoordinator.Status.FETCH_FAILED
                    || sourceState == PublicResearchCoordinator.Status.CANCELLED ? 0 : 2;
            ConversationOutcomeAudit old = ConversationOutcomeAudit.evaluate(
                    "Busca información sobre geometría", "Este es el resultado.",
                    new JsonArray(), true, "delivery");
            ConversationOutcomeAudit now = ConversationOutcomeAudit.evaluate(
                    "Busca información sobre geometría", "Este es el resultado.",
                    new JsonArray(), true, "research",
                    ConversationOutcomeEvidence.research(sourceState, sources, 2));
            assertEquals("El diagnóstico anterior no poseía estados del controlador",
                    ConversationOutcomeAudit.Status.RESPONSE_UNVERIFIED, old.status);
            assertNotEquals("Con el estado real se distingue investigación y mero texto",
                    old.status, now.status);
            assertFalse(now.receipt().get("answer_factually_verified").getAsBoolean());
            discriminated++;
        }
        assertEquals(8, discriminated);
    }
    @Test public void toolFailureIsNotEvidenceOfCompletion() {
        JsonArray failed = new JsonArray();
        com.google.gson.JsonObject receipt = new com.google.gson.JsonObject();
        receipt.addProperty("status", "failed");
        failed.add(receipt);
        ConversationOutcomeAudit outcome = ConversationOutcomeAudit.evaluate(
                "Busca en internet", "No pude realizar la búsqueda", failed, true, "cognitive");
        assertEquals(ConversationOutcomeAudit.Status.EVIDENCE_UNAVAILABLE, outcome.status);
        assertEquals(1, outcome.toolFailed);
    }
}
