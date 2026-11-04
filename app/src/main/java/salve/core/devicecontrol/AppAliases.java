package salve.core.devicecontrol;

import java.util.regex.Pattern;

/**
 * El dictado casi nunca escribe bien «Spotify» ni «WhatsApp» («spoty fay», «espotifai», «whasa»,
 * «guasap»…). Se devuelven a su nombre real antes de reconocer la orden. Lógica pura para pruebas JVM.
 */
public final class AppAliases {
    private static final Pattern SPOTIFY = Pattern.compile(
            "(?iu)\\b(?:e?s\\s?pot+[iy]\\s?f[aiy]+[iy]?e?|e?s\\s?pot+[iy]\\s?fy|e?spot+i\\s?fai|e?spot+[iy])\\b");
    /** Variantes de WhatsApp para los patrones de órdenes (sin grupos de captura). */
    public static final String WHATSAPP = "(?:whats?\\s?app?|whats?\\s?ap|wh?ats?app|whasa+p?|whatsa|wassa+p?|wasa+p?|"
            + "guasa+p?|uasa+p|watsa+p|wasap|wassup|el whats|el wasa)";

    private AppAliases() { }

    public static String normalize(String text) {
        if (text == null) return null;
        java.util.regex.Matcher m = SPOTIFY.matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String found = m.group();
            m.appendReplacement(out, found.equalsIgnoreCase("spotify") ? found : "Spotify");
        }
        return m.appendTail(out).toString();
    }
}
