package salve.core.conversation;

import static org.junit.Assert.*;

import org.junit.Test;

/** Respuestas reales de Gemma 4 E2B con muletillas (14/10/2026). */
public class ReplyStyleTest {
    @Test public void removesFillerOpeningAndGenericClosing() {
        String raw = "Entiendo que estás buscando una respuesta más directa sobre mi identidad y con quién estoy interactuando. "
                + "Soy Salve. Me presento como una compañera con criterio, curiosidad útil y un gran cuidado hacia ti. "
                + "Estoy aquí para conversar contigo y ayudarte a explorar lo que te interese. "
                + "¿Hay algo más específico que te gustaría saber sobre mí o sobre nuestra conversación actual?";
        assertEquals("Soy Salve. Me presento como una compañera con criterio, curiosidad útil y un gran cuidado hacia ti.",
                ReplyStyle.clean(raw));
    }

    @Test public void keepsConcreteFollowUpQuestions() {
        String raw = "El presupuesto de octubre suma 1.240 euros. ¿Quieres que lo separe por categorías?";
        assertEquals(raw, ReplyStyle.clean(raw));
    }

    @Test public void neverEmptiesAReply() {
        String raw = "¿Hay algo en particular que te gustaría compartir?";
        assertEquals(raw, ReplyStyle.clean(raw));
        String two = "Estoy muy bien, gracias por preguntar. ¿Hay algo en particular que te trae por aquí o algo que te interese conversar?";
        assertEquals("Estoy muy bien, gracias por preguntar.", ReplyStyle.clean(two));
    }
}
