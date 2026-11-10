package salve.core.trace;

import android.content.Context;
import java.io.File;

/** One trace per conversation worker turn. Unrelated/background threads cannot inherit its notes. */
public final class ExecutionTrace {
    private static ExecutionJournal journal;
    private static File journalPath;
    private static final ThreadLocal<String> current = new ThreadLocal<>();
    // Sidecar for timing only; a drawing or research task can still start its own journal.
    private static final ThreadLocal<String> conversationMetrics = new ThreadLocal<>();
    private ExecutionTrace() { }
    public static synchronized ExecutionJournal journal(Context context) {
        File expected = new File(context.getApplicationContext().getNoBackupFilesDir(), "execution-trace");
        // Test/application contexts can change over the lifetime of a process.
        // Never expose the previous context's journal from a static singleton.
        if (journal == null || journalPath == null || !journalPath.equals(expected)) {
            journal = new ExecutionJournal(expected);
            journalPath = expected;
        }
        return journal;
    }
    public static void begin(Context context, String type, String request) {
        if (current.get() == null) current.set(journal(context).begin(type, request));
    }
    /**
     * Starts a private performance record on the conversation worker thread, without changing
     * the current drawing/research trace. Repeated calls within one turn are idempotent.
     * No message, answer, prompt, memory or tool argument is written to this record.
     */
    public static void beginConversationMetrics(Context context) {
        if (conversationMetrics.get() != null || current.get() != null) return;
        String id = journal(context).begin("Conversación · rendimiento",
                "Métricas técnicas del turno, sin guardar mensajes ni respuestas.");
        if (id != null) conversationMetrics.set(id);
    }
    private static boolean isPerformanceNote(String kind) {
        return "VELOCIDAD".equals(kind) || "RENDIMIENTO DEL MODELO".equals(kind)
                || "TIEMPO DEL TURNO".equals(kind);
    }
    public static void note(String kind, String text) {
        String id = current.get();
        if (id != null) journal.append(id, kind, text);
        String metricId = conversationMetrics.get();
        if (metricId != null && isPerformanceNote(kind)) journal.append(metricId, kind, text);
    }
    public static boolean active() { return current.get() != null; }
    public static void finish(String status) {
        String id = current.get(); current.remove();
        String metricId = conversationMetrics.get(); conversationMetrics.remove();
        if (id != null) journal.finish(id, status);
        if (metricId != null && !metricId.equals(id)) journal.finish(metricId, status);
    }
}
