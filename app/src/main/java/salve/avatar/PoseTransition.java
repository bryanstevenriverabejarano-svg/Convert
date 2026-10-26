package salve.avatar;

/**
 * Pasos intermedios al sentarse, agacharse o arrodillarse (y al levantarse, en orden inverso), con las
 * ilustraciones de la hoja «bend»: 0 rodillas dobladas, 1 medio sentada, 2 agachándose, 3 rodilla
 * bajando, 4 rodilla al suelo. Sin Android: dice qué fotograma toca o null si ninguno.
 */
public final class PoseTransition {
    private static final int[] SIT = {0, 1}, CROUCH = {0, 1, 2}, KNEEL = {0, 3, 4};
    /** Escala de dibujo de cada fotograma, entre la de pie (1) y la de la postura final (0,62–0,72). */
    private static final float[] SCALE = {.95f, .78f, .70f, .72f, .70f};
    static final float STEP = .14f;
    private AvatarState.Pose last;
    private int[] sequence;
    private boolean reverse;
    private float start;

    public RealisticSpriteAnimation.Frame frame(AvatarState.Pose pose, float time) {
        if (pose == null || !Float.isFinite(time)) return null;
        if (last == null) last = pose;
        if (pose != last) {
            int[] down = steps(pose), up = steps(last);
            if (upright(last) && down != null) { sequence = down; reverse = false; start = time; }
            else if (upright(pose) && up != null) { sequence = up; reverse = true; start = time; }
            else sequence = null;
            last = pose;
        }
        if (sequence == null) return null;
        int i = (int) ((time - start) / STEP);
        if (i < 0 || i >= sequence.length) { sequence = null; return null; }
        int index = sequence[reverse ? sequence.length - 1 - i : i];
        return new RealisticSpriteAnimation.Frame("bend", index, false, SCALE[index]);
    }

    private static boolean upright(AvatarState.Pose pose) {
        return pose == AvatarState.Pose.IDLE || pose == AvatarState.Pose.WALKING;
    }

    private static int[] steps(AvatarState.Pose pose) {
        switch (pose) {
            case SEATED: return SIT;
            case CROUCHED: return CROUCH;
            case KNEELING: return KNEEL;
            default: return null;
        }
    }
}
