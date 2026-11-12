package salve.core.cognition;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Her own functions are chosen by the model; the user's words fix the details. */
public class SelfCommandIntentTest {
    @Test public void legacyOwnCommandsAreNowOfferedToTheModel() {
        assertOffered("¿Cómo está tu memoria?", SelfCommandIntent.Command.MEMORY_HEALTH);
        assertOffered("activa el modo oídos", SelfCommandIntent.Command.EAR_MODE);
        assertOffered("desactiva el modo oídos", SelfCommandIntent.Command.EAR_MODE);
        assertOffered("usa tu voz nueva", SelfCommandIntent.Command.VOICE_ENGINE);
        assertOffered("escucha esta canción", SelfCommandIntent.Command.LISTEN);
        assertOffered("abre el mapa de tu mente", SelfCommandIntent.Command.MIND_MAP);
        assertOffered("abre el registro de ejecución", SelfCommandIntent.Command.OPEN_JOURNAL);
        assertOffered("parpadea", SelfCommandIntent.Command.BODY_ACTION);
        assertOffered("¿qué modelo usas?", SelfCommandIntent.Command.MODEL_STATUS);
        assertOffered("¿cómo va la descarga?", SelfCommandIntent.Command.DOWNLOAD_STATUS);
        assertOffered("juguemos al ajedrez", SelfCommandIntent.Command.CHESS_START);
    }

    @Test public void ordinaryChatAndProhibitionsOfferNothing() {
        assertFalse(SelfCommandIntent.offered("hola, ¿qué tal?"));
        assertFalse(SelfCommandIntent.offered("cuéntame un cuento"));
        assertFalse(SelfCommandIntent.offered("pausa la música"));
        assertFalse(SelfCommandIntent.offered("no abras el mapa de tu mente"));
        assertFalse(SelfCommandIntent.offered(null));
    }

    @Test public void modelCannotPickAFunctionTheUserDidNotAskFor() {
        assertEquals(SelfCommandIntent.Command.EAR_MODE,
                SelfCommandIntent.verified("activa el modo oídos", "ear_mode"));
        reject("activa el modo oídos", "MIND_MAP");
        reject("activa el modo oídos", "SELF_PR_OPEN");
        reject("hola", "EAR_MODE");
        reject("parpadea", "BORRA_TODO");
        reject("parpadea", null);
    }

    @Test public void sheUnderstandsReadOnlyQuestionsWithHerModelNotWithExactPhrases() {
        // Sin frase exacta: el modelo puede elegir leer su estado (no cambia nada) y explicarlo con sus palabras.
        assertEquals(SelfCommandIntent.Command.EVOLUTION_STATUS,
                SelfCommandIntent.verified("¿cómo vas con lo de mejorarte a ti misma?", "EVOLUTION_STATUS"));
        assertEquals(SelfCommandIntent.Command.CAUSAL_MEMORY,
                SelfCommandIntent.verified("¿qué relaciones de causa y efecto te he enseñado?", "causal_memory"));
        // Pero no se ofrecen en cada turno: ocupaban el catálogo y desplazaban sus recuerdos.
        assertTrue(SelfCommandIntent.available("hola").isEmpty());
        // Lo que tiene efecto sigue necesitando que él lo pida: el modelo no enciende nada por su cuenta.
        assertFalse(SelfCommandIntent.available("hola").contains(SelfCommandIntent.Command.EAR_MODE));
        reject("¿cómo estás?", "SELF_PR_OPEN");
        // Y no fuerza la herramienta: ofrecerla no es decidir que la pidió.
        assertFalse(SelfCommandIntent.offered("hola, ¿qué tal?"));
    }

    @Test public void readOnlyFunctionsGoBackToTheModelAndActionsAreHandoffs() {
        assertTrue(SelfCommandIntent.Command.MEMORY_HEALTH.readOnly);
        assertTrue(SelfCommandIntent.Command.MODEL_STATUS.readOnly);
        assertFalse(SelfCommandIntent.Command.MIND_MAP.readOnly);
        assertFalse(SelfCommandIntent.Command.SELF_PR_OPEN.readOnly);
    }

    @Test public void catalogOffersOwnFunctionOnlyToVerifiedCreatorAndNotAsAnApp() {
        String request = "abre el mapa de tu mente";
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                request, false, true, false, (name, args) -> CognitiveTurn.Receipt.handoff());
        assertTrue(tools.containsKey(SelfCommandIntent.TOOL));
        assertFalse("su mente no es una app instalada", tools.containsKey("device.open_app"));
        assertTrue(tools.get(SelfCommandIntent.TOOL).description.contains("MIND_MAP"));
        assertEquals(SelfCommandIntent.TOOL,
                CognitivePrompt.prioritize(tools, request).keySet().iterator().next());

        Map<String, CognitiveTurn.Tool> guest = CognitiveToolCatalog.build(
                request, false, false, false, (name, args) -> CognitiveTurn.Receipt.handoff());
        assertFalse(guest.containsKey(SelfCommandIntent.TOOL));
    }

    @Test public void messagingStaysWithTheDraftTool() {
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                "dile a mi madre por WhatsApp que llego tarde", false, true, false,
                (name, args) -> CognitiveTurn.Receipt.handoff());
        assertFalse(tools.containsKey(SelfCommandIntent.TOOL));
    }

    private static void assertOffered(String input, SelfCommandIntent.Command expected) {
        assertTrue(input + " → " + SelfCommandIntent.detected(input),
                SelfCommandIntent.detected(input).contains(expected));
    }

    private static void reject(String input, String proposed) {
        try {
            SelfCommandIntent.verified(input, proposed);
            fail("debía rechazar " + proposed + " para «" + input + "»");
        } catch (IllegalArgumentException expected) { }
    }
}
