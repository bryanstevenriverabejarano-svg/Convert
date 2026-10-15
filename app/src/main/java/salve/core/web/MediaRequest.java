package salve.core.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * «Pon música de Queen», «ponme Bohemian Rhapsody en YouTube», «busca tutoriales de Java en
 * YouTube». Solo se reconoce cuando hay una señal clara de vídeo o música, para no confundirlo
 * con «pon una alarma» o «pon mi huella».
 */
public final class MediaRequest {
    public final String query;
    /** true: reproducir el primer resultado; false: mostrar los resultados. */
    public final boolean autoplay;

    private MediaRequest(String query, boolean autoplay) { this.query = query; this.autoplay = autoplay; }

    private static final Pattern YOUTUBE_SEARCH = Pattern.compile(
            "^(?:busca(?:me)?|encuentra|mira) (?:en youtube )?(.+?)(?: en youtube)$|^busca en youtube (.+)$");
    private static final Pattern PLAY = Pattern.compile(
            "^(?:pon(?:me|nos)?|reproduce|reproduceme|toca|quiero (?:escuchar|oir|ver)|escuchemos|dale a) "
                    + "(?:(?:un poco de|algo de|la|el|una|un) )?(?:(musica|cancion|canciones|tema|video|videos|playlist|lista)(?: de| del)? )?"
                    + "(.+?)(?: (?:en|por|desde) youtube)?$");

    public static MediaRequest parse(String input) {
        String text = normalize(input);
        if (text.isEmpty()) return null;
        Matcher search = YOUTUBE_SEARCH.matcher(text);
        if (search.matches()) {
            String q = clean(search.group(1) != null ? search.group(1) : search.group(2));
            return q.isEmpty() ? null : new MediaRequest(q, false);
        }
        Matcher play = PLAY.matcher(text);
        if (!play.matches()) return null;
        boolean youtube = text.contains("youtube");
        boolean music = play.group(1) != null || text.startsWith("reproduce") || text.startsWith("quiero escuchar")
                || text.startsWith("escuchemos") || text.startsWith("toca ");
        String q = clean(play.group(2));
        if ((!youtube && !music) || q.isEmpty() || q.matches("(?:la |el |mi |tu |tus |te )?(?:alarma|huella|temporizador|recordatorio|cara|voz|mente|camara)\\b.*") || q.matches("(?:tu|tus|te|mi) .*")) return null;
        if (play.group(1) != null && play.group(1).startsWith("musica") && !q.isEmpty()) q = "música " + q;
        return new MediaRequest(q, true);
    }

    public String searchUrl() { return searchUrl(query); }

    public static String searchUrl(String query) {
        try { return "https://m.youtube.com/results?search_query=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name()); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }

    /** Identificador de vídeo de un enlace /watch?v=… de YouTube, o null. */
    public static String videoId(String href) {
        if (href == null) return null;
        Matcher m = Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})").matcher(href);
        return m.find() ? m.group(1) : null;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("^(?:de|del|a|al) ", "").replaceAll(" (?:por favor|porfa|ya|ahora)$", "").trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:\"«»]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "").replaceFirst("^(?:puedes|podrias) ", "");
    }
}
