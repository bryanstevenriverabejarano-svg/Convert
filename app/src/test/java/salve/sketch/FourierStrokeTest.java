package salve.sketch;

import static org.junit.Assert.*;
import org.junit.Test;

public class FourierStrokeTest {
    private static float[] square() {
        return new float[]{0, 0, 100, 0, 100, 100, 0, 100, 0, 0};
    }

    @Test public void allTermsReproduceEverySampleOfAClosedStroke() {
        FourierStroke f = new FourierStroke(square());
        assertTrue(f.closed);
        assertEquals(1f, f.visibleTurn, 0f);
        float[] p = new float[2];
        float[] src = square();
        for (int i = 0; i < 4; i++) {
            f.point(i / 4f, f.terms(), p);
            assertEquals(src[2 * i], p[0], 1e-3);
            assertEquals(src[2 * i + 1], p[1], 1e-3);
        }
    }

    @Test public void openStrokeIsTracedForwardInTheFirstPartOfTheLoop() {
        float[] line = {0, 0, 10, 5, 20, 0, 30, 5, 40, 0};
        FourierStroke f = new FourierStroke(line);
        assertFalse(f.closed);
        assertEquals(.5f, f.visibleTurn, 1e-6);
        float[] p = new float[2];
        int n = 5, m = 2 * n - 2;
        for (int i = 0; i < n; i++) {
            f.point(i / (float) m, f.terms(), p);
            assertEquals(line[2 * i], p[0], 1e-3);
            assertEquals(line[2 * i + 1], p[1], 1e-3);
        }
    }

    @Test public void termsAreSortedByRadiusAfterTheFixedCenter() {
        FourierStroke f = new FourierStroke(new float[]{0, 0, 50, 80, 120, 10, 60, -40, 0, 0});
        assertEquals(0, f.frequency(0));
        for (int i = 2; i < f.terms(); i++) assertTrue(f.radius(i - 1) >= f.radius(i) - 1e-4);
    }

    @Test public void chainEndsAtThePenWithAllTerms() {
        FourierStroke f = new FourierStroke(square());
        float[] chain = new float[2 * (f.terms() + 2)];
        int count = f.chain(.3f, f.terms(), chain);
        float[] p = new float[2];
        f.point(.3f, f.terms(), p);
        assertEquals(p[0], chain[2 * (count - 1)], 1e-3);
        assertEquals(p[1], chain[2 * (count - 1) + 1], 1e-3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCoordinates() { new FourierStroke(new float[]{0, 0, Float.NaN, 1}); }
}
