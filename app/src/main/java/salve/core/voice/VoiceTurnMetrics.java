package salve.core.voice;

/** One software voice turn, using monotonic milliseconds. Never accepts conversational content. */
public final class VoiceTurnMetrics {
    public enum CaptureStage { STARTING, READY, SPEAKING, FINALIZING, FINISHED }
    public enum Outcome { COMPLETED, INTERRUPTED, RECOGNITION_ERROR, STOPPED }

    private long token;
    private long captureStart = -1, ready = -1, speechEnd = -1, asrFinal = -1;
    private long submitted = -1, reply = -1, ttsStart = -1, ttsEnd = -1, finished = -1;
    private long lastEvent = -1;
    private CaptureStage captureStage = CaptureStage.FINISHED;
    private Outcome outcome;
    private VoiceConversationLoop.StopReason stopReason;
    private Integer recognitionError;
    private boolean reported;

    public long token() { return token; }
    public CaptureStage captureStage() { return captureStage; }
    public boolean isActive() { return captureStart >= 0 && finished < 0; }
    public boolean hasSubmitted() { return submitted >= 0; }

    /** Tokens come from one VoiceConversationLoop and must increase across captures. */
    public boolean begin(long nextToken, long now) {
        if (isActive() || nextToken <= token || now < 0 || (lastEvent >= 0 && now < lastEvent)) return false;
        token = nextToken;
        captureStart = lastEvent = now;
        ready = speechEnd = asrFinal = submitted = reply = ttsStart = ttsEnd = finished = -1;
        captureStage = CaptureStage.STARTING;
        outcome = null;
        stopReason = null;
        recognitionError = null;
        reported = false;
        return true;
    }

    public boolean ready(long id, long now) {
        if (!accepts(id, now) || ready >= 0 || asrFinal >= 0 || speechEnd >= 0) return false;
        ready = lastEvent = now;
        if (captureStage == CaptureStage.STARTING) captureStage = CaptureStage.READY;
        return true;
    }

    public boolean speechBegan(long id, long now) {
        if (!accepts(id, now) || asrFinal >= 0 || speechEnd >= 0
                || captureStage == CaptureStage.SPEAKING) return false;
        lastEvent = now;
        captureStage = CaptureStage.SPEAKING;
        return true;
    }

    public boolean speechEnded(long id, long now) {
        if (!accepts(id, now) || speechEnd >= 0 || asrFinal >= 0) return false;
        speechEnd = lastEvent = now;
        captureStage = CaptureStage.FINALIZING;
        return true;
    }

    public boolean recognized(long id, long now) {
        if (!accepts(id, now) || asrFinal >= 0) return false;
        asrFinal = lastEvent = now;
        captureStage = CaptureStage.FINISHED;
        return true;
    }

    public boolean submitted(long id, long now) {
        if (!accepts(id, now) || asrFinal < 0 || submitted >= 0) return false;
        submitted = lastEvent = now;
        return true;
    }

    public boolean replied(long id, long now) {
        if (!accepts(id, now) || submitted < 0 || reply >= 0) return false;
        reply = lastEvent = now;
        return true;
    }

    public boolean speechStarted(long id, long now) {
        if (!accepts(id, now) || submitted < 0 || ttsStart >= 0) return false;
        ttsStart = lastEvent = now;
        return true;
    }

    public boolean speechFinished(long id, long now) {
        if (!accepts(id, now) || submitted < 0 || ttsEnd >= 0) return false;
        ttsEnd = lastEvent = now;
        return true;
    }

    public boolean complete(long id, long now) { return finish(id, now, Outcome.COMPLETED); }
    public boolean interrupt(long id, long now) { return finish(id, now, Outcome.INTERRUPTED); }

    public boolean recognitionFailed(long id, long now, int errorCode) {
        if (!finish(id, now, Outcome.RECOGNITION_ERROR)) return false;
        recognitionError = errorCode;
        return true;
    }

    public boolean stop(long id, long now, VoiceConversationLoop.StopReason reason) {
        if (!finish(id, now, Outcome.STOPPED)) return false;
        stopReason = reason;
        return true;
    }

    /** A single fixed-schema line per finished attempt, including failed/retried captures. */
    public String takeLogLine() {
        if (finished < 0 || reported) return null;
        reported = true;
        return "voice_turn token=" + token + " result=" + outcome.name()
                + " stop_reason=" + (stopReason == null ? "none" : stopReason.name())
                + " asr_error=" + (recognitionError == null ? "none" : recognitionError)
                + " asr_ready_ms=" + duration(captureStart, ready)
                + " asr_finalize_ms=" + duration(speechEnd, asrFinal)
                + " response_wait_ms=" + duration(submitted, reply)
                + " reply_to_tts_start_ms=" + duration(reply, ttsStart)
                + " submit_to_tts_start_ms=" + duration(submitted, ttsStart)
                + " tts_duration_ms=" + duration(ttsStart, ttsEnd)
                + " turn_total_ms=" + duration(captureStart, finished);
    }

    private boolean finish(long id, long now, Outcome result) {
        if (!accepts(id, now)) return false;
        finished = lastEvent = now;
        outcome = result;
        captureStage = CaptureStage.FINISHED;
        return true;
    }

    private boolean accepts(long id, long now) {
        return id == token && isActive() && now >= lastEvent;
    }

    private static String duration(long from, long to) {
        return from < 0 || to < from ? "missing" : Long.toString(to - from);
    }
}
