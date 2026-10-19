package salve.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import salve.core.SalveLLM;
import salve.core.compute.ComputeHttpTransport;
import salve.core.compute.ComputeNode;
import salve.core.compute.ComputeNodeStore;
import salve.core.compute.ConstellationModel;
import salve.core.compute.NodeStatusCheck;

/**
 * Vista de constelación de los nodos propios. Comprueba su estado solo al abrirse o al pulsar
 * «Comprobar ahora»; nada se consulta en segundo plano. La ubicación es una etiqueta local.
 */
public final class NodeConstellationActivity extends Activity {
    private static final int BG = 0xFF02060C, CYAN = 0xFF22D3EE, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8;

    private final ExecutorService checks = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "salve-constellation"); thread.setDaemon(true); return thread;
    });
    private final Map<String, NodeStatusCheck.Result> results = new HashMap<>();
    private final Map<String, Long> checkedAt = new HashMap<>();
    private ComputeNodeStore store;
    private SalveLLM engine;
    private List<ComputeNode> nodes = new ArrayList<>();
    private NodeConstellationView constellation;
    private TextView summary;
    private Button sending;
    private Future<?> pending;
    private int generation;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Constelación de nodos");
        store = new ComputeNodeStore(this);
        engine = SalveLLM.getInstance(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        int pad = dp(16);
        root.setPadding(pad, dp(18), pad, pad);

        TextView title = new TextView(this);
        title.setText("CONSTELACIÓN DE NODOS");
        title.setTextColor(CYAN); title.setTextSize(15); title.setLetterSpacing(0.18f);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        root.addView(title);
        summary = new TextView(this);
        summary.setTextColor(MUTED); summary.setTextSize(13);
        summary.setPadding(0, dp(4), 0, dp(8));
        root.addView(summary);

        constellation = new NodeConstellationView(this);
        constellation.setListener(this::showCard);
        root.addView(constellation, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(10), 0, 0);
        actions.addView(button("Comprobar ahora", v -> refresh()), weight());
        sending = button("", v -> toggleSending());
        actions.addView(sending, weight());
        actions.addView(button("Cerrar", v -> finish()), weight());
        root.addView(actions);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        nodes = new ArrayList<>(store.load());
        updateSendingButton();
        refresh();
    }

    @Override protected void onPause() {
        super.onPause();
        if (pending != null) pending.cancel(true);
        generation++;
    }

    @Override protected void onDestroy() { checks.shutdownNow(); super.onDestroy(); }

    /** Comprueba los nodos activos; los desactivados no se contactan. */
    private void refresh() {
        if (pending != null) pending.cancel(true);
        int current = ++generation;
        List<ComputeNode> enabled = new ArrayList<>();
        for (ComputeNode node : nodes) {
            results.remove(node.id);
            if (store.nodeEnabled(node.id)) enabled.add(node);
        }
        render();
        summary.setText(nodes.isEmpty() ? "Sin nodos registrados." : enabled.isEmpty()
                ? "Todos los nodos están desactivados." : "Comprobando " + enabled.size() + (enabled.size() == 1 ? " nodo…" : " nodos…"));
        if (enabled.isEmpty()) return;
        long revision = store.revision();
        pending = checks.submit(() -> {
            List<NodeStatusCheck.Result> found = NodeStatusCheck.run(enabled, new ComputeHttpTransport(),
                    () -> Thread.currentThread().isInterrupted() || store.revision() != revision,
                    () -> System.nanoTime() / 1_000_000L);
            runOnUiThread(() -> {
                if (current != generation || isFinishing() || isDestroyed()) return;
                long now = System.currentTimeMillis();
                for (int i = 0; i < found.size(); i++) {
                    results.put(enabled.get(i).id, found.get(i));
                    checkedAt.put(enabled.get(i).id, now);
                }
                int online = 0;
                for (NodeStatusCheck.Result r : found) if (r.online) online++;
                summary.setText(online + " de " + enabled.size() + " nodos activos responden · núcleo: este móvil");
                render();
            });
        });
    }

    private void render() {
        List<NodeConstellationView.Item> items = new ArrayList<>();
        for (ComputeNode node : nodes)
            items.add(new NodeConstellationView.Item(node.name, store.locationLabel(node.id), stateOf(node)));
        constellation.setItems(items);
    }

    private ConstellationModel.State stateOf(ComputeNode node) {
        return ConstellationModel.stateOf(results.get(node.id), store.nodeEnabled(node.id));
    }

    /** Ficha del nodo: datos de la última comprobación y acciones sobre ese nodo. */
    private void showCard(int index) {
        if (index < 0 || index >= nodes.size()) return;
        ComputeNode node = nodes.get(index);
        NodeStatusCheck.Result result = results.get(node.id);
        ConstellationModel.State state = stateOf(node);
        boolean enabled = store.nodeEnabled(node.id);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(12), dp(20), dp(4));
        String location = store.locationLabel(node.id);
        row(card, "Estado", ConstellationModel.label(state));
        row(card, "Ubicación", location.isEmpty() ? "Sin etiqueta" : location);
        if (result != null && result.online) {
            row(card, "Modelo", result.model);
            row(card, "Plazas libres", String.valueOf(result.freeSlots));
            row(card, "Latencia", result.millis + " ms");
        } else if (result != null) {
            row(card, "Motivo", result.problem);
        }
        Long at = checkedAt.get(node.id);
        if (at != null) row(card, "Comprobado", "hace " + Math.max(0, (System.currentTimeMillis() - at) / 1000) + " s");
        row(card, "Dirección", node.endpoint);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(node.name).setView(card)
                .setNegativeButton("Cerrar", null).create();
        card.addView(button("Probar ahora", v -> { dialog.dismiss(); ComputeNodesDialog.probe(this, node); }));
        card.addView(button(enabled ? "Desactivar este nodo" : "Activar este nodo", v -> {
            try {
                store.setNodeEnabled(node.id, !enabled);
                dialog.dismiss();
                refresh();
                if (!enabled && !engine.isDistributedEnabled())
                    summary.setText("Nodo activado. Los envíos a nodos están desactivados: actívalos abajo para usarlo.");
            } catch (RuntimeException failure) { summary.setText(failure.getMessage()); }
        }));
        card.addView(button("Editar ubicación", v -> { dialog.dismiss(); editLocation(node); }));
        dialog.show();
    }

    private void editLocation(ComputeNode node) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Casa — salón, Oficina…");
        input.setText(store.locationLabel(node.id));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog editor = new AlertDialog.Builder(this).setTitle("Ubicación de " + node.name)
                .setMessage("Solo se guarda en este móvil. El nodo no la conoce ni la envía.")
                .setView(box).setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();
        editor.setOnShowListener(d -> editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            try { store.setLocationLabel(node.id, input.getText().toString()); editor.dismiss(); render(); }
            catch (IllegalArgumentException invalid) { input.setError(invalid.getMessage()); }
        }));
        editor.show();
    }

    private void toggleSending() {
        try {
            if (!engine.isDistributedEnabled() && store.loadActive().isEmpty()) {
                summary.setText("Activa al menos un nodo antes de encender los envíos.");
                return;
            }
            engine.setDistributedEnabled(!engine.isDistributedEnabled());
        } catch (RuntimeException failure) { summary.setText(failure.getMessage()); }
        updateSendingButton();
    }

    private void updateSendingButton() {
        sending.setText(engine.isDistributedEnabled() ? "Envíos: activados" : "Envíos: desactivados");
    }

    private void row(LinearLayout parent, String label, String value) {
        TextView view = new TextView(this);
        view.setText(label + ": " + value);
        view.setTextSize(14);
        view.setPadding(0, dp(3), 0, dp(3));
        view.setTextIsSelectable(true);
        parent.addView(view);
    }

    private Button button(String text, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(text); button.setAllCaps(false); button.setOnClickListener(click);
        return button;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }
}
