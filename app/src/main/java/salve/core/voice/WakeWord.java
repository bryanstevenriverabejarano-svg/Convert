package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detecta que Bryan llama a Salve por su nombre al empezar a hablar: «Salve», «oye Salve»,
 * «hola Salve, ¿qué tiempo hace?». Lo que dice detrás del nombre es su primera petición.
 *
 * <p>Solo cuenta al principio de la frase: «Dios te salve» o «me salvé por poco» no la despiertan.</p>
 */
public final class WakeWord {
    private WakeWord() { }

    private static final Pattern CALL = Pattern.compile(
            "^(?:(?:oye|hola|hey|ey|eh|vale|venga|buenas)\\s+)?salve\\b[\\s,.:;!-]*(.*)$");

    /** El resultado de escuchar una frase. */
    public static final class Hit {
        /** Lo que dijo después de «Salve» (vacío si solo la llamó). */
        public final String request;
        Hit(String request) { this.request = request; }
    }

    /** null si la frase no empieza llamándola. */
    public static Hit detect(String heard) {
        if (heard == null || heard.length() > 400) return null;
        String plain = Normalizer.normalize(heard.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        if (!heard.toLowerCase(Locale.ROOT).trim().replaceFirst("^(?:oye|hola|hey|ey|eh|vale|venga|buenas)\\s+", "").startsWith("salve")
                && plain.trim().replaceFirst("^(?:oye|hola|hey|ey|eh|vale|venga|buenas)\\s+", "").startsWith("salve")) {
            return null; // «salvé» con tilde es otra palabra
        }
        Matcher m = CALL.matcher(plain.trim().replaceAll("\\s+", " "));
        if (!m.matches()) return null;
        // Se devuelve el resto en su forma original (con tildes), recortando lo mismo que se reconoció.
        String original = heard.trim().replaceAll("\\s+", " ");
        int cut = original.length() - m.group(1).length();
        String rest = cut >= 0 && cut <= original.length() ? original.substring(cut).trim() : m.group(1).trim();
        return new Hit(rest);
    }

    private static String plain(String input) {
        return input == null ? "" : Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,]", " ").replaceAll("\\s+", " ").trim();
    }

    /** «Escúchame siempre», «activa la palabra Salve», «quiero llamarte por tu nombre». */
    public static boolean asksToEnable(String input) {
        String t = plain(input);
        return t.length() < 120 && t.matches("(?:salve )?(?:activa|enciende|pon|quiero) (?:la )?(?:palabra (?:de activacion|salve)|escucha (?:continua|permanente)"
                + "|modo escucha|que me escuches siempre)|(?:salve )?escuchame siempre|(?:salve )?quiero (?:llamarte|despertarte) (?:por tu nombre|diciendo salve)");
    }

    /** «Deja de escuchar tu nombre», «desactiva la palabra Salve». */
    public static boolean asksToDisable(String input) {
        String t = plain(input);
        return t.length() < 120 && t.matches("(?:salve )?(?:desactiva|apaga|quita) (?:la )?(?:palabra (?:de activacion|salve)|escucha (?:continua|permanente)|modo escucha)"
                + "|(?:salve )?(?:deja de|no) escuchar? (?:tu nombre|siempre)|(?:salve )?no me escuches siempre");
    }
}
