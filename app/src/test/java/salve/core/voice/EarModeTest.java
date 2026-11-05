package salve.core.voice;

import static org.junit.Assert.*;
import org.junit.Test;

public class EarModeTest {
    @Test public void commands() {
        assertEquals(EarMode.Command.ON, EarMode.parse("Escúchame de verdad"));
        assertEquals(EarMode.Command.ON, EarMode.parse("Salve, activa el modo oídos"));
        assertEquals(EarMode.Command.ON, EarMode.parse("usa tus oídos"));
        assertEquals(EarMode.Command.OFF, EarMode.parse("desactiva el modo oídos"));
        assertEquals(EarMode.Command.OFF, EarMode.parse("vuelve a usar el dictado"));
        assertNull(EarMode.parse("escúchame una cosa"));
        assertNull(EarMode.parse("me duelen los oídos"));
    }

    @Test public void readsWhatTheModelHeard() {
        EarMode.Heard h = EarMode.parseHeard("TEXTO: pon reggaetón en Spotify\nTONO: animado, con prisa");
        assertEquals("pon reggaetón en Spotify", h.text);
        assertEquals("animado, con prisa", h.tone);
        EarMode.Heard bold = EarMode.parseHeard("**TEXTO:** «hola Salve»\n**TONO:** cansado.");
        assertEquals("hola Salve", bold.text);
        assertEquals("cansado", bold.tone);
        assertEquals("hola qué tal", EarMode.parseHeard("hola qué tal").text);
        assertEquals("", EarMode.parseHeard("TEXTO:\nTONO: ruido").text);
    }
}
