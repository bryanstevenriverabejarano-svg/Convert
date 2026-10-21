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
        assertTrue(MemoryHealth.isCommand("Que tal tu memoria?"));
        assertTrue(MemoryHealth.isCommand("¿Qué tal va la memoria?"));
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

    @Test public void tellsWhatIsHappeningNow() {
        assertTrue(MemoryHealth.extra(true, "", "", false).contains("creando los vectores"));
        assertTrue(MemoryHealth.extra(false, "OutOfMemoryError", "", false).contains("OutOfMemoryError"));
        String restoring = MemoryHealth.extra(false, "", "Recuperados 45 de 330.", false);
        assertTrue(restoring, restoring.contains("trayendo recuerdos de pCloud") && restoring.contains("45 de 330"));
        assertTrue(MemoryHealth.extra(false, "", "Completa.", true).contains("entera"));
        assertEquals("", MemoryHealth.extra(false, "", "", false));
    }
}
