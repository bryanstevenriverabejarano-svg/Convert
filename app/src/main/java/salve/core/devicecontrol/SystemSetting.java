package salve.core.devicecontrol;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ajustes del sistema que una app normal no puede cambiar (wifi, Bluetooth, datos, modo avión, no
 * molestar, brillo) y que Salve acciona a través de Shizuku con identidad de shell, sin root.
 *
 * Es una lista cerrada. La frase de Bryan fija el ajuste y su valor; el modelo solo decide usarlo.
 * Cada ajuste se traduce aquí a órdenes fijas: ni la app ni el modelo envían texto de shell. El
 * servicio privilegiado ({@code ShizukuSettingsService}) recibe solo el número del ajuste y un entero,
 * y vuelve a consultar esta tabla en su propio proceso.
 */
public final class SystemSetting {
    public static final String TOOL = "device.setting";

    public enum Action {
        WIFI_ON("wifi", "svc wifi enable", "settings get global wifi_on"),
        WIFI_OFF("wifi", "svc wifi disable", "settings get global wifi_on"),
        BLUETOOTH_ON("Bluetooth", "svc bluetooth enable", "settings get global bluetooth_on"),
        BLUETOOTH_OFF("Bluetooth", "svc bluetooth disable", "settings get global bluetooth_on"),
        MOBILE_DATA_ON("datos móviles", "svc data enable", "settings get global mobile_data"),
        MOBILE_DATA_OFF("datos móviles", "svc data disable", "settings get global mobile_data"),
        AIRPLANE_ON("modo avión", "cmd connectivity airplane-mode enable", "settings get global airplane_mode_on"),
        AIRPLANE_OFF("modo avión", "cmd connectivity airplane-mode disable", "settings get global airplane_mode_on"),
        DND_ON("no molestar", "cmd notification set_dnd on", "settings get global zen_mode"),
        DND_OFF("no molestar", "cmd notification set_dnd off", "settings get global zen_mode"),
        BRIGHTNESS_AUTO_ON("brillo automático", "settings put system screen_brightness_mode 1",
                "settings get system screen_brightness_mode"),
        BRIGHTNESS_AUTO_OFF("brillo automático", "settings put system screen_brightness_mode 0",
                "settings get system screen_brightness_mode"),
        /** El valor (0..100 %) se convierte al nivel 1..255 del sistema. */
        BRIGHTNESS_SET("brillo", null, "settings get system screen_brightness");

        public final String label;
        private final String command, verify;
        Action(String label, String command, String verify) { this.label = label; this.command = command; this.verify = verify; }
    }

    public static final class Command {
        public final Action action;
        /** Solo para BRIGHTNESS_SET: 0..100. */
        public final int value;
        Command(Action action, int value) { this.action = action; this.value = value; }
    }

    private SystemSetting() { }

    // ---------------------------------------------------------------- órdenes fijas (proceso privilegiado)

    /** Las órdenes exactas de un ajuste, ya partidas en argumentos; null si el ajuste o el valor no son válidos. */
    public static String[][] commands(int actionOrdinal, int value) {
        Action[] all = Action.values();
        if (actionOrdinal < 0 || actionOrdinal >= all.length) return null;
        Action action = all[actionOrdinal];
        if (action == Action.BRIGHTNESS_SET) {
            if (value < 0 || value > 100) return null;
            int level = Math.max(1, Math.min(255, Math.round(value * 255f / 100f)));
            // Con el brillo automático puesto, el sistema ignoraría el nivel manual.
            return new String[][]{{"settings", "put", "system", "screen_brightness_mode", "0"},
                    {"settings", "put", "system", "screen_brightness", String.valueOf(level)}};
        }
        return new String[][]{action.command.split(" ")};
    }

    /** La consulta fija que lee el estado después del cambio. */
    public static String[] verification(int actionOrdinal) {
        Action[] all = Action.values();
        return actionOrdinal < 0 || actionOrdinal >= all.length ? null : all[actionOrdinal].verify.split(" ");
    }

