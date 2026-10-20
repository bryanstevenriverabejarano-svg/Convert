package salve.core.trace;

import android.content.Context;
import java.io.File;

/** One trace per conversation worker turn. Unrelated/background threads cannot inherit its notes. */
public final class ExecutionTrace {
    private static ExecutionJournal journal;
    private static final ThreadLocal<String> current = new ThreadLocal<>();
    private ExecutionTrace() { }
    public static synchronized ExecutionJournal journal(Context context) {
        if (journal == null) journal = new ExecutionJournal(new File(context.getNoBackupFilesDir(), "execution-trace"));
        return journal;
    }
    public static void begin(Context context, String type, String request) {
        if (current.get() == null) current.set(journal(context).begin(type, request));
    }
    public static void note(String kind, String text) {
        String id = current.get();
        if (id != null) journal.append(id, kind, text);
    }
    public static boolean active() { return current.get() != null; }
    public static void finish(String status) {
        String id = current.get(); current.remove();
        if (id != null) journal.finish(id, status);
    }
}
