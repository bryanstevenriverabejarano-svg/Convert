package salve.avatar;

import static org.junit.Assert.*;
import org.junit.Test;

/** El saludo sube la mano, la agita y la baja con las ilustraciones de Bryan. */
public class WaveAnimationTest {
    @Test public void raisesWavesAndLowersTheHand() {
        StringBuilder seen = new StringBuilder();
        String last = "";
        for (int i = 0; i < 100; i++) {
            RealisticSpriteAnimation.Frame f = RealisticSpriteAnimation.waveFrame(i / 100f);
            String id = f.sheet + f.index;
            if (!id.equals(last)) { seen.append(id).append(' '); last = id; }
        }
        assertEquals("wave0 wave1 poses4 wave2 poses4 wave2 poses4 wave2 poses4 wave1 wave0", seen.toString().trim());
        assertEquals("wave", RealisticSpriteAnimation.waveFrame(1f).sheet);
        assertEquals(0, RealisticSpriteAnimation.waveFrame(Float.NaN).index);
    }

    @Test public void theWaveGestureUsesTheAnimatedFrames() {
        AvatarMotion m = new AvatarMotion();
        m.previewGesture(AvatarMotion.Gesture.WAVE, AvatarMotion.Expression.WARM);
        m.advance(1.1f);
        RealisticSpriteAnimation.Frame mid = RealisticSpriteAnimation.select(new AvatarState(), m.snapshot(), 0, 0, false);
        assertTrue(mid.sheet.equals("wave") || mid.sheet.equals("poses"));
        assertTrue("a mitad del saludo la mano está arriba", mid.sheet.equals("poses") ? mid.index == 4 : mid.index == 2);
    }
}
