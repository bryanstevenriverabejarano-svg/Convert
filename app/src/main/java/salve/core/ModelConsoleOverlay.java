package salve.core;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Consola tipo "hacker" para mostrar el estado de los modelos LLM.
 * - Fondo negro
 * - Texto verde monoespaciado
 * - Solo visible mientras se descargan / descomprimen modelos
 */
public final class ModelConsoleOverlay {

    private static WeakReference<Activity> sActivityRef;
    private static FrameLayout sRoot;
    private static ScrollView sScroll;
    private static TextView sText;
    private static boolean sAttached = false;
    private static boolean sVisible = false;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ModelConsoleOverlay() {}

    /** Llamar una vez (por ejemplo en onCreate de tu Activity principal). */
    public static void attach(Activity activity) {
        sActivityRef = new WeakReference<>(activity);
        ensureViewTree();
    }

    /** Muestra la consola (si aún no estaba visible). */
    public static void show() {
        MAIN.post(() -> {
            ensureViewTree();
            if (sRoot == null) return;
            if (!sVisible) {
                sVisible = true;
                sRoot.setVisibility(View.VISIBLE);
                AlphaAnimation anim = new AlphaAnimation(0f, 1f);
                anim.setDuration(180);
                sRoot.startAnimation(anim);
            }
        });
    }

    /** Oculta la consola inmediatamente. */
    public static void hideImmediate() {
        MAIN.post(() -> {
            if (sRoot == null) return;
            sRoot.setVisibility(View.GONE);
            sVisible = false;
        });
    }

    /** Oculta la consola con un pequeño delay (suave). */
    public static void hideDelayed(long millis) {
        MAIN.postDelayed(() -> {
            if (!sVisible) return;
            AlphaAnimation anim = new AlphaAnimation(1f, 0f);
            anim.setDuration(220);
            sRoot.startAnimation(anim);
            MAIN.postDelayed(() -> {
                if (sRoot != null) sRoot.setVisibility(View.GONE);
                sVisible = false;
            }, 230);
        }, millis);
    }

    /** Limpia el texto actual. */
    public static void clear() {
        MAIN.post(() -> {
            if (sText != null) sText.setText("");
        });
    }

    /** Añade una línea a la consola. */
    public static void log(String line) {
        MAIN.post(() -> {
            ensureViewTree();
            if (sText == null) return;

            String prefix = timePrefix();
            String current = sText.getText().toString();
            if (!current.isEmpty()) current += "\n";
            sText.setText(current + prefix + line);

            // Auto-scroll al final
            MAIN.postDelayed(() -> {
                if (sScroll != null) {
                    sScroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            }, 50);
        });
    }

    // === Helpers internos ===

    private static void ensureViewTree() {
        if (sAttached) return;
        Activity a = (sActivityRef != null) ? sActivityRef.get() : null;
        if (a == null) return;

        ViewGroup parent = a.findViewById(android.R.id.content);
        if (parent == null) return;

        // Capa raíz flotante en la parte inferior
        sRoot = new FrameLayout(a);
        FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        rootLp.bottomMargin = dp(a, 16);
        rootLp.leftMargin = dp(a, 12);
        rootLp.rightMargin = dp(a, 12);
        sRoot.setLayoutParams(rootLp);

        // Caja con fondo "terminal"
        FrameLayout card = new FrameLayout(a);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardLp);
        card.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8));
        card.setBackgroundColor(0xE0000000); // negro semi-opaco

        // Scroll + texto
        sScroll = new ScrollView(a);
        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sScroll.setLayoutParams(scrollLp);

        sText = new TextView(a);
        sText.setTextColor(0xFF4CFF7A); // verde
        sText.setTextSize(11);
        sText.setTypeface(Typeface.MONOSPACE);
        sText.setLineSpacing(0f, 1.1f);
        sText.setMovementMethod(new ScrollingMovementMethod());
        sText.setMaxLines(10);

        sScroll.addView(sText);
        card.addView(sScroll);
        sRoot.addView(card);

        sRoot.setVisibility(View.GONE);
        parent.addView(sRoot);

        sAttached = true;
    }

    private static int dp(Activity a, int v) {
        float d = a.getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }

    private static String timePrefix() {
        long now = System.currentTimeMillis();
        int sec = (int) ((now / 1000L) % 60);
        int min = (int) ((now / 60000L) % 60);
        return String.format(Locale.US, "[%02d:%02d] ", min, sec);
    }
}
