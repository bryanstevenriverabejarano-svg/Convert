package salve.avatar;

/** Visual body requests, independent of speech and facial gestures. No tool execution. */
public enum AvatarBodyCue {
    KEEP, STAND, WALK, SIT, KNEEL, CROUCH, DANCE_POP, DANCE_URBAN;

    public boolean requiresCoreArtwork() { return this == SIT || this == KNEEL || this == CROUCH; }

    public void apply(AvatarState state) {
        switch (this) {
            case STAND: state.wake(); break;
            case WALK: state.walkTo(state.getX() > .5f ? .08f : .92f); break;
            case SIT: state.pose(AvatarState.Pose.SEATED); break;
            case KNEEL: state.pose(AvatarState.Pose.KNEELING); break;
            case CROUCH: state.pose(AvatarState.Pose.CROUCHED); break;
            case DANCE_POP: state.pose(AvatarState.Pose.DANCE_POP); break;
            case DANCE_URBAN: state.pose(AvatarState.Pose.DANCE_URBAN); break;
            default: break;
        }
    }

    public static String referenceFor(AvatarState.Pose pose) {
        switch (pose) {
            case SEATED: return "seated";
            case KNEELING: return "kneeling";
            case CROUCHED: return "crouched";
            default: return null;
        }
    }
    public static boolean isDance(AvatarState.Pose pose) {
        return pose == AvatarState.Pose.DANCE_POP || pose == AvatarState.Pose.DANCE_URBAN;
    }
    public static String label(AvatarState.Pose pose) {
        switch (pose) {
            case WALKING: return "Caminando";
            case SLEEPING: return "Descansando en la cama";
            case SEATED: return "Sentada";
            case KNEELING: return "De rodillas";
            case CROUCHED: return "En cuclillas";
            case DANCE_POP: return "Baile pop ilustrado";
            case DANCE_URBAN: return "Baile urbano ilustrado";
            default: return "De pie";
        }
    }
}
