package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class GenerationSpeedTest {
    @Test public void separatesReadingFromWriting() {
        GenerationSpeed.Sample s = GenerationSpeed.measure(1000, 2200, 4200,
                "{\"mode\":\"respond\",\"message\":\"Hola Bryan, hoy he aprendido algo nuevo sobre ti\",\"action\":null}", "LiteRT-LM GPU · MTP");
        assertEquals(1200, s.firstWordMs);
        assertEquals(2000, s.writingMs);
        assertEquals(9, s.words); // solo el texto de «message»
        assertTrue(s.describe(), s.describe().startsWith("primera palabra en 1,2 s"));
        assertTrue(s.describe(), s.describe().endsWith("LiteRT-LM GPU · MTP"));
    }

    @Test public void tooShortToMeasureSpeedIsNotInvented() {
        GenerationSpeed.Sample s = GenerationSpeed.measure(0, 500, 520, "Sí.", "x");
        assertEquals(0.0, s.wordsPerSecond(), 0.0);
        assertFalse(s.describe().contains("palabras/s"));
        GenerationSpeed.Sample none = GenerationSpeed.measure(0, 0, 3000, "", "x");
        assertEquals(3000, none.firstWordMs);
    }

    @Test public void summaryAveragesRecentAnswers() {
        GenerationSpeed.clear();
        assertEquals("", GenerationSpeed.summary());
        GenerationSpeed.record(GenerationSpeed.measure(0, 1000, 2000, "uno dos tres cuatro cinco seis siete ocho nueve diez", "x"));
        GenerationSpeed.record(GenerationSpeed.measure(0, 2000, 4000, "uno dos tres cuatro cinco seis siete ocho nueve diez", "x"));
        assertEquals("Velocidad (media de 2): primera palabra 1,5 s · 7,5 palabras/s", GenerationSpeed.summary());
        GenerationSpeed.clear();
    }
}
