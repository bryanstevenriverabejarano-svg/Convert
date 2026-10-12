package salve.avatar;

/** Pure frame selection: body actions keep priority over speech and transient gestures. */
public final class RealisticSpriteAnimation {
    public static final class Frame {
        public final String sheet;
        public final int index;
        public final boolean mirror;
        public final float heightScale;
        Frame(String sheet, int index, boolean mirror, float heightScale) {
            this.sheet = sheet; this.index = index; this.mirror = mirror; this.heightScale = heightScale;
        }
    }
    public static Frame select(AvatarState state, AvatarMotion.Snapshot motion,
                               float gaitPhase, float time, boolean sleeping) {
        if (sleeping) return new Frame("poses", 7, false, 1);
        switch (state.getPose()) {
            case WALKING:
                return new Frame("walk", cycle(gaitPhase / (float)(2 * Math.PI), 8), !state.isFacingRight(), 1);
            case DANCE_POP: case DANCE_URBAN:
                float fps = state.getPose() == AvatarState.Pose.DANCE_POP ? 6 : 4;
                return new Frame("dance", cycle(time * fps / 8, 8), false, 1);
            case SEATED: return new Frame("poses", 0, false, .72f);
            case KNEELING: return new Frame("poses", 1, false, .67f);
            case CROUCHED: return new Frame("poses", 2, false, .62f);
            default: break;
        }
        if (motion.gesture == AvatarMotion.Gesture.WAVE) return new Frame("poses", 4, false, 1);
        if (motion.gesture == AvatarMotion.Gesture.EXPLAIN) return new Frame("poses", 5, false, 1);
        if (motion.listening || motion.thinking) return new Frame("poses", 6, false, 1);
        return new Frame("base", 0, false, 1);
    }
    private static int cycle(float turns, int frames) {
        if (!Float.isFinite(turns)) return 0;
        return Math.min(frames - 1, (int)((turns - Math.floor(turns)) * frames));
    }
}
