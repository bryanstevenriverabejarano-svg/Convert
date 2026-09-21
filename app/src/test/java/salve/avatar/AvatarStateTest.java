package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AvatarStateTest {
    @Test public void walksToDestinationWithoutOvershoot() {
        AvatarState s = new AvatarState(); s.walkTo(.8f);
        assertEquals(AvatarState.Pose.WALKING, s.getPose());
        for (int i = 0; i < 200; i++) s.advance(.033f);
        assertEquals(.8f, s.getX(), .0001f); assertEquals(AvatarState.Pose.IDLE, s.getPose());
    }
    @Test public void walkingDirectionFollowsDestination() {
        AvatarState s = new AvatarState(); s.walkTo(0f); assertFalse(s.isFacingRight());
        s.walkTo(1f); assertTrue(s.isFacingRight());
    }
    @Test public void clampsWalkingAndDraggingToScreen() {
        AvatarState s = new AvatarState(); s.walkTo(9f); assertEquals(1f, s.getTargetX(), 0);
        s.moveByUser(-5f, 8f); assertEquals(0f, s.getX(), 0); assertEquals(1f, s.getOverlayY(), 0);
        assertEquals(AvatarState.Pose.IDLE, s.getPose());
    }
    @Test public void cannotSleepWithoutBed() {
        AvatarState s = new AvatarState(); assertFalse(s.sleep());
        assertEquals(AvatarState.Pose.IDLE, s.getPose());
    }
    @Test public void walksToBedThenSleeps() {
        AvatarState s = new AvatarState(); s.createBed(); assertTrue(s.sleep());
        assertTrue(s.isGoingToSleep()); assertEquals(AvatarState.Pose.WALKING, s.getPose());
        for (int i = 0; i < 100; i++) s.advance(.05f);
        assertEquals(AvatarState.BED_X, s.getX(), 0); assertEquals(AvatarState.Pose.SLEEPING, s.getPose());
    }
    @Test public void removingBedCancelsSleepAndWalkToBed() {
        AvatarState s = new AvatarState(); s.createBed(); s.sleep(); s.removeBed();
        assertFalse(s.hasBed()); assertFalse(s.isGoingToSleep()); assertEquals(AvatarState.Pose.IDLE, s.getPose());
    }
    @Test public void newWalkCancelsSleepIntent() {
        AvatarState s = new AvatarState(); s.createBed(); s.sleep(); s.walkTo(.5f);
        for (int i = 0; i < 100; i++) s.advance(.05f);
        assertFalse(s.isGoingToSleep()); assertEquals(AvatarState.Pose.IDLE, s.getPose());
        assertEquals(.5f, s.getX(), 0);
    }
    @Test public void draggingWakesCharacter() {
        AvatarState s = new AvatarState(); s.createBed(); s.moveByUser(AvatarState.BED_X, .4f); s.sleep();
        assertEquals(AvatarState.Pose.SLEEPING, s.getPose()); s.moveByUser(.2f, .4f);
        assertEquals(AvatarState.Pose.IDLE, s.getPose()); assertEquals(.2f, s.getX(), 0);
    }
    @Test public void longPauseCannotTeleport() {
        AvatarState s = new AvatarState(); s.walkTo(1f); s.advance(3600f);
        assertEquals(.318f, s.getX(), .00001f);
    }
    @Test public void ignoresNonFiniteInputsAndBackwardsTime() {
        AvatarState s = new AvatarState(); s.walkTo(Float.NaN);
        assertEquals(.3f, s.getX(), 0); s.walkTo(1f);
        s.advance(Float.POSITIVE_INFINITY); s.advance(Float.NaN); s.advance(-1f);
        assertEquals(.3f, s.getX(), 0); s.moveByUser(Float.NaN, Float.NEGATIVE_INFINITY);
        assertEquals(.3f, s.getX(), 0); assertEquals(.6f, s.getOverlayY(), 0);
    }
    @Test public void preservesRoomWardrobeAndSleepIntentAcrossRestart() {
        AvatarState s = new AvatarState(); s.createBed();
        s.wear(AvatarState.Outfit.PAJAMAS, 0x00112233, AvatarState.Pattern.STARS);
        s.moveByUser(.25f, .75f); s.sleep(); s.advance(.04f);
        AvatarState restored = AvatarState.decode(s.encode());
        assertEquals(s.encode(), restored.encode()); assertEquals(0xFF112233, restored.getAccent());
        assertTrue(restored.isGoingToSleep()); assertTrue(restored.hasBed());
    }
    @Test public void corruptAndUnknownSavedStateResetSafely() {
        String fresh = new AvatarState().encode();
        assertEquals(fresh, AvatarState.decode("unknown").encode());
        assertEquals(fresh, AvatarState.decode("2|.3|.3|.6|false|IDLE|DAY|0|PLAIN|false").encode());
        assertEquals(fresh, AvatarState.decode("1|NaN|.3|.6|false|IDLE|DAY|0|PLAIN|false").encode());
        assertEquals(fresh, AvatarState.decode("1|.3|.3|.6|false|IDLE|SCRIPT|0|PLAIN|false").encode());
    }
    @Test public void corruptedSleepWithoutBedRestoresAwake() {
        AvatarState s = AvatarState.decode("1|.3|.3|.6|false|SLEEPING|DAY|0|PLAIN|true");
        assertEquals(AvatarState.Pose.IDLE, s.getPose()); assertFalse(s.isGoingToSleep());
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsMissingGarment() {
        new AvatarState().wear(null, 0, AvatarState.Pattern.PLAIN);
    }
}
