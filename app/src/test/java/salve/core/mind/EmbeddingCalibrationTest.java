package salve.core.mind;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class EmbeddingCalibrationTest {
    /** Embedder que entiende el significado: cada par documento/paráfrasis comparte dirección. */
    private static MindEmbedder understanding() {
        Map<String, Integer> topic = new HashMap<>();
        for (int i = 0; i < EmbeddingCalibration.DOCS.length; i++) {
            topic.put(EmbeddingCalibration.DOCS[i], i);
            topic.put(EmbeddingCalibration.QUERIES[i], i);
        }
        return text -> {
            float[] v = new float[16];
            for (int i = 0; i < v.length; i++) v[i] = 0.05f;
            v[topic.get(text)] = 1f;
            v[8 + (text.length() % 8)] += 0.2f; // ruido superficial
            return v;
        };
    }

    /** Embedder como gte-small en español: todo se parece a todo. */
    private static MindEmbedder englishOnly() {
        return text -> {
            float[] v = new float[16];
            for (int i = 0; i < v.length; i++) v[i] = 1f;
            v[text.length() % 16] += 0.15f;
            return v;
        };
    }

    @Test public void modelThatUnderstandsSpanishIsActivatedWithCalibratedThreshold() {
        EmbeddingCalibration.Result r = EmbeddingCalibration.run(understanding());
        assertTrue(r.describe(), r.passed);
        assertEquals(1.0, r.accuracy, 1e-9);
        assertTrue(r.threshold < r.relatedMean && r.threshold > r.unrelatedMean);
        assertEquals(0, r.surprise(r.relatedMean), 1e-9);
        assertEquals(1, r.surprise(r.unrelatedMean - 0.1), 1e-9);
    }

    @Test public void modelThatCannotSeparateMeaningStaysOff() {
        EmbeddingCalibration.Result r = EmbeddingCalibration.run(englishOnly());
        assertFalse(r.passed);
        assertTrue(r.describe().contains("desactivada"));
        assertEquals(-1, r.surprise(0.5), 0);
    }

    @Test public void brokenEmbedderIsReportedNotThrown() {
        EmbeddingCalibration.Result r = EmbeddingCalibration.run(text -> { throw new IllegalStateException("onnx"); });
        assertFalse(r.passed);
        assertTrue(r.describe().contains("IllegalStateException"));
    }

    @Test public void retrievalKeepsQueriesAndPassagesSeparate() {
        MindEmbedder semantic = understanding();
        java.util.List<String> queryCalls = new java.util.ArrayList<>();
        java.util.List<String> passageCalls = new java.util.ArrayList<>();
        EmbeddingCalibration.Result r = EmbeddingCalibration.run(
                text -> { queryCalls.add(text); return semantic.embed(text); },
                text -> { passageCalls.add(text); return semantic.embed(text); });
        assertTrue(r.passed);
        assertEquals(java.util.Arrays.asList(EmbeddingCalibration.QUERIES), queryCalls);
        assertEquals(java.util.Arrays.asList(EmbeddingCalibration.DOCS), passageCalls);
    }

    @Test public void observeIsSkippedWhenScaleRejectsMeasurement() {
        MindTestSupport.MemoryStore store = new MindTestSupport.MemoryStore();
        MindTestSupport.Clock clock = new MindTestSupport.Clock();
        SelfModelJournal journal = MindTestSupport.journal(store, clock);
        MindTestSupport.ScriptedGenerator model = new MindTestSupport.ScriptedGenerator().then("algo");
        PredictionLoop loop = new PredictionLoop(journal, model, t -> new float[]{1, 0}, clock::get, c -> -1);
        loop.predict("hola", "hola Bryan");
        assertEquals(-1, loop.observe("otra"), 0);
        assertEquals(0, journal.metrics().predictions);
    }
}
