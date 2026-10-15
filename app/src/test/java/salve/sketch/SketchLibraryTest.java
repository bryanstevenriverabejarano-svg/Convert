package salve.sketch;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

public class SketchLibraryTest {
    @Test public void bundledRepertoireLoadsWithSelfPortrait() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        assertTrue(lib.motifs().size() >= 40);
        SketchLibrary.Motif self = lib.byId("salve");
        assertNotNull(self);
        assertTrue(self.self);
        assertTrue("el autorretrato debe tener figura y circuitos", self.strokeCount() > 20);
        for (SketchLibrary.Motif m : lib.motifs()) {
            assertTrue(m.id, m.strokeCount() > 0);
            new SketchAnimation(SketchScene.compose(java.util.Collections.singletonList(m)));
        }
    }

    @Test public void findsWordsWithAccentsAndPlurals() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        assertEquals("arbol", lib.find("Árboles").id);
        assertEquals("montana", lib.find("montañas").id);
        assertEquals("dragon", lib.find("dragón").id);
        assertEquals("arcoiris", lib.find("arco iris").id);
        assertNull(lib.find("ornitorrinco"));
    }

    @Test public void storyMentionsSkipCommonWords() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        List<SketchLibrary.Mention> m = lib.mentions("Había una vez, en un mundo lleno de luz, un castillo junto al mar.", true);
        assertEquals(2, m.size());
        assertEquals("castillo", m.get(0).motif.id);
        assertEquals("mar", m.get(1).motif.id);
    }
}
