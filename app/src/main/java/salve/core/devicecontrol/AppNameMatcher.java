package salve.core.devicecontrol;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Elige, entre las apps instaladas, la que el usuario nombró de viva voz («abre la cámara»,
 * «abre WhatsApp», «abre ajustes»). Es puro: recibe etiquetas y paquetes, no toca Android.
 *
 * <p>Solo devuelve una app cuando la coincidencia es clara; si dos apps empatan o nada se
 * parece lo suficiente, devuelve -1 y Salve no abre nada por su cuenta.</p>
 */
public final class AppNameMatcher {
    /** Nombres hablados en español → palabras que aparecen en etiquetas o paquetes habituales. */
    private static final Map<String, String[]> ALIASES = new HashMap<>();
    static {
        alias("camara", "camera", "camara");
        alias("ajustes", "settings", "ajustes", "configuracion");
        alias("configuracion", "settings", "ajustes", "configuracion");
        alias("calculadora", "calculator", "calculadora");
        alias("galeria", "gallery", "galeria", "photos", "fotos");
        alias("fotos", "photos", "fotos", "gallery", "galeria");
        alias("reloj", "clock", "reloj", "deskclock");
        alias("alarma", "clock", "reloj", "deskclock");
        alias("calendario", "calendar", "calendario");
        alias("correo", "gmail", "mail", "correo", "email");
        alias("telefono", "dialer", "phone", "telefono");
        alias("marcador", "dialer", "phone", "telefono");
        alias("mensajes", "messaging", "messages", "mensajes", "sms");
        alias("contactos", "contacts", "contactos");
        alias("tienda", "play store", "vending", "tienda");
        alias("play store", "play store", "vending");
        alias("navegador", "chrome", "browser", "navegador");
        alias("notas", "keep", "notes", "notas");
        alias("grabadora", "recorder", "grabadora");
        alias("mapas", "maps", "mapas");
        alias("youtube", "youtube");
        alias("archivos", "files", "archivos", "documentsui");
    }

    private static void alias(String spoken, String... words) { ALIASES.put(spoken, words); }

    private AppNameMatcher() { }

    /** Minúsculas sin tildes ni signos; «App de Cámara» → «app de camara». */
    public static String normalize(String text) {
        if (text == null) return "";
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Quita relleno hablado: «la app de la cámara» → «camara». */
    public static String spokenName(String spoken) {
        String name = normalize(spoken);
        name = name.replaceFirst("^(?:la |el |mi |tu )", "");
        name = name.replaceFirst("^(?:app|aplicacion)(?: de| del)?(?: la| el)? ", "");
        name = name.replaceFirst(" (?:app|aplicacion)$", "");
        return name.trim();
    }

    /**
     * Índice de la mejor app en {@code labels}/{@code packages} (mismo orden), o -1 si no hay
     * una coincidencia clara.
     */
    public static int bestMatch(String spoken, List<String> labels, List<String> packages) {
        String name = spokenName(spoken);
        if (name.isEmpty() || name.length() > 40) return -1;
        String[] wanted = ALIASES.containsKey(name) ? ALIASES.get(name) : new String[]{name};
        int best = -1, bestScore = 0, bestLength = Integer.MAX_VALUE;
        boolean tie = false;
        List<String> pkgs = packages == null ? Collections.<String>emptyList() : packages;
        for (int i = 0; i < labels.size(); i++) {
            String label = normalize(labels.get(i));
            String pkg = i < pkgs.size() ? normalize(pkgs.get(i)) : "";
            int score = 0;
            for (String word : wanted) score = Math.max(score, score(normalize(word), label, pkg));
            if (score == 0) continue;
            if (score > bestScore || (score == bestScore && label.length() < bestLength)) {
                tie = score == bestScore && label.length() == bestLength;
                best = i; bestScore = score; bestLength = label.length();
            } else if (score == bestScore && label.length() == bestLength) {
                tie = true;
            }
        }
        return tie ? -1 : best;
    }

    private static int score(String word, String label, String pkg) {
        if (word.isEmpty()) return 0;
        if (label.equals(word)) return 5;
        if (label.startsWith(word + " ") || label.endsWith(" " + word)) return 4;
        if ((" " + label + " ").contains(" " + word + " ")) return 3;
        if (word.length() >= 4 && label.startsWith(word)) return 2;
        if (word.length() >= 4 && !word.contains(" ") && (" " + pkg + " ").contains(" " + word + " ")) return 1;
        return 0;
    }
}
