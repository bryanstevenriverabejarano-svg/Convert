package salve.sketch;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LearningTest {
    private static List<float[]> square() { return Arrays.asList(new float[]{0, 0, 1000, 0, 1000, 1000, 0, 1000, 0, 0}); }

    @Test public void learnedMotifsPersistAndJoinTheRepertoire() throws Exception {
        File dir = Files.createTempDirectory("sketch").toFile();
        LearnedSketches store = new LearnedSketches(dir);
        SketchLibrary.Motif m = store.learn("Un mueble", square(), 1.2f, "photo");
        assertEquals("mueble", m.id);
        assertEquals("un mueble", m.name);
        assertTrue(m.learned());
        // Otra instancia lee el archivo.
        List<SketchLibrary.Motif> again = new LearnedSketches(dir).all();
        assertEquals(1, again.size());
        SketchLibrary lib = SketchTestAssets.library().withLearned(again);
        assertEquals("mueble", lib.find("muebles").id);
        assertEquals(SketchRequest.Kind.DRAW, SketchRequest.parse("dibújame un mueble", lib).kind);
        assertNotNull(lib.byId("arbol"));
        // Enseñarle "casa" sustituye a la casa de serie.
        store.learn("una casa", square(), 1f, "hand");
        lib = SketchTestAssets.library().withLearned(store.all());
        assertEquals("hand", lib.find("casa").source);
        assertTrue(store.forget("la casa"));
        assertEquals("builtin", SketchTestAssets.library().withLearned(store.all()).find("casa").source);
    }

    @Test public void handDrawingIsNormalizedAndKeepsDots() {
        HandDrawing.Result r = HandDrawing.normalize(Arrays.asList(
                new float[]{100, 100, 300, 100, 300, 200}, new float[]{200, 150}));
        assertEquals(2, r.strokes.size());
        assertEquals(2f, r.aspect, .05f);
        for (float[] s : r.strokes) for (float v : s) assertTrue(v >= -1 && v <= 1001);
    }

    @Test public void shapeProgramAcceptsOnlyItsGrammar() {
        ShapeProgram.Result r = ShapeProgram.parse("Claro, aquí va:\nrect 10 40 80 8\nline 15 48 15 90\nline 85 48 85 90\n"
                + "circle 50 20 10\narc 50 60 10 0 180\nborra el teléfono\nrect 10 10 -5 4\n");
        assertEquals(5, r.shapes);
        assertEquals(1, r.rejected);
        assertTrue(r.isUsable());
        for (float[] s : r.strokes) for (float v : s) assertTrue(v >= -1 && v <= 1001);
        assertFalse(ShapeProgram.parse("No sé dibujar eso.").isUsable());
        assertTrue(ShapeProgram.prompt("una lámpara").endsWith("Ahora «una lámpara»:\n"));
    }

    @Test public void photoRequestsNameWhatTheyTeach() {
        PhotoSketchRequest r = PhotoSketchRequest.parse("Esto es un mueble, dibújame uno", true);
        assertEquals(PhotoSketchRequest.Kind.LEARN, r.kind);
        assertEquals("un mueble", r.name);
        assertEquals("mi taza", PhotoSketchRequest.parse("esto es mi taza", true).name);
        assertEquals("una silla", PhotoSketchRequest.parse("aprende que esto es una silla", false).name);
        assertEquals(PhotoSketchRequest.Kind.DRAW, PhotoSketchRequest.parse("dibújalo", false).kind);
        assertEquals(PhotoSketchRequest.Kind.DRAW, PhotoSketchRequest.parse("Dibuja esta foto", true).kind);
        assertEquals(PhotoSketchRequest.Kind.NONE, PhotoSketchRequest.parse("¿qué es esto?", true).kind);
        assertEquals(PhotoSketchRequest.Kind.NONE, PhotoSketchRequest.parse("describe la foto", true).kind);
        assertEquals(PhotoSketchRequest.Kind.NONE, PhotoSketchRequest.parse("esto es increíble", false).kind);
        assertEquals("toby", PhotoSketchRequest.parse("este se llama Toby, dibújalo", true).name);
        assertEquals(PhotoSketchRequest.Kind.NONE, PhotoSketchRequest.parse("dibújame un árbol", false).kind);
    }
}
