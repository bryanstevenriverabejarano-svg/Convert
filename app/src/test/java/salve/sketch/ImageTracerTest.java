package salve.sketch;

import static org.junit.Assert.*;
import org.junit.Test;

public class ImageTracerTest {
    /** Una "foto" sintética: mesa (tablero y dos patas) sobre pared texturada con suelo. */
    private static int[] table(int w, int h) {
        int[] px = new int[w * h];
        java.util.Random r = new java.util.Random(4);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int base = y > h * .75 ? 150 : 210;
            int v = base + r.nextInt(9) - 4;
            boolean top = y >= 70 && y < 80 && x >= 60 && x < 180;
            boolean leg = y >= 80 && y < 170 && ((x >= 70 && x < 78) || (x >= 162 && x < 170));
            if (top || leg) v = 70 + r.nextInt(5);
            px[y * w + x] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        return px;
    }

    @Test public void tracesTheObjectAndDescribesItsGeometry() {
        ImageTracer.Result r = ImageTracer.trace(table(240, 220), 240, 220, 1200, true);
        assertTrue(r.isUsable());
        assertTrue("tablero y patas: varios tramos", r.geometry.lines >= 6);
        assertTrue(r.geometry.horizontal >= 2);
        assertTrue(r.geometry.vertical >= 2);
        assertTrue(r.geometry.describe().contains("rectos"));
        int points = 0;
        for (float[] s : r.strokes) {
            points += s.length / 2;
            for (float v : s) assertTrue(v >= -1 && v <= 1001);
        }
        assertTrue(points > 40 && points < 2000);
        // El suelo que cruza toda la foto no forma parte del objeto.
        for (float[] s : r.strokes) {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            for (int i = 0; i < s.length; i += 2) { minX = Math.min(minX, s[i]); maxX = Math.max(maxX, s[i]); }
            assertTrue(maxX - minX < 1000);
        }
        new SketchAnimation(SketchScene.compose(java.util.Collections.singletonList(
                new SketchLibrary.Motif("mesa", "una mesa", SketchLibrary.Place.ANY, r.aspect, false,
                        java.util.Collections.singletonList("mesa"), r.strokes.toArray(new float[0][]), "photo"))));
    }

    @Test public void lineDrawingsFollowTheCentreOfEachLine() {
        int w = 160, h = 160;
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, 0xFFFFFFFF);
        for (int t = 0; t < 360; t++) { // círculo dibujado con rotulador de 3 px
            double a = Math.toRadians(t);
            int cx = (int) Math.round(80 + 50 * Math.cos(a)), cy = (int) Math.round(80 + 50 * Math.sin(a));
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) px[(cy + dy) * w + cx + dx] = 0xFF101010;
        }
        assertNotNull(ImageTracer.lineArtSkeleton(ImageTracer.luminance(px, w, h), w, h));
        ImageTracer.Result r = ImageTracer.trace(px, w, h, 800);
        assertTrue("un solo trazo, no dos bordes", r.strokes.size() <= 2);
        assertTrue(r.geometry.curves >= 1);
    }

    @Test public void emptyImageGivesNothingToDraw() {
        int[] px = new int[64 * 64];
        java.util.Arrays.fill(px, 0xFF808080);
        assertFalse(ImageTracer.trace(px, 64, 64, 500).isUsable());
    }

    @Test public void simplifyKeepsCorners() {
        float[] l = {0, 0, 5, .2f, 10, 0, 10, 5, 10, 10};
        float[] s = ImageTracer.simplify(l, 1f);
        assertEquals(6, s.length);
    }
}
