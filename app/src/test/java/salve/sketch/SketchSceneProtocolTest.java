package salve.sketch;

import static org.junit.Assert.*;
import org.junit.Test;

public class SketchSceneProtocolTest {
    @Test public void sceneKeepsSkyAboveGroundInsideTheCanvas() throws Exception {
        SketchScene scene = SketchScene.fromKey(SketchTestAssets.library(), "casa+luna");
        assertEquals("casa+luna", scene.key);
        assertEquals("una casa y la luna", scene.title);
        float skyBottom = 0, groundTop = Float.MAX_VALUE;
        int moonStrokes = SketchTestAssets.library().byId("luna").strokeCount();
        int houseStrokes = SketchTestAssets.library().byId("casa").strokeCount();
        // Se colocan por lugar: primero la fila del cielo (luna), después la del suelo (casa).
        for (int i = 0; i < scene.strokes.size(); i++) {
            float[] s = scene.strokes.get(i);
            for (int k = 0; k < s.length; k += 2) {
                assertTrue(s[k] >= -1 && s[k] <= scene.width + 1);
                assertTrue(s[k + 1] >= -1 && s[k + 1] <= scene.height + 1);
                if (i < moonStrokes) skyBottom = Math.max(skyBottom, s[k + 1]);
                else groundTop = Math.min(groundTop, s[k + 1]);
            }
        }
        assertEquals(houseStrokes + moonStrokes, scene.strokes.size());
        assertTrue("la luna queda encima", skyBottom <= groundTop + 30);
    }

    @Test public void extractionCleansVoiceAndKeepsCanonicalTagsForChat() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        SketchProtocol.Extraction e = SketchProtocol.extract(
                "Mira esto.\n[[salve_draw:Árbol]]\nY ahora [salve_draw roto] la luna [[salve_draw:luna + estrella]].", lib);
        assertEquals("Mira esto.\nY ahora la luna .", e.clean);
        assertEquals(2, e.scenes.size());
        assertTrue(e.display.contains("[[salve_draw:arbol]]"));
        assertTrue(e.display.contains("[[salve_draw:luna+estrella]]"));
        assertFalse(e.display.contains("roto"));
        assertTrue(e.historyNote().contains("un árbol"));
        assertEquals(5, SketchProtocol.segments(e.display).size());
    }

    @Test public void unknownMotifTagsAreDropped() throws Exception {
        SketchProtocol.Extraction e = SketchProtocol.extract("Hola [[salve_draw:ornitorrinco]]", SketchTestAssets.library());
        assertFalse(e.hasSketches());
        assertEquals("Hola", e.clean);
        assertEquals("Hola", e.display);
    }

    @Test public void animationSpendsMoreTimeOnLongerStrokesAndFinishes() throws Exception {
        SketchAnimation a = new SketchAnimation(SketchScene.fromKey(SketchTestAssets.library(), "arbol"));
        assertTrue(a.duration() >= 2.8f && a.duration() <= 7.5f);
        assertEquals(0, a.strokeAt(0));
        assertEquals(a.strokes.size(), a.strokeAt(a.duration() + .01f));
        assertTrue(a.finished(a.duration()));
        SketchAnimation self = new SketchAnimation(SketchScene.fromKey(SketchTestAssets.library(), "salve"));
        assertTrue(self.scene.isSelfPortrait());
        assertTrue(self.duration() > 9f);
    }
}
