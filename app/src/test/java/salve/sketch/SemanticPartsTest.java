package salve.sketch;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Dibujar por partes con sentido: cada parte con nombre, descripción, puntos y armónicos. */
public class SemanticPartsTest {
    private static final String NOTEBOOK = "Claro: {\"thoughtSummary\": \"Dibujando un cuaderno abierto con hojas y encuadernado central.\","
            + "\"parts\": ["
            + "{\"id\":\"portada_exterior\",\"name\":\"Tapas y márgenes\",\"description\":\"Trazo el contorno exterior de las tapas\","
            + "\"isOpenPath\":false,\"maxHarmonics\":35,\"points\":[{\"x\":0.15,\"y\":0.20},{\"x\":0.85,\"y\":0.20},{\"x\":0.85,\"y\":0.80},{\"x\":0.15,\"y\":0.80}]},"
            + "{\"id\":\"lomo\",\"name\":\"Pliegue del lomo\",\"description\":\"Defino el lomo central\",\"isOpenPath\":true,\"maxHarmonics\":20,"
            + "\"points\":[{\"x\":0.50,\"y\":0.18},{\"x\":0.49,\"y\":0.50},{\"x\":0.50,\"y\":0.82}]},"
            + "{\"id\":\"hojas\",\"name\":\"Volumen de las páginas\",\"description\":\"Curvo la caída del papel\",\"isOpenPath\":true,\"maxHarmonics\":40,"
            + "\"points\":[{\"x\":0.18,\"y\":0.25},{\"x\":0.48,\"y\":0.23},{\"x\":0.52,\"y\":0.23},{\"x\":0.82,\"y\":0.25}]}]}";

    @Test public void readsTheDrawingByParts() {
        ShapeProgram.Result r = PartsProgram.parse(NOTEBOOK);
        assertNotNull(r);
        assertEquals(3, r.shapes);
        assertTrue(r.isUsable());
        assertEquals(3, r.parts.size());
        assertEquals("tapas y márgenes", r.parts.get(0).name);
        assertEquals("Defino el lomo central", r.parts.get(1).description);
        assertTrue(r.summary.startsWith("Dibujando un cuaderno"));
        float[] cover = r.strokes.get(0);
        assertTrue("contorno cerrado y suavizado con muchos puntos", cover.length > 40);
        assertEquals(cover[0], cover[cover.length - 2], 1.5);
        for (float[] s : r.strokes) for (float v : s) assertTrue(v >= -5 && v <= 1005);
        assertNull(PartsProgram.parse("rect 1 2 3 4"));
        assertNull(PartsProgram.parse("{\"parts\": []}"));
    }

    @Test public void shapeProgramsCanNameTheirParts() {
        ShapeProgram.Result r = ShapeProgram.parse("# cuerpo: un óvalo sentado\nblob 40 55 52 48 64 56 66 78 58 92 40 92 32 78\n"
                + "# cabeza: redonda sobre el cuerpo\nblob 44 30 52 26 62 30 62 42 53 48 44 42\n"
                + "# orejas\npoly 46 30 47 20 52 27\npoly 56 27 61 20 61 31");
        assertEquals(4, r.shapes);
        assertEquals(3, r.parts.size());
        assertEquals("orejas", r.parts.get(2).name);
        assertEquals(2, r.parts.get(2).from);
        assertEquals(4, r.parts.get(2).to);
        assertEquals("redonda sobre el cuerpo", r.parts.get(1).description);
        assertTrue(ShapeProgram.prompt("gorila").contains("# cabeza"));
    }

    @Test public void uniformPenKeepsCornersAndHarmonicsSmoothNoise() {
        float[] square = {0, 0, 100, 0, 100, 100, 0, 100, 0, 0};
        float[] u = FourierStroke.uniform(square, 10, 500);
        assertEquals(2 * 41, u.length);
        assertEquals(100, u[20], 1e-3); assertEquals(0, u[21], 1e-3); // la esquina sigue ahí
        float[] noisy = new float[2 * 120];
        for (int i = 0; i < 120; i++) {
            double a = 2 * Math.PI * i / 119;
            float r = 300 + (i % 2 == 0 ? 12 : -12);
            noisy[2 * i] = 500 + r * (float) Math.cos(a); noisy[2 * i + 1] = 500 + r * (float) Math.sin(a);
        }
        float[] smooth = FourierStroke.lowPass(noisy, 12);
        double worst = 0;
        for (int i = 0; i < smooth.length; i += 2) worst = Math.max(worst, Math.abs(Math.hypot(smooth[i] - 500, smooth[i + 1] - 500) - 300));
        assertTrue("el temblor de ±12 baja: " + worst, worst < 6);
    }

    @Test public void animationTellsWhichPartItIsDrawing() throws Exception {
        File dir = Files.createTempDirectory("parts").toFile();
        LearnedSketches store = new LearnedSketches(dir);
        List<float[]> strokes = Arrays.asList(new float[]{100, 100, 900, 100, 900, 900, 100, 900, 100, 100},
                new float[]{500, 100, 500, 900}, new float[]{0, 0}, new float[]{200, 300, 800, 300});
        List<SketchLibrary.Part> parts = Arrays.asList(new SketchLibrary.Part("tapas", "el contorno", 0, 1),
                new SketchLibrary.Part("lomo", "", 1, 3), new SketchLibrary.Part("hojas", "curvas", 3, 4));
        SketchLibrary.Motif m = store.learn("un cuaderno", strokes, 1, "model", parts);
        assertEquals(3, m.strokeCount());
        assertEquals("el trazo vacío no descoloca las partes", 2, m.parts.get(2).from);
        SketchLibrary.Motif again = new LearnedSketches(dir).all().get(0);
        assertEquals(3, again.parts.size());
        SketchScene scene = SketchScene.compose(java.util.Collections.singletonList(again));
        assertEquals(3, scene.parts.size());
        SketchAnimation a = new SketchAnimation(scene);
        assertEquals("(1/3) tapas: el contorno", a.caption(0.01f));
        assertEquals(2, a.partAt(a.duration() - 0.01f));
        assertNull(a.caption(a.duration() + 1));
    }
}
