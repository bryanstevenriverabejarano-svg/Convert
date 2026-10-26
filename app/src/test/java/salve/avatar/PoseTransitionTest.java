package salve.avatar;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Al cambiar de postura pasa por sus ilustraciones intermedias, y al levantarse en orden inverso. */
public class PoseTransitionTest {
    private static List<Integer> play(PoseTransition t, AvatarState.Pose pose, float from) {
        List<Integer> seen = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            RealisticSpriteAnimation.Frame f = t.frame(pose, from + i * .02f);
            if (f == null) continue;
            assertEquals("bend", f.sheet);
            if (seen.isEmpty() || seen.get(seen.size() - 1) != f.index) seen.add(f.index);
        }
        return seen;
    }

    @Test public void sitsCrouchesAndKneelsThroughTheirIntermediatePoses() {
        PoseTransition t = new PoseTransition();
        assertNull(t.frame(AvatarState.Pose.IDLE, 0));
        assertEquals(java.util.Arrays.asList(0, 1), play(t, AvatarState.Pose.SEATED, 1));
        assertNull("ya sentada, su ilustración de siempre", t.frame(AvatarState.Pose.SEATED, 3));
        assertEquals("al levantarse, al revés", java.util.Arrays.asList(1, 0), play(t, AvatarState.Pose.IDLE, 4));
        assertEquals(java.util.Arrays.asList(0, 1, 2), play(t, AvatarState.Pose.CROUCHED, 6));
        play(t, AvatarState.Pose.IDLE, 8);
        assertEquals(java.util.Arrays.asList(0, 3, 4), play(t, AvatarState.Pose.KNEELING, 10));
    }

    @Test public void noTransitionBetweenFloorPosesOrWhileDancing() {
        PoseTransition t = new PoseTransition();
        t.frame(AvatarState.Pose.SEATED, 0);
        assertTrue(play(t, AvatarState.Pose.KNEELING, 1).isEmpty());
        assertTrue(play(t, AvatarState.Pose.DANCE_POP, 3).isEmpty());
    }

    @Test public void framesShrinkTowardsTheFinalPose() {
        PoseTransition t = new PoseTransition();
        t.frame(AvatarState.Pose.IDLE, 0);
        float first = t.frame(AvatarState.Pose.CROUCHED, 1).heightScale;
        float last = t.frame(AvatarState.Pose.CROUCHED, 1 + 2 * PoseTransition.STEP + .01f).heightScale;
        assertTrue(first > last);
        assertTrue(last >= .62f && first <= 1f);
    }
}
