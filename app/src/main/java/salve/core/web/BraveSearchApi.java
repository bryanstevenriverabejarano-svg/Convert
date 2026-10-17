package salve.core.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import salve.core.NetworkResourcePolicy;

/**
 * API oficial de Brave Search con la clave del propio dueño. Devuelve resultados en JSON
 * sin CAPTCHA ni scraping: Salve solo usa las URLs y después lee esas páginas públicas.
 * La consulta no lleva contexto privado; la clave nunca aparece en diagnósticos.
 */
public final class BraveSearchApi {
    public static final String ENDPOINT = "https://api.search.brave.com/res/v1/web/search";
    public static final String KEY_HEADER = "X-Subscription-Token";
    public static final int MAX_LINKS = 6;
    private static final int MAX_QUERY = 400; // límite de la API

    private BraveSearchApi() { }

    public static String requestUrl(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Consulta vacía");
        if (value.length() > MAX_QUERY) value = value.substring(0, MAX_QUERY);
        try {
            return ENDPOINT + "?q=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    + "&count=" + MAX_LINKS + "&search_lang=es&safesearch=moderate&text_decorations=false";
        } catch (UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }

    public static boolean validKey(String key) {
        return key != null && key.matches("[A-Za-z0-9_-]{20,128}");
    }

    /** URLs públicas de web.results, en orden y sin duplicados. JSON inesperado = lista vacía. */
    public static List<String> links(String json) {
        Set<String> result = new LinkedHashSet<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject web = root.has("web") && root.get("web").isJsonObject() ? root.getAsJsonObject("web") : null;
            JsonArray items = web != null && web.has("results") && web.get("results").isJsonArray()
                    ? web.getAsJsonArray("results") : new JsonArray();
            int inspected = 0;
            for (JsonElement item : items) {
                if (result.size() == MAX_LINKS || inspected++ >= 50) break;
                if (!item.isJsonObject()) continue;
                JsonElement url = item.getAsJsonObject().get("url");
                if (url == null || !url.isJsonPrimitive()) continue;
                String value = url.getAsString();
                if (value.length() <= 500 && NetworkResourcePolicy.validateKnowledgeUrl(value).allowed) result.add(value);
            }
        } catch (RuntimeException malformed) { return new ArrayList<>(); }
        return new ArrayList<>(result);
    }

    /** Traduce el código HTTP a un motivo útil, sin repetir nada de la respuesta. */
    public static String failure(int status) {
        if (status == 401 || status == 403) return "brave: la clave fue rechazada; revísala en Buscadores web";
        if (status == 402 || status == 429) return "brave: se agotó el límite o el crédito de la API";
        if (status >= 500) return "brave: el servicio no respondió (HTTP " + status + ")";
        return "brave: HTTP " + status;
    }
}
