package salve.core.conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResponseLimiterTest {
    @Test
    public void leavesShortTextUntouched() {
        assertEquals("Respuesta breve.", ResponseLimiter.limit("Respuesta breve.", 100));
    }

    @Test
    public void prefersCompleteSentence() {
        String result = ResponseLimiter.limit(
                "Primera frase completa. Segunda frase demasiado larga para el límite.", 35);
        assertTrue(result.startsWith("Primera frase completa."));
        assertTrue(result.endsWith("…"));
    }
}