    /** Lo que significa la lectura del sistema, en palabras; nunca afirma más de lo leído. */
    public static String explain(Action action, String read) {
        String v = read == null ? "" : read.trim();
        if (v.isEmpty() || v.equals("null")) return action.label + ": Android no devolvió su estado; no hay cambio confirmado.";
        switch (action) {
            case BRIGHTNESS_SET: {
                try { return "brillo: nivel " + v + " de 255 (≈ " + Math.round(Integer.parseInt(v) * 100f / 255f) + " %), en manual."; }
                catch (NumberFormatException odd) { return "brillo: el sistema devolvió «" + v + "»."; }
            }
            case DND_ON: case DND_OFF:
                return "no molestar: " + (v.equals("0") ? "desactivado." : "activado.");
            default:
                return action.label + ": " + (v.equals("0") ? "desactivado." : "activado.") + " (el sistema informa " + v + ")";
        }
    }

    // ---------------------------------------------------------------- lo que pidió Bryan

    private static final String ON = "(?:activa|enciende|prende|pon|conecta|habilita)(?:me)?";
    private static final String OFF = "(?:desactiva|apaga|quita|desconecta|deshabilita)(?:me)?";
    private static final Pattern NEGATED = Pattern.compile(
            "\\b(?:no|nunca|jamas)\\s+(?:(?:me|te|le|lo|la)\\s+)?(?:actives|enciendas|prendas|pongas|conectes|desactives|apagues|"
            + "quites|desconectes|subas|bajes|cambies|toques)\\b");
    private static final Pattern BRIGHTNESS = Pattern.compile(
            "\\b(?:pon|sube|baja|ajusta|deja)(?:me)? (?:el |tu )?brillo (?:al|a|en) (?:(\\d{1,3})(?: por ciento)?|(maximo|minimo|la mitad))\\b");
    private static final Object[][] SWITCHES = {
            {"(?:el |la )?wi ?fi", Action.WIFI_ON, Action.WIFI_OFF},
            {"(?:el )?bluetooth", Action.BLUETOOTH_ON, Action.BLUETOOTH_OFF},
            {"(?:los )?datos(?: moviles)?", Action.MOBILE_DATA_ON, Action.MOBILE_DATA_OFF},
            {"(?:el )?modo avion", Action.AIRPLANE_ON, Action.AIRPLANE_OFF},
            {"(?:el )?(?:modo )?no molestar", Action.DND_ON, Action.DND_OFF},
            {"(?:el )?brillo automatico", Action.BRIGHTNESS_AUTO_ON, Action.BRIGHTNESS_AUTO_OFF},
    };

    /** El ajuste pedido en este turno, o null si no pidió ninguno (o lo prohibió). */
    public static Command parse(String input) {
        if (input == null) return null;
        String s = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace("%", " por ciento").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
        if (s.isEmpty() || s.length() > 200 || NEGATED.matcher(s).find()) return null;
        Matcher brightness = BRIGHTNESS.matcher(s);
        if (brightness.find()) {
            int percent = brightness.group(1) != null ? Integer.parseInt(brightness.group(1))
                    : brightness.group(2).equals("maximo") ? 100 : brightness.group(2).equals("minimo") ? 5 : 50;
            return percent > 100 ? null : new Command(Action.BRIGHTNESS_SET, percent);
        }
        for (Object[] row : SWITCHES) {
            // Se mira antes «apaga»: «apaga… y enciende» no debe leerse como encender.
            if (Pattern.compile("\\b" + OFF + " " + row[0] + "\\b").matcher(s).find()) return new Command((Action) row[2], 0);
            if (Pattern.compile("\\b" + ON + " " + row[0] + "\\b").matcher(s).find()) return new Command((Action) row[1], 0);
        }
        return null;
    }

    public static boolean offered(String input) { return parse(input) != null; }

    public static String description(String input) {
        Command command = parse(input);
        if (command == null) throw new IllegalArgumentException("Sin ajuste pedido.");
        return "{accion:'" + command.action.name() + "'}: cambia ese ajuste (" + command.action.label
                + ") que pidió el usuario. Cuenta solo lo que el sistema informa; si falta Shizuku, dilo.";
    }

    /** El modelo nunca convierte «apaga» en «enciende» ni cambia de ajuste. */
    public static Command verified(String input, String proposed) {
        Command expected = parse(input);
        if (expected == null || proposed == null || !expected.action.name().equalsIgnoreCase(proposed.trim()))
            throw new IllegalArgumentException("Ajuste distinto de la petición original.");
        return expected;
    }
}
