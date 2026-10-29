package salve.work;

import org.junit.Test;
import salve.avatar.AvatarState;
import static org.junit.Assert.*;

/** Tests the persisted avatar-state guard without starting Android background work. */
public class SleepFocusPolicyTest {
    @Test public void userOptInAndBedAreRequired() {
        AvatarState state = new AvatarState();
        assertFalse(SleepFocusPolicy.active(true, state));
        state.createBed();
        assertFalse(SleepFocusPolicy.active(true, state));
        assertTrue(state.sleep());
        assertTrue(SleepFocusPolicy.active(true, state));
        assertFalse(SleepFocusPolicy.active(false, state));
    }

    @Test public void wakingMovingOrRemovingBedStopsFocusedCycles() {
        AvatarState state = new AvatarState();
        state.createBed();
        state.sleep();
        assertTrue(SleepFocusPolicy.active(true, state)); // Walking to bed also counts.
        state.wake();
        assertFalse(SleepFocusPolicy.active(true, state));
        state.sleep();
        state.removeBed();
        assertFalse(SleepFocusPolicy.active(true, state));
    }

    @Test public void focusedSleepOwnsScheduledWorkButNeverCancelsExplicitUserRequest() {
        assertTrue(SleepFocusPolicy.allowStandalone(false, false));
        assertTrue(SleepFocusPolicy.allowStandalone(false, true));
        assertFalse(SleepFocusPolicy.allowStandalone(true, false));
        assertTrue(SleepFocusPolicy.allowStandalone(true, true));
    }

    @Test public void savedSleepingStateSurvivesReloadButNotCorruptData() {
        AvatarState state = new AvatarState();
        state.createBed();
        state.sleep();
        for (int i = 0; i < 100; i++) state.advance(.1f);
        assertEquals(AvatarState.Pose.SLEEPING, state.getPose());
        assertTrue(SleepFocusPolicy.active(true, AvatarState.decode(state.encode())));
        assertFalse(SleepFocusPolicy.active(true, AvatarState.decode("corrupt")));
        assertFalse(SleepFocusPolicy.active(true, null));
    }
}
