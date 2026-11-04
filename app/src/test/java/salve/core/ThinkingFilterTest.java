package salve.core;

import static org.junit.Assert.*;
import org.junit.Test;

/** Salve nunca dice ni guarda el «pensamiento en voz alta» (Qwen, Gemma 4). */
public class ThinkingFilterTest {
    @Test public void answersWithoutThinkingStayTheSame() {
        assertEquals("Hola, Bryan.", ThinkingFilter.strip("Hola, Bryan."));
        assertNull(ThinkingFilter.strip(null));
        assertEquals("{\"respuesta\":\"vale\"}", ThinkingFilter.strip("{\"respuesta\":\"vale\"}"));
    }

    @Test public void removesThinkingBlocks() {
        assertEquals("Son las cinco.", ThinkingFilter.strip("<think>\nEl usuario pregunta la hora…\n</think>\n\nSon las cinco."));
        assertEquals("Hecho.", ThinkingFilter.strip("<think></think>Hecho."));
        assertEquals("A y B.", ThinkingFilter.strip("<think>uno</think>A y <think>dos</think>B."));
    }

    @Test public void cutOrOrphanThinking() {
        assertEquals("cortado por el límite: nada que decir", "", ThinkingFilter.strip("<think>pensando sin terminar"));
        assertEquals("Respuesta.", ThinkingFilter.strip("razonamiento previo</think>\nRespuesta."));
    }

    @Test public void gemma4ThoughtChannelsAndTurnMarkers() {
        assertEquals("Hola, Bryan.", ThinkingFilter.strip("<|channel>thought\nEl usuario saluda.<channel|>Hola, Bryan."));
        assertEquals("Hola.", ThinkingFilter.strip("<|channel>thought\n<channel|>Hola.<turn|>"));
        assertEquals("", ThinkingFilter.strip("<|channel>thought\npensando sin terminar"));
        assertEquals("Vale.", ThinkingFilter.strip("<|turn>model\nVale."));
        assertEquals("ninguna marca: tal cual", "Me encanta el canal de YouTube.", ThinkingFilter.strip("Me encanta el canal de YouTube."));
    }
}
