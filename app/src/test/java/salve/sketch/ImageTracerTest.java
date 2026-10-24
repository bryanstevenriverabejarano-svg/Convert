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

    /** Figura de líneas con cruces (cabeza, cuerpo, brazos y alas que se tocan), como el ángel de la prueba en el S24. */
    static int[] crossingFigure(int w, int h) {
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, 0xFFFFFFFF);
        java.util.function.BiConsumer<Integer, Integer> ink = (x, y) -> {
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) px[(y + dy) * w + x + dx] = 0xFF101010;
        };
        for (int t = 0; t < 360; t++) { // cabeza
            double a = Math.toRadians(t);
            ink.accept((int) Math.round(100 + 18 * Math.cos(a)), (int) Math.round(48 + 18 * Math.sin(a)));
        }
        for (int y = 66; y <= 170; y++) ink.accept(100, y);          // cuerpo, toca la cabeza
        for (int x = 55; x <= 145; x++) ink.accept(x, 95);           // brazos, cruzan el cuerpo
        for (int i = 0; i <= 60; i++) {                              // alas, salen de los hombros
            ink.accept(100 - i, 95 - i / 2); ink.accept(100 + i, 95 - i / 2);
        }
        for (int x = 70; x <= 130; x++) ink.accept(x, 170);          // bajo de la túnica, cruza el cuerpo
        return px;
    }

    @Test public void contoursCutAtCrossingsAreJoinedAgain() {
        int w = 200, h = 200;
        int[] px = crossingFigure(w, h);
        ImageTracer.Result r = ImageTracer.trace(px, w, h, 1200);
        assertTrue(r.isUsable());
        // Antes cada cruce dejaba rayas sueltas; ahora los trozos se empalman por sus extremos.
        assertTrue("antes 7 trazos; ahora " + r.strokes.size(), r.strokes.size() <= 6);
    }

    /** Escudo heráldico con rellenos de color (como «Male angel heraldry»): se calca por bordes, no por esqueleto. */
    static int[] heraldicAngel(int w, int h) {
        int[] px = new int[w * h];
        java.util.Random r = new java.util.Random(7);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = 0xFF3A6EA5;
            double hx = (x - 160) / 22.0, hy = (y - 70) / 22.0;
            boolean head = hx * hx + hy * hy < 1;
            boolean body = y > 92 && y < 280 && Math.abs(x - 160) < 18 + (y - 92) * 0.35;
            boolean wingL = x < 150 && x > 40 && y > 80 && y < 80 + (150 - x) * 0.9 && y > 120 - (150 - x) * 0.25;
            boolean wingR = x > 170 && x < 280 && y > 80 && y < 80 + (x - 170) * 0.9 && y > 120 - (x - 170) * 0.25;
            if (wingL || wingR) c = 0xFFE8E8E8;
            if (body) c = 0xFFF2D27A;
            if (head) c = 0xFFF5C9A0;
            if (body && x % 23 == 0) c = 0xFF8A6A20; // pliegues de la túnica
            int n = r.nextInt(7) - 3;
            int red = Math.min(255, Math.max(0, ((c >> 16) & 255) + n)), green = Math.min(255, Math.max(0, ((c >> 8) & 255) + n)),
                    blue = Math.min(255, Math.max(0, (c & 255) + n));
            px[y * w + x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
        return px;
    }

    @Test public void filledHeraldryIsNotLeftInPieces() {
        int w = 320, h = 300;
        ImageTracer.Result r = ImageTracer.trace(heraldicAngel(w, h), w, h, 1200);
        assertTrue(r.isUsable());
        assertTrue("antes 25 trozos; ahora " + r.strokes.size(), r.strokes.size() <= 20);
        SketchStyle.Result mine = SketchStyle.ownVersion(r.strokes, SketchStyle.MAX_STROKES);
        assertEquals("no tira ninguna línea", r.strokes.size(), mine.kept);
    }

    @Test public void joinMergesNearbyEndsButNotDistantLines() {
        java.util.List<int[]> chains = new java.util.ArrayList<>();
        chains.add(new int[]{0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0});
        chains.add(new int[]{13, 0, 12, 0, 11, 0, 10, 0, 9, 0, 8, 0, 7, 0}); // al revés, a 2 px
        chains.add(new int[]{14, 1, 15, 1, 16, 1, 17, 1});                  // pegada al final
        chains.add(new int[]{0, 50, 1, 50, 2, 50, 3, 50, 4, 50, 5, 50});    // lejos: aparte
        java.util.List<int[]> joined = ImageTracer.join(chains, ImageTracer.joinGap(100));
        assertEquals(2, joined.size());
        int[] line = joined.get(0);
        assertEquals("una sola línea de 0 a 17", 17, Math.max(line[0], line[line.length - 2]));
        assertEquals(0, Math.min(line[0], line[line.length - 2]));
        for (int i = 2; i < line.length; i += 2)
            assertTrue("sin saltos de ida y vuelta", Math.abs(line[i] - line[i - 2]) <= 3);
    }
}

