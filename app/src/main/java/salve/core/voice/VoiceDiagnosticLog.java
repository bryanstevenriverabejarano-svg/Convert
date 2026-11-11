package salve.core.voice;

import android.content.Context;
import android.os.SystemClock;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/**
 * One private execution-journal task per foreground voice session.
 * Only fixed event enums, monotonic durations and numeric system codes are accepted.
 * Never receives audio, transcripts, prompts, assistant responses or personal content.
 */
public final class VoiceDiagnosticLog {
    public enum Event {
        SESSION_STARTED, SESSION_ENDED, LISTEN_STARTED, REPLY_READY, REPLY_WITHOUT_AUDIO,
        INTERRUPT_MANUAL, INTERRUPT_AUTOMATIC, AUDIO_FOCUS_CHANGE, LOOP_STOP,
        RECOGNITION_ERROR, TTS_STARTED, TTS_DONE, TTS_ERROR, TTS_ERROR_CODE,
        TTS_STOPPED, TTS_ENQUEUE_OK, TTS_ENQUEUE_FAILED, TTS_FINISHED_OK,
        TTS_FINISHED_FAILED, MIC_AEC_UNAVAILABLE, MIC_BUFFER_INVALID,
        MIC_INIT_FAILED, MIC_AEC_CREATE_FAILED, MIC_AEC_ENABLE_FAILED,
        MIC_CAPTURE_FAILED, MIC_CAPTURE_ACTIVE, MIC_READ_FAILED,
        MIC_DETECTOR_TRIGGERED, MIC_CAPTURE_EXCEPTION
    }

    private final ExecutionJournal journal;
    private String taskId;
    private long started;

    public VoiceDiagnosticLog(Context context) {
        journal = ExecutionTrace.journal(context.getApplicationContext());
    }

    public synchronized void start() {
        if (taskId != null) return;
        taskId = journal.begin("Conversación · diagnóstico de voz",
                "Solo eventos técnicos del micrófono y TTS, sin audio, mensajes ni respuestas.");
        started = SystemClock.elapsedRealtime();
        event(0L, Event.SESSION_STARTED, 0L);
    }

    public synchronized void event(long token, Event event, long code) {
        if (taskId == null || event == null || token < 0) return;
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - started);
        journal.append(taskId, "VOZ", "token=" + token + " elapsed_ms=" + elapsed
                + " event=" + event.name() + " code=" + code);
    }

    /** Accepts only the existing fixed-schema, machine-generated VoiceTurnMetrics line. */
    public synchronized void metrics(String line) {
        if (taskId == null || line == null || !line.startsWith("voice_turn token=")
                || !line.matches("[a-zA-Z0-9_= \\-]+") || line.length() > 600) return;
        journal.append(taskId, "MÉTRICAS DE VOZ", line);
    }

    public synchronized void finish(long token) {
        if (taskId == null) return;
        event(token, Event.SESSION_ENDED, 0L);
        journal.finish(taskId, "TERMINADA");
        taskId = null;
    }
}
