package salve.core.reading;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import salve.core.conversation.ChatMessage;
import salve.core.conversation.TurnChecks;

public class ToldStoryTest {
    static final class Mem implements salve.core.mind.SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }

    static final String STORY = "El mar era una vasta extensión de azur profundo, un manto sedoso que abrazaba las costas rocosas de un lugar "
            + "olvidado por el tiempo. Allí se alzaba el faro.\n\n[[salve_draw:mar + faro]]\n\nNo era un faro común; sus luces no eran meros "
            + "puntos, sino destellos de memoria, historias tejidas con la bruma salada y el eco de las olas rompiendo. Salve se encontraba "
            + "en la cima de la torre, el viento afilando su silueta, sintiendo el frío cortante que se aferraba a la piedra.";

    static List<ChatMessage> chat(String... turns) {
        List<ChatMessage> out = new ArrayList<>();
        for (int i = 0; i < turns.length; i++)
            out.add(new ChatMessage(i % 2 == 0 ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT, turns[i], i));
        return out;
    }

    @Test public void recognisesTheRequest() {
        assertEquals("", ToldStory.saveRequest("Guarda ese cuento en tu repertorio"));
        assertEquals("", ToldStory.saveRequest("guárdate el cuento que me has contado"));
        assertEquals("El faro del susurro", ToldStory.saveRequest("Guarda ese cuento con el nombre «El faro del susurro»"));
        assertNull(ToldStory.saveRequest("guarda silencio"));
        assertNull(ToldStory.saveRequest("¿qué cuentos tienes?"));
    }

    @Test public void findsTheStoryAndItsTitle() {
        List<ChatMessage> h = chat("Cuentsme el cuento de faro del susurro", "Claro, con gusto te cuento el relato del faro del susurro.",
                "Dale cuentame el cuento", STORY, "Guarda ese cuento en tu repertorio");
        String[] last = ToldStory.last(h);
        assertNotNull(last);
        assertFalse(last[0].contains("salve_draw"));
        assertTrue(last[0].startsWith("El mar era"));
        // La petición inmediata («dale, cuéntame el cuento») no tiene título: se busca más atrás.
        assertEquals("Faro del susurro", last[1]);
        assertNull(ToldStory.last(chat("hola", "Hola, ¿qué tal?")));
        assertEquals("El mar era una vasta extensión", ToldStory.fallbackTitle(STORY));
        assertTrue(ToldStory.summary(STORY).endsWith("se alzaba el faro."));
    }

    @Test public void keepsEarlierVersions() {
        Mem store = new Mem();
        StoryShelf shelf = new StoryShelf(store, () -> 1L);
        shelf.addTold("Faro del susurro", "uno", "a.txt");
        StoryShelf.Story s = shelf.addTold("faro del susurro", "dos", "b.txt");
        assertEquals(2, s.versions());
        assertEquals(Arrays.asList("a.txt"), s.earlier);
        assertEquals(1, new StoryShelf(store, () -> 2L).all().size());
        assertEquals("b.txt", new StoryShelf(store, () -> 2L).find("faro del susurro").file);
        assertEquals("faro del susurro", DocumentReading.storyToRetell("Cuentsme el cuento de faro del susurro"));
    }

    @Test public void sayingItWasSavedIsNotSavingIt() {
        assertTrue(TurnChecks.claimsSaving("He guardado el cuento con el nombre El faro del susurro en mi repertorio."));
        assertNotNull(TurnChecks.problem("Guarda ese cuento", "He guardado el cuento en mi repertorio. ¿Algo más?", true));
        assertFalse(TurnChecks.claimsSaving("Me gusta guardar los momentos bonitos."));
        assertTrue(TurnChecks.announcesOnly("Claro, con gusto te cuento el relato del faro del susurro. Es una historia que me gusta mucho."));
    }

    @Test public void storiesAreSavedOnTheirOwnButNotPoems() {
        assertTrue(ToldStory.isStoryRequest("Cuéntame un cuento de un zorro que aprende a volar", null));
        assertFalse(ToldStory.isStoryRequest("Escríbeme un poema sobre la lluvia", null));
        assertFalse(ToldStory.isStoryRequest("¿qué hora es?", null));
        List<ChatMessage> h = chat("Cuentsme el cuento de faro del susurro", "Claro, con gusto te cuento el relato.", "Dale cuentame el cuento");
        assertTrue(ToldStory.isStoryRequest("Dale cuentame el cuento", h));
        assertTrue(ToldStory.isStoryRequest("dale", chat("Inventa una historia de piratas", "¿Larga o corta?", "dale")));
        assertEquals("Faro del susurro", ToldStory.titleFromHistory(h));
    }

    @Test public void theRepertoireIsOpenedByName() {
        assertTrue(DocumentReading.asksForStoryList("Repertorio de cuentos"));
        assertTrue(DocumentReading.asksForStoryList("ábreme tu repertorio de cuentos"));
        assertTrue(DocumentReading.asksForStoryList("abre tus cuentos"));
        assertTrue(DocumentReading.asksForStoryList("¿Qué cuentos tienes?"));
        assertFalse(DocumentReading.asksForStoryList("el repertorio de trazos"));
        assertEquals("el faro del susurro", DocumentReading.storyToRetell("Léeme el cuento «El faro del susurro»"));
    }
}
