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
        /** De dónde viene («Wikimedia Commons», «Google Imágenes»…), para contarlo con honestidad. */
        public final String source;
        /** Imagen ya vista en la ventana (miniatura de Google): no se vuelve a descargar. */
        final byte[] bytes;
        /** Posición de la miniatura en la ventana en directo para señalarla; -1 si no está en pantalla. */
        public final int screenIndex;
        Candidate(String title, String imageUrl, String pageUrl, boolean drawing) {
            this(title, imageUrl, pageUrl, drawing, sourceOf(title, imageUrl), null, -1);
        }
        private Candidate(String title, String imageUrl, String pageUrl, boolean drawing, String source, byte[] bytes, int screenIndex) {
            this.title = title; this.imageUrl = imageUrl; this.pageUrl = pageUrl; this.drawing = drawing;
            this.source = source; this.bytes = bytes; this.screenIndex = screenIndex;
        }
        /**
         * Miniatura que Salve está viendo en Google Imágenes dentro de la ventana en directo. Su
         * identificador es una huella de la imagen, no la imagen, para no repetirla al mejorar.
         * @param alt texto alternativo que Google da a la miniatura
         */
        public static Candidate fromScreen(String alt, byte[] bytes, String pageUrl, int screenIndex) {
            if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("Miniatura vacía");
            String title = alt == null || alt.trim().isEmpty() ? "miniatura " + (screenIndex + 1) : alt.trim();
            boolean drawing = title.toLowerCase(Locale.ROOT).matches(".*" + DRAWING_WORDS + ".*");
            return new Candidate(title, "google-imagenes:" + fingerprint(bytes), pageUrl == null ? "" : pageUrl, drawing,
                    "Google Imágenes", bytes.clone(), screenIndex);
        }
        /** La misma referencia, pero ya sabiendo (por lo que vio) si es un dibujo. */
        public Candidate seenAs(boolean isDrawing) {
            return new Candidate(title, imageUrl, pageUrl, isDrawing, source, bytes, screenIndex);
        }
        public boolean fromScreen() { return bytes != null; }
        /** Nombre legible del archivo («Pig icon»). */
        public String label() {
            return title.replaceFirst("^(File|Archivo|Openverse|Wikipedia \\w+):\\s*", "")
                    .replaceFirst("\\.[A-Za-z0-9]{2,4}$", "").replace('_', ' ');
        }
    }

    /** Palabras que dicen que una imagen es un dibujo y no una foto. */
    static final String DRAWING_WORDS = "\\b(dibujos?|drawings?|silhouette|silueta|outline|line ?art|lineart|clip ?art|icon[oa]?s?"
            + "|sketch|boceto|contorno|vector|vectorial|ilustraci[oó]n|illustration|para colorear|coloring|colouring|cartoon"
            + "|caricatura)\\b";

    static String sourceOf(String title, String imageUrl) {
        if (title != null && title.startsWith("Openverse")) return "Openverse";
        if (title != null && title.startsWith("Wikipedia")) return "Wikipedia";
        return imageUrl != null && imageUrl.startsWith("google-imagenes:") ? "Google Imágenes" : "Wikimedia Commons";
    }

    static String fingerprint(byte[] bytes) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 12; i++) hex.append(String.format(Locale.ROOT, "%02x", d[i]));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static final String COMMONS = "https://commons.wikimedia.org/w/api.php";
    private static final String UNSUITABLE = "(?i).*\\b(map|mapa|flag|bandera|coat of arms|escudo|logo|diagram|diagrama|chart|"
            + "gr[aá]fico|locator|blank|template|plantilla|stamp|sello|text|signature|firma|qr|seal|emblem)\\b.*";

    private final Http http;

    public ImageReferenceSearch(Http http) { this.http = http; }

    /** Por qué fuente empezar («commons», «wikipedia», «openverse»): lo decide su laboratorio de habilidades. */
    private volatile String first = "commons";

    public ImageReferenceSearch startWith(String source) {
        first = "wikipedia".equals(source) || "openverse".equals(source) ? source : "commons";
        return this;
    }

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
        String es = withoutRequestWords(clean(subject));
        if (es.isEmpty()) return new ArrayList<>();
        log.clear();
        // «improved gorilla»: la petición («mejóralo») no es parte del objeto y estropea la búsqueda.
        String hint = englishHint == null ? "" : withoutRequestWords(clean(englishHint));
        String main = hint.isEmpty() ? es : hint;
        Set<String> seen = new java.util.HashSet<>();
        if (exclude != null) seen.addAll(exclude);
        long deadline = System.currentTimeMillis() + budgetMs;
        // Todas las fuentes a la vez: una que tarda o no responde ya no bloquea a las demás.
        List<Query> wave = new ArrayList<>();
        wave.add(new Query("es→en", () -> {
            String en = englishTerm(es);
            return en == null ? java.util.Collections.<Candidate>emptyList() : commons(en + " filetype:drawing", 8);
        }));
        wave.add(new Query("silueta", () -> commons(main + " silhouette filetype:drawing", 6)));
        wave.add(new Query("line art", () -> commons(main + " line art filetype:drawing", 6)));
        wave.add(new Query("dibujos", () -> commons(main + " filetype:drawing", 8)));
        wave.add(new Query("fotos", () -> commons(main + " filetype:bitmap", 6)));
        wave.add(new Query("commons simple", () -> commonsList(main, 6)));
        wave.add(new Query("wikipedia en", () -> wikipediaImage("en", main)));
        wave.add(new Query("wikipedia es", () -> wikipediaImage("es", es)));
        wave.add(new Query("openverse", () -> openverse(main, 8)));
        if (!hint.isEmpty() && !hint.equals(es)) wave.add(new Query("dibujos es", () -> commons(es + " filetype:drawing", 6)));
        // Prioridad al juntar resultados: la fuente que mejor le ha funcionado (su laboratorio) va primero;
        // después siluetas y dibujos de línea, dibujos, fotos y el resto.
        List<Query> order = new ArrayList<>(wave);
        if ("wikipedia".equals(first) || "openverse".equals(first))
            order.sort((x, y) -> Boolean.compare(!x.name.startsWith(first), !y.name.startsWith(first)));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(
                Math.min(PARALLEL, wave.size()), r -> { Thread t = new Thread(r, "salve-referencias"); t.setDaemon(true); return t; });
        IOException last = null;
        List<Candidate> out = new ArrayList<>();
        try {
            for (Query q : wave) q.future = pool.submit(q.call);
            for (Query q : order) {
                long left = deadline - System.currentTimeMillis();
                try {
                    List<Candidate> found = left <= 0 && !q.future.isDone() ? null
                            : q.future.get(Math.max(1, left), java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (found == null) { log.add(q.name + ": sin tiempo"); continue; }
                    addAll(out, seen, found, max);
                } catch (java.util.concurrent.TimeoutException slow) {
                    log.add(q.name + ": no respondió en " + (budgetMs / 1000) + " s");
                    last = new IOException("las fuentes de imágenes no respondieron a tiempo");
                } catch (java.util.concurrent.ExecutionException failed) {
                    Throwable cause = failed.getCause();
                    log.add(q.name + ": " + (cause == null ? "error" : cause.getClass().getSimpleName() + " " + cause.getMessage()));
                    last = cause instanceof IOException ? (IOException) cause : new IOException(String.valueOf(cause));
                } catch (InterruptedException stop) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally { pool.shutdownNow(); }
        if (out.isEmpty() && last != null) throw last; // sin red o todo caído: que se sepa
        return out;
    }

    /** Peticiones simultáneas y presupuesto total de la búsqueda. */
    static final int PARALLEL = 5;
    private volatile long budgetMs = 28_000;

    /** Para pruebas o redes muy lentas. */
    public ImageReferenceSearch budget(long ms) { budgetMs = Math.max(1000, ms); return this; }

    private interface Source { List<Candidate> get() throws IOException; }

    private static final class Query {
        final String name;
        final java.util.concurrent.Callable<List<Candidate>> call;
        java.util.concurrent.Future<List<Candidate>> future;
        Query(String name, Source source) { this.name = name; this.call = source::get; }
    }

    private void addAll(List<Candidate> out, Set<String> seen, List<Candidate> found, int max) {
        for (Candidate c : found) {
            if (out.size() >= max) return;
            if (seen.add(c.imageUrl)) out.add(c);
        }
    }

    /** Descarga la imagen de una referencia, de cualquier servidor público por HTTPS. */
    public byte[] download(Candidate c, int maxBytes) throws IOException {
        if (c != null && c.bytes != null) {
            if (c.bytes.length > maxBytes) throw new IOException("Demasiado grande");
            return c.bytes.clone(); // ya la vio en la ventana: no pide nada a nadie
        }
        if (c == null || !allowedImage(c.imageUrl)) throw new IOException("Origen no permitido");
        return http.get(c.imageUrl, maxBytes);
    }

    /** Miniatura de Commons por nombre de archivo: redirige a upload.wikimedia.org. */
    private static final String FILE_PATH = "https://commons.wikimedia.org/wiki/Special:FilePath/";

    /**
     * ¿Se puede pedir esta imagen? Cualquier servidor público por HTTPS: no importa de dónde venga,
     * sino si cumple con lo pedido (eso lo deciden el calco y su mirada). Solo se cierra la puerta a
     * direcciones de la red de casa o del propio móvil, que una respuesta ajena no debe poder tocar.
     */
    static boolean allowedImage(String url) {
        if (url == null) return false;
        if (url.startsWith(FILE_PATH)) return !url.substring(FILE_PATH.length()).contains("/");
        return publicHttps(url);
    }

    /** HTTPS a un nombre de dominio público: ni IPs escritas a mano, ni localhost, ni nombres de red local. */
    public static boolean publicHttps(String url) {
        if (url == null || url.length() > 2048) return false;
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) return false;
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("[") || host.matches("[0-9.]+") || !host.contains(".")) return false; // IPs y nombres sueltos
            return !host.matches(".*\\.(local|localhost|lan|home|internal|intranet|corp|arpa|test|invalid)$");
        } catch (java.net.URISyntaxException bad) { return false; }
    }

    /** ¿Esta dirección IP es de la red de casa o del propio móvil? (para filtrar lo que resuelve el DNS) */
    public static boolean privateAddress(java.net.InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isAnyLocalAddress() || address.isMulticastAddress()
                || (address instanceof java.net.Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc) // fc00::/7
                || (address instanceof java.net.Inet4Address && (address.getAddress()[0] & 0xff) == 100
                        && (address.getAddress()[1] & 0xc0) == 64); // 100.64.0.0/10
    }

    /**
     * La miniatura que da la API, venga del servidor que venga, si se puede pedir por HTTPS
     * («//…» y «http://…» se pasan a https). Si no (vacía, red local, rara) → null.
     */
    static String wikimediaThumb(String thumb) {
        if (thumb == null) return null;
        String t = thumb.trim();
        if (t.startsWith("//")) t = "https:" + t;
        else if (t.startsWith("http://")) t = "https://" + t.substring(7);
        return publicHttps(t) && !t.contains("..") ? t : null;
    }

    /** «File:Gorilla silhouette.svg» → su miniatura de 480 px, sin depender de que la API traiga {@code thumburl}. */
    static String filePathThumb(String title) {
        String name = title == null ? "" : title.replaceFirst("^(File|Archivo):", "").trim().replace(' ', '_');
        if (name.isEmpty() || name.contains("/")) return null;
        return FILE_PATH + encode(name).replace("+", "%20") + "?width=480";
    }

    /** Nombre del artículo de Wikipedia en inglés para el término español (Commons está sobre todo en inglés). */
    String englishTerm(String es) throws IOException {
        String json = text(http.get(esWikipediaUrl(es), 200_000));
        return parseEnglishTitle(json);
    }

    List<Candidate> commons(String query, int limit) throws IOException {
        try {
            String body = text(http.get(commonsUrl(query, limit), 400_000));
            StringBuilder why = new StringBuilder();
            List<Candidate> found = parseCommons(body, why);
            log.add("«" + query.replace(" filetype:", " ·") + "»: " + found.size() + why
                    + (found.isEmpty() && why.length() == 0 ? snippet(body) : ""));
            return found;
        } catch (IOException error) {
            log.add("«" + query.replace(" filetype:", " ·") + "»: error " + error.getMessage());
            throw error;
        }
    }

    private final List<String> log = java.util.Collections.synchronizedList(new ArrayList<>());

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
        StringBuilder why = new StringBuilder();
        List<Candidate> found = parseCommons(text(http.get(COMMONS + "?action=query&format=json&formatversion=2"
                + "&prop=imageinfo&iiprop=url%7Cmime&iiurlwidth=480&titles=" + encode(String.join("|", titles)), 300_000)), why);
        log.add("Commons lista «" + term + "»: " + found.size() + why);
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
                if (!publicHttps(url)) continue;
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
    public String diagnostic() { synchronized (log) { return String.join("; ", log); } }

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

    static List<Candidate> parseCommons(String json) { return parseCommons(json, null); }

    /**
     * Archivos de una búsqueda de Commons. Si un archivo llega sin {@code imageinfo} o sin miniatura
     * (pasa cuando Commons corta el lote o no generó el tamaño pedido), no se pierde: se pide su
     * miniatura por nombre. Cada página se lee por separado: una rara ya no tira el lote entero.
     * @param why si no es null, cuenta por qué se descartó cada archivo (para el diagnóstico)
     */
    static List<Candidate> parseCommons(String json, StringBuilder why) {
        List<Candidate> out = new ArrayList<>();
        java.util.Map<String, Integer> dropped = new java.util.LinkedHashMap<>();
        int recovered = 0, foreign = 0;
        try {
            JsonObject query = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("query");
            if (query == null) return out;
            List<JsonObject> pages = new ArrayList<>();
            JsonElement all = query.get("pages");
            if (all != null && all.isJsonArray()) for (JsonElement p : all.getAsJsonArray()) { if (p.isJsonObject()) pages.add(p.getAsJsonObject()); }
            else if (all != null && all.isJsonObject()) for (java.util.Map.Entry<String, JsonElement> p : all.getAsJsonObject().entrySet())
                if (p.getValue().isJsonObject()) pages.add(p.getValue().getAsJsonObject()); // formatversion=1
            pages.sort((a, b) -> Integer.compare(index(a), index(b)));
            for (JsonObject page : pages) {
                try {
                    String title = text(page, "title");
                    if (title.isEmpty() || page.has("missing")) { count(dropped, "sin archivo"); continue; }
                    if (title.matches(UNSUITABLE)) { count(dropped, "mapa/logo/texto"); continue; }
                    JsonObject i = null;
                    JsonElement info = page.get("imageinfo");
                    if (info != null && info.isJsonArray() && info.getAsJsonArray().size() > 0
                            && info.getAsJsonArray().get(0).isJsonObject()) i = info.getAsJsonArray().get(0).getAsJsonObject();
                    String mime = i == null ? "" : text(i, "mime");
                    String thumb = i == null ? "" : text(i, "thumburl");
                    String lower = title.toLowerCase(Locale.ROOT);
                    boolean svg = mime.startsWith("image/svg") || (mime.isEmpty() && lower.endsWith(".svg"));
                    boolean raster = mime.equals("image/png") || mime.equals("image/jpeg")
                            || (mime.isEmpty() && lower.matches(".*\\.(png|jpe?g)$"));
                    if (!svg && !raster) { count(dropped, "formato " + (mime.isEmpty() ? "desconocido" : mime)); continue; }
                    String given = thumb;
                    thumb = wikimediaThumb(thumb);
                    if (thumb != null && !thumb.startsWith("https://upload.wikimedia.org/")) foreign++;
                    if (thumb == null) {
                        // Sin miniatura utilizable: se pide la del archivo a Commons por su nombre.
                        thumb = filePathThumb(title);
                        if (thumb == null) { count(dropped, "sin miniatura"); continue; }
                        recovered++;
                    }
                    String pageUrl = i == null ? "" : text(i, "descriptionurl");
                    out.add(new Candidate(title, thumb, pageUrl, svg));
                } catch (RuntimeException odd) { count(dropped, "página rara"); }
            }
        } catch (RuntimeException malformed) { count(dropped, "respuesta no JSON"); }
        if (why != null) {
            if (recovered > 0) why.append(" (").append(recovered).append(" sin miniatura válida en la API, pedidas por nombre)");
            if (foreign > 0) why.append(" (").append(foreign).append(" de otro servidor)");
            if (!dropped.isEmpty()) why.append(" [descartados: ").append(dropped.toString().replaceAll("[{}]", "").replace("=", "×")).append("]");
        }
        return out;
    }

    private static int index(JsonObject page) {
        JsonElement i = page.get("index");
        try { return i != null && i.isJsonPrimitive() ? i.getAsInt() : 99; } catch (RuntimeException odd) { return 99; }
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    private static void count(java.util.Map<String, Integer> map, String reason) { map.merge(reason, 1, Integer::sum); }

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
        if (title.matches(".*" + DRAWING_WORDS + ".*")) s *= 1.35f;
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

    /** Palabras de la petición, no del objeto: «gorila mejorado», «improved gorilla drawing». */
    private static final java.util.regex.Pattern REQUEST_WORDS = java.util.regex.Pattern.compile(
            "\\b(improved|improve|better|enhanced|refined|detailed|nicer|prettier|realistic|version"
            + "|mejorad[oa]s?|mejor|mejora|detallad[oa]s?|bonit[oa]s?|realista|versi[oó]n)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

    static String withoutRequestWords(String term) {
        if (term == null) return "";
        String t = REQUEST_WORDS.matcher(term).replaceAll(" ")
                .replaceAll("(?i)^\\s*(a|an|the|of|de|del)\\s+", " ").replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? term.trim() : t; // si no queda nada, mejor la palabra original que nada
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
