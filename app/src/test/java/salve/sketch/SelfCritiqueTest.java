package salve.sketch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SelfCritiqueTest {
    static final List<String> PARTS = Arrays.asList("fachada", "puerta", "cruz");

    @Test public void recognisedMeansNothingToFix() {
        SelfCritique.Verdict v = SelfCritique.parse("SE_RECONOCE: sí\nPEOR: nada\nPOR_QUE: se ve la iglesia", PARTS);
        assertTrue(v.recognized);
        assertNull(v.worst);
    }

    @Test public void namesTheWorstPart() {
        SelfCritique.Verdict v = SelfCritique.parse("SE_RECONOCE: no\nPEOR: la puerta\nPOR_QUE: parece una ventana", PARTS);
        assertFalse(v.recognized);
        assertEquals("puerta", v.worst);
        assertEquals("parece una ventana", v.why);
    }

    @Test public void theWholeShapeCanBeTheProblem() {
        assertEquals(SelfCritique.WHOLE, SelfCritique.parse("SE_RECONOCE: no\nPEOR: todo\nPOR_QUE: garabato", PARTS).worst);
        assertEquals("sin decir cuál, es la forma", SelfCritique.WHOLE, SelfCritique.parse("SE_RECONOCE: no", PARTS).worst);
    }

    @Test public void unclearAnswersTouchNothing() {
        assertTrue(SelfCritique.parse("Es un dibujo bonito.", PARTS).recognized);
        assertTrue(SelfCritique.parse(null, PARTS).recognized);
        assertNull("parte que no existe", SelfCritique.parse("SE_RECONOCE: no\nPEOR: el dragón", PARTS).worst);
    }

    @Test public void onlyMeaningfulPartsAreNamed() {
        List<SketchLibrary.Part> parts = Arrays.asList(new SketchLibrary.Part("silueta", "", 0, 1),
                new SketchLibrary.Part("detalles", "", 1, 3), new SketchLibrary.Part("iglesia", "", 3, 4),
                new SketchLibrary.Part("puerta", "", 4, 5), new SketchLibrary.Part("puerta", "", 5, 6));
        assertEquals(Arrays.asList("puerta"), SelfCritique.nameable(parts, "iglesia"));
        assertTrue(SelfCritique.prompt("iglesia", Arrays.asList("puerta")).contains("SE_RECONOCE"));
    }

    @Test public void replacingAPartKeepsTheRestInPlace() {
        List<float[]> strokes = new ArrayList<>();
        strokes.add(new float[]{0, 0, 1000, 0, 1000, 1000, 0, 1000, 0, 0}); // fachada
        strokes.add(new float[]{400, 600, 600, 600, 600, 1000});            // puerta mala
        strokes.add(new float[]{500, 0, 500, 100});                          // cruz
        List<SketchLibrary.Part> parts = Arrays.asList(new SketchLibrary.Part("fachada", "", 0, 1),
                new SketchLibrary.Part("puerta", "", 1, 2), new SketchLibrary.Part("cruz", "", 2, 3));
        float[] rect = PartPlan.bounds(strokes.subList(1, 2));
        List<float[]> door = PartPlan.fitInto(PartPlan.primitive("ventana"), rect, true);
        float[] placed = PartPlan.bounds(door);
        assertEquals("apoyada abajo", rect[3], placed[3], .5f);
        PartPlan.Combined c = PartPlan.replacePart(strokes, parts, 1, door);
        assertEquals(3, c.parts.size());
        assertEquals("puerta", c.parts.get(1).name);
        assertEquals(1 + door.size(), c.parts.get(1).to);
        assertEquals("la cruz sigue siendo la última", "cruz", c.parts.get(2).name);
        assertEquals(c.strokes.size(), c.parts.get(2).to);
    }
}
