package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * «Usa tu voz nueva», «pon una voz más femenina» → la voz neuronal de Salve; «usa la voz de
 * Android», «vuelve a tu voz de antes» → la de siempre. Lógica pura para pruebas JVM.
 */
public final class VoiceEngineCommand {
    public enum Target { NEURAL, ANDROID }

    private static final Pattern VERB = Pattern.compile(
            "^(?:quiero que |a partir de ahora )?(?:us[ae]\\w*|pon\\w*|activ\\w*|cambi\\w*|desc\\w{0,3}g[ae]\\w*|instal\\w*|habla\\w*|vuelv\\w*|volver|quit\\w*|desactiv\\w*|apag\\w*|deshabilit\\w*)\\b.*");
    private static final Pattern NEURAL = Pattern.compile(
            "\\bvoz (?:nueva|neuronal|natural|mas natural|humana|mas humana|de mujer|femenina|mas femenina|realista|mejor)\\b");
    private static final Pattern ANDROID = Pattern.compile(
            "\\bvoz (?:de android|de google|de antes|antigua|vieja|de siempre|normal|del sistema|robotica)\\b");
    private static final Pattern REMOVE = Pattern.compile("^(?:quit\\w*|desactiv\\w*)\\b");

    private VoiceEngineCommand() { }

    public static Target parse(String input) {
        String text = normalize(input);
        if (text.isEmpty() || text.split(" ").length > 14 || !VERB.matcher(text).matches()) return null;
        boolean neural = NEURAL.matcher(text).find(), android = ANDROID.matcher(text).find();
        // «quita la voz nueva» = volver a la de Android.
        if (neural && (REMOVE.matcher(text).find() || text.startsWith("apaga ") || text.startsWith("deshabilita "))) return Target.ANDROID;
        if (neural == android) return null;
        return neural ? Target.NEURAL : Target.ANDROID;
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:salve |oye salve )", "").replaceFirst("^(?:puedes |podrias )", "")
                .replaceAll("\\b(?:tu|una|la|el|mi|esa|esta)\\b ?", "").replaceAll("\\s+", " ")
                .replaceFirst(" (?:por favor|porfa)$", "").trim();
    }
}
