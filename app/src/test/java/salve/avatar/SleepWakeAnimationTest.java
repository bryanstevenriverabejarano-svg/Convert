package salve.avatar;

import static org.junit.Assert.*;

import org.junit.Test;

public class SleepWakeAnimationTest {
    @Test public void sleepsOnHerSideFirstAndTurnsOverWithACrossfade() {
        SleepWakeAnimation.Lying start = SleepWakeAnimation.lying(0);
        assertEquals(0, start.from); assertEquals(0, start.mix, 0);
        SleepWakeAnimation.Lying fading = SleepWakeAnimation.lying(SleepWakeAnimation.SLEEP_HOLD + .75f);
        assertEquals(0, fading.from); assertEquals(1, fading.to); assertEquals(.5f, fading.mix, .01f);
        SleepWakeAnimation.Lying curled = SleepWakeAnimation.lying(2 * SleepWakeAnimation.SLEEP_HOLD + 5);
        assertEquals(2, curled.from); assertEquals(0, curled.mix, 0);
        // Vuelve a empezar el ciclo, nunca con la de ojos abiertos (3).
        for (float t = 0; t < 600; t += 3.3f) {
            SleepWakeAnimation.Lying l = SleepWakeAnimation.lying(t);
            assertTrue(l.from < 3 && l.to < 3);
            assertTrue(l.mix >= 0 && l.mix <= 1);
        }
        assertEquals(0, SleepWakeAnimation.lying(Float.NaN).from);
    }

    @Test public void wakesInBedThenRubsHerEyesStretchesYawnsWinksAndStandsUp() {
        assertTrue(SleepWakeAnimation.inBed(0));
        assertEquals("sleep", SleepWakeAnimation.wake(.2f).sheet);
        assertEquals(3, SleepWakeAnimation.wake(.2f).index);
        assertFalse(SleepWakeAnimation.inBed(2f));
        StringBuilder seen = new StringBuilder();
        String last = "";
        for (float t = 0; t < SleepWakeAnimation.wakeLength(); t += .02f) {
            RealisticSpriteAnimation.Frame f = SleepWakeAnimation.wake(t);
            assertNotNull(f);
            String key = f.sheet + f.index;
            if (!key.equals(last)) { seen.append(key).append(' '); last = key; }
            assertTrue(f.heightScale > .5f && f.heightScale <= 1);
        }
        assertEquals("sleep3 wake0 wake1 wake2 wake3 wake4 wake5 wake6 bend4 bend3 bend0 ", seen.toString());
        assertNull(SleepWakeAnimation.wake(SleepWakeAnimation.wakeLength() + .01f));
        assertNull(SleepWakeAnimation.wake(-1));
    }

    @Test public void standingGesturesKneelFirstAndGetUpAfterwards() {
        RealisticSpriteAnimation.Frame first = SleepWakeAnimation.gesture(AvatarMotion.Gesture.YAWN, 0, false);
        RealisticSpriteAnimation.Frame middle = SleepWakeAnimation.gesture(AvatarMotion.Gesture.YAWN, .45f, false);
        RealisticSpriteAnimation.Frame end = SleepWakeAnimation.gesture(AvatarMotion.Gesture.YAWN, 1, false);
        assertEquals("bend", first.sheet); assertEquals(0, first.index);
        assertEquals("wake", middle.sheet); assertEquals(3, middle.index);
        assertEquals("bend", end.sheet); assertEquals(0, end.index);
        // Ya de rodillas: directamente el bostezo, el estiramiento o el guiño.
        assertEquals("wake", SleepWakeAnimation.gesture(AvatarMotion.Gesture.YAWN, 0, true).sheet);
        assertEquals(2, SleepWakeAnimation.gesture(AvatarMotion.Gesture.STRETCH, .5f, true).index);
        assertEquals(6, SleepWakeAnimation.gesture(AvatarMotion.Gesture.WINK, .5f, true).index);
        assertNull(SleepWakeAnimation.gesture(AvatarMotion.Gesture.WAVE, .5f, false));
    }

    @Test public void commandsAndModelDirectivesReachTheNewGestures() {
        assertEquals(AvatarMotion.Gesture.YAWN, AvatarMotionProtocol.parseCommand("Bosteza").gesture);
        assertEquals(AvatarMotion.Gesture.STRETCH, AvatarMotionProtocol.parseCommand("estírate").gesture);
        assertEquals(AvatarMotion.Gesture.WINK, AvatarMotionProtocol.parseCommand("guíñame un ojo").gesture);
        assertEquals(AvatarMotion.Gesture.WINK, AvatarMotionProtocol.parse("Claro. [[salve_motion:WINK:WARM]]").gesture);
    }
}
