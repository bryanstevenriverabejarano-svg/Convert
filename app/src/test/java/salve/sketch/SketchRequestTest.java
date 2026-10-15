package salve.sketch;

import static org.junit.Assert.*;
import org.junit.Test;

public class SketchRequestTest {
    private static SketchRequest parse(String s) throws Exception { return SketchRequest.parse(s, SketchTestAssets.library()); }

    @Test public void drawsWhatIsAskedInOrder() throws Exception {
        SketchRequest r = parse("Dibújame un árbol y la luna, porfa");
        assertEquals(SketchRequest.Kind.DRAW, r.kind);
        assertEquals("arbol+luna", r.scene.key);
        assertEquals(SketchRequest.Kind.DRAW, parse("¿Puedes dibujar un castillo con un dragón?").kind);
        assertEquals(SketchRequest.Kind.DRAW, parse("hazme un dibujo de un gato").kind);
    }

    @Test public void selfPortraitAndRepertoire() throws Exception {
        assertEquals(SketchRequest.Kind.SELF, parse("Dibújate").kind);
        assertEquals(SketchRequest.Kind.SELF, parse("dibuja tu cuerpo astral").kind);
        assertEquals("salve", parse("hazte un autorretrato").scene.key);
        assertEquals(SketchRequest.Kind.LIST, parse("¿Qué sabes dibujar?").kind);
    }

    @Test public void freeChoiceUnknownAndNonRequests() throws Exception {
        assertEquals(SketchRequest.Kind.FREE, parse("dibuja lo que quieras").kind);
        SketchRequest unknown = parse("dibuja un ornitorrinco azul");
        assertEquals(SketchRequest.Kind.UNKNOWN, unknown.kind);
        assertEquals("ornitorrinco azul", unknown.unknown);
        assertEquals(SketchRequest.Kind.NONE, parse("me encanta tu dibujo").kind);
        assertEquals(SketchRequest.Kind.NONE, parse("no dibujes nada ahora").kind);
        assertEquals(SketchRequest.Kind.NONE, parse("cuéntame un cuento de dragones").kind);
    }

    @Test public void freeChoiceFollowsTheSky() throws Exception {
        SketchScene night = SketchRequest.choose(SketchTestAssets.library(), 23, new java.util.Random(3));
        assertNotNull(night);
        assertFalse(night.isSelfPortrait());
    }
}
