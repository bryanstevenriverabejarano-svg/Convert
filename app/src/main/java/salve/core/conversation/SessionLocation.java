package salve.core.conversation;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A user declaration, never geolocation or a long-term residence/profile fact. */
final class SessionLocation {
    static final long TTL_MILLIS = 2 * 60 * 60 * 1000L;
    private static final Pattern DECLARATION = Pattern.compile(
            "^(?:(?:no,|correcci[oó]n,?|en realidad,?)\\s+)?(?:ahora\\s+)?"
                    + "(?:estoy en|mi ubicaci[oó]n (?:actual )?es)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NON_LOCATION = Pattern.compile(
            "^(?:(?:el|la|un|una)\\s+)?(?:desacuerdo|acuerdo|contra|favor|proceso|estado|condiciones|condicion|contacto|"
                    + "relacion|busca|busqueda|espera|duda|dudas|problemas|peligro|deuda|tratamiento|"
                    + "terapia|recuperacion|shock|forma|paz|guerra|crisis|depresion|silencio|"
                    + "lo cierto|lo correcto|lo mismo|ello|eso|error|situacion|relacion)\\b.*");
    private static final Pattern RELEVANT = Pattern.compile(
            ".*\\b(?:donde estoy|donde me encuentro|mi ubicacion|mi localizacion|ubicacion actual|"
                    + "hora|fecha|que dia|aqui|alli)\\b.*");

    private final String place;
    private final long declaredAt;

    private SessionLocation(String place, long declaredAt) {
        this.place = place;
        this.declaredAt = declaredAt;
    }

    static SessionLocation fromUser(String input, long nowMillis) {
        if (input == null) return null;
        // Capture a leading declarative sentence, including "Estoy en Quito. ¿Qué hora es?".
        // Questions, quotes, conditions and indirect reports cannot create a location.
        String firstSentence = input.trim().split("[.!\\n]|,\\s*[¿?]", 2)[0].trim();
        if (firstSentence.indexOf('?') >= 0 || firstSentence.indexOf('¿') >= 0) return null;
        Matcher match = DECLARATION.matcher(firstSentence);
        if (!match.matches()) return null;
        String place = match.group(1).trim();
        if (place.isEmpty() || place.length() > 120 || !place.matches("[\\p{L}\\p{N} ,'-]+")) return null;
        String normalized = normalize(place);
        if (NON_LOCATION.matcher(normalized).matches()
                || normalized.matches(".*\\b(?:porque|pero|aunque|si|cuando|que|donde|cuanto|como|"
                    + "y (?:ahora|quiero|necesito|son|tengo|estoy))\\b.*")) return null;
        return new SessionLocation(place, nowMillis);
    }

    static boolean relevantTo(String query) {
        return RELEVANT.matcher(normalize(query)).matches() || fromUser(query, 0L) != null;
    }

    boolean isCurrent(long nowMillis) {
        // A wall-clock correction backwards invalidates the observation rather than extending it.
        return nowMillis >= declaredAt && nowMillis - declaredAt < TTL_MILLIS;
    }

    boolean isDeniedBy(String input) {
        String declaration = normalize(input).replaceAll("[.!]+$", "").trim();
        String oldPlace = normalize(place);
        return declaration.equals("no estoy en " + oldPlace)
                || declaration.equals("ya no estoy en " + oldPlace)
                || declaration.equals("mi ubicacion ya no es " + oldPlace);
    }

    String promptContext() {
        return "UBICACIÓN TEMPORAL DECLARADA: «" + place + "». "
                + "Fuente: declaración directa del usuario en esta sesión; recibida "
                + Instant.ofEpochMilli(declaredAt) + "; válida como contexto durante un máximo de 2 horas. "
                + "Es un dato proporcionado por el usuario, no una lectura GPS ni una residencia permanente. "
                + "No determina por sí solo la zona horaria ni confirma que el usuario siga allí.";
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
}
