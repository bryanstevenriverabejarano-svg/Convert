package salve.core.cognition;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.ModelResult;
import static org.junit.Assert.*;

/** Route natural-language goals to the model, without granting send authority. */
public class WhatsAppDraftIntentTest {
    private static final String REQUEST =
            "¿Puedes mandar un WhatsApp a mami diciendo Llego tarde hoy?";

    @Test public void modelCanChooseDraftForNaturalLanguageRequest() {
        assertTrue(WhatsAppDraftIntent.offered(REQUEST));
        WhatsAppDraftIntent.validate(REQUEST, "mami", "Hola, voy a llegar un poco tarde hoy. Disculpa.");
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                REQUEST, false, true, true, (name, args) -> CognitiveTurn.Receipt.handoff());
        assertTrue(tools.containsKey("whatsapp.draft"));
        assertFalse("No se ofrecen toques arbitrarios", tools.containsKey("TAP"));
        assertFalse("No se ofrece inyeccion de texto arbitraria", tools.containsKey("ESCRIBIR"));
        assertEquals("whatsapp.draft",
                CognitivePrompt.prioritize(tools, REQUEST).keySet().iterator().next());
        assertTrue(CognitivePrompt.limitTools(CognitivePrompt.prioritize(tools, REQUEST), 3000)
                .containsKey("whatsapp.draft"));
    }

    @Test public void llmMayComposeMessageWithoutExactUserWording() {
        String input = "Salve, dile a mi madre que necesito ayuda, pero dilo con carino.";
        assertTrue(WhatsAppDraftIntent.offered(input));
        WhatsAppDraftIntent.validate(input, "mi madre",
                "Mama, necesito hablar contigo. Me ayudaria mucho contar contigo hoy.");
        String excuse = "Dile a Daniel que no puedo ir y dale una excusa amable";
        assertTrue(WhatsAppDraftIntent.offered(excuse));
        WhatsAppDraftIntent.validate(excuse, "Daniel",
                "Hola Daniel, lo siento pero hoy no me sera posible asistir. Gracias por entenderlo.");
        assertTrue(CognitiveToolCatalog.build(input, false, true, false,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey("whatsapp.draft"));
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
        String json = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"whatsapp.draft\",\"arguments\":{\"contact\":\"mami\",\"message\":\"Hola, llegare un poco tarde hoy. Disculpa.\"}}}";
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
        assertFalse(WhatsAppDraftIntent.offered("Abre mi WhatsApp"));
        assertFalse(WhatsAppDraftIntent.offered("Escribe hola en la pantalla"));
        assertFalse(WhatsAppDraftIntent.offered(salve.core.reports.SelfReportTest.BRYAN));
        assertFalse(WhatsAppDraftIntent.offered("Redacta un informe PDF y guárdalo"));
        assertTrue(WhatsAppDraftIntent.offered("Dale una excusa a Daniel porque no puedo ir"));
        assertFalse(CognitiveToolCatalog.build("Mira este mensaje de WhatsApp", false, true, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey("whatsapp.draft"));
        assertTrue(CognitiveToolCatalog.build(REQUEST, true, true, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).isEmpty());
    }

    @Test public void fabricatedRecipientIsRejectedButCreativeMessageAllowed() {
        reject(REQUEST, "papa", "Llego tarde hoy");
        reject(REQUEST, "mami", "");
        reject("¿Qué es WhatsApp?", "mami", "Llego tarde hoy");
        reject(REQUEST, "ma\nmi", "Llego tarde hoy");
        WhatsAppDraftIntent.validate(REQUEST, "mami",
                "Hola, hoy se me ha complicado el horario. ¿Te importa si llego un poco mas tarde?");
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
            fail("Debio rechazar la accion no autorizada");
        } catch (IllegalArgumentException expected) { }
    }
}
