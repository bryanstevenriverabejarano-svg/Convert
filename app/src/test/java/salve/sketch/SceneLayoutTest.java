package salve.sketch;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Dibujos con plano: cada motivo dentro de la caja que se midió en una foto. */
public class SceneLayoutTest {
    private static float[] bounds(SketchScene s, SketchLibrary.Part p) {
        float x1 = Float.MAX_VALUE, y1 = Float.MAX_VALUE, x2 = -Float.MAX_VALUE, y2 = -Float.MAX_VALUE;
        for (int i = p.from; i < p.to; i++) {
            float[] st = s.strokes.get(i);
            for (int k = 0; k < st.length; k += 2) {
                x1 = Math.min(x1, st[k]); x2 = Math.max(x2, st[k]); y1 = Math.min(y1, st[k + 1]); y2 = Math.max(y2, st[k + 1]);
            }
        }
        return new float[]{x1, y1, x2, y2};
    }

    @Test public void eachMotifGoesInsideItsBoxStandingOnItsBase() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        List<SketchLibrary.Motif> motifs = Arrays.asList(lib.byId("arbol"), lib.byId("casa"), lib.byId("sol"));
        List<int[]> boxes = Arrays.asList(new int[]{5, 30, 30, 95}, new int[]{40, 45, 90, 95}, new int[]{70, 3, 90, 25});
        SketchScene s = SketchScene.layout(motifs, boxes, null);
        assertNotNull(s);
        assertEquals(3, s.parts.size());
        // Primero lo de detrás (el sol acaba más arriba), luego árbol y casa.
        assertEquals(lib.byId("sol").name, s.parts.get(0).name);
        for (SketchLibrary.Part p : s.parts) {
            int i = p.name.equals(lib.byId("arbol").name) ? 0 : p.name.equals(lib.byId("casa").name) ? 1 : 2;
            int[] b = boxes.get(i);
            float[] got = bounds(s, p);
            float bx1 = b[0] * 10f, by1 = b[1] * 7.5f, bx2 = b[2] * 10f, by2 = b[3] * 7.5f;
            assertTrue(p.name + " dentro en x " + Arrays.toString(got), got[0] >= bx1 - 2 && got[2] <= bx2 + 2);
            assertTrue(p.name + " dentro en y " + Arrays.toString(got), got[1] >= by1 - 2 && got[3] <= by2 + 2);
            assertTrue(p.name + " apoyado en la base", by2 - got[3] < 0.15f * (by2 - by1) + 40);
        }
    }

    @Test public void theKeyTravelsInTheChatAndRebuildsTheSameDrawing() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        SketchScene s = SketchScene.layout(Arrays.asList(lib.byId("libro"), lib.byId("vela")),
                Arrays.asList(new int[]{20, 60, 60, 90}, new int[]{65, 30, 80, 90}), null);
        assertTrue(s.key, s.key.matches("[a-z0-9 +]{2,120}"));
        SketchScene back = SketchScene.fromKey(lib, s.key);
        assertNotNull(back);
        assertEquals(s.strokes.size(), back.strokes.size());
        assertArrayEquals(s.strokes.get(0), back.strokes.get(0), 0.01f);
        // Y sigue valiendo el protocolo del chat.
        SketchProtocol.Extraction e = SketchProtocol.extract("Mira: " + SketchProtocol.tag(s), lib);
        assertFalse(e.scenes.isEmpty());
    }

    @Test public void plainKeysStillWorkAsBefore() throws Exception {
        assertEquals("casa+luna", SketchScene.fromKey(SketchTestAssets.library(), "casa+luna").key);
    }
}
