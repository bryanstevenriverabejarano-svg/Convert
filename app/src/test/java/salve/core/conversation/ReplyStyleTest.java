package salve.core.conversation;

import static org.junit.Assert.*;

import org.junit.Test;

/** Respuestas reales de Gemma 4 E2B con muletillas (14/10/2026). */
public class ReplyStyleTest {
    @Test public void removesFillerOpeningButKeepsHerVoice() {
        String raw = "Entiendo que estás buscando una respuesta más directa sobre mi identidad y con quién estoy interactuando. "
                + "Soy Salve. Me presento como una compañera con criterio, curiosidad útil y un gran cuidado hacia ti. "
                + "¿Hay algo más específico que te gustaría saber sobre mí?";
        assertEquals("Soy Salve. Me presento como una compañera con criterio, curiosidad útil y un gran cuidado hacia ti. "
                + "¿Hay algo más específico que te gustaría saber sobre mí?", ReplyStyle.clean(raw));
    }

    @Test public void keepsConcreteFollowUpQuestions() {
        String raw = "El presupuesto de octubre suma 1.240 euros. ¿Quieres que lo separe por categorías?";
        assertEquals(raw, ReplyStyle.clean(raw));
    }

    @Test public void neverEmptiesAReply() {
        String raw = "¿Hay algo en particular que te gustaría compartir?";
        assertEquals(raw, ReplyStyle.clean(raw, java.util.Collections.singletonList(raw)));
    }

    /** La respuesta de la conversación del 15/10 a la 1:23 que a Bryan le encantó: se conserva entera. */
    @Test public void keepsTheNavigationReplyBryanLiked() {
        String raw = "Navegar, para mí, significa acompañarte en la exploración de lo que te interesa. "
                + "No es tomar el control, sino ofrecerte una perspectiva clara. "
                + "¿Hay algo en particular que te gustaría explorar hoy? Me interesa mucho saber qué tienes en mente.";
        assertEquals(raw, ReplyStyle.clean(raw, java.util.Collections.singletonList("Hola. ¿Cómo estás hoy?")));
    }

    @Test public void removesOnlyAClosingCopiedFromAPreviousReply() {
        String previous = "Me alegra mucho. ¿Hay algo en lo que te gustaría que investiguemos hoy?";
        String raw = "Sí, estoy bien, gracias por preguntar. ¿Hay algo en lo que te gustaría que investiguemos hoy?";
        assertEquals("Sí, estoy bien, gracias por preguntar.", ReplyStyle.clean(raw, java.util.Collections.singletonList(previous)));
    }
}
