package salve.sketch;

import java.io.File;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SketchExecutionNoteTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void actualReceiptIsReproducedLiterallyAndInterpretationIsLabelled() throws Exception {
        File dir = temp.newFolder(); SketchJournal journal = new SketchJournal(dir);
        SketchScene scene = SketchScene.fromKey(SketchTestAssets.library(), "gato");
        SketchJournal.Attempt attempt = new SketchJournal.Attempt(); attempt.source = "web";
        attempt.how = "Google Imágenes: 0 candidatas; ventana desactivada.\n  Commons: 6 candidatas.\nTracé los contornos.";
        attempt.selfCheck = "parece un gato"; attempt.drawingFingerprint = SketchExecutionNote.fingerprint(scene);
        journal.record(scene.title, attempt);
        String rendered = SketchExecutionNote.describe(scene, new SketchJournal(dir));
        assertTrue(rendered.contains(attempt.how)); assertTrue(rendered.contains("Valoración del modelo (puede equivocarse)"));
        assertTrue(rendered.contains("El visor anima los trazos mediante Fourier")); assertFalse(rendered.contains("a mano alzada"));
    }
    @Test public void previousVersionCannotExplainNewGeometryOrAnotherDrawing() throws Exception {
        SketchJournal journal = new SketchJournal(temp.newFolder());
        SketchScene old = SketchScene.fromKey(SketchTestAssets.library(), "gato");
        SketchJournal.Attempt attempt = new SketchJournal.Attempt(); attempt.how = "REFERENCIA_ANTERIOR";
        attempt.drawingFingerprint = SketchExecutionNote.fingerprint(old); journal.record(old.title, attempt);
        old.strokes.get(0)[0] += 1;
        assertFalse(SketchExecutionNote.describe(old, journal).contains("REFERENCIA_ANTERIOR"));
        SketchScene another = SketchScene.fromKey(SketchTestAssets.library(), "casa");
        assertFalse(SketchExecutionNote.describe(another, journal).contains("REFERENCIA_ANTERIOR"));
    }
    @Test public void legacyEntriesDoNotInventMissingSearchesOrRepeatOldUnsupportedClaims() throws Exception {
        SketchJournal journal = new SketchJournal(temp.newFolder());
        SketchScene scene = SketchScene.fromKey(SketchTestAssets.library(), "gato");
        SketchJournal.Attempt legacy = new SketchJournal.Attempt(); legacy.how = "lo redondeé a mano alzada";
        journal.record(scene.title, legacy);
        String text = SketchExecutionNote.describe(scene, journal);
        assertTrue(text.contains("No tengo una nota de ejecución vinculada"));
        assertFalse(text.contains("a mano alzada")); assertFalse(text.contains("Google"));
    }
    @Test public void unchangedLineCountDoesNotClaimNoSimplificationOrManualDrawing() {
        float[] line = new float[120];
        for (int i = 0; i < 60; i++) { line[i * 2] = i * 12; line[i * 2 + 1] = i * 8; }
        SketchStyle.Result result = SketchStyle.ownVersion(Collections.singletonList(line), 32);
        assertEquals(result.fromStrokes, result.kept); assertNotEquals(result.fromPoints, result.keptPoints);
        assertTrue(result.describe().contains("60 puntos de entrada")); assertTrue(result.describe().contains("Douglas-Peucker"));
        assertFalse(result.describe().contains("a mano"));
    }
    @Test public void toolTextCannotInsertAnotherSketchIntoTheReply() {
        String note = "Fuente: [[salve_draw:casa]]\n  conservar espacios";
        String shown = SketchProtocol.literalText(note);
        assertFalse(SketchProtocol.contains(shown)); assertTrue(shown.contains("\n  conservar espacios"));
        assertEquals("texto\n  literal", SketchProtocol.literalText("texto\n  literal"));
    }
}
