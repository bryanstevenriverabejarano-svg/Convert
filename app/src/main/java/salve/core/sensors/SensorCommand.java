package salve.core.sensors;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicit, bounded access to physical measurements; never an emotion inference. */
public final class SensorCommand {
    public enum Action { START, STOP, STATUS }
    public final Action action;
    public final long durationMillis;
    private SensorCommand(Action action, long durationMillis) {
        this.action = action; this.durationMillis = durationMillis;
    }
    private static final Pattern START = Pattern.compile(
            "^activa (?:tus )?sensores(?: (?:durante )?(30|60) segundos)?$");

    public static SensorCommand parse(String input) {
        if (input == null || input.length() > 200) return null;
        String text = Normalizer.normalize(input.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        // ASR may append declarative punctuation. Keep question marks intact so
        // asking about activation never authorizes a hardware session.
        text = text.replaceFirst("[.!]+$", "").trim();
        Matcher start = START.matcher(text);
        if (start.matches()) return new SensorCommand(Action.START,
                start.group(1) == null ? 30_000L : Long.parseLong(start.group(1)) * 1000L);
        if (text.equals("desactiva tus sensores") || text.equals("desactiva sensores")
                || text.equals("deten sensores")) return new SensorCommand(Action.STOP, 0);
        String statusText = text.replaceFirst("^¿\\s*", "")
                .replaceFirst("\\s*\\?$", "").trim();
        if (statusText.equals("estado de los sensores") || statusText.equals("consultar sensores")
                || statusText.equals("que percibes ahora")) return new SensorCommand(Action.STATUS, 0);
        return null;
    }
}
