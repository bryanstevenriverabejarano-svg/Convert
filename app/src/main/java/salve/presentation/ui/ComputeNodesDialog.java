package salve.presentation.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import java.util.concurrent.*;
import salve.core.SalveLLM;
import salve.core.compute.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

/** Owner-controlled registry; nothing in a generated reply can add or enable a node. */
public final class ComputeNodesDialog {
    private static final ExecutorService CHECKS = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "salve-node-check"); thread.setDaemon(true); return thread;
    });
    private ComputeNodesDialog() { }
    public static void show(Activity activity) {
        ComputeNodeStore store = new ComputeNodeStore(activity);
        SalveLLM engine = SalveLLM.getInstance(activity);
        List<ComputeNode> nodes = store.load();
        List<String> labels = new ArrayList<>();
        labels.add(engine.isDistributedEnabled() ? "Desactivar: usar solo el móvil" : "Activar nodos propios");
        labels.add("Añadir un nodo");
        for (ComputeNode n : nodes) labels.add(n.name + " · " + n.endpoint);
        new AlertDialog.Builder(activity).setTitle("Nodos de cómputo")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        if (engine.isDistributedEnabled()) { engine.setDistributedEnabled(false); show(activity); }
                        else if (nodes.isEmpty()) add(activity);
                        else new AlertDialog.Builder(activity).setTitle("Activar mis nodos")
                                .setMessage("Salve enviará tareas de texto pesadas a estos servidores, incluido el contexto y los recuerdos presentes en cada tarea. Úsalos en equipos tuyos o con permiso. Si fallan, responderá el móvil. Las fotos siguen en el móvil y este modo no usa Gemini.")
                                .setPositiveButton("Activar", (d, w) -> {
                                    try { engine.setDistributedEnabled(true); show(activity); }
                                    catch (RuntimeException error) { message(activity, error.getMessage()); }
                                }).setNegativeButton("Cancelar", null).show();
                    } else if (which == 1) add(activity);
                    else inspect(activity, nodes.get(which - 2));
                }).setNeutralButton("Estado", (d, w) -> message(activity, engine.getComputeStatus()
                        + "\n" + nodes.size() + " nodos registrados (no implica que estén conectados)."
                        + "\nCada nodo necesita el servicio salve_compute_node.py y su modelo en ejecución. Una web o una carpeta de almacenamiento no aportan cómputo por sí solas."))
                .setNegativeButton("Cerrar", null).show();
    }
    private static void add(Activity activity) {
        LinearLayout form = new LinearLayout(activity); form.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * activity.getResources().getDisplayMetrics().density); form.setPadding(pad, pad, pad, pad);
        TextView explanation = new TextView(activity);
        explanation.setText("Registra un servidor que controles. Necesita HTTPS con certificado válido y el token del componente de Salve. Añadirlo no activa los envíos automáticos."); form.addView(explanation);
        Button scan = button(activity, form, "Escanear QR del PC");
        Button find = button(activity, form, "Buscar en mi red");
        Button guide = button(activity, form, "Enviar script a mi PC");
        EditText name = field(activity, form, "Nombre: mi PC");
        EditText url = field(activity, form, "https://mi-servidor:8443");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        EditText token = field(activity, form, "Token del nodo");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        scan.setOnClickListener(v -> scanQr(activity, name, url, token));
        find.setOnClickListener(v -> discover(activity, name, url, token));
        guide.setOnClickListener(v -> NodeSetupShare.share(activity));
        ScrollView scroll = new ScrollView(activity); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Añadir nodo autorizado").setView(scroll)
                .setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                new ComputeNodeStore(activity).add(name.getText().toString(), url.getText().toString(), token.getText().toString());
                token.setText(""); dialog.dismiss(); show(activity);
            } catch (RuntimeException invalid) { token.setError(invalid.getMessage()); }
        })); dialog.show();
    }
    /** Rellena el formulario; guardar sigue siendo una decisión explícita del dueño. */
    private static void fill(NodePairing node, EditText name, EditText url, EditText token) {
        name.setText(node.name); url.setText(node.endpoint); url.setError(null);
        if (!node.token.isEmpty()) { token.setText(node.token); token.setError(null); }
        else { token.setText(""); token.requestFocus(); token.setError("Anunciado en tu red: comprueba que es tuyo y escribe su token o escanea su QR"); }
    }
    private static void scanQr(Activity activity, EditText name, EditText url, EditText token) {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        GmsBarcodeScanning.getClient(activity, options).startScan()
                .addOnSuccessListener(code -> {
                    try { fill(NodePairing.fromQr(code.getRawValue()), name, url, token); }
                    catch (IllegalArgumentException invalid) { url.setError(invalid.getMessage()); }
                })
                .addOnFailureListener(error -> message(activity, "No se pudo abrir el lector de QR. Necesita Google Play Services actualizado."));
    }
    private static void discover(Activity activity, EditText name, EditText url, EditText token) {
        List<NodePairing> found = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, new ArrayList<>());
        int pad = (int)(20 * activity.getResources().getDisplayMetrics().density);
        TextView status = new TextView(activity); status.setPadding(pad, pad / 2, pad, pad / 2);
        status.setText("Buscando nodos anunciados en tu Wi-Fi…");
        ListView list = new ListView(activity); list.addHeaderView(status, null, false); list.setAdapter(adapter);
        NodeDiscovery discovery = new NodeDiscovery(activity);
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Nodos en mi red").setView(list)
                .setNegativeButton("Cerrar", null).create();
        Runnable none = () -> { if (found.isEmpty()) status.setText("No hay nodos anunciados. En tu PC ejecuta:\npython salve_node_setup.py anunciar --url <tu-url>\nEl móvil y el PC deben estar en la misma Wi-Fi."); };
        list.setOnItemClickListener((parent, view, position, id) -> {
            fill(found.get(position - list.getHeaderViewsCount()), name, url, token); dialog.dismiss();
        });
        dialog.setOnDismissListener(d -> { handler.removeCallbacks(none); discovery.stop(); });
        discovery.start(new NodeDiscovery.Listener() {
            @Override public void onFound(NodePairing node) {
                for (NodePairing known : found) if (known.endpoint.equalsIgnoreCase(node.endpoint)) return;
                found.add(node); adapter.add(node.name + "\n" + node.endpoint);
                status.setText("Toca tu nodo. Solo se rellenan nombre y URL; el token lo pones tú.");
            }
            @Override public void onStopped(String problem) { if (problem != null && dialog.isShowing()) status.setText(problem); }
        });
        handler.postDelayed(none, 12_000); dialog.show();
    }
    private static Button button(Activity activity, LinearLayout parent, String text) {
        Button button = new Button(activity); button.setText(text); button.setAllCaps(false); parent.addView(button); return button;
    }
    private static EditText field(Activity activity, LinearLayout parent, String hint) {
        EditText field = new EditText(activity); field.setHint(hint); field.setSingleLine(true); parent.addView(field); return field;
    }
    private static void inspect(Activity activity, ComputeNode node) {
        new AlertDialog.Builder(activity).setTitle(node.name).setMessage(node.endpoint)
                .setPositiveButton("Probar", (d, w) -> probe(activity, node))
                .setNeutralButton("Retirar", (d, w) -> {
                    try { ComputeNodeStore store = new ComputeNodeStore(activity);
                        store.remove(node.id);
                        if (store.load().isEmpty()) SalveLLM.getInstance(activity).setDistributedEnabled(false);
                        show(activity); }
                    catch (RuntimeException error) { message(activity, error.getMessage()); }
                }).setNegativeButton("Cerrar", null).show();
    }
    private static void probe(Activity activity, ComputeNode node) {
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Probar " + node.name)
                .setMessage("Comprobando conexión y una respuesta breve…").setNegativeButton("Cerrar", null).show();
        ComputeNodeStore store = new ComputeNodeStore(activity); long revision = store.revision();
        Future<?> pending = CHECKS.submit(() -> {
            String result;
            try {
                ComputeHttpTransport client = new ComputeHttpTransport();
                java.util.function.BooleanSupplier stopped = () -> Thread.currentThread().isInterrupted() || store.revision() != revision;
                ComputeRouter.Health health = client.health(node, stopped);
                if (health.freeSlots == 0) result = "Conectado, pero ocupado. Modelo: " + health.model;
                else {
                    String text = client.generate(node, health, UUID.randomUUID().toString(), "CONVERSACIONAL", "Responde únicamente con un saludo breve en español.", stopped);
                    result = text == null || text.trim().isEmpty() ? "El nodo no devolvió texto." : "Respuesta comprobada · " + health.model + "\n\n" + text;
                }
            } catch (Exception unavailable) { result = "No se pudo verificar el nodo. Revisa el servicio, HTTPS, el token y el modelo."; }
            String message = result;
            activity.runOnUiThread(() -> { if (!activity.isFinishing() && !activity.isDestroyed() && dialog.isShowing()) dialog.setMessage(message); });
        });
        dialog.setOnDismissListener(d -> pending.cancel(true));
    }
    private static void message(Activity activity, String text) {
        new AlertDialog.Builder(activity).setTitle("Cómputo de Salve").setMessage(text).setPositiveButton("Cerrar", null).show();
    }
}
