package salve.core.mind;

import static org.junit.Assert.*;
import org.junit.Test;

public class LessonBookTest {
    static final class Memory implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }

    @Test public void learnsFromCorrectionsAndRemembersThem() {
        assertTrue(LessonBook.isCorrection("Pues no me estas mostrando el dibujo joder"));
        assertTrue(LessonBook.isCorrection("No. Muéstrame el dibujo"));
        assertTrue(LessonBook.isCorrection("otra vez lo mismo"));
        assertFalse(LessonBook.isCorrection("noche estrellada"));
        assertFalse(LessonBook.isCorrection("¿Qué tal estás?"));

        Memory m = new Memory();
        long[] now = {1000};
        LessonBook book = new LessonBook(m, () -> now[0]);
        book.learn("bryan", "dibuja un vestido", "Estoy terminando de refinar el detalle del vestido.",
                "Bryan me corrigió: «no me estás mostrando el dibujo»");
        now[0] = 2000;
        LessonBook.Lesson again = book.learn("bryan", "dibuja un vestido de verano", "Claro que sí.",
                "Bryan me corrigió: «no me estás mostrando el dibujo del vestido»");
        assertEquals("se refuerza en vez de duplicarse", 1, book.size());
        assertEquals(2, again.times);
        String block = new LessonBook(m, () -> 3000).render("dibuja un vestido", 3);
        assertTrue("persiste y se aplica", block.contains("no me estás mostrando el dibujo"));
        assertTrue(block.contains("×2"));
    }
}
