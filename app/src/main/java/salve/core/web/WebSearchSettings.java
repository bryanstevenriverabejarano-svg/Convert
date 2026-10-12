package salve.core.web;

import android.content.Context;

public final class WebSearchSettings {
    private static final String PREFS = "salve_web_search";
    private WebSearchSettings() { }
    public static SearchEngine load(Context context, String question) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String explicit = SearchEngine.explicitEngine(question);
        try { return new SearchEngine(explicit == null ? prefs.getString("engine", "auto") : explicit, prefs.getString("custom", "")); }
        catch (IllegalArgumentException invalid) { return new SearchEngine("auto", ""); }
    }
    public static void save(Context context, String id, String custom) {
        SearchEngine engine = new SearchEngine(id, custom);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("engine", id)
                .putString("custom", id.equals("custom") ? engine.template : "").apply();
    }
    public static void lastSearch(Context context, String url, String diagnostic) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("last_url", url.startsWith("https://www.bing.com/search?") ? url.replace("&format=rss", "") : url)
                .putString("diagnostic", diagnostic).apply();
    }
    public static String lastUrl(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last_url", "https://www.google.com/"); }
    public static String diagnostic(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("diagnostic", "Todavía no hay una búsqueda comprobada."); }
}
