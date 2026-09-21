package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;

/** Bounded foreground conversation turns. No microphone, audio, clock or model side effects. */
public final class VoiceConversationLoop {
    public static final long SESSION_LIMIT_MS = 10 * 60 * 1000L;
    // The recognizer normally reports speech end earlier; this bounds a stalled/long capture.
    public static final long CAPTURE_TIMEOUT_MS = 60 * 1000L;
    public static final long RESPONSE_TIMEOUT_MS = 120 * 1000L;
    public static final long SPEECH_TIMEOUT_MS = 120 * 1000L;
    public static final long ECHO_GUARD_MS = 300L;
    public static final int MAX_SILENCE_RETRIES = 2;
    public static final int MAX_TRANSIENT_RETRIES = 1;

    public enum State { STOPPED, LISTENING, THINKING, SPEAKING, COOLDOWN }
    public enum Action { NONE, LISTEN, SUBMIT, WAIT, STOP }
    public enum RecognitionFailure { SILENCE, TRANSIENT, PERMISSION, UNAVAILABLE }
    public enum StopReason {
        USER, SESSION_LIMIT, NO_SPEECH, RECOGNIZER_ERROR, MICROPHONE_PERMISSION,
        RECOGNIZER_UNAVAILABLE, RESPONSE_TIMEOUT, RESPONSE_ERROR, SPEECH_TIMEOUT, AUDIO_UNAVAILABLE,
        AUDIO_ERROR, AUDIO_FOCUS, LIFECYCLE
    }

    public static final class Step {
        public final Action action;
        public final long token;
        public final String text;
        public final StopReason reason;

        private Step(Action action, long token, String text, StopReason reason) {
            this.action = action;
            this.token = token;
            this.text = text;
            this.reason = reason;
        }
    }

    private State state = State.STOPPED;
    private long tokenSequence;
    private long activeToken;
    private long sessionDeadline;
    private long phaseDeadline;
    private int silenceRetries;
    private int transientRetries;
    private StopReason stopReason = StopReason.USER;

    public State state() { return state; }
    public long token() { return activeToken; }
    public StopReason stopReason() { return stopReason; }
    public boolean isActive() { return state != State.STOPPED; }
    public boolean owns(long token) { return isActive() && activeToken == token; }

    /** The adapter uses one timer for the nearest phase/session deadline. */
    public long nextDeadlineMillis() {
        return isActive() ? Math.min(sessionDeadline, phaseDeadline) : Long.MAX_VALUE;
    }

    public Step start(long now) {
        if (isActive()) return none();
        sessionDeadline = now + SESSION_LIMIT_MS;
        silenceRetries = transientRetries = 0;
        return listen(now);
    }

    public Step recognized(long token, String transcript, long now) {
        if (!accepts(token, State.LISTENING)) return none();
        if (now >= sessionDeadline) return stop(StopReason.SESSION_LIMIT);
        if (now >= phaseDeadline) return recognitionFailed(token, RecognitionFailure.SILENCE, now);
        String text = transcript == null ? "" : transcript.trim();
        if (text.isEmpty()) return recognitionFailed(token, RecognitionFailure.SILENCE, now);
        if (isStopCommand(text)) return stop(StopReason.USER);
        silenceRetries = transientRetries = 0;
        state = State.THINKING;
        phaseDeadline = now + RESPONSE_TIMEOUT_MS;
        return step(Action.SUBMIT, text, null);
    }

    public Step recognitionFailed(long token, RecognitionFailure failure, long now) {
        if (!accepts(token, State.LISTENING)) return none();
        if (now >= sessionDeadline) return stop(StopReason.SESSION_LIMIT);
        switch (failure) {
            case SILENCE:
                if (silenceRetries++ >= MAX_SILENCE_RETRIES) return stop(StopReason.NO_SPEECH);
                return waitUntil(now + 500L);
            case TRANSIENT:
                if (transientRetries++ >= MAX_TRANSIENT_RETRIES) return stop(StopReason.RECOGNIZER_ERROR);
                return waitUntil(now + 1500L);
            case PERMISSION: return stop(StopReason.MICROPHONE_PERMISSION);
            default: return stop(StopReason.RECOGNIZER_UNAVAILABLE);
        }
    }

