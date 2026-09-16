package salve.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lector web acotado. Wikipedia se usa solo para descubrir una fuente cuando no hay URL. */
public class WikipediaResearchClient {
    public static final int MAX_PAGES = 3;
    public static final int MAX_EXTRACT_CHARS = 2_500;
    private static final int MAX_REDIRECTS = 3;
    private static final Pattern URL = Pattern.compile("https://[^\\s<>\\[\\](){}]+", Pattern.CASE_INSENSITIVE);

    public List<Page> research(String question) throws IOException {
        List<String> urls = extractUrls(question);
        if (urls.isEmpty()) urls = discoverOnWikipedia(question);
        List<Page> pages = new ArrayList<>();
        for (String url : urls) {
            if (pages.size() == MAX_PAGES) break;
            try { pages.add(fetch(url)); } catch (IOException ignored) { /* conserva las otras fuentes */ }
        }
        return pages;
    }

    public List<String> extractUrls(String text) {
        if (text == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        Matcher matcher = URL.matcher(text);
        while (matcher.find() && result.size() < MAX_PAGES) {
            String value = matcher.group().replaceFirst("[.,;:!?]+$", "");
            if (NetworkResourcePolicy.validateKnowledgeUrl(value).allowed && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    protected List<String> discoverOnWikipedia(String query) throws IOException {
        String cleanQuery = query == null ? "" : query.replaceAll("https://\\S+", " ").trim();
        if (cleanQuery.isEmpty()) return Collections.emptyList();
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.name());
        Page response = fetch("https://es.wikipedia.org/w/api.php?action=opensearch&limit=3&namespace=0&format=json&search=" + encoded);
        Matcher matcher = Pattern.compile("https:(?:\\\\/|/){2}es\\.wikipedia\\.org(?:\\\\/|/)wiki(?:\\\\/|/)[^\"\\s]+")
                .matcher(response.text);
        List<String> urls = new ArrayList<>();
        while (matcher.find() && urls.size() < MAX_PAGES) {
            urls.add(matcher.group().replace("\\/", "/"));
        }
        return urls;
    }

    public Page fetch(String requestedUrl) throws IOException {
        String current = requestedUrl;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            NetworkResourcePolicy.Validation validation =
                    NetworkResourcePolicy.validateKnowledgeDestination(current);
            if (!validation.allowed) throw new IOException(validation.reason);
            HttpURLConnection connection = (HttpURLConnection) new URL(validation.normalizedUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Salve/1.0 (+lector de conocimiento)");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects == MAX_REDIRECTS) throw new IOException("Redireccion no permitida");
                    current = URI.create(validation.normalizedUrl).resolve(location).toString();
                    continue;
                }
                if (status < 200 || status >= 300) throw new IOException("Respuesta HTTP " + status);
                String type = connection.getContentType();
                if (type == null || !(type.toLowerCase(Locale.ROOT).startsWith("text/")
                        || type.toLowerCase(Locale.ROOT).startsWith("application/json"))) {
                    throw new IOException("Tipo de contenido no permitido");
                }
                long length = connection.getContentLengthLong();
                if (length > NetworkResourcePolicy.MAX_KNOWLEDGE_BYTES) throw new IOException("Respuesta demasiado grande");
                String raw = new String(readLimited(connection.getInputStream()), StandardCharsets.UTF_8);
                return new Page(validation.normalizedUrl, extractText(raw, type));
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Demasiadas redirecciones");
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8_192];
            int total = 0, read;
            while ((read = stream.read(buffer)) != -1) {
                total += read;
                if (total > NetworkResourcePolicy.MAX_KNOWLEDGE_BYTES) throw new IOException("Respuesta demasiado grande");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    static String extractText(String raw, String contentType) {
        String text = raw;
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            text = text.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ")
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
        public Page(String url, String text) { this.url = url; this.text = text; }
    }
}
