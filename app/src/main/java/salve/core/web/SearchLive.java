package salve.core.web;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/**
 * Puente entre lo que Salve hace y la mini ventana flotante «Salve en directo»: lo que piensa,
 * lo que busca, las páginas que lee y lo que escribe. Es solo visualización; la ventana no tiene
 * acceso a la memoria.
 *
 * <p>Para no competir con el modelo por la RAM, no abre un navegador nuevo si queda poca
 * memoria libre: en ese caso la búsqueda sigue, pero sin ventana.</p>
 */
public final class SearchLive {
    public interface Listener {
        void onSearch(String query);
        void onReading(String url, String title);
        /** Lo que está haciendo ahora («Pensando qué responderte…»). */
        default void onActivity(String text) { }
        /** Lo que acaba de escribir: la ventana lo teclea letra a letra. */
        default void onTyping(String text) { }
        /** Lo que está escribiendo ahora mismo (cuentos y textos largos, palabra a palabra). */
        default void onDraft(String text) { }
    }

    private static final String PREFS = "salve_web_search";
    private static final String KEY = "live_panel";
    /** Margen de memoria para abrir un WebView (~150-250 MB) sin poner en riesgo el modelo. */
    private static final long MIN_FREE_BYTES = 600L * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Listener listener;
    private static volatile String pendingQuery = "", lastActivity = "";
    private static final String PERMISSION_OFFERED = "live_permission_offered";

    private SearchLive() { }

    public static void register(Listener value) {
        listener = value;
        String activity = lastActivity;
        if (!activity.isEmpty()) MAIN.post(() -> { if (listener == value) value.onActivity(activity); });
    }
    public static void unregister(Listener value) { if (listener == value) listener = null; }
    public static boolean panelOpen() { return listener != null; }

    public static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true);
    }

    public static void setEnabled(Context context, boolean value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
        if (!value) { pendingQuery = ""; lastActivity = ""; }
    }

    public static boolean canShow(Context context) { return Settings.canDrawOverlays(context); }

    /** Open a status window before planning, without submitting the user's raw request to a browser. */
    public static String prepare(Context context, boolean foreground) {
        pendingQuery = ""; lastActivity = "Interpretando el objetivo antes de buscar…";
        if (!enabled(context)) return null;
        if (canShow(context)) return show(context);
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (foreground && !prefs.getBoolean(PERMISSION_OFFERED, false)) {
            prefs.edit().putBoolean(PERMISSION_OFFERED, true).apply();
            try {
                context.startActivity(new android.content.Intent(context, salve.presentation.ui.DeviceControlActivity.class)
                        .putExtra(salve.presentation.ui.DeviceControlActivity.EXTRA_PANEL, true)
                        .putExtra(salve.services.MobileWorkspaceService.LIVE, true)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                return "Te he abierto el ajuste de la ventana flotante. Confirma el permiso y vuelve: la ventana continuará automáticamente.";
            } catch (RuntimeException unavailable) { /* Android may reject background activity launches. */ }
        }
        return "La búsqueda continúa. Para ver la ventana, di «muestra tus búsquedas» y confirma el permiso de Android.";
    }

    /** Called after the user grants the real system permission. No simulated clicks or permission grants. */
    public static boolean resume(Context context) {
        if (!enabled(context) || !canShow(context) || lowMemory(context)) return false;
        return pendingQuery.isEmpty() ? salve.services.MobileWorkspaceService.openLive(context)
                : salve.services.MobileWorkspaceService.open(context, pendingQuery);
    }

    /**
     * Enseña la búsqueda en la mini ventana. Devuelve null si se muestra (o está desactivado) y
     * una explicación breve si no se pudo.
     */
    public static String begin(Context context, String query) {
        if (query == null || query.trim().isEmpty() || !enabled(context)) return null;
        pendingQuery = query;
        Listener current = listener;
        if (current != null) { MAIN.post(() -> current.onSearch(query)); return null; }
        if (!canShow(context)) return "Si quieres ver mis búsquedas en directo, di «muestra tus búsquedas» y permite que aparezca sobre otras apps.";
        if (lowMemory(context)) return "No abro la mini ventana ahora: queda poca memoria y la necesito para pensar.";
        return salve.services.MobileWorkspaceService.open(context, query) ? null
                : "Android no me dejó abrir la mini ventana.";
    }

    /** Abre la ventana en directo sin búsqueda. Mismas condiciones que {@link #begin}. */
    public static String show(Context context) {
        if (!enabled(context)) return null;
        if (listener != null) return null;
        if (!canShow(context)) return "Para enseñarte lo que hago necesito permiso para aparecer sobre otras apps.";
        if (lowMemory(context)) return "Ahora queda poca memoria libre; abro la ventana cuando tenga más.";
        return salve.services.MobileWorkspaceService.openLive(context) ? null : "Android no me dejó abrir la mini ventana.";
    }

    /** Lo que está haciendo (solo si la ventana está abierta; si no, no cuesta nada). */
    public static void activity(String text) {
        if (text != null && !text.trim().isEmpty()) lastActivity = text.trim();
        Listener current = listener;
        if (current != null && text != null && !text.trim().isEmpty()) MAIN.post(() -> current.onActivity(text.trim()));
    }

    /** Su respuesta, para teclearla en la ventana (sin marcas internas de dibujo). */
    public static void typing(String text) {
        Listener current = listener;
        if (current == null || text == null) return;
        String clean = text.replaceAll("\\[\\[[^\\]]*\\]\\]", " ").replaceAll("\\s+", " ").trim();
        if (!clean.isEmpty()) MAIN.post(() -> current.onTyping(clean));
    }

    /** Borrador en curso: la ventana lo enseña tal cual va saliendo. */
    public static void draft(String text) {
        Listener current = listener;
        if (current != null && text != null && !text.trim().isEmpty()) MAIN.post(() -> current.onDraft(text));
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
