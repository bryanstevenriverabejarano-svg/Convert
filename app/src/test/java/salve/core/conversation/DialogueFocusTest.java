package salve.core.conversation;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

/** La conversación real del 16/10/2026: «enséñame inglés» → «dale, hazlo» acabó en un relato. */
public class DialogueFocusTest {
    private static final String Q1 = "Claro que sí. Me encantaría ayudarte a aprender inglés desde cero. Es un viaje fascinante. "
            + "¿Te gustaría que empecemos con algunas frases básicas, o prefieres que nos enfoquemos en la gramática desde el principio? "
            + "Cuéntame qué te parece mejor para ti.";
    private static final String Q2 = "Me alegra mucho que quieras empezar con la gramática desde el principio. Es una base muy sólida. "
            + "¿Te gustaría que empecemos con los conceptos más fundamentales, como el orden de las palabras en una oración, "
            + "o quizás con el uso básico de los verbos? O si prefieres, podemos empezar con algunas estructuras sencillas para "
            + "presentarte a alguien. Estoy aquí para acompañarte.";

    @Test public void readsTheOptionsSheOffered() {
        List<String> o = DialogueFocus.options(Q1);
        assertEquals(2, o.size());
        assertTrue(o.get(0).contains("frases básicas"));
        assertTrue(o.get(1).contains("gramática"));
        assertEquals("sin pregunta no hay opciones", 0, DialogueFocus.options("Hola. Qué tal.").size());
    }

    @Test public void followsTheThread() {
        DialogueFocus.State s = new DialogueFocus.State();
        DialogueFocus.observeUser(s, "Enséñame ingles desde cero", 0);
        assertEquals("Enséñame ingles desde cero", s.task);

        DialogueFocus.Reading r = DialogueFocus.read(s, "Gramtica desde el inici", Q1, 1000);
        assertEquals(DialogueFocus.Kind.CHOOSE, r.kind);
        assertTrue(r.choice.contains("gramática"));
        assertTrue(r.prompt().contains("No vuelvas a preguntar"));
        assertTrue(r.prompt().contains("Enséñame ingles desde cero"));

        DialogueFocus.observeUser(s, "No se nada de español asi que hasme un pla apra que me enseñes", 2000);
        r = DialogueFocus.read(s, "No se nada de español asi que hasme un pla apra que me enseñes", Q2, 2000);
        assertTrue("lapsus español/inglés", r.prompt().contains("la tarea es aprender inglés"));

        r = DialogueFocus.read(s, "Si el orden de las palabras dale haslo", Q2, 3000);
        assertEquals(DialogueFocus.Kind.CHOOSE, r.kind);
        assertTrue(r.choice.contains("orden de las palabras"));
        assertTrue(r.prompt().contains("te dice que lo hagas ya"));
        assertTrue(r.prompt().contains("no como relato"));
    }

    @Test public void acceptsRejectsAndStaysQuietOtherwise() {
        DialogueFocus.State s = new DialogueFocus.State();
        assertEquals(DialogueFocus.Kind.ACCEPT, DialogueFocus.read(s, "Vale, haslo", Q1, 0).kind);
        assertEquals(DialogueFocus.Kind.ACCEPT, DialogueFocus.read(s, "sí por favor", "¿Quieres que te lo explique?", 0).kind);
        assertEquals(DialogueFocus.Kind.REJECT, DialogueFocus.read(s, "no, ninguna", Q1, 0).kind);
        DialogueFocus.Reading none = DialogueFocus.read(s, "¿Qué tiempo hace mañana?", Q1, 0);
        assertEquals(DialogueFocus.Kind.NONE, none.kind);
        assertEquals("", none.prompt());
    }

    @Test public void taskExpires() {
        DialogueFocus.State s = new DialogueFocus.State();
        DialogueFocus.observeUser(s, "explícame la fotosíntesis", 0);
        assertFalse(DialogueFocus.read(s, "vale", "¿Seguimos?", 1000).task.isEmpty());
        assertTrue(DialogueFocus.read(s, "vale", "¿Seguimos?", DialogueFocus.TASK_IDLE_MS + 5).task.isEmpty());
    }
}
