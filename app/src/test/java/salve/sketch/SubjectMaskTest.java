package salve.sketch;

import static org.junit.Assert.*;

import org.junit.Test;

public class SubjectMaskTest {
    static final int W = 40, H = 30;

    /** Un sujeto rectangular en el centro de una foto con fondo «ruidoso». */
    static float[] centreMask() {
        float[] m = new float[W * H];
        for (int y = 8; y < 22; y++) for (int x = 12; x < 28; x++) m[y * W + x] = 1f;
        return m;
    }

    static int gray(int argb) { return argb & 255; }

    static int[] photo(boolean whiteSubject) {
        int[] px = new int[W * H];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            boolean subject = y >= 8 && y < 22 && x >= 12 && x < 28;
            int g = subject ? (whiteSubject ? 250 : ((x + y) % 2 == 0 ? 40 : 200)) // escamas: tablero de ajedrez
                    : ((x * 7 + y * 3) % 5) * 50;                                  // fondo con cosas
            px[y * W + x] = 0xFF000000 | (g << 16) | (g << 8) | g;
        }
        return px;
    }

    @Test public void backgroundBecomesWhite() {
        int[] out = SubjectMask.cutout(photo(false), centreMask(), W, H);
        assertEquals(255, gray(out[0]));
        assertEquals(255, gray(out[(H - 1) * W + W - 1]));
    }

    @Test public void theOutlineNeverDisappearsEvenForAWhiteSubject() {
        int[] out = SubjectMask.cutout(photo(true), centreMask(), W, H);
        int inside = gray(out[15 * W + 20]);
        assertTrue("el sujeto blanco queda bastante más oscuro que el fondo: " + inside, inside <= 205);
    }

    @Test public void textureIsSmoothedInsideTheSubject() {
        int[] px = photo(false);
        int[] out = SubjectMask.cutout(px, centreMask(), W, H);
        int before = Math.abs(gray(px[15 * W + 20]) - gray(px[15 * W + 21]));
        int after = Math.abs(gray(out[15 * W + 20]) - gray(out[15 * W + 21]));
        assertTrue("las escamas casi desaparecen: " + before + " → " + after, after * 8 < before);
    }

    @Test public void uselessMasksAreRejected() {
        assertTrue(SubjectMask.usable(centreMask(), W, H));
        assertFalse("vacía", SubjectMask.usable(new float[W * H], W, H));
        float[] all = new float[W * H];
        java.util.Arrays.fill(all, 1f);
        assertFalse("todo es sujeto", SubjectMask.usable(all, W, H));
        assertFalse("tamaño equivocado", SubjectMask.usable(centreMask(), W + 1, H));
    }

    @Test public void framesTheSubjectWithAMargin() {
        float[] b = SubjectMask.bounds(centreMask(), W, H);
        assertTrue(b[0] < 12f / W && b[0] > 0);
        assertTrue(b[2] > 28f / W && b[2] < 1);
        assertNull(SubjectMask.bounds(new float[W * H], W, H));
    }

    @Test public void cropResizeKeepsTheSubjectAligned() {
        float[] box = SubjectMask.bounds(centreMask(), W, H);
        float[] small = SubjectMask.cropResize(centreMask(), W, H, box, 20, 20);
        assertEquals("el centro sigue siendo sujeto", 1f, small[10 * 20 + 10], 0f);
        assertEquals("la esquina es margen", 0f, small[0], 0f);
    }

    @Test public void aCutoutPhotoTracesToFewerCleanerLines() {
        int w = 120, h = 90;
        int[] px = new int[w * h];
        float[] mask = new float[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - 60) / 35.0, dy = (y - 45) / 25.0;
            boolean subject = dx * dx + dy * dy <= 1;
            mask[y * w + x] = subject ? 1f : 0f;
            int g = subject ? (((x / 3) + (y / 3)) % 2 == 0 ? 60 : 190) : (((x / 5) * 13 + (y / 4) * 7) % 3) * 110;
            px[y * w + x] = 0xFF000000 | (g << 16) | (g << 8) | g;
        }
        ImageTracer.Result raw = ImageTracer.trace(px, w, h, 1600, true);
        ImageTracer.Result clean = ImageTracer.trace(SubjectMask.cutout(px, mask, w, h), w, h, 1600, true);
        assertTrue("antes " + raw.strokes.size() + ", después " + clean.strokes.size(), clean.strokes.size() < raw.strokes.size());
        assertTrue("queda al menos el contorno", clean.strokes.size() >= 1);
    }
}
