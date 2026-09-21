package salve.avatar;

/** Transient, deterministic visual state. Call from one thread; no Android, storage, audio or model. */
public final class AvatarMotion {
    public enum Gesture { NONE, WAVE, NOD, SHAKE, EXPLAIN, THINK, CELEBRATE }
    public enum Expression { NEUTRAL, WARM, CURIOUS, CONCERNED }

    /** Angles in degrees; arms name screen sides. Positive is clockwise, negative counterclockwise.
     * Both arms stay within 25 degrees to preserve the supplied flat illustration. */
    public static final class Snapshot {
        public final Gesture gesture;
        public final Expression expression;
        public final boolean listening, thinking, speaking;
        public final float blink, gazeX, gazeY, headTilt, headYaw, headPitch, bodyTilt;
        public final float leftArm, rightArm, leftKnee, rightKnee, mouthOpen, breath;
        private Snapshot(AvatarMotion m) {
            gesture = m.gesture; expression = m.expression;
            listening = m.listening; thinking = m.thinking; speaking = m.speaking;
            blink = bounded(m.blink, 0, 1); gazeX = bounded(m.gazeX, -1, 1); gazeY = bounded(m.gazeY, -1, 1);
            headTilt = bounded(m.headTilt, -12, 12); headYaw = bounded(m.headYaw, -14, 14);
            headPitch = bounded(m.headPitch, -10, 10); bodyTilt = bounded(m.bodyTilt, -5, 5);
            leftArm = bounded(m.leftArm, -25, 25); rightArm = bounded(m.rightArm, -25, 25);
            leftKnee = bounded(m.leftKnee, 0, 1); rightKnee = bounded(m.rightKnee, 0, 1);
            mouthOpen = bounded(m.mouthOpen, 0, 1); breath = bounded(m.breath, 0, 1);
        }
    }

    private long owner, turn;
    private boolean ownerOpen;
    private boolean acceptsTurn, enabled = true, listening, thinking, speaking, responseCue;
    private String utterance;
    private float age, gestureAge, gesturePhase, expressionAge, audioAge, rangePulse;
    private MotionPreferenceProfile preferences = MotionPreferenceProfile.defaults();
    private Gesture gesture = Gesture.NONE;
    private Expression expression = Expression.NEUTRAL;
    private AvatarMotionProtocol.Result fallback;
    private float blink, gazeX, gazeY, headTilt, headYaw, headPitch, bodyTilt;
    private float leftArm, rightArm, leftKnee, rightKnee, mouthOpen, breath;

    public void activate(long session) {
        if (session <= 0 || session <= owner) return;
        reset(); owner = session; ownerOpen = true; turn = 0; enabled = true;
    }
    public void close(long session) { if (owns(session)) { reset(); enabled = false; ownerOpen = false; } }
    public void pause(long session) { if (owns(session)) { reset(); enabled = false; } }
    public void beginTurn(long session, long id, String userInput) {
        if (!owns(session) || id <= turn || id <= 0) return;
        stopAudio(); enabled = true; turn = id; acceptsTurn = true;
        listening = false; thinking = true; responseCue = false;
        fallback = AvatarMotionProtocol.fallback(userInput);
        setGesture(Gesture.THINK, Expression.CURIOUS);
    }
    public void listening(long session, boolean value) {
        if (!owns(session)) return;
        listening = value;
        if (value) {
            enabled = true; acceptsTurn = false; thinking = false; fallback = null;
            stopAudio(); setGesture(Gesture.NONE, Expression.CURIOUS);
        }
    }
    public void response(long session, long id, AvatarMotionProtocol.Result result) {
        if (!validTurn(session, id) || result == null) return;
        thinking = false; listening = false;
        if (responseCue && !result.hasDirective) { responseCue = false; return; }
        responseCue = false;
        AvatarMotionProtocol.Result chosen = result.hasDirective ? result : fallback;
        if (chosen == null) setGesture(Gesture.NONE, Expression.NEUTRAL);
        else setGesture(chosen.gesture, chosen.expression);
    }
    public void clarification(long session, long id) {
        if (validTurn(session, id)) { thinking = false; responseCue = true; setGesture(Gesture.NONE, Expression.CURIOUS); }
    }
    public void error(long session, long id) {
        if (validTurn(session, id)) { thinking = false; responseCue = true; stopAudio(); setGesture(Gesture.NONE, Expression.CONCERNED); }
    }
    /** Finishing generation must not erase a gesture or stop speech that has already started. */
    public void endTurn(long session, long id) { if (validTurn(session, id)) thinking = false; }
    /** Explicit room preview changes only the visual cue, never a turn or audio identifier. */
    public void previewGesture(Gesture value, Expression face) {
        if (value == null || face == null) return;
        enabled = true; setGesture(value, face);
    }
    /** Changes gesture comfort only; the audio clock, turn ownership and expiry remain untouched. */
    public void setPreferences(MotionPreferenceProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Missing movement preferences");
        preferences = profile;
    }
    public void speechPending(long session, long id, String audioId) {
        if (!validTurn(session, id) || audioId == null || audioId.isEmpty() || audioId.length() > 160) return;
        stopAudio(); utterance = audioId; audioAge = 0;
    }
    public void speechStart(long session, String audioId) {
        if (validAudio(session, audioId)) { speaking = true; thinking = false; audioAge = 0; }
    }
    public void speechRange(long session, String audioId, int start, int end) {
        if (validAudio(session, audioId) && speaking && start >= 0 && end > start) {
            rangePulse = .9f; audioAge = 0;
        }
    }
    public void speechEnd(long session, String audioId) { if (validAudio(session, audioId)) stopAudio(); }

