package salve.core;

import static org.junit.Assert.*;
import org.junit.Test;

public class ReplyVoiceTest {
    @Test public void salveNeverCallsBryanSalve() {
        assertEquals("Con gusto. He tomado nota.", ReplyVoice.withoutSelfVocative("Con gusto, Salve. He tomado nota."));
        assertEquals("Claro, te lo dibujo.", ReplyVoice.withoutSelfVocative("Claro Salve, te lo dibujo."));
        assertEquals("[[salve_draw:bryan]]\nHola! Mira.", ReplyVoice.withoutSelfVocative("[[salve_draw:bryan]]\nHola, Salve! Mira."));
        assertEquals("Aquí lo tienes.", ReplyVoice.withoutSelfVocative("Aquí lo tienes, Salve."));
        assertEquals("sujeto, no vocativo", "Salve dibujó un gorila.", ReplyVoice.withoutSelfVocative("Salve dibujó un gorila."));
        assertEquals("Soy Salve y te ayudo.", ReplyVoice.withoutSelfVocative("Soy Salve y te ayudo."));
        assertNull(ReplyVoice.withoutSelfVocative(null));
    }

    @Test public void aStatusOnlyGoesWithItsOwnDrawing() {
        assertFalse("el dibujo de Bryan no lleva el estado del gorila",
                ReplyVoice.sketchAbout("[[salve_draw:bryan]]", "gorila mejorado"));
        assertTrue(ReplyVoice.sketchAbout("[[salve_draw:gorila]]\nAquí está", "un gorila"));
        assertTrue(ReplyVoice.sketchAbout("[[salve_draw:muneco_de_nieve]]", "muñeco de nieve"));
        assertTrue(ReplyVoice.sketchAbout("[[salve_draw:arbol+luna]]", "árbol"));
        assertTrue("sin dibujo en la respuesta, el estado explica qué pasó", ReplyVoice.sketchAbout("Texto", "gorila"));
        assertTrue(ReplyVoice.sketchAbout("[[salve_draw:bryan]]", null));
    }
}
