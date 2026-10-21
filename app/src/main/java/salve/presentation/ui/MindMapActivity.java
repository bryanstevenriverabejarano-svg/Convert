package salve.presentation.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Mapa de la mente de Salve dentro de la app: la página se genera en segundo plano con sus datos
 * y se muestra en un WebView sin red (no carga nada de internet ni comparte el archivo).
 */
public final class MindMapActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Mapa de su mente");
        TextView wait = new TextView(this);
        wait.setText("Leyendo mi mente…"); wait.setTextColor(0xFFDCECFF); wait.setGravity(Gravity.CENTER);
        wait.setBackgroundColor(0xFF0B1420); wait.setTextSize(16);
        setContentView(wait);
        new Thread(() -> {
            String page;
            try {
                java.io.File file = salve.core.MindRuntime.get(getApplicationContext()).exportMindMap();
                page = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } catch (Exception failed) {
                page = null;
            }
            String html = page;
            main.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (html == null) { wait.setText("No pude leer mi mente ahora. Inténtalo en un momento."); return; }
                WebView web = new WebView(this);
                web.setBackgroundColor(Color.rgb(11, 20, 32));
                WebSettings s = web.getSettings();
                s.setJavaScriptEnabled(true);
                s.setAllowFileAccess(false); s.setAllowContentAccess(false);
                s.setBlockNetworkLoads(true); s.setBlockNetworkImage(true);
                web.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView v, android.webkit.WebResourceRequest r) { return true; }
                });
                web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                setContentView(web);
            });
        }, "Salve-MapaMente").start();
    }
}
