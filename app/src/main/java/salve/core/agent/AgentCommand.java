package salve.core.agent;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only user turns enter this parser; schedule creation is an explicit continuing grant. */
public final class AgentCommand {
    public enum Action { CREATE, LIST, RESULT, CANCEL, RESUME, SUBSCRIBE, SUBSCRIPTIONS, UNSUBSCRIBE, STATUS, CONFIGURE }
    public final Action action;
    public final String argument, event;
    public final boolean code;
    public final long interval;
    private AgentCommand(Action action, String argument, String event, boolean code, long interval) {
        this.action = action; this.argument = argument; this.event = event; this.code = code; this.interval = interval;
    }
    public static AgentCommand parse(String input) {
        if (input == null || input.length() > 2400) return null;
        String original = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);
        String plain = Normalizer.normalize(original, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        Matcher create = Pattern.compile("^(?:salve[, ]+)?resuelve (y recuerda|con codigo)\\s*:\\s*(.+)$", Pattern.DOTALL).matcher(plain);
        if (create.matches()) return new AgentCommand(Action.CREATE, original.substring(create.start(2)).trim(), "", create.group(1).equals("con codigo"), 0);
        Matcher schedule = Pattern.compile("^vigila cada (\\d{1,3}) horas?\\s*:\\s*(.+)$", Pattern.DOTALL).matcher(plain);
        if (schedule.matches()) return new AgentCommand(Action.SUBSCRIBE, original.substring(schedule.start(2)).trim(), "TIMER", false,
                Long.parseLong(schedule.group(1)) * 3_600_000L);
        Matcher event = Pattern.compile("^vigila (al abrir salve|mientras carga)\\s*:\\s*(.+)$", Pattern.DOTALL).matcher(plain);
        if (event.matches()) return new AgentCommand(Action.SUBSCRIBE, original.substring(event.start(2)).trim(),
                event.group(1).equals("al abrir salve") ? "APP_OPEN" : "CHARGING", false, 6 * 3_600_000L);
        plain = plain.replaceAll("\\s+", " ");
        if (plain.equals("mis planes")) return command(Action.LIST, "");
        if (plain.equals("mis vigilancias")) return command(Action.SUBSCRIPTIONS, "");
        if (plain.equals("estado del agente") || plain.equals("herramientas del agente")) return command(Action.STATUS, "");
        if (plain.equals("configura herramientas")) return command(Action.CONFIGURE, "");
        if (plain.equals("resultado de mi ultimo plan") || plain.equals("resultado del ultimo plan")) return command(Action.RESULT, "ultimo");
        Matcher task = Pattern.compile("^(resultado(?: del?)?|cancela|reanuda) (?:el )?plan ([a-f0-9-]{8,36}|ultimo)$").matcher(plain);
        if (task.matches()) return command(task.group(1).startsWith("resultado") ? Action.RESULT
                : task.group(1).equals("cancela") ? Action.CANCEL : Action.RESUME, task.group(2));
        Matcher stop = Pattern.compile("^cancela vigilancia ([a-f0-9-]{8,36})$").matcher(plain);
        return stop.matches() ? command(Action.UNSUBSCRIBE, stop.group(1)) : null;
    }
    private static AgentCommand command(Action action, String arg) { return new AgentCommand(action, arg, "", false, 0); }
}
