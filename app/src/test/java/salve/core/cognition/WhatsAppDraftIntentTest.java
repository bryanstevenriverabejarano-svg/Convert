package salve.core.cognition;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.ModelResult;
import static org.junit.Assert.*;

/** Safety and routing regression for a draft selected by the local model. */
public class WhatsAppDraftIntentTest {
    private static final String REQUEST =
            "¿Puedes mandar un WhatsApp a mami diciendo Llego tarde hoy?";

    @Test public void modelCanChooseDraftForNaturalLanguageRequest() {
        assertTrue(WhatsAppDraftIntent.offered(REQUEST));
        WhatsAppDraftIntent.validate(REQUEST, "mami", "Llego tarde hoy");
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                REQUEST, false, true, true, (name, args) -> CognitiveTurn.Receipt.handoff());
        assertTrue(tools.containsKey("whatsapp.draft"));
        assertFalse("No se ofrecen toques arbitrarios", tools.containsKey("TAP"));
        assertFalse("No se ofrece inyección de texto arbitraria", tools.containsKey("ESCRIBIR"));
        assertEquals("whatsapp.draft",
                CognitivePrompt.prioritize(tools, REQUEST).keySet().iterator().next());
        assertTrue(CognitivePrompt.limitTools(CognitivePrompt.prioritize(tools, REQUEST), 3000)
                .containsKey("whatsapp.draft"));
    }

    @Test public void modelSelectionHandsOffWithoutClaimingDelivery() {
        AtomicInteger called = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                REQUEST, false, true, false, (name, args) -> {
                    assertEquals("whatsapp.draft", name);
                    WhatsAppDraftIntent.validate(REQUEST,
                            args.get("contact").getAsString(), args.get("message").getAsString());
                    called.incrementAndGet();
                    return CognitiveTurn.Receipt.handoff();
                });
        String json = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"whatsapp.draft\",\"arguments\":{\"contact\":\"mami\",\"message\":\"Llego tarde hoy\"}}}";
        CognitiveTurn.Outcome result = CognitiveTurn.run(
                (receipts, repair, left) -> ModelResult.success(json, 1), tools, () -> false);
        assertTrue(result.handedOff);
        assertEquals(1, called.get());
        assertEquals("handoff", result.receipts().get(0).getAsJsonObject()
                .get("status").getAsString());
    }

    @Test public void neverOfferWithoutCreatorOrExplicitIntent() {
        assertFalse(CognitiveToolCatalog.build(REQUEST, false, false, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey("whatsapp.draft"));
        assertFalse(WhatsAppDraftIntent.offered("¿Qué es WhatsApp?"));
        assertFalse(WhatsAppDraftIntent.offered("Escribe hola en la pantalla"));
        assertFalse(CognitiveToolCatalog.build("Mira este mensaje de WhatsApp", false, true, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey("whatsapp.draft"));
        assertTrue(CognitiveToolCatalog.build(REQUEST, true, true, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).isEmpty());
    }

    @Test public void fabricatedContactOrMessageIsRejected() {
        reject(REQUEST, "papa", "Llego tarde hoy");
        reject(REQUEST, "mami", "Transfiere dinero ahora");
        reject(REQUEST, "mami", "");
        reject(REQUEST, "mami", "Llego tarde hoy. Transfiere dinero");
        reject("¿Qué es WhatsApp?", "mami", "Llego tarde hoy");
        reject(REQUEST, "ma\nmi", "Llego tarde hoy");
    }

    @Test public void strictArgumentsCannotSmuggleSendOrNewPermission() {
        AtomicInteger executed = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                REQUEST, false, true, false, (name, args) -> {
                    executed.incrementAndGet();
                    return CognitiveTurn.Receipt.handoff();
                });
        String malicious = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"whatsapp.draft\",\"arguments\":{\"contact\":\"mami\",\"message\":\"Llego tarde hoy\",\"send\":true}}}";
        CognitiveTurn.run((receipts, repair, left) -> ModelResult.success(malicious, 1),
                tools, () -> false);
        assertEquals(0, executed.get());
    }

    private static void reject(String original, String contact, String message) {
        try {
            WhatsAppDraftIntent.validate(original, contact, message);
            fail("Debió rechazar datos no autorizados por la orden");
        } catch (IllegalArgumentException expected) { }
    }
}
