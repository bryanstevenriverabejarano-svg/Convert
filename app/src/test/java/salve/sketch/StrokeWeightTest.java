package salve.sketch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class StrokeWeightTest {
    static List<float[]> face() {
        List<float[]> s = new ArrayList<>();
        s.add(new float[]{0, 0, 1000, 0, 1000, 1000, 0, 1000, 0, 0}); // contorno de la cabeza
        s.add(new float[]{300, 350, 340, 350});                       // ojo (detalle corto)
        s.add(new float[]{660, 350, 700, 350});                       // ojo
        s.add(new float[]{350, 700, 500, 760, 650, 700});             // boca (interior, mediana)
        return s;
    }

    @Test public void contourIsThickerThanDetails() {
        float[] w = StrokeWeight.weights(face(), false);
        assertEquals(StrokeWeight.CONTOUR, w[0], 0f);
        assertEquals(StrokeWeight.DETAIL, w[1], 0f);
        assertEquals(StrokeWeight.DETAIL, w[2], 0f);
        assertEquals("la boca, normal", StrokeWeight.NORMAL, w[3], 0f);
    }

    @Test public void uniformWhenAsked() {
        for (float v : StrokeWeight.weights(face(), true)) assertEquals(1f, v, 0f);
    }

    @Test public void animationCarriesTheWeightOfEachStroke() throws Exception {
        SketchScene casa = SketchScene.fromKey(SketchTestAssets.library(), "casa");
        SketchAnimation a = new SketchAnimation(casa);
        boolean thick = false, thin = false;
        for (int i = 0; i < a.strokes.size(); i++) {
            thick |= a.weight(i) > 1f;
            thin |= a.weight(i) <= 1f;
        }
        assertTrue("una casa tiene contorno grueso", thick);
        assertTrue("y detalles normales o finos", thin);
        assertEquals("fuera de rango, normal", 1f, a.weight(999), 0f);
    }
}
