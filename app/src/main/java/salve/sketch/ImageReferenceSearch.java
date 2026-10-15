package salve.sketch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Busca imágenes de referencia en Wikimedia Commons (libres, sin clave) para que Salve pueda
 * dibujar algo que no conoce: «dibuja un cochinillo» → busca el término (también en inglés, vía
 * Wikipedia), prefiere dibujos vectoriales (líneas limpias) y, si no hay, fotos.
 *
 * Sólo construye URLs y lee las respuestas JSON; la descarga la hace quien implementa {@link Http}.
 * La consulta lleva únicamente el nombre del objeto, nunca datos personales.
 */
public final class ImageReferenceSearch {
    public interface Http { byte[] get(String url, int maxBytes) throws IOException; }

    public static final class Candidate {
        public final String title, imageUrl, pageUrl;
        public final boolean drawing;
        Candidate(String title, String imageUrl, String pageUrl, boolean drawing) {
            this.title = title; this.imageUrl = imageUrl; this.pageUrl = pageUrl; this.drawing = drawing;
        }
        /** Nombre legible del archivo («Pig icon»). */
        public String label() {
            return title.replaceFirst("^(File|Archivo):", "").replaceFirst("\\.[A-Za-z0-9]{2,4}$", "").replace('_', ' ');
        }
    }

    private static final String COMMONS = "https://commons.wikimedia.org/w/api.php";
    private static final String UNSUITABLE = "(?i).*\\b(map|mapa|flag|bandera|coat of arms|escudo|logo|diagram|diagrama|chart|"
            + "gr[aá]fico|locator|blank|template|plantilla|stamp|sello|text|signature|firma|qr|seal|emblem)\\b.*";

    private final Http http;

    public ImageReferenceSearch(Http http) { this.http = http; }

    /** Hasta {@code max} referencias: primero dibujos vectoriales, luego fotos. */
    public List<Candidate> search(String subject, int max) throws IOException {
        String es = clean(subject);
        if (es.isEmpty()) return new ArrayList<>();
        Set<String> terms = new LinkedHashSet<>();
        String english = null;
        try { english = englishTerm(es); } catch (IOException ignored) { /* seguimos con el término en español */ }
        if (english != null) terms.add(english);
        terms.add(es);
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (String term : terms) addAll(out, seen, commons(term + " filetype:drawing", 8), max);
        if (out.size() < max) for (String term : terms) addAll(out, seen, commons(term + " filetype:bitmap", 6), max);
        return out;
    }

    private void addAll(List<Candidate> out, Set<String> seen, List<Candidate> found, int max) {
        for (Candidate c : found) {
            if (out.size() >= max) return;
            if (seen.add(c.imageUrl)) out.add(c);
        }
    }

    /** Descarga la imagen de una referencia (sólo de upload.wikimedia.org). */
    public byte[] download(Candidate c, int maxBytes) throws IOException {
        if (c == null || !c.imageUrl.startsWith("https://upload.wikimedia.org/")) throw new IOException("Origen no permitido");
        return http.get(c.imageUrl, maxBytes);
    }

    /** Nombre del artículo de Wikipedia en inglés para el término español (Commons está sobre todo en inglés). */
    String englishTerm(String es) throws IOException {
        String json = text(http.get(esWikipediaUrl(es), 200_000));
        return parseEnglishTitle(json);
    }

    List<Candidate> commons(String query, int limit) throws IOException {
        return parseCommons(text(http.get(commonsUrl(query, limit), 400_000)));
    }

    static String esWikipediaUrl(String es) {
        return "https://es.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&generator=search&gsrlimit=1"
                + "&prop=langlinks&lllang=en&gsrsearch=" + encode(es);
    }

    static String commonsUrl(String query, int limit) {
        return COMMONS + "?action=query&format=json&formatversion=2&generator=search&gsrnamespace=6&gsrlimit="
                + Math.max(1, Math.min(20, limit)) + "&prop=imageinfo&iiprop=url%7Cmime&iiurlwidth=480&gsrsearch=" + encode(query);
    }

    static String parseEnglishTitle(String json) {
        try {
            JsonObject query = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("query");
            if (query == null) return null;
            for (JsonElement p : query.getAsJsonArray("pages")) {
                JsonArray links = p.getAsJsonObject().getAsJsonArray("langlinks");
                if (links == null) continue;
                for (JsonElement l : links) {
                    JsonObject link = l.getAsJsonObject();
                    if ("en".equals(link.get("lang").getAsString()) && link.has("title"))
                        return clean(link.get("title").getAsString().replaceAll("\\s*\\(.*\\)$", ""));
                }
            }
        } catch (RuntimeException malformed) { /* respuesta inesperada: sin traducción */ }
        return null;
    }

    static List<Candidate> parseCommons(String json) {
        List<Candidate> out = new ArrayList<>();
        try {
            JsonObject query = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("query");
            if (query == null) return out;
            List<JsonObject> pages = new ArrayList<>();
            for (JsonElement p : query.getAsJsonArray("pages")) pages.add(p.getAsJsonObject());
            pages.sort((a, b) -> Integer.compare(a.has("index") ? a.get("index").getAsInt() : 99,
                    b.has("index") ? b.get("index").getAsInt() : 99));
            for (JsonObject page : pages) {
                String title = page.has("title") ? page.get("title").getAsString() : "";
                JsonArray info = page.getAsJsonArray("imageinfo");
                if (info == null || info.size() == 0 || title.matches(UNSUITABLE)) continue;
                JsonObject i = info.get(0).getAsJsonObject();
                String mime = i.has("mime") ? i.get("mime").getAsString() : "";
                String thumb = i.has("thumburl") ? i.get("thumburl").getAsString() : null;
                if (thumb == null || !thumb.startsWith("https://upload.wikimedia.org/")) continue;
                if (!(mime.startsWith("image/svg") || mime.equals("image/png") || mime.equals("image/jpeg"))) continue;
                String pageUrl = i.has("descriptionurl") ? i.get("descriptionurl").getAsString() : "";
                out.add(new Candidate(title, thumb, pageUrl, mime.startsWith("image/svg")));
            }
        } catch (RuntimeException malformed) { /* respuesta inesperada: sin candidatos */ }
        return out;
    }

    /**
     * Puntuación de un trazado como referencia: líneas claras y en cantidad razonable; los dibujos
     * vectoriales ganan a las fotos. 0 = no sirve.
     */
    public static float score(Candidate candidate, ImageTracer.Result traced) {
        if (traced == null || !traced.isUsable()) return 0;
        int strokes = traced.strokes.size();
        if (strokes > 65) return 0; // demasiado ruido o texto
        float s = Math.min(strokes, 25) / 25f + Math.min(1f, (traced.geometry.lines + traced.geometry.curves) / 20f);
        if (strokes < 2) s *= .5f;
        if (candidate.drawing) s *= 1.6f;
        return s;
    }

    static String clean(String subject) {
        if (subject == null) return "";
        String s = subject.replaceAll("[\\r\\n\"«»]+", " ").trim()
                .replaceFirst("(?i)^(un|una|unos|unas|el|la|los|las)\\s+", "").trim();
        s = s.length() > 60 ? s.substring(0, 60).trim() : s;
        return s.toLowerCase(Locale.ROOT);
    }

    private static String encode(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String text(byte[] body) { return body == null ? "" : new String(body, StandardCharsets.UTF_8); }
}
