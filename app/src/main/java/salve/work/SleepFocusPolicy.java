package salve.work;

import salve.avatar.AvatarState;

/** Pure guard: only an explicitly enabled, sleeping avatar may run a focused sleep cycle. */
public final class SleepFocusPolicy {
    private SleepFocusPolicy() { }

    public static boolean active(boolean enabled, AvatarState state) {
        if (!enabled || state == null) return false;
        return state.getPose() == AvatarState.Pose.SLEEPING
                || state.getPose() == AvatarState.Pose.WALKING && state.isGoingToSleep();
    }
}
