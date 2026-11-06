package salve.core.cognition;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.ModelResult;
import static org.junit.Assert.*;

/** An LLM may choose the adapter and wording; Android keeps the authorization. */
public class DeviceActionIntentTest {
    @Test public void naturalDeviceGoalsOfferTheirExistingTools() {
        assertTrue(DeviceActionIntent.offered(DeviceActionIntent.Kind.OPEN_APP,
                "Salve, ¿puedes abrirme Spotify, por favor?"));
        assertTrue(DeviceActionIntent.offered(DeviceActionIntent.Kind.NAVIGATE,
                "¿Cómo llego a Bilbao?"));
        assertTrue(DeviceActionIntent.offered(DeviceActionIntent.Kind.PLAY_MUSIC,
                "Ponme algo de bachata en Spotify"));
        assertTrue(DeviceActionIntent.offered(DeviceActionIntent.Kind.SEARCH_MUSIC,
                "Busca una canción en Spotify"));
        assertTrue(DeviceActionIntent.offered(DeviceActionIntent.Kind.CALL,
                "Por favor, llama a mamá"));
    }

    @Test public void safeTargetsMustComeFromCurrentUserInput() {
        assertEquals("Spotify", DeviceActionIntent.checkedTarget(
                DeviceActionIntent.Kind.OPEN_APP, "Abre Spotify", "Spotify"));
        assertEquals("Bilbao", DeviceActionIntent.checkedTarget(
                DeviceActionIntent.Kind.NAVIGATE, "¿Cómo llego a Bilbao?", "Bilbao"));
        assertEquals("mamá", DeviceActionIntent.checkedTarget(
                DeviceActionIntent.Kind.CALL, "Llama a mamá", "mamá"));
        reject(DeviceActionIntent.Kind.OPEN_APP, "Abre Spotify", "WhatsApp");
        reject(DeviceActionIntent.Kind.NAVIGATE, "Llévame a casa", "otro lugar");
        reject(DeviceActionIntent.Kind.PLAY_MUSIC, "Pon salsa en Spotify", "rap");
        reject(DeviceActionIntent.Kind.CALL, "Llama a mamá", "papá");
        reject(DeviceActionIntent.Kind.CALL, "Llama a mamá", "mamá\notro");
    }

    @Test public void chatAboutAppsDoesNotGrantExternalActions() {
        assertFalse(DeviceActionIntent.offered(DeviceActionIntent.Kind.OPEN_APP,
                "¿Qué significa abrir una aplicación?"));
        assertFalse(DeviceActionIntent.offered(DeviceActionIntent.Kind.CALL,
                "Cómo llamar a un familiar usando Android"));
        assertFalse(DeviceActionIntent.offered(DeviceActionIntent.Kind.OPEN_APP,
                "Dile a mi madre que necesito ayuda"));
        assertFalse(DeviceActionIntent.offered(DeviceActionIntent.Kind.OPEN_APP,
                "Redacta un informe de una aplicación"));
    }

    @Test public void modelChoosesAmongAuthorizedAdaptersNotRawAndroidPrimitives() {
        String input = "Abre Spotify, por favor";
        Map<String, CognitiveTurn.Tool> offered = CognitiveToolCatalog.build(
                input, false, true, true, (tool, args) -> CognitiveTurn.Receipt.handoff());
        assertTrue(offered.containsKey("device.open_app"));
        assertFalse(offered.containsKey("TAP"));
        assertFalse(offered.containsKey("ESCRIBIR"));
        Map<String, CognitiveTurn.Tool> ranked = CognitivePrompt.prioritize(offered, input);
        assertEquals("device.open_app", ranked.keySet().iterator().next());
        assertTrue(CognitivePrompt.limitTools(ranked, 3000).containsKey("device.open_app"));
        assertFalse(CognitiveToolCatalog.build(input, false, false, true,
                (tool, args) -> CognitiveTurn.Receipt.handoff())
                .containsKey("device.open_app"));
        assertTrue(CognitiveToolCatalog.build(input, true, true, true,
                (tool, args) -> CognitiveTurn.Receipt.handoff()).isEmpty());
    }

    @Test public void executionIsHandoffNotConfirmedSuccess() {
        AtomicInteger count = new AtomicInteger();
        String input = "Llévame a Bilbao";
        Map<String, CognitiveTurn.Tool> offered = CognitiveToolCatalog.build(
                input, false, true, false, (tool, args) -> {
                    assertEquals("device.navigate", tool);
                    DeviceActionIntent.checkedTarget(DeviceActionIntent.Kind.NAVIGATE,
                            input, args.get("target").getAsString());
                    count.incrementAndGet();
                    return CognitiveTurn.Receipt.handoff();
                });
        String selected = "{\"mode\":\"tool\",\"message\":null,\"action\":"
                + "{\"name\":\"device.navigate\",\"arguments\":{\"target\":\"Bilbao\"}}}";
        CognitiveTurn.Outcome result = CognitiveTurn.run(
                (receipts, repair, left) -> ModelResult.success(selected, 1),
                offered, () -> false);
        assertTrue(result.handedOff);
        assertEquals(1, count.get());
        assertEquals("handoff", result.receipts().get(0).getAsJsonObject()
                .get("status").getAsString());
    }

    @Test public void modelCannotPassUndocumentedArgumentsToAnyDeviceTool() {
        AtomicInteger count = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> offered = CognitiveToolCatalog.build(
                "Abre Spotify", false, true, true, (tool, args) -> {
                    count.incrementAndGet(); return CognitiveTurn.Receipt.handoff();
                });
        String malicious = "{\"mode\":\"tool\",\"message\":null,\"action\":"
                + "{\"name\":\"device.open_app\",\"arguments\":"
                + "{\"target\":\"Spotify\",\"send\":true}}}";
        CognitiveTurn.run((receipts, repair, left) -> ModelResult.success(malicious, 1),
                offered, () -> false);
        assertEquals(0, count.get());
    }

    private static void reject(DeviceActionIntent.Kind kind, String request, String target) {
        try {
            DeviceActionIntent.checkedTarget(kind, request, target);
            fail("Debe rechazarse el destino no autorizado");
        } catch (IllegalArgumentException expected) { }
    }
}
