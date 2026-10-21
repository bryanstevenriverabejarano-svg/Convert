package salve.core.mind;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class MemoryHealthTest {
    @Test public void commandPhrases() {
        assertTrue(MemoryHealth.isCommand("¿Cómo está tu memoria?"));
        assertTrue(MemoryHealth.isCommand("Salve, estado de tu memoria"));
        assertTrue(MemoryHealth.isCommand("¿Tienes memoria por significado?"));
        assertFalse(MemoryHealth.isCommand("tengo mala memoria para los nombres"));
    }

    @Test public void saysPlainlyWhetherMeaningSearchWorks() {
        String ready = MemoryHealth.describe(288, 200, MemoryHealth.Semantic.READY, "ok", Arrays.asList("Bryan", "Vivina"));
        assertTrue(ready, ready.contains("288 recuerdos"));
        assertTrue(ready, ready.contains("200 de 288"));
        assertTrue(ready, ready.contains("sigo con los demás"));
        assertTrue(ready, ready.contains("Bryan, Vivina"));
        String waiting = MemoryHealth.describe(288, 0, MemoryHealth.Semantic.DOWNLOADING, "Esperando Wi-Fi.", Collections.emptyList());
        assertTrue(waiting, waiting.contains("solo puedo buscarlos por palabras"));
        assertTrue(waiting, waiting.contains("Esperando Wi-Fi"));
        assertTrue(MemoryHealth.describe(1, 0, MemoryHealth.Semantic.FAILED, "no distingue el español", null).contains("no está funcionando"));
    }
}
