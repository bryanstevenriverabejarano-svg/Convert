package salve.core.reading;

import static org.junit.Assert.*;
import org.junit.Test;
import salve.core.mind.SelfModelJournal;

public class StoryShelfTest {
    static final class Mem implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }

    private static DocumentReading.Analysis story(String title, String... characters) {
        StringBuilder json = new StringBuilder("{\"titulo\":\"" + title + "\",\"resumen\":\"Resumen de " + title + ".\",\"personajes\":[");
        for (int i = 0; i < characters.length; i++) json.append(i == 0 ? "" : ",").append('"').append(characters[i]).append('"');
        return DocumentReading.parse(DocumentReading.Mode.STORY, json.append("]}").toString(), "x.pdf");
    }

    @Test public void keepsStoriesFindsThemAndSurvivesRestart() {
        Mem store = new Mem();
        StoryShelf shelf = new StoryShelf(store, () -> 1L);
        assertTrue(shelf.describe().contains("Todavía no he leído ningún cuento"));
        shelf.add(story("El dragón y la luna", "Lía: niña valiente", "Fuego: dragón tímido"), "el-dragon-1.txt");
        shelf.add(story("Caperucita Roja", "Caperucita", "El lobo"), "caperucita-2.txt");
        shelf.add(story("El dragón y la luna", "Lía: niña valiente"), null); // releerlo no lo duplica
        StoryShelf again = new StoryShelf(store, () -> 2L);
        assertEquals(2, again.all().size());
        assertEquals("el-dragon-1.txt", again.find("dragon y la luna").file);
        assertEquals("Caperucita Roja", again.find("caperucita").title);
        assertEquals("El dragón y la luna", again.find("el cuento de lia").title);
        assertNull(again.find("piratas"));
        String d = again.describe();
        assertTrue(d, d.startsWith("Tengo 2 cuentos: «Caperucita Roja» (Caperucita, El lobo); «El dragón y la luna» (Lía)"));
    }
}
