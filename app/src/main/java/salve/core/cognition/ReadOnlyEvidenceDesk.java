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

    /** Evidence for existing improvement proposals; no new store or analysis engine. */
    public static String recentVoiceFailures(Context context, int max) {
        ExecutionJournal journal = ExecutionTrace.journal(context.getApplicationContext());
        StringBuilder out = new StringBuilder();
        int shown = 0;
        for (ExecutionJournal.Task task : journal.recent()) {
            if (shown >= Math.max(0, Math.min(max, 3))) break;
            if (task == null || task.type == null || !task.type.startsWith("Voz · ")
                    || !"ERROR".equals(task.status)) continue;
            appendTask(out, task);
            shown++;
        }
        if (shown == 0) return "";
        return "\n\nFallos recientes de voz observados (no prueban una causa):\n" + out
                + "Compara las trazas y valida cualquier corrección antes de aplicarla.";
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
        // Recent voice failures are relevant evidence; do not bury them under sleep reports.
        for (ExecutionJournal.Task task : tasks) {
            if (shown >= max) break;
            if (task == null || task.type == null || !task.type.startsWith("Voz · ")) continue;
            if (!"ERROR".equals(task.status)) continue;
            appendTask(out, task);
            shown++;
        }
        // Prefer the sleep run when the previous request is about a night cycle.
        for (ExecutionJournal.Task task : tasks) {
            if (shown >= max || task == null || task.type == null || !task.type.equals("Sueño autónomo")) continue;
            appendTask(out, task);
            if (++shown >= max) break;
        }
        for (ExecutionJournal.Task task : tasks) {
            if (shown >= max) break;
            if (task == null || task.type == null || task.type.equals("Sueño autónomo") || (task.type.startsWith("Voz · ") && "ERROR".equals(task.status))) continue;
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
        // Voice traces store controlled event names and numeric values, not speech text.
        if (task.type != null && task.type.startsWith("Voz · ")) {
            int emitted = 0;
            for (int i = task.events.size() - 1; i >= 0 && emitted < 4; i--) {
                ExecutionJournal.Event event = task.events.get(i);
                if (event == null || !"AUDIO".equals(event.kind) || event.text == null) continue;
                if (!event.text.matches("elapsed_ms=[0-9]+ event=[A-Z0-9_]{1,50} code=-?[0-9]+")) continue;
                out.append("  AUDIO: ").append(bounded(event.text, 110)).append('\n');
                emitted++;
            }
            if (task.omitted > 0) out.append("  Aviso: eventos omitidos; traza incompleta.\n");
            return;
        }
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
