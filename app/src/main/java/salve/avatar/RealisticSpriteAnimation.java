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
            case WALKING: {
                // Preserve gait speed: each original step gets one new midpoint frame.
                int step = cycle(gaitPhase / (float)(2 * Math.PI), 16);
                return new Frame((step & 1) == 0 ? "walk" : "walk_inbetween", step / 2,
                        !state.isFacingRight(), 1);
            }
            case DANCE_POP: case DANCE_URBAN:
                float fps = state.getPose() == AvatarState.Pose.DANCE_POP ? 6 : 4;
                return new Frame("dance", cycle(time * fps / 8, 8), false, 1);
            case SEATED: return new Frame("poses", 0, false, .72f);
            case KNEELING: return new Frame("poses", 1, false, .67f);
            case CROUCHED: return new Frame("poses", 2, false, .62f);
            default: break;
        }
        if (motion.gesture == AvatarMotion.Gesture.REST) return new Frame("poses", 7, false, 1);
        if (motion.gesture == AvatarMotion.Gesture.WAVE) return new Frame("poses", 4, false, 1);
        if (motion.gesture == AvatarMotion.Gesture.EXPLAIN) return new Frame("poses", 5, false, 1);
        if (motion.listening || motion.thinking) return new Frame("poses", 6, false, 1);
        return new Frame("base", 0, false, 1);
    }

    /** H1 small opening, H2 wide 'a', H3 rounded 'o'; preserves older two-frame packs. */
    public static int mouthFrame(float open, float time, int availableFrames) {
        if (availableFrames <= 1) return 0;
        int syllable = Float.isFinite(time) ? (int) Math.floor(time * 6.5f) : 0;
        if (availableFrames == 2) return Math.floorMod(syllable, 4) == 3 ? 1 : 0;
        float amplitude = Float.isFinite(open) ? Math.max(0, Math.min(1, open)) : 0;
        if (amplitude >= .25f && Math.floorMod(syllable, 5) == 4) return 2;
        return amplitude >= .56f ? 1 : 0;
    }
    private static int cycle(float turns, int frames) {
        if (!Float.isFinite(turns)) return 0;
        return Math.min(frames - 1, (int)((turns - Math.floor(turns)) * frames));
    }
}
