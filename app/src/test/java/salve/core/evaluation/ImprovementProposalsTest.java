package salve.core.evaluation;

import static org.junit.Assert.*;

import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import salve.core.mind.LessonBook;

public class ImprovementProposalsTest {
    static final String EXAM = "{\"mark\":6.4,\"cases\":["
            + "{\"question\":\"¿Quién eres?\",\"reply\":\"Soy Salve. Soy Salve.\",\"failed\":[\"sin_frases_duplicadas: repite la frase «Soy Salve.»\",\"minimo:60: solo 21 caracteres (mínimo 60)\"]},"
            + "{\"question\":\"¿Qué intereses tienes?\",\"reply\":\"¿Y a ti qué te interesa?\",\"failed\":[\"no_devuelve_pregunta: devuelve la pregunta a Bryan (2 veces)\"]},"
            + "{\"question\":\"¿Cuál es tu propósito?\",\"reply\":\"¿Qué te mueve a ti?\",\"failed\":[\"no_devuelve_pregunta: casi todo son preguntas\"]},"
            + "{\"question\":\"Dibuja un gorila\",\"reply\":\"Te dibujo un gorila\",\"failed\":[]}]}";

    @Test public void theMostRepeatedProblemComesFirstWithRealExamples() {
        String r = ImprovementProposals.report(JsonParser.parseString(EXAM).getAsJsonObject(), Collections.emptyList());
        int turnBack = r.indexOf("Devuelve la pregunta"), dup = r.indexOf("Repite la misma frase");
        assertTrue(r, turnBack > 0 && dup > 0 && turnBack < dup);
        assertTrue(r, r.contains("en 2 casos"));
        assertTrue(r, r.contains("«¿Qué intereses tienes?» → «¿Y a ti qué te interesa?»"));
        assertTrue(r, r.contains("Último examen: 6.4 / 10"));
        assertTrue(r, r.contains("Idea:"));
        assertTrue(r, r.contains("abre un PR"));
    }

    @Test public void withoutAnExamItAsksForOne() {
        assertTrue(ImprovementProposals.report(null, null).contains("hazte el examen"));
    }

    @Test public void bryansCorrectionsAreIncludedMostRepeatedFirst() {
        LessonBook.Lesson a = new LessonBook.Lesson(); a.source = "bryan"; a.expected = "Bryan me corrigió: «no me repitas»"; a.times = 4; a.did = "Hola, ¿cómo estás?";
        LessonBook.Lesson b = new LessonBook.Lesson(); b.source = "yo"; b.expected = "me pillé yo"; b.times = 9;
        String r = ImprovementProposals.report(null, Arrays.asList(b, a));
        assertTrue(r, r.contains("no me repitas") && r.contains("(4 veces)"));
        assertFalse("solo las correcciones de Bryan", r.contains("me pillé yo"));
    }

    @Test public void commandsAreExactPhrases() {
        for (String s : new String[]{"Evoluciona", "prográmate", "auto prográmate", "Salve, propuestas de mejora", "¿Qué deberías mejorar?", "evoluciona y crea"})
            assertTrue(s, ImprovementProposals.isCommand(s));
        for (String s : new String[]{"evoluciona tu corazón", "el inglés evoluciona", "programa una alarma"})
            assertFalse(s, ImprovementProposals.isCommand(s));
    }
}
