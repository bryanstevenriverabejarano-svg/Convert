package salve.presentation.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.compute.ComputeHttpTransport;
import salve.core.compute.ComputeNode;
import salve.core.compute.ComputeNodeStore;

/**
 * Mi casa: los dispositivos que Bryan permitió en Salve Nodo (Home Assistant local). Cada botón es
 * una orden explícita; Salve no actúa sobre la casa por su cuenta. Sin nubes: móvil → nodo (TLS
 * mutuo) → Home Assistant en la red de casa.
 */
public final class HomeActivity extends AppCompatActivity {
    private static final int BG = 0xFF02060C, PANEL = 0xFF071622, CYAN = 0xFF22D3EE, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8;
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private LinearLayout list;
    private TextView status;
    private ComputeNode homeNode;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        TextView title = text("MI CASA", CYAN, 15, true); root.addView(title);
        TextView about = text("Solo aparecen los dispositivos que permitiste en Salve Nodo. Por voz también funciona: "
                + "«enciende la luz del salón», y te pido confirmación antes.", MUTED, 13, false);
        about.setPadding(0, dp(6), 0, dp(10)); root.addView(about);
        status = text("Buscando tu Home Assistant…", MUTED, 13, false); root.addView(status);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        LinearLayout bar = new LinearLayout(this);
        bar.addView(button("Actualizar", this::load), weight()); bar.addView(button("Cerrar", this::finish), weight());
        root.addView(bar);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.addView(root);
        setContentView(scroll);
        load();
    }

    private void load() {
        status.setText("Buscando tu Home Assistant…");
        work.execute(() -> {
            ComputeHttpTransport transport = new ComputeHttpTransport(this);
            JsonObject found = null; ComputeNode node = null; String problem = null;
            for (ComputeNode n : new ComputeNodeStore(this).load()) {
                try { found = transport.home(n, this::isDestroyed); node = n; break; }
                catch (Exception failure) {
                    if (failure.getMessage() == null || !failure.getMessage().contains("(404)")) problem = "No pude hablar con " + n.name + ".";
                }
            }
            final JsonObject data = found; final ComputeNode owner = node; final String error = problem;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                homeNode = owner; list.removeAllViews();
                if (data == null) {
                    status.setText(error != null ? error + " ¿Está abierto Salve Nodo?"
                            : "Ninguno de tus nodos tiene Home Assistant. Actívalo en Salve Nodo con --reconfigurar.");
                    return;
                }
                status.setText("Desde " + owner.name + ":");
                for (JsonElement el : data.getAsJsonArray("entities")) list.addView(card(el.getAsJsonObject()));
            });
        });
    }

    private LinearLayout card(JsonObject e) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(PANEL); bg.setCornerRadius(dp(10)); card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4)); card.setLayoutParams(lp);
        String entity = e.get("entity").getAsString();
        TextView name = text(e.get("name").getAsString() + " · " + e.get("state").getAsString(), INK, 15, false);
        card.addView(name);
        LinearLayout actions = new LinearLayout(this);
        List<String> services = new java.util.ArrayList<>();
        for (JsonElement s : e.getAsJsonArray("services")) services.add(s.getAsString());
        for (String service : services) {
            String label = label(service);
            if (label == null) continue;
            actions.addView(button(label, () -> act(entity, service, name)), weight());
        }
        card.addView(actions);
        return card;
    }

    private void act(String entity, String service, TextView name) {
        if (homeNode == null) return;
        ComputeNode node = homeNode;
        status.setText("Enviando…");
        work.execute(() -> {
            String message;
            try {
                JsonObject result = new ComputeHttpTransport(this).homeAction(node, entity, service, null, this::isDestroyed);
                JsonObject e = result.getAsJsonObject("entity");
                String text = e.get("name").getAsString() + " · " + e.get("state").getAsString();
                runOnUiThread(() -> name.setText(text));
                message = "Hecho.";
            } catch (Exception failure) {
                String m = failure.getMessage() == null ? "" : failure.getMessage();
                message = m.contains("(429)") ? "Demasiados cambios seguidos; espera un minuto." : "No se pudo: " + m;
            }
            final String done = message;
            runOnUiThread(() -> { if (!isDestroyed()) status.setText(done); });
        });
    }

    /** La temperatura se ajusta por voz («pon la calefacción a 21 grados»): necesita un valor. */
    private static String label(String service) {
        switch (service) {
            case "turn_on": return "Encender";
            case "turn_off": return "Apagar";
            case "toggle": return "Alternar";
            case "open_cover": return "Abrir";
            case "close_cover": return "Cerrar";
            case "stop_cover": return "Parar";
            default: return null;
        }
    }

    private TextView text(String value, int color, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size);
        if (bold) { t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); t.setLetterSpacing(0.12f); }
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v -> action.run()); return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), dp(2), dp(2), dp(2)); return p;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() { work.shutdownNow(); super.onDestroy(); }
}
