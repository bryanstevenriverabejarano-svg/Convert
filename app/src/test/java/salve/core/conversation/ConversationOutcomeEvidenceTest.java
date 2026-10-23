package salve.core.conversation;

import com.google.gson.JsonArray;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.memory.ConversationMemoryGrounding;
import salve.core.research.PublicResearchCoordinator;

public class ConversationOutcomeEvidenceTest {
    @Test public void webSourcesAreProvenanceNotFactChecks() {
        ConversationOutcomeEvidence found = ConversationOutcomeEvidence.research(
                PublicResearchCoordinator.Status.ANSWERED, 3, 2);
        ConversationOutcomeAudit audit = ConversationOutcomeAudit.evaluate(
                "Investiga la geometría de una habitación",
                "Las fuentes consultadas indican relaciones de diseño entre paredes y recorridos.",
                new JsonArray(), true, "research", found);
        assertEquals(ConversationOutcomeAudit.Status.EVIDENCE_OBSERVED_NOT_VERIFIED, audit.status);
        assertEquals("RESEARCH", audit.receipt().getAsJsonObject("controller_evidence").get("kind").getAsString());
        assertFalse(audit.receipt().get("answer_factually_verified").getAsBoolean());
        assertFalse(audit.receipt().get("human_verified").getAsBoolean());
        assertEquals(3, audit.evidence.sources);
        assertEquals(2, audit.evidence.operations);
    }
    @Test public void absenceOfResultsIsNotPresentedAsConfirmedAnswer() {
        ConversationOutcomeEvidence missing = ConversationOutcomeEvidence.research(
                PublicResearchCoordinator.Status.NO_SOURCES, 0, 3);
        ConversationOutcomeAudit audit = ConversationOutcomeAudit.evaluate(
                "Investiga la geometría de una habitación",
                "La búsqueda no devolvió fuentes utilizables.",
                new JsonArray(), true, "research", missing);
        assertEquals(ConversationOutcomeAudit.Status.EVIDENCE_UNAVAILABLE, audit.status);
        ConversationOutcomeEvidence failed = ConversationOutcomeEvidence.research(
                PublicResearchCoordinator.Status.INVALID_MODEL_REPLY, 1, 1);
        assertEquals(ConversationOutcomeEvidence.State.ERROR, failed.state);
        ConversationOutcomeEvidence partial = ConversationOutcomeEvidence.research(
                PublicResearchCoordinator.Status.SOURCES_ONLY, 1, 1);
        assertEquals(ConversationOutcomeEvidence.State.PARTIAL, partial.state);
    }
    @Test public void memoryNotAvailableIsDifferentFromNoMemoryExists() {
        ConversationOutcomeEvidence unavailable = ConversationOutcomeEvidence.memory(
                ConversationMemoryGrounding.Result.unavailable());
        assertEquals(ConversationOutcomeEvidence.State.ERROR, unavailable.state);
        ConversationOutcomeAudit audit = ConversationOutcomeAudit.evaluate(
                "¿Qué recuerdas?", "No pude consultar ahora los recuerdos.",
                new JsonArray(), true, "memory", unavailable);
        assertEquals(ConversationOutcomeAudit.Status.EVIDENCE_UNAVAILABLE, audit.status);
        assertEquals("memory", audit.route);
        assertFalse(audit.receipt().toString().contains("No pude consultar ahora los recuerdos"));
    }
    @Test public void missingSourcesTakePriorityOverMissingCausalExplanation() {
        ConversationOutcomeAudit audit = ConversationOutcomeAudit.evaluate(
                "¿Por qué funciona la geometría de este sistema?",
                "No encontré información suficiente para responder.",
                new JsonArray(), true, "research",
                ConversationOutcomeEvidence.research(PublicResearchCoordinator.Status.NO_SOURCES, 0, 2));
        assertEquals(ConversationOutcomeAudit.Status.EVIDENCE_UNAVAILABLE, audit.status);
        assertFalse(audit.receipt().get("answer_factually_verified").getAsBoolean());
    }

    @Test public void emptyDeliveryFailsEvenWhenControllerSawSources() {
        ConversationOutcomeAudit audit = ConversationOutcomeAudit.evaluate(
                "Busca un tema", "", new JsonArray(), true, "research",
                ConversationOutcomeEvidence.research(PublicResearchCoordinator.Status.ANSWERED, 2, 2));
        assertEquals(ConversationOutcomeAudit.Status.FAILED, audit.status);
    }
}