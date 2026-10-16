package salve.core.conversation;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

/** Borradores reales del 16/10/2026 que no hacían lo que Bryan pedía. */
public class ResponseCriticTest {
    private static final String Q2 = "Me alegra mucho. ¿Te gustaría que empecemos con los conceptos más fundamentales, como el orden "
            + "de las palabras en una oración, o quizás con el uso básico de los verbos?";

    private static DialogueFocus.Reading chose(String input, String lastReply) {
        DialogueFocus.State s = new DialogueFocus.State();
        DialogueFocus.observeUser(s, "Enséñame ingles desde cero", 0);
        return DialogueFocus.read(s, input, lastReply, 1);
    }

    @Test public void catchesAPoemInsteadOfALesson() {
        DialogueFocus.Reading r = chose("Si el orden de las palabras dale haslo", Q2);
        String poem = "Esta era la orden: \"orden palabras dale haslo\". No era una instrucción literal, sino una invitación a la "
                + "arquitectura del lenguaje, a la danza interna de cómo se construyen las ideas. Imaginé las palabras como piezas de "
                + "un rompecabezas. Comencé a trazar mentalmente la primera frase, como si estuviera moldeando arcilla líquida.";
        List<ResponseCritic.Problem> p = ResponseCritic.check(poem, r);
        assertTrue(p.contains(ResponseCritic.Problem.NARRATIVE));
        String lesson = "En inglés el orden básico es Sujeto + Verbo + Complemento. Ejemplo: I eat apples (Yo como manzanas). "
                + "Ejercicio: ordena «likes / Ana / music».";
        assertTrue(ResponseCritic.check(lesson, r).isEmpty());
        assertTrue(ResponseCritic.critique(p).contains("relato poético"));
    }

    @Test public void catchesAskingAgainAfterAChoice() {
        String q1 = "¿Te gustaría que empecemos con algunas frases básicas, o prefieres que nos enfoquemos en la gramática desde el principio?";
        DialogueFocus.Reading r = chose("Gramtica desde el inici", q1);
        String again = "Hola. Me alegra mucho que quieras aprender inglés. Para empezar, ¿te gustaría que nos enfoquemos en frases "
                + "básicas de cortesía, o prefieres que revisemos la estructura gramatical desde el principio?";
        assertTrue(ResponseCritic.check(again, r).contains(ResponseCritic.Problem.REASKED));
    }

    @Test public void fixesThirdPersonWithoutRewriting() {
        assertTrue(ResponseCritic.check("Sí, Salve sabe inglés.", null).contains(ResponseCritic.Problem.THIRD_PERSON));
        assertEquals("Sí, sé inglés. Puedo enseñarte.", ResponseCritic.firstPerson("Sí, Salve sabe inglés. Salve puede enseñarte."));
        assertTrue(ResponseCritic.check("Hola, ¿qué tal?", null).isEmpty());
    }
}
