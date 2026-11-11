package salve.core.voice;

import android.content.Context;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/**
 * Records real playback and neural synthesis separately from model-inference timing.
 *
 * Receives ONLY a generated utterance id, controlled event names and numeric values.
 * No prompts, transcripts, speech samples, artist names or TTS text are persisted.
 * I/O never runs on the synthesis, binder or UI thread.
 */
public final class VoicePlaybackJournal {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(job -> {
        Thread worker = new Thread(job, "salve-playback-trace");
        worker.setDaemon(true);
        return worker;
    });
    /** Accessed exclusively by IO; one registered task per actual utterance/synthesis. */
    private static final Map<String, Task> TASKS = new HashMap<>();
    private static final class Task {
        final ExecutionJournal journal;
        final String id;
        final long started;
        Task(ExecutionJournal journal, String id, long started) {
            this.journal = journal;
            this.id = id;
            this.started = started;
        }
    }

    private VoicePlaybackJournal() { }

    public static void begin(Context context, String key, String category, int textChars) {
        if (context == null || key == null || !safeKey(key) || category == null || textChars < 0) return;
        Context app = context.getApplicationContext();
        long start = SystemClock.elapsedRealtime();
        IO.execute(() -> {
            ExecutionJournal journal = ExecutionTrace.journal(app);
            String id = journal.begin("Voz · " + category,
                    "Eventos reales del sintetizador; sin audio ni texto de la conversación.");
            if (id == null) return;
            Task previous = TASKS.put(key, new Task(journal, id, start));
            if (previous != null) previous.journal.finish(previous.id, "SUSTITUIDA");
            journal.append(id, "INICIO", "chars=" + textChars);
        });
    }

    public static void event(String key, String event, long numericCode) {
        if (!safeKey(key) || !safeEvent(event)) return;
        long moment = SystemClock.elapsedRealtime();
        IO.execute(() -> {
            Task task = TASKS.get(key);
            if (task == null) return;
            task.journal.append(task.id, "AUDIO",
                    "elapsed_ms=" + Math.max(0, moment - task.started)
                            + " event=" + event + " code=" + numericCode);
        });
    }

    public static void finish(String key, String state) {
        if (!safeKey(key) || !safeEvent(state)) return;
        IO.execute(() -> {
            Task task = TASKS.remove(key);
            if (task != null) task.journal.finish(task.id, state);
        });
    }

    private static boolean safeKey(String key) {
        return key != null && key.length() <= 100 && key.matches("[a-zA-Z0-9_]+");
    }
    private static boolean safeEvent(String event) {
        return event != null && event.length() <= 50 && event.matches("[A-Z0-9_]+");
    }

    /** For unit tests only; never block Android audio or UI threads waiting for writes. */
    static boolean awaitIdleForTests() throws Exception {
        return IO.submit(() -> true).get(5, TimeUnit.SECONDS);
    }
}
