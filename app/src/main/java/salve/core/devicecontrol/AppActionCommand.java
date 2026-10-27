package salve.core.devicecontrol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Órdenes directas del usuario para otras apps: ruta en Maps, música, llamadas y abrir apps.
 *
 * <p>Solo se aplica a lo que el usuario dice o escribe; nunca a texto de páginas, mensajes
 * recibidos ni salida del modelo. Navegar y poner música se hacen al momento (se deshacen solos);
 * llamar siempre pasa por una confirmación en pantalla con el nombre y el número.</p>
 */
public final class AppActionCommand {
    public enum Type { NAVIGATE, MUSIC, CALL, OPEN_APP }

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern NAVIGATE = Pattern.compile(
            "^(?:ll[eé]vame|navega|gu[ií]ame|(?:pon|ponme|dame|traza) (?:la |una )?ruta|c[oó]mo (?:llego|voy)|vamos)"
                    + "\\s+(?:a|al|hasta|hacia|para)\\s+(.+)$", FLAGS);
    private static final Pattern NAVIGATE_MAPS = Pattern.compile(
            "^(?:abre|abrir|busca) (?:en )?(?:google )?maps (?:a|al|hacia|hasta|con|para)\\s+(.+)$", FLAGS);
    private static final Pattern MUSIC_CUE = Pattern.compile(
            "^(?:pon|ponme|reproduce|toca|quiero escuchar|escuchar)\\s+(?:(?:la |algo de )?m[uú]sica de|canciones de|la canci[oó]n|"
                    + "algo de|el (?:disco|[aá]lbum))\\s+(.+?)(?:\\s+en spotify)?$", FLAGS);
    private static final Pattern MUSIC_SPOTIFY = Pattern.compile(
            "^(?:pon|ponme|reproduce|toca)\\s+(.+?)\\s+en spotify$", FLAGS);
    private static final Pattern MUSIC_PLAIN = Pattern.compile(
            "^(?:reproduce|quiero escuchar)\\s+(.+)$", FLAGS);
    private static final Pattern CALL = Pattern.compile(
            "^(?:ll[aá]ma(?:le)?|llamar|telefonea)\\s+a\\s+(.+?)(?:\\s+(?:por tel[eé]fono|con (?:el )?altavoz|en altavoz|ahora))?$", FLAGS);
    /** «abre la cámara», «abre WhatsApp», «lanza Spotify». Solo un nombre corto, sin encadenar acciones. */
    private static final Pattern OPEN_APP = Pattern.compile(
            "^(?:abre|abrir|[aá]breme|lanza|inicia|arranca)\\s+(?:(?:la )?(?:app|aplicaci[oó]n) (?:de |del )?)?(.+)$", FLAGS);
    private static final Pattern NUMBER = Pattern.compile("\\+?[0-9][0-9 ()-]{2,19}");

    public final Type type;
    public final String target;

    private AppActionCommand(Type type, String target) { this.type = type; this.target = target; }

    public static AppActionCommand parse(String input) {
        if (input == null || input.length() > 300 || input.indexOf('\n') >= 0) return null;
        String text = input.trim().replaceFirst("(?iu)^salve[,:]?\\s+", "")
                .replaceFirst("^[¿¡]+", "").replaceFirst("[.!?¿¡ ]+$", "").trim();
        if (text.isEmpty()) return null;
        AppActionCommand found = match(Type.NAVIGATE, NAVIGATE, text);
        if (found == null) found = match(Type.NAVIGATE, NAVIGATE_MAPS, text);
        if (found == null) found = match(Type.MUSIC, MUSIC_CUE, text);
        if (found == null) found = match(Type.MUSIC, MUSIC_SPOTIFY, text);
        if (found == null) found = match(Type.MUSIC, MUSIC_PLAIN, text);
        if (found == null) found = match(Type.CALL, CALL, text);
        if (found == null) found = match(Type.OPEN_APP, OPEN_APP, text);
        return found;
    }

    /** Número marcable tal cual (sin letras); los nombres se buscan en los contactos. */
    public static boolean isPhoneNumber(String target) {
        return target != null && NUMBER.matcher(target.trim()).matches() && target.replaceAll("[^0-9]", "").length() >= 3;
    }

    /** Texto para {@code DISPLAY_NAME LIKE ? ESCAPE '\'}: los comodines del usuario son literales. */
    public static String likeArgument(String name) {
        String escaped = name.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static AppActionCommand match(Type type, Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.matches()) return null;
        String target = m.group(1).trim().replaceFirst("(?iu)^(?:el|la|los|las)\\s+(?=\\S)", type == Type.MUSIC ? "" : "$0");
        if (target.isEmpty() || target.length() > (type == Type.CALL ? 60 : 160)) return null;
        if (type == Type.OPEN_APP && (target.length() > 40 || target.split("\\s+").length > 4
                || target.matches("(?iu).*\\b(?:y|luego|despu[eé]s|que|para)\\b.*"))) return null;
        return new AppActionCommand(type, target);
    }
}
