package salve.avatar;

/** Adapts Salve's existing conversation state to the approved GLB, without owning audio. */
public final class Avatar3dMotion {
    private Avatar3dMotion() { }
    public static String clip(AvatarState.Pose pose, AvatarMotion.Snapshot motion) {
        switch (pose) {
            case WALKING: return "Walk_InPlace";
            case SEATED: return "Sit_Down";
            case KNEELING: return "Kneel";
            case CROUCHED: return "Crouch";
            case DANCE_POP: case DANCE_URBAN: return "Gentle_Dance";
            case SLEEPING: return "Idle_Breathe";
            default:
                if (motion.gesture == AvatarMotion.Gesture.WAVE) return "Wave";
                if (motion.gesture == AvatarMotion.Gesture.CELEBRATE) return "Gentle_Dance";
                return "Idle_Breathe";
        }
    }
    public static boolean holds(String clip) {
        return "Sit_Down".equals(clip) || "Kneel".equals(clip) || "Crouch".equals(clip) || "Wave".equals(clip);
    }
    public static float clipTime(String clip, float seconds, float duration) {
        if (!Float.isFinite(seconds) || duration <= 0) return 0;
        return holds(clip) ? Math.min(Math.max(0, seconds), duration) : Math.max(0, seconds) % duration;
    }
    public static float weight(String name, AvatarMotion.Snapshot m, boolean sleeping) {
        // Use the combined Blink only; adding Blink.L/R would double the eyelid deformation.
        if ("Blink".equals(name)) return sleeping ? 1 : m.blink;
        if (sleeping) return 0;
        switch (name) {
            case "JawOpen": return m.mouthOpen;
            case "Smile": return m.expression == AvatarMotion.Expression.WARM ? .5f : m.expression == AvatarMotion.Expression.SHY ? .25f : 0;
            case "MouthFrown": return m.expression == AvatarMotion.Expression.SAD || m.expression == AvatarMotion.Expression.CONCERNED ? .4f : 0;
            case "BrowUp": return m.expression == AvatarMotion.Expression.SURPRISED ? .65f : m.thinking || m.listening ? .25f : 0;
            case "BrowDown": return m.expression == AvatarMotion.Expression.ANGRY ? .55f : 0;
            case "BrowUp.L": return m.expression == AvatarMotion.Expression.CURIOUS && !m.thinking && !m.listening ? .35f : 0;
            default: return 0;
        }
    }
}
