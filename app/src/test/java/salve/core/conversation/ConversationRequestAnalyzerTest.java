package salve.core.conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConversationRequestAnalyzerTest {
    @Test
    public void classifiesCommonConversationActs() {
        assertAct(ConversationAct.EXPLANATION_REQUEST, "Explícame cómo funciona la memoria");
        assertAct(ConversationAct.CORRECTION, "Eso no es correcto");
        assertAct(ConversationAct.OPINION, "Creo que esta respuesta es demasiado larga");
        assertAct(ConversationAct.COMMAND, "Analiza este texto");
        assertAct(ConversationAct.CASUAL, "Hola");
        assertAct(ConversationAct.QUESTION, "¿Cuál es el resultado?");
    }

    @Test
    public void orphanReferenceRequestsClarification() {
        ConversationAnalysis analysis = ConversationRequestAnalyzer.analyze("Hazlo", false);

        assertEquals(ConversationAct.COMMAND, analysis.getAct());
        assertTrue(analysis.needsClarification());
    }

    @Test
    public void referenceUsesAvailableConversationContext() {
        ConversationAnalysis analysis = ConversationRequestAnalyzer.analyze("Hazlo", true);

        assertFalse(analysis.needsClarification());
    }

    private static void assertAct(ConversationAct expected, String input) {
        assertEquals(expected, ConversationRequestAnalyzer.analyze(input, true).getAct());
    }
}
