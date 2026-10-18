package salve.core.identity;

import static org.junit.Assert.*;

import org.junit.Test;

public class SensitiveRequestTest {
    private static boolean asks(String text) { return SensitiveRequest.needsVerification(text, false); }

    @Test public void questionsAboutTheCreatorArePrivateCheckpoints() {
        for (String text : new String[]{"¿Quién soy?", "quien soy yo", "Qué sabes de mí", "¿Me conoces?",
                "¿Cómo se llama mi hermana?", "dime quién es mi familia", "cuéntame mis recuerdos", "¿Dónde vivo?",
                "cuál es mi contraseña", "¿cuánto son mis gastos?", "recuérdame lo que te conté ayer", "¿Cómo me llamo?"})
            assertTrue(text, asks(text));
    }

    @Test public void givingInformationOrOrdinaryTalkIsNot() {
        for (String text : new String[]{"Mi hermana se llama Ana", "Hoy he visto a mi madre", "¿Qué tiempo hace?",
                "dibuja un gato", "", "   "})
            assertFalse(text, asks(text));
        assertFalse(asks(null));
    }

    @Test public void privateBudgetTurnsAlwaysCount() {
        assertTrue(SensitiveRequest.needsVerification("hola", true));
    }
}
