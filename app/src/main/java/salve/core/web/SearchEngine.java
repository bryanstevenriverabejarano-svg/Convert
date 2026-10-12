package salve.core.web;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import salve.core.NetworkResourcePolicy;

/** Public search URLs and bounded discovery. Search pages are not article evidence. */
public final class SearchEngine {
    public static final String[] IDS = {"auto", "google", "bing", "duckduckgo", "wikipedia", "custom"};
    public static final String[] LABELS = {"Automático", "Google", "Bing", "DuckDuckGo", "Wikipedia", "Otro buscador"};
    public final String id, template;
    public SearchEngine(String id, String custom) {
        if (!Arrays.asList(IDS).contains(id)) throw new IllegalArgumentException("Buscador desconocido");
        this.id = id;
        template = id.equals("custom") ? validateTemplate(custom) : id.equals("bing") ? "https://www.bing.com/search?q={query}&format=rss"
                : id.equals("duckduckgo") ? "https://html.duckduckgo.com/html/?q={query}"
                : id.equals("wikipedia") ? "https://es.wikipedia.org/w/index.php?search={query}"
                : "https://www.google.com/search?q={query}&hl=es";
    }
    public String url(String query) {
        try { return template.replace("{query}", URLEncoder.encode(query, StandardCharsets.UTF_8.name())); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
    private static String decode(String value) {
        try { return URLDecoder.decode(value, StandardCharsets.UTF_8.name()); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
    public static String validateTemplate(String value) {
        if (value == null || value.length() > 500 || value.indexOf("{query}") < 0
                || value.indexOf("{query}") != value.lastIndexOf("{query}"))
            throw new IllegalArgumentException("Usa una URL HTTPS con {query} una sola vez, por ejemplo https://buscador.com/search?q={query}");
        URI uri;
        try { uri = URI.create(value.replace("{query}", "salve")); }
        catch (RuntimeException bad) { throw new IllegalArgumentException("La URL del buscador no es válida"); }
        if (!NetworkResourcePolicy.validateKnowledgeUrl(uri.toString()).allowed || uri.getRawQuery() == null
                || !value.substring(value.indexOf('?')).contains("{query}") || uri.getFragment() != null)
            throw new IllegalArgumentException("La consulta debe estar en los parámetros de una URL HTTPS pública, sin fragmento");
        return value;
    }
    public static String explicitEngine(String question) {
        String text = question == null ? "" : question.toLowerCase(Locale.ROOT);
        Matcher m = Pattern.compile("^(?:busca(?:r|me)?\\s+|investiga\\s+)?(?:en|con|usando)\\s+(google|bing|duckduckgo|wikipedia)\\b").matcher(text);
        if (m.find()) return m.group(1);
        m = Pattern.compile("\\s+(?:en|con|usando)\\s+(google|bing|duckduckgo|wikipedia)[.!? ]*$").matcher(text);
        return m.find() ? m.group(1) : null;
    }
    public static String cleanQuery(String question) {
        return question.replaceFirst("(?i)^(?:busca(?:r|me)?|investiga)\\s+", "")
                .replaceFirst("(?i)^(?:en|con|usando)\\s+(?:google|bing|duckduckgo|wikipedia)\\s+", "")
                .replaceFirst("(?i)\\s+(?:en|con|usando)\\s+(?:google|bing|duckduckgo|wikipedia)[.!? ]*$", "").trim();
    }
    public static boolean interactionRequired(String url, String html) {
        String text = html.toLowerCase(Locale.ROOT);
        return url.contains("consent.google.") || url.contains("/sorry/")
                || text.contains("our systems have detected unusual traffic")
                || text.contains("anomaly-modal") || text.contains("id=\"captcha")
                || text.contains("id='captcha") || text.contains("g-recaptcha");
    }
    public static List<String> links(String html, String base) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Matcher rss = Pattern.compile("(?is)<link\\b[^>]*>([^<]{1,2048})</link>").matcher(html);
        int inspected = 0;
        while (rss.find() && result.size() < 6 && inspected++ < 100) add(result, rss.group(1), base);
        Matcher anchors = Pattern.compile("(?is)<a\\b").matcher(html);
        Pattern href = Pattern.compile("(?is)\\bhref\\s*=\\s*([\"'])(.*?)\\1");
        int cursor = 0; inspected = 0;
        while (result.size() < 6 && anchors.find(cursor) && inspected++ < 1000) {
            int from = anchors.end(), end = html.indexOf('>', from);
            if (end < 0 || end - from > 4096) { cursor = Math.min(html.length(), from + 4096); continue; }
            cursor = end + 1;
            Matcher link = href.matcher(html.substring(from, end));
            if (link.find()) add(result, link.group(2), base);
        }
        return new ArrayList<>(result);
    }
    private static void add(Set<String> result, String raw, String base) {
        try {
            String value = raw.trim().replace("&amp;", "&").replace("&#38;", "&").replace("<![CDATA[", "").replace("]]>", "");
            URI uri = URI.create(base).resolve(value);
            String host = uri.getHost();
            if (host == null) return;
            if (isEngineHost(host) && uri.getRawQuery() != null) {
                Map<String,String> args = new HashMap<>();
                for (String pair : uri.getRawQuery().split("&")) {
                    String[] parts = pair.split("=", 2);
                    if (parts.length == 2) args.put(parts[0], decode(parts[1]));
                }
                String target = args.get("uddg");
                if (target == null && uri.getPath().equals("/url")) target = args.containsKey("q") ? args.get("q") : args.get("url");
                if (target == null && uri.getPath().startsWith("/ck/a") && args.containsKey("u") && args.get("u").startsWith("a1"))
                    target = new String(Base64.getUrlDecoder().decode(args.get("u").substring(2)), StandardCharsets.UTF_8);
                if (target != null) uri = URI.create(target);
            }
            value = uri.toString(); host = uri.getHost();
            if (value.length() <= 500 && NetworkResourcePolicy.validateKnowledgeUrl(value).allowed
                    && !isEngineHost(host) && !host.equalsIgnoreCase(URI.create(base).getHost())) result.add(value);
        } catch (RuntimeException malformed) { /* One malformed result cannot discard its siblings. */ }
    }
    private static boolean isEngineHost(String host) {
        if (host == null) return true;
        String value = host.toLowerCase(Locale.ROOT);
        return value.equals("google.com") || value.endsWith(".google.com") || value.equals("google.es") || value.endsWith(".google.es")
                || value.equals("bing.com") || value.endsWith(".bing.com") || value.equals("microsoft.com") || value.endsWith(".microsoft.com")
                || value.equals("duckduckgo.com") || value.endsWith(".duckduckgo.com");
    }
}
