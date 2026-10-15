package salve.core.web;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/**
 * Puente entre lo que Salve busca y la mini ventana flotante: abre la búsqueda en la ventana y
 * va mostrando qué páginas lee. Es solo visualización; la ventana no tiene acceso a la memoria.
 *
 * <p>Para no competir con el modelo por la RAM, no abre un navegador nuevo si queda poca
 * memoria libre: en ese caso la búsqueda sigue, pero sin ventana.</p>
 */
public final class SearchLive {
    public interface Listener {
        void onSearch(String query);
        void onReading(String url, String title);
    }

    private static final String PREFS = "salve_web_search";
    private static final String KEY = "live_panel";
    /** Margen de memoria para abrir un WebView (~150-250 MB) sin poner en riesgo el modelo. */
    private static final long MIN_FREE_BYTES = 600L * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Listener listener;

    private SearchLive() { }

    public static void register(Listener value) { listener = value; }
    public static void unregister(Listener value) { if (listener == value) listener = null; }
    public static boolean panelOpen() { return listener != null; }

    public static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true);
    }

    public static void setEnabled(Context context, boolean value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
    }

    public static boolean canShow(Context context) { return Settings.canDrawOverlays(context); }

    /**
     * Enseña la búsqueda en la mini ventana. Devuelve null si se muestra (o está desactivado) y
     * una explicación breve si no se pudo.
     */
    public static String begin(Context context, String query) {
        if (query == null || query.trim().isEmpty() || !enabled(context)) return null;
        Listener current = listener;
        if (current != null) { MAIN.post(() -> current.onSearch(query)); return null; }
        if (!canShow(context)) return "Si quieres ver mis búsquedas en directo, di «muestra tus búsquedas» y permite que aparezca sobre otras apps.";
        if (lowMemory(context)) return "No abro la mini ventana ahora: queda poca memoria y la necesito para pensar.";
        return salve.services.MobileWorkspaceService.open(context, query) ? null
                : "Android no me dejó abrir la mini ventana.";
    }

    /** Página que Salve está leyendo ahora mismo (solo si la ventana está abierta). */
    public static void reading(String url, String title) {
        Listener current = listener;
        if (current != null && url != null) MAIN.post(() -> current.onReading(url, title == null ? "" : title));
    }

    public static boolean lowMemory(Context context) {
        try {
            ActivityManager manager = context.getSystemService(ActivityManager.class);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(info);
            return info.lowMemory || info.availMem < MIN_FREE_BYTES;
        } catch (RuntimeException unknown) { return false; }
    }
}
