package salve.core.devicecontrol;
import salve.core.NetworkResourcePolicy;
import salve.core.web.SearchEngine;

public final class MobileBrowserPolicy {
    private MobileBrowserPolicy() { }
    public static boolean allowed(String url) {
        return url != null && url.length() <= 4096 && NetworkResourcePolicy.validateKnowledgeUrl(url).allowed;
    }
    public static String searchUrl(SearchEngine engine, String query) {
        if (query == null || query.trim().isEmpty() || query.length() > 1000)
            throw new IllegalArgumentException("Escribe una búsqueda de hasta 1000 caracteres.");
        if (query.trim().startsWith("https://")) {
            if (!allowed(query.trim())) throw new IllegalArgumentException("La dirección debe ser HTTPS pública.");
            return query.trim();
        }
        String clean = SearchEngine.cleanQuery(query);
        if (clean.isEmpty()) throw new IllegalArgumentException("Añade lo que quieres buscar después del nombre del buscador.");
        String url = engine.url(clean);
        if (engine.id.equals("bing")) url = url.replace("&format=rss", "");
        if (engine.id.equals("duckduckgo")) url = url.replace("https://html.duckduckgo.com/html/", "https://duckduckgo.com/");
        if (!allowed(url)) throw new IllegalArgumentException("El buscador necesita una dirección HTTPS pública.");
        return url;
    }
}
