package salve.core.tasks;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicit user commands only. Quoted observations and generated text never enter this parser. */
public final class TaskCommand {
    public enum Action { CREATE, LIST, RESULT, CANCEL, RESUME, PAUSE_ALL, RESUME_ALL }
    public final Action action;
    public final String argument;
    private TaskCommand(Action action, String argument) { this.action = action; this.argument = argument; }
    private static final Pattern CREATE = Pattern.compile(
            "^(?:salve[, ]+)?(?:investiga y recuerda|investiga en segundo plano|crea una tarea de investigacion)\\s*:?\\s*(.*)$",
            Pattern.DOTALL);
    public static TaskCommand parse(String input) {
        if (input == null || input.length() > 2400) return null;
        // Accent removal preserves offsets; match the original topic to keep case-sensitive URLs intact.
        String original = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);
        String plain = Normalizer.normalize(original, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String lower = plain.toLowerCase(Locale.ROOT);
        Matcher create = CREATE.matcher(lower);
        if (create.matches()) return new TaskCommand(Action.CREATE, original.substring(create.start(1)).trim());
        lower = lower.replaceAll("\\s+", " ");
        if (lower.equals("mis tareas") || lower.equals("estado de tareas") || lower.equals("estado tareas"))
            return new TaskCommand(Action.LIST, "");
        if (lower.equals("pausa tus tareas") || lower.equals("pausa tareas")) return new TaskCommand(Action.PAUSE_ALL, "");
        if (lower.equals("reanuda tus tareas") || lower.equals("reanuda tareas")) return new TaskCommand(Action.RESUME_ALL, "");
        Matcher action = Pattern.compile("^(resultado(?: de)?|cancela(?:r)?|reanuda(?:r)?) (?:la )?tarea ([a-f0-9-]{8,36}|ultima)$").matcher(lower);
        if (!action.matches()) {
            if (lower.equals("resultado de la ultima tarea") || lower.equals("resultado de mi ultima tarea"))
                return new TaskCommand(Action.RESULT, "ultima");
            return null;
        }
        return new TaskCommand(action.group(1).startsWith("resultado") ? Action.RESULT
                : action.group(1).startsWith("cancela") ? Action.CANCEL : Action.RESUME, action.group(2));
    }
    public static Boolean globalAutonomyPause(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return normalized.equals("pausa tu autonomia") ? Boolean.TRUE
                : normalized.equals("reanuda tu autonomia") ? Boolean.FALSE : null;
    }
}