    /** A reply without playable audio ends this voice session, while the adapter keeps its text. */
    public Step replied(long token, boolean audioQueued, long now) {
        if (!acceptsResponse(token)) return none();
        if (now >= sessionDeadline) return stop(StopReason.SESSION_LIMIT);
        if (now >= phaseDeadline) return stop(state == State.THINKING
                ? StopReason.RESPONSE_TIMEOUT : StopReason.SPEECH_TIMEOUT);
        if (!audioQueued) return stop(StopReason.AUDIO_UNAVAILABLE);
        if (state != State.SPEAKING) {
            state = State.SPEAKING;
            phaseDeadline = now + SPEECH_TIMEOUT_MS;
        }
        return step(Action.WAIT, "", null);
    }

    /** TTS engines may report start before the main-thread reply event reaches the adapter. */
    public Step speechStarted(long token, long now) {
        return replied(token, true, now);
    }

    public Step speechFinished(long token, boolean completed, long now) {
        if (!acceptsResponse(token)) return none();
        if (now >= sessionDeadline) return stop(StopReason.SESSION_LIMIT);
        if (now >= phaseDeadline) return stop(state == State.THINKING
                ? StopReason.RESPONSE_TIMEOUT : StopReason.SPEECH_TIMEOUT);
        if (!completed) return stop(StopReason.AUDIO_ERROR);
        return waitUntil(now + ECHO_GUARD_MS);
    }

    /** Invalidate immediately, then leave time for the stopped speaker's acoustic tail. */
    public Step interrupt(long now) {
        if (!isActive() || state == State.LISTENING) return none();
        if (now >= sessionDeadline) return stop(StopReason.SESSION_LIMIT);
        silenceRetries = transientRetries = 0;
        activeToken = ++tokenSequence;
        return waitUntil(now + ECHO_GUARD_MS);
    }

    public Step tick(long now) {
        if (!isActive()) return none();
        if (now >= sessionDeadline) return stop(StopReason.SESSION_LIMIT);
        if (now < phaseDeadline) return none();
        switch (state) {
            case LISTENING: return recognitionFailed(activeToken, RecognitionFailure.SILENCE, now);
            case THINKING: return stop(StopReason.RESPONSE_TIMEOUT);
            case SPEAKING: return stop(StopReason.SPEECH_TIMEOUT);
            case COOLDOWN: return listen(now);
            default: return none();
        }
    }

    public Step stop(StopReason reason) {
        if (!isActive()) return none();
        state = State.STOPPED;
        activeToken = ++tokenSequence;
        stopReason = reason;
        return step(Action.STOP, "", reason);
    }

    private Step listen(long now) {
        state = State.LISTENING;
        activeToken = ++tokenSequence;
        phaseDeadline = now + CAPTURE_TIMEOUT_MS;
        return step(Action.LISTEN, "", null);
    }

    private Step waitUntil(long deadline) {
        state = State.COOLDOWN;
        phaseDeadline = deadline;
        return step(Action.WAIT, "", null);
    }

    private boolean accepts(long token, State expected) {
        return owns(token) && state == expected;
    }

    private boolean acceptsResponse(long token) {
        return owns(token) && (state == State.THINKING || state == State.SPEAKING);
    }

    private Step none() { return step(Action.NONE, "", null); }
    private Step step(Action action, String text, StopReason reason) {
        return new Step(action, activeToken, text, reason);
    }

    public static boolean isStopCommand(String text) {
        if (text == null) return false;
        String clean = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]", "")
                .trim().replaceAll("\\s+", " ");
        return clean.equals("termina modo voz") || clean.equals("termina el modo voz")
                || clean.equals("termina el modo de voz") || clean.equals("salir del modo voz")
                || clean.equals("salir del modo de voz") || clean.equals("cierra el modo voz")
                || clean.equals("cierra el modo de voz") || clean.equals("sal del modo voz")
                || clean.equals("sal del modo de voz");
    }
}
