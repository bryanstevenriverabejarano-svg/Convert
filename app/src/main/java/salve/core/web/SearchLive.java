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
        if (!canShow(context)) return askOverlay(context, query);
        if (lowMemory(context)) return "No abro la mini ventana ahora: queda poca memoria y la necesito para pensar.";
        return salve.services.MobileWorkspaceService.open(context, query) ? null
                : "Android no me dejó abrir la mini ventana.";
    }

    /** Abre la ventana en directo sin búsqueda. Mismas condiciones que {@link #begin}. */
    public static String show(Context context) {
        if (!enabled(context)) return null;
        if (listener != null) return null;
        if (!canShow(context)) return askOverlay(context, LIVE_ONLY);
        if (lowMemory(context)) return "Ahora queda poca memoria libre; abro la ventana cuando tenga más.";
        return salve.services.MobileWorkspaceService.openLive(context) ? null : "Android no me dejó abrir la mini ventana.";
    }

    private static final String PENDING = "pending_window", ASKED_AT = "overlay_asked_at", LIVE_ONLY = "\u0000live";
    /** No vuelve a abrir el ajuste solo más de una vez cada 12 h si Bryan no lo activó. */
    private static final long ASK_EVERY_MS = 12L * 60 * 60 * 1000;

    /**
     * Android no deja que una app se dé a sí misma el permiso de aparecer sobre otras apps: lo
     * activa Bryan una vez. Salve abre directamente ese ajuste, recuerda qué quería enseñar y,
     * al volver con el permiso dado, abre la ventana sola ({@link #resumePending}).
     */
    static String askOverlay(Context context, String pending) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(PENDING, pending).apply();
        long now = System.currentTimeMillis();
        if (now - prefs.getLong(ASKED_AT, 0) < ASK_EVERY_MS)
            return "Mi ventana está esperando el permiso «Mostrar sobre otras apps»; cuando lo actives, la abro sola.";
        try {
            context.startActivity(new android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + context.getPackageName()))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            prefs.edit().putLong(ASKED_AT, now).apply();
            return "Te abro el ajuste para poder enseñarte mi ventana: activa «Mostrar sobre otras apps» para Salve y vuelve. "
                    + "Solo hace falta una vez; después abro mis ventanas yo sola.";
        } catch (RuntimeException unavailable) {
            return "Activa «Mostrar sobre otras apps» para Salve en Ajustes y abriré mis ventanas yo sola.";
        }
    }

    /** Al volver a Salve: si ya hay permiso y quedó algo por enseñar, lo abre sin que se lo pidan. */
    public static void resumePending(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String pending = prefs.getString(PENDING, null);
        if (pending == null || !canShow(context) || !enabled(context)) return;
        prefs.edit().remove(PENDING).remove(ASKED_AT).apply();
        if (listener != null) { if (!LIVE_ONLY.equals(pending)) { Listener l = listener; MAIN.post(() -> l.onSearch(pending)); } return; }
        if (LIVE_ONLY.equals(pending)) salve.services.MobileWorkspaceService.openLive(context);
        else salve.services.MobileWorkspaceService.open(context, pending);
    }

    /** Lo que está haciendo (solo si la ventana está abierta; si no, no cuesta nada). */
    public static void activity(String text) {
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