    public Snapshot snapshot() { return new Snapshot(this); }
    public void advance(float seconds) {
        if (!enabled || !Float.isFinite(seconds) || seconds <= 0) return;
        float dt = Math.min(seconds, .1f);
        // Wall time expires stale cues even after a hidden view resumes. Only interpolation is capped.
        float elapsed = Math.min(seconds, 1000f);
        age = (age + dt) % 420f;
        gestureAge = Math.min(1000f, gestureAge + elapsed);
        gesturePhase = Math.min(1000f, gesturePhase + elapsed * preferences.tempo());
        expressionAge = Math.min(1000f, expressionAge + elapsed);
        if (utterance != null && (audioAge += elapsed) > (speaking ? 90f : 10f)) stopAudio();
        if (gesture != Gesture.NONE && gestureAge > duration(gesture)) gesture = Gesture.NONE;
        if (expressionAge > 8f && !listening && !thinking) expression = Expression.NEUTRAL;
        float strength = Math.min(1f, gestureAge * 5f);
        if (gesture != Gesture.NONE) strength *= Math.min(1f, Math.max(0f, duration(gesture) - gestureAge) * 4f);
        float targetTilt = (float)Math.sin(age * .9f) * .6f, targetYaw = 0, targetPitch = 0;
        float targetLeft = 0, targetRight = 0, targetBody = 0, targetKnee = 0;
        switch (gesture) {
            case WAVE: targetRight = -19 + (float)Math.sin(gesturePhase * 12f) * 5; targetTilt = -4; break;
            case NOD: targetPitch = (float)Math.sin(gesturePhase * 10f) * 8; break;
            case SHAKE: targetYaw = (float)Math.sin(gesturePhase * 10f) * 12; break;
            case EXPLAIN: targetLeft = 9; targetRight = -13 + (float)Math.sin(gesturePhase * 4f) * 3; targetTilt = 3; break;
            case THINK: targetRight = -11; targetTilt = 6; break;
            case CELEBRATE: targetLeft = 21; targetRight = -21; targetBody = (float)Math.sin(gesturePhase * 6f) * 3;
                targetKnee = (1 + (float)Math.sin(gesturePhase * 8f)) * .12f; break;
            default: break;
        }
        float blend = 1f - (float)Math.exp(-dt * 13f);
        float gestureBlend = 1f - (float)Math.exp(-dt * 13f * preferences.tempo());
        float gestureStrength = strength * (gesture == Gesture.NONE ? 1f : preferences.amplitude());
        headTilt = approach(headTilt, targetTilt * gestureStrength, gestureBlend);
        headYaw = approach(headYaw, targetYaw * gestureStrength, gestureBlend);
        headPitch = approach(headPitch, targetPitch * gestureStrength, gestureBlend);
        bodyTilt = approach(bodyTilt, targetBody * gestureStrength, gestureBlend);
        leftArm = approach(leftArm, targetLeft * gestureStrength, gestureBlend); rightArm = approach(rightArm, targetRight * gestureStrength, gestureBlend);
        leftKnee = approach(leftKnee, targetKnee * gestureStrength, gestureBlend); rightKnee = approach(rightKnee, targetKnee * gestureStrength, gestureBlend);
        gazeX = approach(gazeX, thinking ? .24f : listening ? 0f : (float)Math.sin(age * .4f) * .09f, blend);
        gazeY = approach(gazeY, thinking ? -.25f : 0f, blend);
        float blinkPhase = age % 4.2f;
        blink = blinkPhase > 4.04f ? Math.max(0, 1 - Math.abs(blinkPhase - 4.12f) / .08f) : 0;
        breath = .5f + (float)Math.sin(age * 1.7f) * .5f;
        rangePulse = Math.max(0, rangePulse - dt * 3f);
        // Timing-based mouth motion; never claim phoneme-accurate lip sync without audio features.
        float targetMouth = speaking ? .12f + Math.abs((float)Math.sin(age * 15f)) * .48f + rangePulse * .25f : 0;
        mouthOpen = speaking ? approach(mouthOpen, targetMouth, blend) : 0;
    }

    private boolean owns(long session) { return ownerOpen && session > 0 && owner == session; }
    private boolean validTurn(long session, long id) { return owns(session) && enabled && acceptsTurn && id > 0 && id == turn; }
    private boolean validAudio(long session, String id) {
        return validTurn(session, turn) && utterance != null && utterance.equals(id);
    }
    private void setGesture(Gesture value, Expression face) {
        gesture = value; expression = face; gestureAge = 0; gesturePhase = 0; expressionAge = 0;
    }
    private void stopAudio() { speaking = false; utterance = null; mouthOpen = 0; audioAge = 0; rangePulse = 0; }
    private void reset() {
        acceptsTurn = false; listening = false; thinking = false; responseCue = false; fallback = null; stopAudio();
        gesture = Gesture.NONE; expression = Expression.NEUTRAL;
        headTilt = headYaw = headPitch = bodyTilt = leftArm = rightArm = leftKnee = rightKnee = blink = gazeX = gazeY = 0;
        breath = .5f;
    }
    private static float duration(Gesture value) {
        switch (value) {
            case WAVE: return 2.3f; case NOD: case SHAKE: return 1.5f; case EXPLAIN: return 3f;
            case THINK: return 3.5f; case CELEBRATE: return 2.5f; default: return 0;
        }
    }
    private static float approach(float current, float target, float blend) { return current + (target - current) * blend; }
    private static float bounded(float value, float low, float high) {
        return Float.isFinite(value) ? Math.min(high, Math.max(low, value)) : 0;
    }
}
