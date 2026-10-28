package salve.core.cognition;

import android.content.Context;
import java.util.List;
import salve.core.evaluation.SleepImprovementRuntime;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/** Grounded, read-only response sources. Never generates an event merely to answer a query. */
public final class ReadOnlyEvidenceDesk {
    private ReadOnlyEvidenceDesk() { }

    public static String render(Context context, String scope) {
        if (!"sleep".equals(scope) && !"execution".equals(scope) && !"both".equals(scope))
            throw new IllegalArgumentException("Tipo de informe desconocido.");
        boolean sleep = !"execution".equals(scope);
        boolean execution = !"sleep".equals(scope);
        StringBuilder out = new StringBuilder();
        if (sleep) out.append("INFORME NOCTURNO (datos locales):\n")
                .append(SleepImprovementRuntime.status(context));
        if (execution) {
            if (out.length() > 0) out.append("\n\n");
            out.append("REGISTRO DE EJECUCIÓN (tareas observadas):\n")
                    .append(recent(context, 3));
        }
        return out.toString();
    }

    /** The existing journal is authoritative; these are metadata, not an invented chain of thought. */
    public static String recent(Context context, int max) {
        ExecutionJournal journal = ExecutionTrace.journal(context.getApplicationContext());
        List<ExecutionJournal.Task> tasks = journal.recent();
        String notice = journal.notice();
        StringBuilder out = new StringBuilder();
        if (notice != null && !notice.isEmpty()) out.append("Aviso: ").append(notice).append('\n');
        if (tasks.isEmpty()) return out.append("Todavía no hay tareas registradas en este dispositivo.").toString();
        int shown = 0;
        // Prefer the sleep run when the previous request is about a night cycle.
        for (ExecutionJournal.Task task : tasks) {
            if (task == null || task.type == null || !task.type.equals("Sueño autónomo")) continue;
            appendTask(out, task);
            if (++shown >= max) break;
        }
        for (ExecutionJournal.Task task : tasks) {
            if (shown >= max) break;
            if (task == null || task.type == null || task.type.equals("Sueño autónomo")) continue;
            appendTask(out, task);
            shown++;
        }
        out.append("El registro completo se puede abrir en IA y cámara → Registro de ejecución.");
        return out.toString();
    }

    private static void appendTask(StringBuilder out, ExecutionJournal.Task task) {
        out.append("• ").append(bounded(task.type, 45)).append(" · ")
                .append(bounded(task.status, 36)).append(" · ")
                .append(ExecutionJournal.date(task.started)).append('\n');
        if (task.events == null) return;
        // Include only short non-user-origin statuses; full request/prompt stays inside the journal UI.
        for (int i = task.events.size() - 1; i >= 0; i--) {
            ExecutionJournal.Event event = task.events.get(i);
            if (event == null || event.kind == null || event.text == null
                    || "PETICIÓN".equals(event.kind)) continue;
            if (!"Sueño autónomo".equals(task.type)) break;
            out.append("  ").append(bounded(event.kind, 32)).append(": ")
                    .append(bounded(event.text, 140)).append('\n');
            break;
        }
    }

    private static String bounded(String raw, int limit) {
        String s = raw == null ? "" : raw.replaceAll("[\\p{Cntrl}]+", " ").trim();
        return s.length() <= limit ? s : s.substring(0, limit - 1) + "…";
    }
}
