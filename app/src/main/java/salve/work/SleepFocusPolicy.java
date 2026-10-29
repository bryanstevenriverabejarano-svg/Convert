package salve.work;

import salve.avatar.AvatarState;

/** Pure guard: only an explicitly enabled, sleeping avatar may run a focused sleep cycle. */
public final class SleepFocusPolicy {
    private SleepFocusPolicy() { }

    /** Explicit one-shot requests retain priority, even while focused sleep owns periodic work. */
    public static final String EXPLICIT_REQUEST = "salve_user_requested";

    /** Avoid two independent periodic dream/play loops when the avatar is sleeping. */
    public static boolean allowStandalone(boolean focusedSleepActive, boolean explicitlyRequested) {
        return !focusedSleepActive || explicitlyRequested;
    }

    public static boolean active(boolean enabled, AvatarState state) {
        if (!enabled || state == null) return false;
        return state.getPose() == AvatarState.Pose.SLEEPING
                || state.getPose() == AvatarState.Pose.WALKING && state.isGoingToSleep();
    }
}
