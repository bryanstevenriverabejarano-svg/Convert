package salve.core.cognition;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mandos físicos del teléfono que Salve puede accionar: linterna, volumen y vibración.
 *
 * Igual que con la música, este reconocedor solo decide qué mando puede elegir el modelo en este
 * turno y con qué valor: la acción y el porcentaje salen de lo que dijo el usuario, nunca de los
 * argumentos del modelo. No ejecuta nada. Son efectos locales, inmediatos y reversibles; lo que
 * envía, llama, compra o borra sigue por sus rutas con revisión.
 */
public final class DeviceControlIntent {
    public static final String TOOL = "device.control";

    public enum Action { TORCH_ON, TORCH_OFF, VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE, VOLUME_SET, VIBRATE }

    public static final class Command {
        public final Action action;
        /** Solo para VOLUME_SET: 0..100. */
        public final int percent;
        Command(Action action, int percent) { this.action = action; this.percent = percent; }
    }

    private static final Pattern NEGATED = Pattern.compile(
            "\\b(?:no|nunca|jamas)\\s+(?:(?:me|te|le|lo|la)\\s+)?(?:enciendas|prendas|apagues|subas|bajes|pongas|silencies|vibres|toques|cambies)\\b");
    private static final Pattern TORCH_ON = Pattern.compile(
            "\\b(?:enciende|prende|activa|pon|dame|ilumina con)(?:me)? (?:la |tu )?(?:linterna|luz del flash|flash)\\b");
    private static final Pattern TORCH_OFF = Pattern.compile(
            "\\b(?:apaga|desactiva|quita)(?:me)? (?:la |tu )?(?:linterna|luz del flash|flash)\\b");
    private static final Pattern VOLUME_SET = Pattern.compile(
            "\\b(?:pon|sube|baja|ajusta|deja)(?:me)? (?:el |tu )?volumen (?:al|a|en) (\\d{1,3})(?: por ciento)?\\b");
    private static final Pattern VOLUME_UP = Pattern.compile(
            "\\b(?:sube(?:le)?|aumenta) (?:un poco )?(?:el |tu )?volumen\\b|\\bmas volumen\\b|\\bhabla mas alto\\b");
    private static final Pattern VOLUME_DOWN = Pattern.compile(
            "\\b(?:baja(?:le)?|reduce) (?:un poco )?(?:el |tu )?volumen\\b|\\bmenos volumen\\b|\\bhabla mas bajo\\b");
    private static final Pattern VOLUME_MUTE = Pattern.compile(
            "\\b(?:silencia (?:el )?(?:movil|telefono|volumen|sonido)|quita el (?:sonido|volumen)|volumen a cero|sin sonido)\\b");
    private static final Pattern VIBRATE = Pattern.compile(
            "^(?:salve )?(?:vibra|haz vibrar (?:el |mi )?(?:movil|telefono)|haz que vibre (?:el |mi )?(?:movil|telefono))(?: una vez| un momento)?$");

    private DeviceControlIntent() { }

    /**
     * El mando pedido en este turno, o null si no pidió ninguno (o lo prohibió). Si no es la frase
     * exacta pero su modelo de significado lo entendió («no veo nada, dame luz»), vale su canónica.
     */
    public static Command parse(String input) {
        Command exact = parseExact(input);
        if (exact != null || input == null) return exact;
        String canonical = ActionMeaning.canonicalFor(input, TOOL);
        return canonical == null ? null : parseExact(canonical);
    }

    static Command parseExact(String input) {
        if (input == null) return null;
        String s = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace("%", " por ciento").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
        if (s.isEmpty() || s.length() > 200 || NEGATED.matcher(s).find()) return null;
        if (TORCH_OFF.matcher(s).find()) return new Command(Action.TORCH_OFF, 0);
        if (TORCH_ON.matcher(s).find()) return new Command(Action.TORCH_ON, 0);
        Matcher set = VOLUME_SET.matcher(s);
        if (set.find()) {
            int percent = Integer.parseInt(set.group(1));
            return percent > 100 ? null : new Command(Action.VOLUME_SET, percent);
        }
        if (VOLUME_MUTE.matcher(s).find()) return new Command(Action.VOLUME_MUTE, 0);
        if (VOLUME_UP.matcher(s).find()) return new Command(Action.VOLUME_UP, 0);
        if (VOLUME_DOWN.matcher(s).find()) return new Command(Action.VOLUME_DOWN, 0);
        if (VIBRATE.matcher(s).find()) return new Command(Action.VIBRATE, 0);
        return null;
    }

    public static boolean offered(String input) { return parse(input) != null; }

    /** Una línea de catálogo con el único mando que pidió el usuario. */
    public static String description(String input) {
        Command command = parse(input);
        if (command == null) throw new IllegalArgumentException("Sin mando pedido.");
        return "{accion:'" + command.action.name() + "'}: acciona ese mando del teléfono que pidió el usuario. "
                + "Cuenta solo el estado real que recibes.";
    }

    /** El modelo nunca convierte «apaga» en «enciende» ni «baja» en «sube». */
    public static Command verified(String input, String proposed) {
        Command expected = parse(input);
        if (expected == null || proposed == null || !expected.action.name().equalsIgnoreCase(proposed.trim()))
            throw new IllegalArgumentException("Mando distinto de la petición original.");
        return expected;
    }

    /** Nuevo índice de volumen para un máximo dado; nunca fuera de 0..max. */
    public static int targetVolume(Command command, int current, int max) {
        if (max <= 0) return 0;
        int step = Math.max(1, Math.round(max * .15f));
        int target;
        switch (command.action) {
            case VOLUME_UP: target = current + step; break;
            case VOLUME_DOWN: target = current - step; break;
            case VOLUME_MUTE: target = 0; break;
            case VOLUME_SET: target = Math.round(max * command.percent / 100f); break;
            default: target = current; break;
        }
        return Math.max(0, Math.min(max, target));
    }
}
