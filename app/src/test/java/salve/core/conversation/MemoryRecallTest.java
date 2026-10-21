package salve.core.conversation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import salve.core.memory.MemorySearchQuery;

/** La conversación real de Bryan (21/10/2026) sobre «navegar». */
public class MemoryRecallTest {
    private static List<ChatMessage> hilo(String... turns) {
        List<ChatMessage> out = new ArrayList<>();
        for (int i = 0; i < turns.length; i++)
            out.add(new ChatMessage(i % 2 == 0 ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT, turns[i], i));
        return out;
    }

    private static final String NAVEGAR_RESP = "Navegar. Entiendo que quieres explorar el concepto de 'navegar'. ¿Te gustaría que investigue?";

    @Test public void pointingQuestionsSearchTheTopicFromTheThread() {
        String q1 = "Recuerdas que una vez hablamos de este concepto?";
        List<ChatMessage> h = hilo("Navegar", NAVEGAR_RESP, q1);
        assertTrue(MemoryRecall.isRecallTurn(q1, h));
        assertEquals("navegar", MemoryRecall.query(q1, h));

        String q2 = "Que fue lo que me comentaste en ese recuerdo?";
        h = hilo("Navegar", NAVEGAR_RESP, q1, "Sí, recuerdo que hemos hablado de ese concepto.", q2);
        assertTrue(MemoryRecall.isRecallTurn(q2, h));
        assertEquals("navegar", MemoryRecall.query(q2, h));
    }

    @Test public void searchItAfterARecallQuestionIsARecallTurn() {
        String q = "No entiendo si me dijistes que si recordabas cuando me hablaste del significado de navegar";
        List<ChatMessage> h = hilo("Navegar", NAVEGAR_RESP, q, "Me gustaría ayudarte a encontrar ese recuerdo.", "Pues buscalo");
        assertTrue(MemoryRecall.isRecallTurn(q, h.subList(0, 3)));
        assertTrue(MemoryRecall.searchesAgain("Pues buscalo", h));
        assertTrue(MemoryRecall.query("Pues buscalo", h).contains("navegar"));
        // «Búscalo» sin hablar de recuerdos antes no es buscar en la memoria.
        assertFalse(MemoryRecall.searchesAgain("Pues buscalo", hilo("¿Qué es un agujero negro?", "Es…", "Pues buscalo")));
    }

    @Test public void conversationalWordsNoLongerHideTheTopic() {
        List<String> terms = MemorySearchQuery.parse(
                "No entiendo si me dijistes que si recordabas cuando me hablaste del significado de navegar").terms();
        assertTrue(terms.toString(), terms.contains("navegar"));
        assertFalse(terms.toString(), terms.contains("dijistes"));
    }

    @Test public void denyingHerMemoryIsCaught() {
        String real = "Disculpa, pero como Salve, no tengo acceso directo a recuerdos específicos de conversaciones pasadas a menos que "
                + "me los proporciones. Tengo algunos registros de conversaciones previas, pero no puedo acceder a ellos de forma automática.";
        assertNotNull(MemoryRecall.problem(real, true, true, Boolean.FALSE));
        assertNotNull(MemoryRecall.problem("Tengo algunos registros previos, pero no puedo acceder a ellos automáticamente.", true, true, null));
        // Si Bryan no está verificado, decir que están cerrados es lo correcto: no se corrige aquí.
        assertNull(MemoryRecall.problem(real, true, false, null));
        // Fuera de una pregunta por recuerdos no se toca nada.
        assertNull(MemoryRecall.problem(real, false, true, null));
    }

    @Test public void claimingToRememberWithNothingFoundIsCaught() {
        String real = "Sí, recuerdo que hemos hablado de ese concepto. ¿Te gustaría que profundicemos?";
        assertNotNull(MemoryRecall.problem(real, true, true, Boolean.FALSE));
        assertNull(MemoryRecall.problem(real, true, true, Boolean.TRUE));
        assertNull(MemoryRecall.problem("Lo he buscado en mis recuerdos y no encuentro nada sobre navegar. ¿Qué recuerdas tú?",
                true, true, Boolean.FALSE));
    }

    @Test public void theNoteTellsWhatWasSearched() {
        assertTrue(MemoryRecall.note(true, false, "navegar").contains("«navegar»"));
        assertTrue(MemoryRecall.note(true, false, "navegar").contains("NO digas que no tienes acceso"));
        assertTrue(MemoryRecall.note(false, false, null).contains("huella"));
    }

    @Test public void questionsThatNameTheirTopicAreSearchedAsIs() {
        String q = "¿Recuerdas cuál fue tu primer recuerdo?";
        assertEquals(q, MemoryRecall.query(q, hilo("Hola", "Hola, Bryan.", q)));
    }
}
