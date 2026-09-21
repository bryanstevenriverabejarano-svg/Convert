package salve.avatar;

/** Deterministic visual state. No model output, Android APIs, permissions or executable code. */
public final class AvatarState {
    public enum Pose { IDLE, WALKING, SLEEPING }
    public enum Outfit { DAY, PAJAMAS }
    public enum Pattern { PLAIN, STARS, STRIPES }
    public static final float BED_X = .70f;
    public static final int DEFAULT_COLOR = 0xFF15CCC8;
    private float x = .30f, targetX = .30f, overlayY = .60f;
    private boolean bed, sleepAfterWalk;
    private Pose pose = Pose.IDLE;
    private Outfit outfit = Outfit.DAY;
    private Pattern pattern = Pattern.PLAIN;
    private int accent = DEFAULT_COLOR;

    public float getX() { return x; }
    public float getTargetX() { return targetX; }
    public float getOverlayY() { return overlayY; }
    public Pose getPose() { return pose; }
    public Outfit getOutfit() { return outfit; }
    public Pattern getPattern() { return pattern; }
    public int getAccent() { return accent; }
    public boolean hasBed() { return bed; }
    public boolean isFacingRight() { return targetX >= x; }
    public boolean isGoingToSleep() { return sleepAfterWalk; }

    public void createBed() { bed = true; }
    public void removeBed() { bed = false; wake(); }
    public void wear(Outfit value, int color, Pattern decoration) {
        if (value == null || decoration == null) throw new IllegalArgumentException("Missing outfit");
        outfit = value;
        accent = color | 0xFF000000;
        pattern = decoration;
    }
    public void walkTo(float destination) {
        targetX = clamp(destination, x);
        sleepAfterWalk = false;
        pose = Math.abs(targetX - x) < .001f ? Pose.IDLE : Pose.WALKING;
    }
    /** A bed is required; walking there precedes the sleeping pose. */
    public boolean sleep() {
        if (!bed) return false;
        walkTo(BED_X);
        sleepAfterWalk = true;
        if (pose == Pose.IDLE) finishWalk();
        return true;
    }
    public void wake() { pose = Pose.IDLE; targetX = x; sleepAfterWalk = false; }
    public void moveByUser(float horizontal, float vertical) {
        wake();
        x = targetX = clamp(horizontal, x);
        overlayY = clamp(vertical, overlayY);
    }
    /** Elapsed time is capped so a resumed screen cannot teleport the character. */
    public void advance(float seconds) {
        if (pose != Pose.WALKING || !Float.isFinite(seconds) || seconds <= 0) return;
        float distance = Math.min(seconds, .1f) * .18f;
        if (Math.abs(targetX - x) <= distance) { x = targetX; finishWalk(); }
        else x += Math.copySign(distance, targetX - x);
    }
    private void finishWalk() {
        pose = sleepAfterWalk && bed ? Pose.SLEEPING : Pose.IDLE;
        sleepAfterWalk = false;
    }
    private static float clamp(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0f, Math.min(1f, value)) : fallback;
    }
    public String encode() {
        return "1|" + x + "|" + targetX + "|" + overlayY + "|" + bed + "|" + pose.name()
                + "|" + outfit.name() + "|" + accent + "|" + pattern.name() + "|" + sleepAfterWalk;
    }
    /** Unknown/corrupt versions reset only the visual state. */
    public static AvatarState decode(String saved) {
        AvatarState result = new AvatarState();
        if (saved == null || saved.length() > 300) return result;
        try {
            String[] fields = saved.split("\\|", -1);
            if (fields.length != 10 || !"1".equals(fields[0])) return result;
            float x = Float.parseFloat(fields[1]), target = Float.parseFloat(fields[2]);
            float y = Float.parseFloat(fields[3]);
            if (!Float.isFinite(x) || !Float.isFinite(target) || !Float.isFinite(y)) return result;
            if (!("true".equals(fields[4]) || "false".equals(fields[4]))
                    || !("true".equals(fields[9]) || "false".equals(fields[9]))) return result;
            Pose pose = Pose.valueOf(fields[5]);
            Outfit outfit = Outfit.valueOf(fields[6]);
            int color = Integer.parseInt(fields[7]);
            Pattern pattern = Pattern.valueOf(fields[8]);
            result.x = clamp(x, .3f);
            result.targetX = clamp(target, result.x);
            result.overlayY = clamp(y, .6f);
            result.bed = Boolean.parseBoolean(fields[4]);
            result.pose = pose;
            result.wear(outfit, color, pattern);
            result.sleepAfterWalk = Boolean.parseBoolean(fields[9]) && result.bed && pose == Pose.WALKING;
            if (pose == Pose.SLEEPING) {
                if (result.bed) result.x = result.targetX = BED_X;
                else result.wake();
            }
            if (pose == Pose.IDLE) result.targetX = result.x;
            return result;
        } catch (IllegalArgumentException ignored) { return new AvatarState(); }
    }
}
