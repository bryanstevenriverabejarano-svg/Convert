package salve.avatar;

/** Regression scenarios for the replacement renderer, executable without an Android SDK. */
public final class SpriteChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        AvatarState state = new AvatarState();
        AvatarMotion motion = new AvatarMotion();
        motion.activate(1); motion.beginTurn(1, 1, "hola");
        motion.speechPending(1, 1, "audio"); motion.speechStart(1, "audio");
        state.walkTo(.9f);
        for (int i = 0; i < 16; i++) {
            RealisticSpriteAnimation.Frame frame = RealisticSpriteAnimation.select(state, motion.snapshot(),
                    (float)((i + .2) / 16 * 2 * Math.PI), 0, false);
            String expected = i % 2 == 0 ? "walk" : "walk_inbetween";
            check(expected.equals(frame.sheet) && frame.index == i / 2 && !frame.mirror,
                    "Speech replaced gait " + i);
        }
        RealisticSpriteAnimation.Frame closing = RealisticSpriteAnimation.select(state,
                motion.snapshot(), (float)(2 * Math.PI * .999), 0, false);
        check("walk_inbetween".equals(closing.sheet) && closing.index == 7, "Walk cycle does not close");
        RealisticSpriteAnimation.Frame restart = RealisticSpriteAnimation.select(state,
                motion.snapshot(), 0, 0, false);
        check("walk".equals(restart.sheet) && restart.index == 0, "Walk cycle does not restart");
        state.walkTo(0);
        check(RealisticSpriteAnimation.select(state, motion.snapshot(), 1, 1, false).mirror, "Left walk is not mirrored");
        for (AvatarState.Pose pose : new AvatarState.Pose[]{AvatarState.Pose.DANCE_POP, AvatarState.Pose.DANCE_URBAN}) {
            state.pose(pose);
            boolean[] seen = new boolean[8];
            for (int i = 0; i < 160; i++) {
                RealisticSpriteAnimation.Frame frame = RealisticSpriteAnimation.select(state, motion.snapshot(), 0, i / 40f, false);
                check("dance".equals(frame.sheet), "Speech replaced dance"); seen[frame.index] = true;
            }
            for (boolean present : seen) check(present, "Missing dance frame");
        }
        for (AvatarState.Pose pose : new AvatarState.Pose[]{AvatarState.Pose.SEATED, AvatarState.Pose.KNEELING, AvatarState.Pose.CROUCHED}) {
            state.pose(pose);
            check("poses".equals(RealisticSpriteAnimation.select(state, motion.snapshot(), 0, 0, false).sheet), "Lost held posture");
        }
        state.wake(); motion.close(1);
        check("base".equals(RealisticSpriteAnimation.select(state, motion.snapshot(), 0, 0, false).sheet), "Stale speech/listening after close");
        state.walkTo(1);
        check(RealisticSpriteAnimation.select(state, motion.snapshot(), Float.NaN, Float.NaN, false).index == 0, "Invalid clock escaped");
        AvatarLocomotion gait = new AvatarLocomotion();
        gait.advance(1000, .3f, true); gait.advance(1033, .31f, true);
        float phase = gait.phase(); gait.advance(1066, .31f, true);
        check(gait.phase() == phase, "Stationary sprite advanced gait");
        for (float[] viewport : new float[][]{{280,420},{360,620},{412,760},{760,220},{280,100}}) {
            for (float x : new float[]{0, .5f, 1}) {
                AvatarStageLayout layout = AvatarStageLayout.fit(viewport[0], viewport[1], x);
                check(layout.centerX - .38f * layout.height >= 0 && layout.centerX + .38f * layout.height <= viewport[0], "Clipped sprite at edge");
            }
        }
        System.out.println("PASS: 16 gait frames, loop closure, directions, both dance tempos, speech/body priority, held poses, close, invalid clocks, stationary gait and narrow stages");
    }
}
