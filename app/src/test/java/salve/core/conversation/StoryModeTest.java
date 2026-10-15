package salve.core.conversation;

import static org.junit.Assert.*;

import org.junit.Test;

/** Partida del 15/10/2026, 13:32-13:38: Salve como narradora y Bryan como explorador. */
public class StoryModeTest {
    @Test public void startsContinuesAndEndsAStory() {
        StoryMode.State s = new StoryMode.State();
        assertFalse(StoryMode.enter(s, "Hola Salve", 1));
        assertTrue(StoryMode.enter(s, "Algo de imaginativo invéntate un juego y jugamos Salve", 2));
        assertTrue("dentro de la historia, todo es ficción", StoryMode.enter(s, "Siento una presencia que me recuerda algo que añoro", 3));
        assertTrue(StoryMode.endsStory("fin del juego"));
        s.active = false;
        StoryMode.addBeat(s, "Entras en el pantano de las deidades dormidas. Las raíces se mueven bajo tus botas.");
        assertTrue(StoryMode.enter(s, "continuemos la historia", 4));
        assertFalse("el ajedrez tiene su propio tablero", StoryMode.startsStory("juguemos ajedrez"));
        assertTrue(StoryMode.startsStory("Me gusta, yo soy el explorador y tú la guía, dale empecemos"));
    }

    @Test public void delegatingMeansSheDecidesAndAdvances() {
        StoryMode.State s = new StoryMode.State();
        StoryMode.enter(s, "juguemos a una fantasía épica", 1);
        String block = StoryMode.promptBlock(s, "La búsqueda de un conocimiento prohibido. Y lo demás decídelo tú");
        assertTrue(block.contains("decide tú"));
        assertTrue(block.contains("nunca lo tomes como una confesión personal"));
        assertTrue(block.contains("No repitas ni parafrasees su mensaje"));
        assertFalse(StoryMode.promptBlock(s, "Arcana antigua").contains("decide tú"));
    }

    @Test public void removesTheEchoAtTheStartOfTheScene() {
        assertEquals("Me gusta mucho esa imagen. Las raíces se mueven bajo tus botas.",
                StoryMode.stripEcho("Un pantano mágico. Me gusta mucho esa imagen. Las raíces se mueven bajo tus botas.", "Pantano magico"));
        assertEquals("Qué elección fascinante. La magia antigua pesa en el aire.",
                StoryMode.stripEcho("¡Arcana antigua! Qué elección fascinante. La magia antigua pesa en el aire.", "Arcana antigua"));
        String scene = "El guardián del templo te cierra el paso con su lanza de obsidiana. Dos caminos se abren a tus pies.";
        assertEquals(scene, StoryMode.stripEcho(scene, "Avanzo hacia el templo"));
    }

    @Test public void keepsAShortStorySoFar() {
        StoryMode.State s = new StoryMode.State();
        StoryMode.enter(s, "inventa una historia", 1);
        for (int i = 0; i < 12; i++) StoryMode.addBeat(s, "Escena número " + i + " en la que ocurre algo importante en el bosque.");
        assertEquals(StoryMode.MAX_BEATS, s.beats.size());
        assertTrue(StoryMode.promptBlock(s, "sigo").contains("HISTORIA_HASTA_AHORA: Escena número 4"));
    }
}
