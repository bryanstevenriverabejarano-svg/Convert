package salve.core.cognition;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.ModelResult;
import salve.core.voice.MusicControlCommand;
import static org.junit.Assert.*;

/** The language model chooses a transport action; the user's intent bounds it. */
public class MediaActionIntentTest {
    @Test public void alreadyRecognizedCommandsAreOfferedToTheModel() {
        assertTrue(MediaActionIntent.offered("Pausa la música"));
        assertTrue(MediaActionIntent.offered("Siguiente canción"));
        assertTrue(MediaActionIntent.offered("Dale play"));
        assertTrue(MediaActionIntent.offered("¿Qué canción está sonando?"));
        assertFalse(MediaActionIntent.offered("Para, no respondas"));
        assertFalse(MediaActionIntent.offered("Siguiente paso del informe"));
    }

    @Test public void modelCannotInvertTransportAction() {
        assertEquals(MusicControlCommand.Action.PAUSE,
                MediaActionIntent.verifiedAction("Pausa la música", "pause"));
        assertEquals(MusicControlCommand.Action.NEXT,
                MediaActionIntent.verifiedAction("Siguiente canción", "NEXT"));
        reject("Pausa la música", "PLAY");
        reject("Siguiente canción", "PREVIOUS");
        reject("¿Qué canción está sonando?", "NEXT");
        reject("Cuéntame una historia", "PLAY");
    }

    @Test public void offeredMediaControlFitsSmallPromptAndStopsOnHandoff() {
        String request = "Pausa la música";
        AtomicInteger executed = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                request, false, true, true, (name, args) -> {
                    assertEquals("device.media_control", name);
                    MediaActionIntent.verifiedAction(request,
                            args.get("action").getAsString());
                    executed.incrementAndGet();
                    return CognitiveTurn.Receipt.handoff();
                });
        assertTrue(tools.containsKey("device.media_control"));
        assertEquals("device.media_control",
                CognitivePrompt.prioritize(tools, request).keySet().iterator().next());
        assertTrue(CognitivePrompt.limitTools(CognitivePrompt.prioritize(tools, request), 3000)
                .containsKey("device.media_control"));
        assertFalse(CognitiveToolCatalog.build(request, false, false, true,
                (name, args) -> CognitiveTurn.Receipt.handoff())
                .containsKey("device.media_control"));
        String json = "{\"mode\":\"tool\",\"message\":null,\"action\":"
                + "{\"name\":\"device.media_control\",\"arguments\":{\"action\":\"PAUSE\"}}}";
        CognitiveTurn.Outcome result = CognitiveTurn.run(
                (receipts, repair, left) -> ModelResult.success(json, 1),
                tools, () -> false);
        assertTrue(result.handedOff);
        assertEquals(1, executed.get());
        assertEquals("handoff", result.receipts().get(0).getAsJsonObject()
                .get("status").getAsString());
    }

    private static void reject(String request, String proposed) {
        try {
            MediaActionIntent.verifiedAction(request, proposed);
            fail("No debe aceptar acciones inventadas por el modelo");
        } catch (IllegalArgumentException expected) { }
    }
}
