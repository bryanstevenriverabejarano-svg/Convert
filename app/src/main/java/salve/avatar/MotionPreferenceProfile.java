package salve.avatar;

/** Explicit comfort preferences for existing gestures. This does not train a model or invent poses. */
public final class MotionPreferenceProfile {
    public enum Feedback { TOO_FAST, TOO_STRONG, COMFORTABLE, RESET }

    public static final float MIN_AMPLITUDE = .45f, MAX_AMPLITUDE = 1f;
    public static final float MIN_TEMPO = .65f, MAX_TEMPO = 1f;
    private static final int MAX_FEEDBACK_COUNT = 1_000_000;
    private final float amplitude, tempo;
    private final boolean confirmed;
    private final int feedbackCount;

    private MotionPreferenceProfile(float amplitude, float tempo, boolean confirmed, int feedbackCount) {
        this.amplitude = amplitude;
        this.tempo = tempo;
        this.confirmed = confirmed;
        this.feedbackCount = feedbackCount;
    }

    public static MotionPreferenceProfile defaults() { return new MotionPreferenceProfile(1f, 1f, false, 0); }
    public float amplitude() { return amplitude; }
    public float tempo() { return tempo; }
    public boolean isConfirmed() { return confirmed; }
    public int feedbackCount() { return feedbackCount; }

    /** A small, reversible update only when the user explicitly supplies feedback. */
    public MotionPreferenceProfile withFeedback(Feedback feedback) {
        if (feedback == null) throw new IllegalArgumentException("Missing movement feedback");
        if (feedback == Feedback.RESET) return defaults();
        float nextAmplitude = feedback == Feedback.TOO_STRONG ? Math.max(MIN_AMPLITUDE, amplitude * .9f) : amplitude;
        float nextTempo = feedback == Feedback.TOO_FAST ? Math.max(MIN_TEMPO, tempo * .9f) : tempo;
        return new MotionPreferenceProfile(nextAmplitude, nextTempo, feedback == Feedback.COMFORTABLE,
                Math.min(MAX_FEEDBACK_COUNT, feedbackCount + 1));
    }

    public String description() {
        return "Intensidad " + Math.round(amplitude * 100) + "% · Ritmo " + Math.round(tempo * 100)
                + "%" + (confirmed ? " · A tu gusto" : "");
    }

    /** Stores only numeric preferences; no conversations, inferred emotions, images or audio. */
    public String encode() {
        return "1|" + amplitude + "|" + tempo + "|" + confirmed + "|" + feedbackCount;
    }

    public static MotionPreferenceProfile decode(String saved) {
        if (saved == null || saved.length() > 100) return defaults();
        try {
            String[] fields = saved.split("\\|", -1);
            if (fields.length != 5 || !"1".equals(fields[0])) return defaults();
            float amplitude = Float.parseFloat(fields[1]), tempo = Float.parseFloat(fields[2]);
            int count = Integer.parseInt(fields[4]);
            if (!Float.isFinite(amplitude) || amplitude < MIN_AMPLITUDE || amplitude > MAX_AMPLITUDE
                    || !Float.isFinite(tempo) || tempo < MIN_TEMPO || tempo > MAX_TEMPO
                    || !("true".equals(fields[3]) || "false".equals(fields[3]))
                    || count < 0 || count > MAX_FEEDBACK_COUNT) return defaults();
            return new MotionPreferenceProfile(amplitude, tempo, Boolean.parseBoolean(fields[3]), count);
        } catch (IllegalArgumentException ignored) { return defaults(); }
    }
}
