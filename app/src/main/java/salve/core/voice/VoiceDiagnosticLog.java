package salve.core.voice;

import android.content.Context;
import android.os.SystemClock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/**
 * One private execution-journal task per foreground voice session.
 * Only fixed event enums, monotonic durations and numeric system codes are accepted.
 * Journal I/O is serial and off the audio/UI threads. No audio, text or prompts are received.
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
        MIC_DETECTOR_TRIGGERED, MIC_CAPTURE_EXCEPTION,
        /** Sale por el altavoz del teléfono: el micrófono oiría su propia voz, así que no se escucha encima. */
        MIC_BARGE_IN_SKIPPED_SPEAKER
    }

    private final ExecutionJournal journal;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "salve-voice-diagnostic-io");
        thread.setDaemon(true);
        return thread;
    });
    /** Only the single I/O worker reads and writes the journal task ID. */
    private String taskId;
    private long started;
    private boolean recording;
    private boolean closed;

    public VoiceDiagnosticLog(Context context) {
        journal = ExecutionTrace.journal(context.getApplicationContext());
    }

    public synchronized void start() {
        if (recording || closed) return;
        recording = true;
        started = SystemClock.elapsedRealtime();
        io.execute(() -> {
            taskId = journal.begin("Conversación · diagnóstico de voz",
                    "Solo eventos técnicos del micrófono y TTS, sin audio, mensajes ni respuestas.");
        });
        event(0L, Event.SESSION_STARTED, 0L);
    }

    public synchronized void event(long token, Event event, long code) {
        if (!recording || closed || event == null || token < 0) return;
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - started);
        String text = "token=" + token + " elapsed_ms=" + elapsed
                + " event=" + event.name() + " code=" + code;
        io.execute(() -> {
            if (taskId != null) journal.append(taskId, "VOZ", text);
        });
    }

    /** Accepts only the existing fixed-schema, machine-generated VoiceTurnMetrics line. */
    public synchronized void metrics(String line) {
        if (!recording || closed || line == null || !line.startsWith("voice_turn token=")
                || !line.matches("[a-zA-Z0-9_= \\-]+") || line.length() > 600) return;
        io.execute(() -> {
            if (taskId != null) journal.append(taskId, "MÉTRICAS DE VOZ", line);
        });
    }

    public synchronized void finish(long token) {
        if (!recording || closed) return;
        event(token, Event.SESSION_ENDED, 0L);
        closed = true;
        io.execute(() -> {
            if (taskId != null) journal.finish(taskId, "TERMINADA");
        });
        io.shutdown();
    }

    /** Only used by Robolectric tests; never wait for disk on the Android UI/audio thread. */
    boolean awaitWrittenForTests() throws InterruptedException {
        return io.awaitTermination(5, TimeUnit.SECONDS);
    }
}
