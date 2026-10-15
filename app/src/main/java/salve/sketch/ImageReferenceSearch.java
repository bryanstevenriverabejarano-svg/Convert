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
        return search(subject, null, max, null);
    }

    /**
     * Busca primero siluetas y dibujos de línea (su forma se reconoce con pocas líneas), luego dibujos
     * vectoriales cualesquiera y por último fotos.
     * @param englishHint nombre en inglés que ella pensó (si Wikipedia no da traducción)
     * @param exclude URLs ya usadas para este objeto: al mejorar un dibujo no repite la misma referencia
     */
    public List<Candidate> search(String subject, String englishHint, int max, java.util.Collection<String> exclude)
            throws IOException {
        String es = clean(subject);
        if (es.isEmpty()) return new ArrayList<>();
        Set<String> terms = new LinkedHashSet<>();
        log.clear();
        // Primero el nombre en inglés que ella pensó (corrige erratas: «vesrido» → dress); luego Wikipedia.
        if (englishHint != null && !clean(englishHint).isEmpty()) terms.add(clean(englishHint));
        String english = null;
        try { english = englishTerm(es); } catch (IOException error) { log.add("Wikipedia: " + error.getMessage()); }
        if (english != null) terms.add(english);
        terms.add(es);
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        if (exclude != null) seen.addAll(exclude);
        String main = terms.iterator().next();
        for (String shape : new String[]{" silhouette", " line art"}) {
            if (out.size() >= max) break;
            try { addAll(out, seen, commons(main + shape + " filetype:drawing", 6), max); }
            catch (IOException ignored) { /* seguimos con la búsqueda general */ }
        }
        IOException last = null;
        for (String term : terms) if (out.size() < max) {
            try { addAll(out, seen, commons(term + " filetype:drawing", 8), max); } catch (IOException e) { last = e; }
        }
        for (String term : terms) if (out.size() < max) {
            try { addAll(out, seen, commons(term + " filetype:bitmap", 6), max); } catch (IOException e) { last = e; }
        }
        // Otras vías si Commons no responde bien: búsqueda simple de Commons, la foto principal del
        // artículo de Wikipedia y Openverse (imágenes libres de muchas fuentes).
        if (out.size() < 2) try { addAll(out, seen, commonsList(main, 6), max); } catch (IOException e) { last = e; }
        if (out.size() < 2) for (String[] wiki : new String[][]{{"en", main}, {"es", es}})
            try { addAll(out, seen, wikipediaImage(wiki[0], wiki[1]), max); } catch (IOException e) { last = e; }
        if (out.size() < 2) try { addAll(out, seen, openverse(main, 8), max); } catch (IOException e) { last = e; }
        if (out.isEmpty() && last != null) throw last; // sin red o todo caído: que se sepa
        return out;
    }

    private void addAll(List<Candidate> out, Set<String> seen, List<Candidate> found, int max) {
        for (Candidate c : found) {
            if (out.size() >= max) return;
            if (seen.add(c.imageUrl)) out.add(c);
        }
    }

    /** Descarga la imagen de una referencia (sólo de Wikimedia o de las miniaturas de Openverse). */
    public byte[] download(Candidate c, int maxBytes) throws IOException {
        if (c == null || !allowedImage(c.imageUrl)) throw new IOException("Origen no permitido");
        return http.get(c.imageUrl, maxBytes);
    }

    static boolean allowedImage(String url) {
        return url != null && (url.startsWith("https://upload.wikimedia.org/") || url.startsWith("https://api.openverse.org/v1/images/"));
    }

    /** Nombre del artículo de Wikipedia en inglés para el término español (Commons está sobre todo en inglés). */
    String englishTerm(String es) throws IOException {
        String json = text(http.get(esWikipediaUrl(es), 200_000));
        return parseEnglishTitle(json);
    }

    List<Candidate> commons(String query, int limit) throws IOException {
        try {
            String body = text(http.get(commonsUrl(query, limit), 400_000));
            List<Candidate> found = parseCommons(body);
            log.add("«" + query.replace(" filetype:", " ·") + "»: " + found.size() + (found.isEmpty() ? snippet(body) : ""));
            return found;
        } catch (IOException error) {
            log.add("«" + query.replace(" filetype:", " ·") + "»: error " + error.getMessage());
            throw error;
        }
    }

    private final List<String> log = new ArrayList<>();

    /** Principio de una respuesta vacía: dice si fue un error, un límite o de verdad nada. */
    static String snippet(String body) {
        String t = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) return " (respuesta vacía)";
        if (t.startsWith("{\"batchcomplete\"") && t.length() < 40) return "";
        return " (" + (t.length() > 90 ? t.substring(0, 90) + "…" : t) + ")";
    }

    /** Commons en dos pasos (lista de archivos y luego sus miniaturas), por si la búsqueda combinada falla. */
    List<Candidate> commonsList(String term, int limit) throws IOException {
        String body = text(http.get(COMMONS + "?action=query&format=json&formatversion=2&list=search&srnamespace=6&srlimit="
                + limit + "&srsearch=" + encode(term), 300_000));
        List<String> titles = new ArrayList<>();
        try {
            JsonObject q = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("query");
            if (q != null) for (JsonElement e : q.getAsJsonArray("search")) titles.add(e.getAsJsonObject().get("title").getAsString());
        } catch (RuntimeException malformed) { /* sin resultados */ }
        if (titles.isEmpty()) { log.add("Commons lista «" + term + "»: 0" + snippet(body)); return new ArrayList<>(); }
        List<Candidate> found = parseCommons(text(http.get(COMMONS + "?action=query&format=json&formatversion=2"
                + "&prop=imageinfo&iiprop=url%7Cmime&iiurlwidth=480&titles=" + encode(String.join("|", titles)), 300_000)));
        log.add("Commons lista «" + term + "»: " + found.size());
        return found;
    }

    /** La imagen principal del artículo de Wikipedia («Gorilla» → su foto). */
    List<Candidate> wikipediaImage(String lang, String term) throws IOException {
        String body = text(http.get("https://" + lang + ".wikipedia.org/w/api.php?action=query&format=json&formatversion=2"
                + "&prop=pageimages&piprop=thumbnail&pithumbsize=480&redirects=1&titles=" + encode(term), 200_000));
        List<Candidate> out = new ArrayList<>();
        try {
            JsonObject q = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("query");
            if (q != null) for (JsonElement p : q.getAsJsonArray("pages")) {
                JsonObject page = p.getAsJsonObject();
                if (!page.has("thumbnail")) continue;
                String url = page.getAsJsonObject("thumbnail").get("source").getAsString();
                if (!url.startsWith("https://upload.wikimedia.org/")) continue;
                out.add(new Candidate("Wikipedia " + lang + ": " + page.get("title").getAsString(), url,
                        "https://" + lang + ".wikipedia.org/wiki/" + page.get("title").getAsString().replace(' ', '_'),
                        url.toLowerCase(Locale.ROOT).contains(".svg")));
            }
        } catch (RuntimeException malformed) { /* sin imagen */ }
        log.add("Wikipedia " + lang + " «" + term + "»: " + out.size() + (out.isEmpty() ? snippet(body) : ""));
        return out;
    }

    /** Openverse: buscador de imágenes con licencia libre (sin clave). Sólo usa sus miniaturas. */
    List<Candidate> openverse(String term, int limit) throws IOException {
        String body = text(http.get("https://api.openverse.org/v1/images/?page_size=" + limit + "&q=" + encode(term), 300_000));
        List<Candidate> out = new ArrayList<>();
        try {
            JsonArray results = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("results");
            if (results != null) for (JsonElement e : results) {
                JsonObject r = e.getAsJsonObject();
                String thumb = r.has("thumbnail") && !r.get("thumbnail").isJsonNull() ? r.get("thumbnail").getAsString() : null;
                if (!allowedImage(thumb)) continue;
                String title = r.has("title") && !r.get("title").isJsonNull() ? r.get("title").getAsString() : term;
                if (title.matches(UNSUITABLE)) continue;
                String page = r.has("foreign_landing_url") && !r.get("foreign_landing_url").isJsonNull()
                        ? r.get("foreign_landing_url").getAsString() : "";
                out.add(new Candidate("Openverse: " + title, thumb, page, false));
            }
        } catch (RuntimeException malformed) { /* sin resultados */ }
        log.add("Openverse «" + term + "»: " + out.size() + (out.isEmpty() ? snippet(body) : ""));
        return out;
    }

    /** Qué buscó y cuántas imágenes dio cada búsqueda (para contárselo a Bryan si falla). */
    public String diagnostic() { return String.join("; ", log); }

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
        if (strokes > 220) return 0; // casi todo ruido o texto
        float s = Math.min(strokes, 25) / 25f + Math.min(1f, (traced.geometry.lines + traced.geometry.curves) / 20f);
        if (strokes > 65) s *= .6f; // se puede quedar con los contornos largos, pero vale menos
        if (strokes < 2) s *= .5f;
        if (candidate.drawing) s *= 1.6f;
        String title = candidate.title.toLowerCase(Locale.ROOT);
        if (title.matches(".*\\b(silhouette|silueta|outline|line ?art|lineart|clipart|clip art|icon|sketch|contorno)\\b.*")) s *= 1.35f;
        if (title.matches(".*\\b(skull|skeleton|anatomy|range|distribution|track|footprint|cr[aá]neo|esqueleto|huella)\\b.*")) s *= .3f;
        return s;
    }

    /**
     * Se queda con los contornos más largos (la forma del objeto) y descarta el detalle menudo: una
     * foto de un gorila da cientos de bordes de pelo; su silueta está en los más largos.
     */
    public static ImageTracer.Result focus(ImageTracer.Result traced, int maxStrokes) {
        if (traced == null || traced.strokes.size() <= maxStrokes) return traced;
        List<float[]> sorted = new ArrayList<>(traced.strokes);
        sorted.sort((a, b) -> Float.compare(length(b), length(a)));
        java.util.Set<float[]> keep = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        keep.addAll(sorted.subList(0, maxStrokes));
        List<float[]> kept = new ArrayList<>();
        for (float[] s : traced.strokes) if (keep.contains(s)) kept.add(s); // conserva el orden de trazado
        ImageTracer.Geometry g = traced.geometry;
        float share = maxStrokes / (float) traced.strokes.size();
        ImageTracer.Geometry focused = new ImageTracer.Geometry(Math.round(g.lines * share), Math.round(g.horizontal * share),
                Math.round(g.vertical * share), Math.round(g.diagonal * share), Math.round(g.curves * share),
                Math.min(g.closed, maxStrokes), kept.size(), g.symmetry, g.aspect);
        return new ImageTracer.Result(kept, traced.aspect, focused);
    }

    private static float length(float[] s) {
        float d = 0;
        for (int i = 2; i + 1 < s.length; i += 2) d += (float) Math.hypot(s[i] - s[i - 2], s[i + 1] - s[i - 1]);
        return d;
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
