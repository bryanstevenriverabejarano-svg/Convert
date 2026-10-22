package salve.core.conversation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversationOutcomeAuditTest {
    private JsonArray one(String status, String privateData) {
        JsonArray arr = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("status", status);
        entry.addProperty("data", privateData);
        arr.add(entry);
        return arr;
    }
    @Test public void aGenericSelfGreetingIsNotCertifiedAsCausalExplanation() {
        ConversationOutcomeAudit generic = ConversationOutcomeAudit.evaluate(
                "¿Por qué eres única en tu especie?", "Soy Salve. Es un placer conversar contigo.",
                new JsonArray(), true, "cognitive");
        assertEquals(ConversationOutcomeAudit.Status.NEEDS_REVISION, generic.status);
        assertNotNull(generic.demandIssue);
        ConversationOutcomeAudit real = ConversationOutcomeAudit.evaluate(
                "¿Por qué eres única en tu especie?",
                "Me distingo por la continuidad de mis recuerdos y por las capacidades que desarrollo con tus interacciones.",
                new JsonArray(), true, "cognitive");
        assertEquals(ConversationOutcomeAudit.Status.RESPONSE_UNVERIFIED, real.status);
        assertFalse(real.receipt().get("answer_factually_verified").getAsBoolean());
    }
    @Test public void realToolReceiptStillDoesNotCertifyEntireAnswer() {
        String sensitive = "datos privados obtenidos por la herramienta";
        ConversationOutcomeAudit observed = ConversationOutcomeAudit.evaluate(
                "¿Qué encontraron las fuentes?", "Las fuentes indican varias posibles explicaciones.",
                one("completed", sensitive), true, "cognitive");
        assertEquals(ConversationOutcomeAudit.Status.EVIDENCE_OBSERVED_NOT_VERIFIED, observed.status);
        assertEquals(1, observed.toolCompleted);
        assertFalse(observed.receipt().toString().contains(sensitive));
        assertFalse(observed.receipt().toString().contains("fuentes"));
    }
    @Test public void deferredActionAndFailuresAreNotSuccess() {
        assertEquals(ConversationOutcomeAudit.Status.PENDING_APPROVAL,
                ConversationOutcomeAudit.evaluate("Pulsa esto", "Pendiente de aprobación",
                        one("handoff", "no"), true, "cognitive").status);
        assertEquals(ConversationOutcomeAudit.Status.FAILED,
                ConversationOutcomeAudit.evaluate("Hola", "", new JsonArray(), true, "delivery").status);
        assertEquals(ConversationOutcomeAudit.Status.FAILED,
                ConversationOutcomeAudit.evaluate("Hola", "Gracias", new JsonArray(), false, "delivery").status);
    }
}