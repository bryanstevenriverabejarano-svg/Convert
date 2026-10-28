package salve.sketch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PartPlanTest {
    static List<float[]> square(float x1, float y1, float x2, float y2) {
        List<float[]> l = new ArrayList<>();
        l.add(new float[]{x1, y1, x2, y1, x2, y2, x1, y2, x1, y1});
        return l;
    }

    // ── dónde va cada parte ──

    @Test public void aDoorGoesAtTheBottomCentreOfAChurch() {
        float[] box = PartPlan.boxFor("puerta", "", null);
        assertTrue("centrada", box[0] > .3f && box[2] < .7f);
        assertEquals("apoyada en el suelo", 1f, box[3], 1e-3);
        assertTrue(box[1] > .5f);
    }

    @Test public void aCrossGoesAboveTheTop() {
        float[] box = PartPlan.boxFor("la cruz", "", null);
        assertTrue("sobresale por encima", box[1] < 0);
        assertTrue(box[3] <= 0.01f);
    }

    @Test public void whatTheModelSaysWins() {
        float[] mine = PartPlan.parseBox("arco alto [10 20 30 40]");
        assertArrayEquals(new float[]{.1f, .2f, .3f, .4f}, PartPlan.boxFor("puerta", "", mine), 1e-3f);
        assertArrayEquals(new float[]{.4f, .6f, .6f, 1f}, PartPlan.parseBox("CAJA: 40 60 60 100"), 1e-3f);
        assertArrayEquals(new float[]{.45f, -.4f, .55f, -.25f}, PartPlan.parseBox("dos palos [45 -40 55 -25]"), 1e-3f);
        assertNull(PartPlan.parseBox("sin caja"));
        assertEquals("arco alto", PartPlan.withoutBox("arco alto [10 20 30 40]"));
    }

    @Test public void unknownPartsUseThePositionWords() {
        float[] right = PartPlan.boxFor("farol", "pequeño, abajo a la derecha", null);
        assertTrue((right[0] + right[2]) / 2 > .7f);
        assertTrue((right[1] + right[3]) / 2 > .7f);
    }

    @Test public void pluralsAndPairs() {
        assertEquals("ventana", PartPlan.key("las ventanas"));
        assertEquals("puerta", PartPlan.key("su puerta"));
        assertTrue(PartPlan.paired("ventanas"));
        assertFalse(PartPlan.paired("ventana"));
        float[] left = PartPlan.boxFor("ventana", "", null), other = PartPlan.mirrored(left);
        assertEquals(1f - left[2], other[0], 1e-4);
    }

    // ── encajarla en el dibujo real ──

    @Test public void theDoorFitsInsideTheChurchAndRestsOnItsGround() {
        List<float[]> church = square(100, 200, 500, 800); // 400 de ancho, 600 de alto
        List<float[]> door = square(0, 0, 780, 1000);       // proporción de la puerta del repertorio
        List<float[]> placed = PartPlan.attach(church, door, PartPlan.boxFor("puerta", "", null));
        float[] b = PartPlan.bounds(placed);
        assertEquals("se apoya en el suelo de la iglesia", 800f, b[3], 0.5f);
        assertTrue("dentro de la iglesia", b[0] >= 100 && b[2] <= 500 && b[1] >= 200);
        float doorW = b[2] - b[0], churchW = 400;
        assertTrue("más pequeña que la iglesia", doorW < churchW * .25f);
        assertEquals("centrada", 300f, (b[0] + b[2]) / 2, 1f);
    }

    @Test public void originalsAreNotModified() {
        List<float[]> door = square(0, 0, 10, 10);
        float[] before = door.get(0).clone();
        PartPlan.attach(square(0, 0, 100, 100), door, new float[]{.4f, .6f, .6f, 1f});
        assertArrayEquals(before, door.get(0), 0f);
    }

    @Test public void combineKeepsTheChurchAndNamesTheNewPart() {
        List<float[]> church = square(100, 200, 500, 800);
        List<PartPlan.Placed> added = new ArrayList<>();
        added.add(new PartPlan.Placed("puerta", "abajo en el centro", PartPlan.attach(church, PartPlan.primitive("ventana"),
                PartPlan.boxFor("puerta", "", null))));
        added.add(new PartPlan.Placed("cruz", "en lo alto", PartPlan.attach(church, PartPlan.primitive("cruz"),
                PartPlan.boxFor("cruz", "", null))));
        PartPlan.Combined c = PartPlan.combine(church, null, "iglesia", added);
        assertEquals(3, c.parts.size());
        assertEquals("iglesia", c.parts.get(0).name);
        assertEquals("puerta", c.parts.get(1).name);
        assertEquals("cruz", c.parts.get(2).name);
        float[] b = PartPlan.bounds(c.strokes);
        assertTrue("todo cabe en el lienzo", b[0] >= -0.5f && b[1] >= -0.5f && b[2] <= 1000.5f && b[3] <= 1000.5f);
        assertTrue("con la cruz encima es más alto que ancho", c.aspect < 1f);
    }

    @Test public void simplePartsAreAbstractedWithGeometry() {
        assertNotNull(PartPlan.primitive("cruz"));
        assertNotNull(PartPlan.primitive("ventanas"));
        assertNotNull(PartPlan.primitive("campanario"));
        assertNull(PartPlan.primitive("dragón"));
    }

    // ── qué pide Bryan ──

    @Test public void splitsAnObjectFromItsParts() {
        assertEquals(Arrays.asList("iglesia", "puerta"), PartPlan.split("una iglesia con su puerta"));
        assertEquals(Arrays.asList("casa", "ventanas", "chimenea"), PartPlan.split("una casa con ventanas y una chimenea"));
        assertNull(PartPlan.split("un gato con mas detalle"));
        assertNull(PartPlan.split("un gato con otra pose"));
        assertNull(PartPlan.split("una iglesia"));
    }

    @Test public void addingAPartToTheDrawing() {
        PartPlan.Add a = PartPlan.add("mejora el dibujo y agregale una puerta");
        assertEquals(Arrays.asList("puerta"), a.parts);
        assertEquals("", a.target);
        PartPlan.Add b = PartPlan.add("ponle una cruz a la iglesia");
        assertEquals(Arrays.asList("cruz"), b.parts);
        assertEquals("iglesia", b.target);
        PartPlan.Add c = PartPlan.add("anadele una ventana arriba a la derecha");
        assertEquals(Arrays.asList("ventana"), c.parts);
        assertEquals("", c.target);
        assertTrue(c.where.contains("arriba") && c.where.contains("derecha"));
        PartPlan.Add d = PartPlan.add("ponle una puerta en el centro");
        assertEquals(Arrays.asList("puerta"), d.parts);
        assertEquals("", d.target);
        PartPlan.Add e = PartPlan.add("a la iglesia le falta una puerta y una cruz");
        assertEquals(Arrays.asList("puerta", "cruz"), e.parts);
        assertNull(PartPlan.add("dibuja un gato"));
        assertNull(PartPlan.add("traza un circulo"));
        assertNull(PartPlan.add("ponle color al dibujo"));
    }

    // ── lo que ve al mirarse ──

    @Test public void readsWhichPartsAreMissing() {
        List<String> asked = Arrays.asList("puerta", "cruz", "campanario");
        assertEquals(Arrays.asList("puerta"), PartPlan.missing("VEO: cruz, campanario\nFALTA: puerta", asked));
        assertTrue(PartPlan.missing("VEO: puerta, cruz, campanario\nFALTA: nada", asked).isEmpty());
        assertTrue(PartPlan.missing("FALTA: un dragón", asked).isEmpty());
        assertTrue(PartPlan.lookForPartsPrompt("iglesia", asked).contains("puerta, cruz, campanario"));
    }

    @Test public void herConceptSaysWhereEachPartGoes() {
        VisualConcept c = VisualConcept.parse("NOMBRE: iglesia\nINGLES: church\nTIPO: edificio religioso\nPARTES:\n"
                + "- fachada: rectángulo alto con tejado a dos aguas [0 20 100 100]\n"
                + "- puerta principal: arco alto, abajo en el centro [40 60 60 100]\n"
                + "- cruz: dos palos en lo más alto [45 -40 55 -25]\n- campanario: torre estrecha encima del tejado\n"
                + "POSE: de frente", "iglesia");
        assertEquals(4, c.parts.size());
        VisualConcept.Part door = c.part("puerta");
        assertNotNull("«puerta» encuentra «puerta principal»", door);
        assertArrayEquals(new float[]{.4f, .6f, .6f, 1f}, door.box, 1e-3f);
        assertEquals("arco alto, abajo en el centro", door.shape);
        assertEquals(-.4f, c.part("cruz").box[1], 1e-3f);
        assertNull("sin caja: usará su sitio típico", c.part("campanario").box);
    }
}
