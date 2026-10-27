package salve.core.voice;

import static org.junit.Assert.*;

import org.junit.Test;

public class WakeWordTest {
    @Test public void wakesWhenCalledByNameAtTheStart() {
        assertEquals("", WakeWord.detect("Salve").request);
        assertEquals("", WakeWord.detect("oye Salve").request);
        assertEquals("¿qué tiempo hace?", WakeWord.detect("Hola Salve, ¿qué tiempo hace?").request);
        assertEquals("pon música de Rosalía", WakeWord.detect("salve pon música de Rosalía").request);
    }

    @Test public void ignoresTheWordInsideOtherSentences() {
        assertNull(WakeWord.detect("Dios te salve María"));
        assertNull(WakeWord.detect("me salvé por poco"));
        assertNull(WakeWord.detect("Salvé el archivo"));
        assertNull(WakeWord.detect("salvavidas"));
        assertNull(WakeWord.detect("hola qué tal"));
        assertNull(WakeWord.detect(null));
    }

    @Test public void bryanTurnsItOnAndOffWithWords() {
        assertTrue(WakeWord.asksToEnable("Escúchame siempre"));
        assertTrue(WakeWord.asksToEnable("Salve, activa la palabra de activación"));
        assertTrue(WakeWord.asksToDisable("Desactiva la palabra Salve"));
        assertTrue(WakeWord.asksToDisable("deja de escuchar tu nombre"));
        assertFalse(WakeWord.asksToEnable("¿qué es una palabra de activación?"));
        assertFalse(WakeWord.asksToDisable("hola"));
    }
}
