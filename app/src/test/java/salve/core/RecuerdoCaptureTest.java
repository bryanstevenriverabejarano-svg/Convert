package salve.core;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.Assert.*;

public class RecuerdoCaptureTest {
    @Test public void originalTextDoesNotDependOnLegacyWordCodes() {
        String text = "Bryan vive en Bogotá.  Prefiere Café ☕";
        Recuerdo memory = new Recuerdo(text, "neutral", 7, 7,
                Collections.emptyList(), ignored -> "0001-0002-", 123456789L);
        assertEquals(text, memory.getTexto(null));
        assertEquals("0001-0002-", memory.getBinarioCodificado());
        assertEquals(123456789L, memory.getTimestamp());
    }

    @Test public void captureMetadataIsImmutableEvenIfWriterRunsLater() {
        ArrayList<String> tags = new ArrayList<>(Arrays.asList("hecho_usuario"));
        Recuerdo first = new Recuerdo("Primero", "neutral", 7, 7, tags, text -> text, 10L);
        Recuerdo second = new Recuerdo("Después", "neutral", 7, 7, tags, text -> text, 20L);
        tags.clear();
        assertTrue(first.getTimestamp() < second.getTimestamp());
        assertEquals(Arrays.asList("hecho_usuario"), first.getEtiquetas());
    }
}
