package salve.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Real bounded HTTPS text reader. It does not render JavaScript or operate a browser DOM. */
public class WikipediaResearchClient {
    public static final int MAX_PAGES = 3;
    public static final int MAX_EXTRACT_CHARS = 2_500;
    public static final int MAX_QUERY_CHARS = 2_048;
    private static final int MAX_REDIRECTS = 3;
    private static final long REQUEST_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(20);
    private static final Pattern URL = Pattern.compile("https://[^\\s<>\\[\\](){}]+", Pattern.CASE_INSENSITIVE);
    private final OkHttpClient client;

    public enum Status { COMPLETE, PARTIAL, EMPTY, ERROR, CANCELLED, BUDGET_EXHAUSTED }
    public static final class ResearchBatch {
        public final Status status;
        public final List<Page> pages;
        public final int failures;
        public final String diagnostic;
        public ResearchBatch(Status status, List<Page> pages, int failures) {
            this(status, pages, failures, "");
        }
        public ResearchBatch(Status status, List<Page> pages, int failures, String diagnostic) {
            if (status == null || pages == null || pages.size() > MAX_PAGES || failures < 0)
                throw new IllegalArgumentException("Informe de lectura inválido");
            this.status = status;
            this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
            this.failures = failures;
            this.diagnostic = diagnostic == null ? "" : diagnostic;
        }
    }

    /** Package-private transport seam for deterministic HTTP integration tests. */
    WikipediaResearchClient(OkHttpClient client) { this.client = client; }

