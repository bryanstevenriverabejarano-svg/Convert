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
        public ResearchBatch(Status status, List<Page> pages, int failures) {
            if (status == null || pages == null || pages.size() > MAX_PAGES || failures < 0)
                throw new IllegalArgumentException("Informe de lectura inválido");
            this.status = status;
            this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
            this.failures = failures;
        }
    }

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

    public ResearchBatch researchResult(String question, BooleanSupplier stopped) {
        List<Page> pages = new ArrayList<>();
        int failures = 0;
        long deadline = System.nanoTime() + REQUEST_BUDGET_NANOS;
        try {
            check(stopped, deadline);
            if (question == null || question.trim().isEmpty() || question.length() > MAX_QUERY_CHARS)
                return new ResearchBatch(Status.ERROR, pages, 1);
            List<String> urls = extractUrls(question);
            if (urls.isEmpty() && Pattern.compile("https?://", Pattern.CASE_INSENSITIVE).matcher(question).find())
                return new ResearchBatch(Status.ERROR, pages, 1);
            if (urls.isEmpty()) urls = discoverOnWikipedia(question, stopped, deadline);
            for (String url : urls) {
                check(stopped, deadline);
                if (pages.size() == MAX_PAGES) break;
                try { pages.add(fetch(url, stopped, deadline)); }
                catch (BudgetExceeded timeout) { throw timeout; }
                catch (IOException unavailable) { failures++; }
            }
            check(stopped, deadline);
            Status status = pages.isEmpty() ? failures == 0 ? Status.EMPTY : Status.ERROR
                    : failures == 0 ? Status.COMPLETE : Status.PARTIAL;
            return new ResearchBatch(status, pages, failures);
        } catch (CancellationException cancelled) {
            return new ResearchBatch(Status.CANCELLED, pages, failures);
        } catch (BudgetExceeded exhausted) {
            return new ResearchBatch(Status.BUDGET_EXHAUSTED, pages, failures);
        } catch (IOException | IllegalArgumentException unavailable) {
            return new ResearchBatch(pages.isEmpty() ? Status.ERROR : Status.PARTIAL, pages, failures + 1);
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
        return discoverOnWikipedia(query, () -> false, System.nanoTime() + REQUEST_BUDGET_NANOS);
    }

    private List<String> discoverOnWikipedia(String query, BooleanSupplier stopped, long deadline) throws IOException {
        String cleanQuery = query == null ? "" : query.replaceAll("https://\\S+", " ").trim();
        if (cleanQuery.isEmpty()) return Collections.emptyList();
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.name());
        Page response = fetch("https://es.wikipedia.org/w/api.php?action=opensearch&limit=3&namespace=0&format=json&search=" + encoded,
                stopped, deadline);
        Matcher matcher = Pattern.compile("https:(?:\\\\/|/){2}es\\.wikipedia\\.org(?:\\\\/|/)wiki(?:\\\\/|/)[^\"\\s]+")
                .matcher(response.text);
        List<String> urls = new ArrayList<>();
        while (matcher.find() && urls.size() < MAX_PAGES) urls.add(matcher.group().replace("\\/", "/"));
        return urls;
    }

    public Page fetch(String requestedUrl) throws IOException {
        return fetch(requestedUrl, () -> false, System.nanoTime() + REQUEST_BUDGET_NANOS);
    }

    public Page fetch(String requestedUrl, BooleanSupplier stopped) throws IOException {
        return fetch(requestedUrl, stopped, System.nanoTime() + REQUEST_BUDGET_NANOS);
    }

    private Page fetch(String requestedUrl, BooleanSupplier stopped, long deadline) throws IOException {
        String current = requestedUrl;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            check(stopped, deadline);
            NetworkResourcePolicy.Validation validation = NetworkResourcePolicy.validateKnowledgeUrl(current);
            if (!validation.allowed) throw new IOException(validation.reason);
            Request request = new Request.Builder().url(validation.normalizedUrl)
                    .header("User-Agent", "Salve/1.0 (+lector de conocimiento)").get().build();
            Call call = client.newCall(request);
            call.timeout().timeout(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            try (Response response = call.execute()) {
                check(stopped, deadline);
                int status = response.code();
                if (status >= 300 && status < 400) {
                    String location = response.header("Location");
                    if (location == null || redirects == MAX_REDIRECTS) throw new IOException("Redirección no permitida");
                    current = URI.create(validation.normalizedUrl).resolve(location).toString();
                    continue;
                }
                if (!response.isSuccessful()) throw new IOException("Respuesta HTTP " + status);
                ResponseBody body = response.body();
                String type = response.header("Content-Type", "").toLowerCase(Locale.ROOT);
                if (body == null || !(type.startsWith("text/") || type.startsWith("application/json")))
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
                return new Page(validation.normalizedUrl, extractText(raw, type), status, bodyBytes.length, pageTitle);
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
            text = text.replaceAll("(?is)<(script|style|noscript)\\b[^>]*>.*?(?:</\\1\\s*>|\\z)", " ")
                    .replaceAll("(?s)<!--.*?-->", " ").replaceAll("(?s)<[^>]+>", " ");
        }
        text = text.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replaceAll("\\s+", " ").trim();
        return text.length() <= MAX_EXTRACT_CHARS ? text : text.substring(0, MAX_EXTRACT_CHARS);
    }

    public static final class Page {
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
}
