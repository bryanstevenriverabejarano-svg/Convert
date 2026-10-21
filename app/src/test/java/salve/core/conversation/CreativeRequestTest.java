package salve.core.conversation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/** Caso real (15/10/2026): «invéntate un cuento» → «Aquí tienes uno corto…» y nada más. */
public class CreativeRequestTest {
    private static final List<String> NONE = Collections.emptyList();

    @Test public void recognisesRequestsForStoriesPoemsAndLetters() {
        assertTrue(CreativeRequest.isCreative("Cuéntame un cuento inventado por ti para dormir", NONE));
        assertTrue(CreativeRequest.isCreative("Invéntate un cuento corto", NONE));
        assertTrue(CreativeRequest.isCreative("Escríbeme un poema sobre el mar", NONE));
        assertTrue(CreativeRequest.isCreative("quiero que me cuentes una historia de dragones", NONE));
        assertTrue(CreativeRequest.isCreative("hazme una canción para mi gato", NONE));
        assertTrue(CreativeRequest.isCreative("otro cuento", NONE));
        assertTrue(CreativeRequest.isCreative("un cuento para dormir", NONE));
    }

    @Test public void ordinaryMessagesStayInTheNormalTurn() {
        assertFalse(CreativeRequest.isCreative("ten en cuenta que mañana trabajo", NONE));
        assertFalse(CreativeRequest.isCreative("¿Cuál es la historia de Roma?", NONE));
        assertFalse(CreativeRequest.isCreative("busca la historia del ajedrez", NONE));
        assertFalse(CreativeRequest.isCreative("crea una rutina para despertarme", NONE));
        assertFalse(CreativeRequest.isCreative("te voy a contar una historia que me pasó", NONE));
        assertFalse(CreativeRequest.isCreative("ayer escribí un poema", NONE));
        assertFalse(CreativeRequest.isCreative("hola Salve", NONE));
        assertFalse(CreativeRequest.isCreative("y?", Arrays.asList("Hoy hace buen día.")));
    }

    @Test public void nudgeAfterAnEmptyPromiseAsksForTheMissingText() {
        List<String> promised = Arrays.asList("Claro que sí, Bryan. Aquí tienes uno corto, inventado para ti:");
        assertTrue(CreativeRequest.isCreative("y?", promised));
        assertTrue(CreativeRequest.isCreative("??", promised));
        assertTrue(CreativeRequest.isCreative("pero no me contaste nada", promised));
        assertTrue(CreativeRequest.isCreative("dale", promised));
        List<String> failedAfterPromise = Arrays.asList(
                "No pude completar la respuesta con el modelo. La respuesta llegó vacía o incompleta.", promised.get(0));
        assertTrue(CreativeRequest.isCreative("y?", failedAfterPromise));
        assertTrue(CreativeRequest.isCreative("y?", Arrays.asList(
                "No me salió bien la respuesta: Mi respuesta salió vacía o a medias. Si me lo dices de otra forma, lo intento de nuevo.",
                promised.get(0))));
        assertFalse(CreativeRequest.isCreative("y?", Arrays.asList("Hoy hace sol.", promised.get(0))));
        assertTrue(CreativeRequest.promisedWithoutDelivering("Claro que sí. Aquí tienes uno corto:"));
        assertFalse(CreativeRequest.promisedWithoutDelivering("Aquí tienes uno corto: " + repeat("Había una vez un faro que soñaba. ", 12)));
    }