    public WikipediaResearchClient() {
        client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
                // CONNECT proxies resolve destinations themselves, outside this reader's DNS checks.
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false).followSslRedirects(false)
                // These same checked addresses are used by the socket, without a second DNS lookup.
                .dns(host -> {
                    List<InetAddress> addresses = Dns.SYSTEM.lookup(host);
                    for (InetAddress address : addresses) {
                        if (!NetworkResourcePolicy.isPublicAddress(address.getAddress()))
                            throw new UnknownHostException("El destino no es público");
                    }
                    return addresses;
                }).build();
    }

    /** Compatibility entry point; new callers use the typed batch to distinguish failure from absence. */
    public List<Page> research(String question) throws IOException {
        ResearchBatch batch = researchResult(question, () -> false);
        if (batch.status == Status.ERROR || batch.status == Status.BUDGET_EXHAUSTED)
            throw new IOException("La lectura pública no se completó");
        if (batch.status == Status.CANCELLED) throw new CancellationException();
        return batch.pages;
    }

    /** Batch compatibility; web discovery overrides this to deliver each page before the next fetch. */
    public ResearchBatch researchResult(String question, BooleanSupplier stopped, java.util.function.Consumer<Page> consumer) {
        return deliverPages(researchResult(question, stopped), stopped, consumer);
    }

    protected static ResearchBatch deliverPages(ResearchBatch batch, BooleanSupplier stopped, java.util.function.Consumer<Page> consumer) {
        if (consumer != null && batch.status != Status.CANCELLED) for (Page page : batch.pages) {
            if (Thread.currentThread().isInterrupted() || stopped != null && stopped.getAsBoolean()) throw new CancellationException();
            consumer.accept(page);
        }
        return batch;
    }

    public ResearchBatch researchResult(String question, BooleanSupplier stopped) {
        List<Page> pages = new ArrayList<>();
        int failures = 0;
        String diagnostic = "";
        long deadline = System.nanoTime() + REQUEST_BUDGET_NANOS;
        try {
            check(stopped, deadline);
            if (question == null || question.trim().isEmpty() || question.length() > MAX_QUERY_CHARS)
                return new ResearchBatch(Status.ERROR, pages, 1);
            List<String> urls = extractUrls(question);
            if (urls.isEmpty() && Pattern.compile("https?://", Pattern.CASE_INSENSITIVE).matcher(question).find())
                return new ResearchBatch(Status.ERROR, pages, 1);
            if (urls.isEmpty()) {
                pages.addAll(searchWikipedia(question, stopped, deadline));
                check(stopped, deadline);
                return new ResearchBatch(pages.isEmpty() ? Status.EMPTY : Status.COMPLETE, pages, 0);
            }
            for (String url : urls) {
                check(stopped, deadline);
                if (pages.size() == MAX_PAGES) break;
                try { pages.add(fetch(url, stopped, deadline)); }
                catch (BudgetExceeded timeout) { throw timeout; }
                catch (IOException unavailable) { failures++; diagnostic = failureReason(unavailable); }
            }
            check(stopped, deadline);
            Status status = pages.isEmpty() ? failures == 0 ? Status.EMPTY : Status.ERROR
                    : failures == 0 ? Status.COMPLETE : Status.PARTIAL;
            return new ResearchBatch(status, pages, failures, diagnostic);
        } catch (CancellationException cancelled) {
            return new ResearchBatch(Status.CANCELLED, pages, failures);
        } catch (BudgetExceeded exhausted) {
            return new ResearchBatch(Status.BUDGET_EXHAUSTED, pages, failures, "Se agotó el tiempo de lectura web.");
        } catch (IOException | IllegalArgumentException unavailable) {
            return new ResearchBatch(pages.isEmpty() ? Status.ERROR : Status.PARTIAL, pages, failures + 1, failureReason(unavailable));
        }
    }

    public List<String> extractUrls(String text) {
        if (text == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        Matcher matcher = URL.matcher(text);
        while (matcher.find() && result.size() < MAX_PAGES) {
            String value = matcher.group().replaceFirst("[.,;:!?]+$", "");
            if (NetworkResourcePolicy.validateKnowledgeUrl(value).allowed && !result.contains(value)) result.add(value);
        }
        return result;
    }

    protected List<String> discoverOnWikipedia(String query) throws IOException {
        List<String> urls = new ArrayList<>();
        for (Page page : searchWikipedia(query, () -> false, System.nanoTime() + REQUEST_BUDGET_NANOS)) urls.add(page.url);
        return urls;
    }

    private List<Page> searchWikipedia(String query, BooleanSupplier stopped, long deadline) throws IOException {
        String cleanQuery = query == null ? "" : query.replaceAll("https://\\S+", " ").trim();
        if (cleanQuery.isEmpty()) return Collections.emptyList();
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.name());
        // Search full text, not only title prefixes. Read complete JSON before limiting each extract.
        Page response = fetch("https://es.wikipedia.org/w/api.php?action=query&generator=search&gsrnamespace=0"
                + "&gsrlimit=3&prop=extracts%7Cinfo&inprop=url&exintro=1&explaintext=1&exchars=1800"
                + "&format=json&formatversion=2&gsrsearch=" + encoded, stopped, deadline, 1);
        return wikipediaPages(response.text, response.statusCode, response.bodyBytes);
    }

    static List<Page> wikipediaPages(String json, int status, int bytes) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("error")) throw new ReadFailure("Wikipedia rechazó la consulta.");
            if (!root.has("query")) {
                if (root.has("batchcomplete")) return Collections.emptyList();
                throw new ReadFailure("Wikipedia devolvió una respuesta incompleta.");
            }
            List<JsonObject> entries = new ArrayList<>();
            for (JsonElement item : root.getAsJsonObject("query").getAsJsonArray("pages")) entries.add(item.getAsJsonObject());
            entries.sort(java.util.Comparator.comparingInt(p -> p.has("index") ? p.get("index").getAsInt() : Integer.MAX_VALUE));
            List<Page> result = new ArrayList<>();
            for (JsonObject p : entries) {
                if (p.has("missing") || !p.has("extract") || !p.has("fullurl")) continue;
                String url = p.get("fullurl").getAsString(), excerpt = p.get("extract").getAsString().trim();
                if (excerpt.isEmpty() || !NetworkResourcePolicy.validateKnowledgeUrl(url).allowed
                        || !"es.wikipedia.org".equalsIgnoreCase(URI.create(url).getHost())) continue;
                result.add(new Page(url, excerpt.substring(0, Math.min(MAX_EXTRACT_CHARS, excerpt.length())),
                        status, bytes, p.has("title") ? p.get("title").getAsString() : ""));
                if (result.size() == MAX_PAGES) break;
            }
            return result;
        } catch (RuntimeException malformed) { throw new ReadFailure("Wikipedia devolvió un formato de respuesta no válido."); }
    }

    private static final class ReadFailure extends IOException { ReadFailure(String message) { super(message); } }
    private static String failureReason(Exception failure) {
        if (failure instanceof ReadFailure) return failure.getMessage();
        if (failure instanceof UnknownHostException) return "No se pudo resolver el servidor público; revisa la conexión o el DNS.";
        if (failure instanceof java.net.SocketTimeoutException || failure instanceof java.io.InterruptedIOException)
            return "El servidor público no respondió a tiempo.";
        if (failure instanceof javax.net.ssl.SSLException) return "No se pudo establecer una conexión HTTPS válida.";
        return "No se pudo completar la conexión con la fuente pública.";
    }

    public Page fetch(String requestedUrl) throws IOException {
        return fetch(requestedUrl, () -> false, System.nanoTime() + REQUEST_BUDGET_NANOS);
    }

    public Page fetch(String requestedUrl, BooleanSupplier stopped) throws IOException {
        return fetch(requestedUrl, stopped, System.nanoTime() + REQUEST_BUDGET_NANOS);
    }

    private Page fetch(String requestedUrl, BooleanSupplier stopped, long deadline) throws IOException {
        return fetch(requestedUrl, stopped, deadline, 0);
    }
    protected Page readDocument(String url, BooleanSupplier stopped, long deadline) throws IOException {
        return fetch(url, stopped, deadline, 2);
    }
    protected Page readPage(String url, BooleanSupplier stopped, long deadline) throws IOException {
        return fetch(url, stopped, deadline, 0);
    }
    /** JSON de una API con credencial propia. La credencial nunca sigue redirecciones. */
    protected Page readApiJson(String url, String header, String value, BooleanSupplier stopped, long deadline) throws IOException {
        return fetch(url, stopped, deadline, 1, header, value);
    }
    private Page fetch(String requestedUrl, BooleanSupplier stopped, long deadline, int documentType) throws IOException {
        return fetch(requestedUrl, stopped, deadline, documentType, null, null);
    }
    private Page fetch(String requestedUrl, BooleanSupplier stopped, long deadline, int documentType,
                       String credentialHeader, String credential) throws IOException {
        String current = requestedUrl;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            check(stopped, deadline);
            NetworkResourcePolicy.Validation validation = NetworkResourcePolicy.validateKnowledgeUrl(current);
            if (!validation.allowed) throw new IOException(validation.reason);
            Request.Builder builder = new Request.Builder().url(validation.normalizedUrl)
                    .header("User-Agent", "Salve/1.1 (https://github.com/bryanstevenriverabejarano-svg/Convert)");
            if (credentialHeader != null) builder.header(credentialHeader, credential).header("Accept", "application/json");
            Request request = builder.get().build();
            Call call = client.newCall(request);
            call.timeout().timeout(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            try (Response response = call.execute()) {
                check(stopped, deadline);
                int status = response.code();
                if (status >= 300 && status < 400) {
                    if (credentialHeader != null) throw new IOException("Redirección no permitida con credencial");
                    String location = response.header("Location");
                    if (location == null || redirects == MAX_REDIRECTS) throw new IOException("Redirección no permitida");
                    current = URI.create(validation.normalizedUrl).resolve(location).toString();
                    continue;
                }
                if (!response.isSuccessful()) throw new ReadFailure("La fuente pública devolvió HTTP " + status + ".");
                ResponseBody body = response.body();
                String type = response.header("Content-Type", "").toLowerCase(Locale.ROOT);
                if (documentType == 1 && !type.startsWith("application/json")) throw new ReadFailure("La fuente no devolvió datos JSON.");
                if (body == null || !(type.startsWith("text/") || type.startsWith("application/json") || type.startsWith("application/xml")))
                    throw new IOException("Tipo de contenido no permitido");
                if (body.contentLength() > NetworkResourcePolicy.MAX_KNOWLEDGE_BYTES)
                    throw new IOException("Respuesta demasiado grande");
                byte[] bodyBytes = readLimited(body.byteStream(), stopped, deadline);
                String raw = new String(bodyBytes, body.contentType() == null ? StandardCharsets.UTF_8
                        : body.contentType().charset(StandardCharsets.UTF_8));
                check(stopped, deadline);
                Matcher title = Pattern.compile("(?is)<title\\b[^>]*>(.*?)</title\\s*>").matcher(raw);
                String pageTitle = type.startsWith("text/html") && title.find()
                        ? extractText(title.group(1), "text/html") : "";
                if (pageTitle.length() > 200) pageTitle = pageTitle.substring(0, 200);
                Page page = new Page(validation.normalizedUrl, documentType > 0 ? raw : extractText(raw, type), status, bodyBytes.length, pageTitle);
                if (type.startsWith("text/html")) page.links.addAll(extractLinks(raw, validation.normalizedUrl));
                return page;
            } finally { call.cancel(); }
        }
        throw new IOException("Demasiadas redirecciones");
    }

    private static byte[] readLimited(InputStream input, BooleanSupplier stopped, long deadline) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0, read;
            while (true) {
                check(stopped, deadline);
                read = stream.read(buffer);
                if (read == -1) break;
                total += read;
                if (total > NetworkResourcePolicy.MAX_KNOWLEDGE_BYTES) throw new IOException("Respuesta demasiado grande");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static void check(BooleanSupplier stopped, long deadline) throws BudgetExceeded {
        if (Thread.currentThread().isInterrupted() || stopped != null && stopped.getAsBoolean()) throw new CancellationException();
        if (System.nanoTime() - deadline >= 0) throw new BudgetExceeded();
    }
    private static final class BudgetExceeded extends IOException { }

    static String extractText(String raw, String contentType) {
        String text = raw;
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            Matcher main = Pattern.compile("(?is)<(?:main|article)\\b[^>]*>(.*?)</(?:main|article)\\s*>").matcher(text);
            if (main.find()) text = main.group(1);
            text = text.replaceAll("(?is)<(script|style|noscript)\\b[^>]*>.*?(?:</\\1\\s*>|\\z)", " ")
                    .replaceAll("(?s)<!--.*?-->", " ").replaceAll("(?s)<[^>]+>", " ");
        }
        text = text.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replaceAll("\\s+", " ").trim();
        return text.length() <= MAX_EXTRACT_CHARS ? text : text.substring(0, MAX_EXTRACT_CHARS);
    }

    public static final class Page {
        public final List<String> links = new ArrayList<>();
        public final String url;
        public final String text;
        public final int statusCode, bodyBytes;
        public final String title;
        /** Source fixtures or adapters without transport metadata do not invent an HTTP status. */
        public Page(String url, String text) { this(url, text, 0, 0, ""); }
        public Page(String url, String text, int statusCode, int bodyBytes, String title) {
            this.url = url; this.text = text; this.statusCode = statusCode;
            this.bodyBytes = bodyBytes; this.title = title;
        }
    }

    /** Bounded public hyperlinks; callers still validate each destination when it is fetched. */
    public static List<String> extractLinks(String html, String base) {
        List<String> links = new ArrayList<>();
        Matcher anchors = Pattern.compile("(?is)<a\\b").matcher(html);
        Pattern href = Pattern.compile("(?is)\\bhref\\s*=\\s*([\"'])(.*?)\\1");
        int cursor = 0, inspected = 0;
        while (cursor < html.length() && anchors.find(cursor) && links.size() < 12 && inspected++ < 128) {
            int from = anchors.end(), end = html.indexOf('>', from);
            if (end < 0 || end - from > 4096) { cursor = Math.min(html.length(), from + 4096); continue; }
            cursor = end + 1;
            Matcher match = href.matcher(html.substring(from, end));
            if (!match.find()) continue;
            try {
                String link = URI.create(base).resolve(match.group(2).replace("&amp;", "&")).toString();
                if (link.length() <= 500 && NetworkResourcePolicy.validateKnowledgeUrl(link).allowed && !links.contains(link)) links.add(link);
            } catch (IllegalArgumentException malformed) { }
        }
        return links;
    }
}
