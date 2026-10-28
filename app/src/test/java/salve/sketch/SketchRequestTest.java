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

    @Test public void anObjectWithItsPartsIsOneDrawing() throws Exception {
        SketchRequest r = parse("Dibuja una iglesia con su puerta");
        assertEquals(SketchRequest.Kind.WITH_PARTS, r.kind);
        assertEquals("iglesia", r.unknown);
        assertEquals(java.util.Arrays.asList("puerta"), r.parts);
        SketchRequest casa = parse("dibújame una casa con ventanas y una chimenea");
        assertEquals(SketchRequest.Kind.WITH_PARTS, casa.kind);
        assertNotNull("la casa ya la sabe dibujar", casa.scene);
        assertEquals(java.util.Arrays.asList("ventanas", "chimenea"), casa.parts);
        // Dos dibujos que conoce juntos siguen siendo una escena.
        assertEquals(SketchRequest.Kind.DRAW, parse("¿Puedes dibujar un castillo con un dragón?").kind);
        assertEquals(SketchRequest.Kind.UNKNOWN, parse("dibuja un gorila con más detalle").kind);
    }

    @Test public void addingAPartToTheLastDrawing() throws Exception {
        SketchRequest r = parse("Mejora el dibujo y agrégale una puerta");
        assertEquals(SketchRequest.Kind.ADD, r.kind);
        assertEquals(java.util.Arrays.asList("puerta"), r.parts);
        assertTrue(r.improve);
        assertFalse("habla del dibujo", r.implicit);
        SketchRequest cruz = parse("ponle una cruz a la iglesia");
        assertEquals(SketchRequest.Kind.ADD, cruz.kind);
        assertEquals("iglesia", cruz.target);
        SketchRequest solo = parse("ponle una puerta");
        assertEquals(SketchRequest.Kind.ADD, solo.kind);
        assertTrue("sin dibujo reciente no cuenta", solo.implicit);
        // No son dibujos.
        assertNotEquals(SketchRequest.Kind.ADD, parse("pon la tele").kind);
        assertNotEquals(SketchRequest.Kind.ADD, parse("me falta tiempo").kind);
        assertNotEquals(SketchRequest.Kind.ADD, parse("dibuja un gato").kind);
    }
}