    @Test public void promptAsksForTheWholeTextWithoutPreambleOrJson() {
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, "Hola, ¿qué tal el día?", 1L));
        history.add(new ChatMessage(ChatMessage.Role.USER, "Cuéntame un cuento", 2L));
        String prompt = CreativeRequest.prompt("Cuéntame un cuento", "INTERLOCUTOR: Alex, tu creador.", null, history, "Alex");
        assertTrue(prompt.contains("texto completo"));
        assertTrue(prompt.contains("«Aquí tienes»"));
        assertTrue(prompt.contains("Salve: Hola, ¿qué tal el día?"));
        assertFalse("la petición no se duplica en el historial", prompt.contains("Alex: Cuéntame un cuento"));
        assertTrue(prompt.endsWith("PETICIÓN DE AHORA: Cuéntame un cuento\nSalve:"));
        assertFalse(prompt.contains("\"mode\""));
        assertTrue(prompt.contains("180 y 350 palabras"));
        String scene = CreativeRequest.prompt("abro la puerta", "", " | MODO_HISTORIA (activo): narra", history, "Alex");
        assertTrue(scene.contains("MODO_HISTORIA"));
        assertFalse("la escena sigue la extensión del modo historia", scene.contains("180 y 350 palabras"));
    }

    @Test public void preambleIsRemovedButTheStoryAndItsParagraphsStay() {
        String story = "Había una vez una luciérnaga que no sabía brillar.\n\nCada noche miraba a las demás y ensayaba en silencio, hasta que una tormenta apagó el bosque entero.";
        assertEquals(story, CreativeRequest.stripPreamble("Claro que sí. Aquí tienes uno corto:\n\n" + story));
        assertEquals(story, CreativeRequest.stripPreamble("Salve: " + story));
        assertEquals(story, CreativeRequest.stripPreamble("{\"mode\":\"respond\",\"message\":\"" + story.replace("\n", "\\n") + "\",\"action\":null}"));
        assertEquals("Aquí tienes uno corto:", CreativeRequest.stripPreamble("Aquí tienes uno corto:"));
    }

    private static String repeat(String s, int n) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) out.append(s);
        return out.toString();
    }

    @Test public void longStoriesAreThoughtFirstAndWrittenInParts() {
        assertEquals(1, CreativeRequest.parts("Cuéntame un cuento para dormir"));
        assertEquals(2, CreativeRequest.parts("Cuéntame un cuento largo"));
        assertEquals(3, CreativeRequest.parts("invéntate una historia muy larga de piratas"));
        assertEquals(3, CreativeRequest.parts("escríbeme una aventura por capítulos"));
        assertEquals(4, CreativeRequest.parts("escribe una historia en cuatro capítulos"));
        assertEquals(1, CreativeRequest.parts("cuéntame un cuento que pase a lo largo del río"));
        assertTrue(CreativeRequest.maxCharsFor(4) <= 11000);
        String plan = CreativeRequest.planPrompt("Cuéntame un cuento largo", "", "Alex", 2);
        assertTrue(plan.contains("2 partes") && plan.contains("SOLO tu plan"));
        String first = CreativeRequest.partPrompt("Cuéntame un cuento largo", "", "Alex", "Título: El faro", "", 1, 2);
        assertTrue(first.contains("parte 1 de 2") && first.contains("No cierres todavía") && first.contains("TU PLAN"));
        String written = repeat("palabra ", 400) + "y el faro calló.";
        String last = CreativeRequest.partPrompt("Cuéntame un cuento largo", "", "Alex", "Título: El faro", written, 2, 2);
        assertTrue(last.contains("y el faro calló.") && last.contains("última parte") && last.contains("sin repetir"));
        assertTrue("solo el final de lo escrito", last.length() < written.length() + 1200);
    }

    @Test public void continuationKeepsTheEndOfTheLastStory() {
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, repeat("Había una vez. ", 200) + "Y la puerta se abrió sola.", 1L));
        String prompt = CreativeRequest.prompt("sigue con el cuento", "", null, history, "Alex");
        assertTrue(prompt.contains("Y la puerta se abrió sola."));
        assertTrue(CreativeRequest.isCreative("sigue con el cuento", NONE));
        assertTrue(CreativeRequest.isCreative("continúa la historia", NONE));
        assertFalse(CreativeRequest.isCreative("sigue con lo de la nube", NONE));
    }
}
