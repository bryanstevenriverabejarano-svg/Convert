package salve.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/** Literal text view, never an LLM summary or HTML renderer. Disk I/O stays off the UI thread. */
public class ExecutionJournalActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable poll = this::refresh;
    private final List<ExecutionJournal.Task> tasks = new ArrayList<>();
    private Spinner selector;
    private LinearLayout reportsPanel, executionPanel;
    private final List<java.io.File> reportFiles = new ArrayList<>();
    private Spinner reportSelector;
    private TextView log, notice;
    private String selected = "", shown = "";
    private boolean visible, loading;
    private long revision;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) selected = state.getString("selected", "");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20); root.setBackgroundColor(Color.rgb(15, 30, 43));
        TextView title = text(root, "Registro de ejecución", 23);
        text(root, "Peticiones, operaciones y resultados de dibujos, búsquedas y sueño autónomo. Se conservan las últimas 20 tareas en este dispositivo. Solo se muestra actividad registrada y verificable; no es una transcripción del pensamiento interno del modelo.", 14);
        LinearLayout tabs = new LinearLayout(this); root.addView(tabs);
        Button executionTab = new Button(this); executionTab.setText("Ejecución"); tabs.addView(executionTab);
        Button reportsTab = new Button(this); reportsTab.setText("Informes"); tabs.addView(reportsTab);
        executionPanel = new LinearLayout(this); executionPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(executionPanel);
        reportsPanel = new LinearLayout(this); reportsPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(reportsPanel);
        reportsPanel.setVisibility(View.GONE);
        executionTab.setOnClickListener(v -> { executionPanel.setVisibility(View.VISIBLE); reportsPanel.setVisibility(View.GONE); });
        reportsTab.setOnClickListener(v -> { executionPanel.setVisibility(View.GONE); reportsPanel.setVisibility(View.VISIBLE); refreshReports(); });
        text(reportsPanel, "Informes PDF de propuestas e investigaciones. Solo se crean desde actividad registrada; las hipótesis no son cambios comprobados.", 14);
        reportSelector = new Spinner(this); reportsPanel.addView(reportSelector);
        Button generate = new Button(this); generate.setText("Crear informe de la tarea seleccionada"); reportsPanel.addView(generate);
        generate.setOnClickListener(v -> createReport());
        Button preview = new Button(this); preview.setText("Previsualizar informe"); reportsPanel.addView(preview);
        preview.setOnClickListener(v -> previewReport());
        selector = new Spinner(this); executionPanel.addView(selector);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < tasks.size()) { selected = tasks.get(position).id; render(); }
            }
        });
        LinearLayout actions = new LinearLayout(this); executionPanel.addView(actions);
        Button copy = new Button(this); copy.setText("Copiar"); actions.addView(copy);
        copy.setOnClickListener(v -> {
            if (!shown.isEmpty()) {
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Registro de Salve", shown));
                Toast.makeText(this, "Registro copiado", Toast.LENGTH_SHORT).show();
            }
        });
        Button clear = new Button(this); clear.setText("Borrar"); actions.addView(clear);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("¿Borrar el registro local de ejecución?")
                .setNegativeButton("Cancelar", null).setPositiveButton("Borrar", (d, w) -> {
                    revision++; selected = ""; shown = ""; tasks.clear(); log.setText("");
                    worker.execute(() -> { ExecutionTrace.journal(this).clear(); main.post(this::refresh); });
                }).show());
        Button back = new Button(this); back.setText("Volver"); actions.addView(back); back.setOnClickListener(v -> finish());
        notice = text(root, "", 14);
        ScrollView scroll = new ScrollView(this); executionPanel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        log = new TextView(this); log.setTextColor(Color.rgb(230, 241, 247)); log.setTextSize(15);
        log.setTextIsSelectable(true); log.setPadding(0, 12, 0, 30); scroll.addView(log);
        setContentView(root);
        refreshReports();
    }

    private java.io.File reportDirectory() {
        java.io.File dir = new java.io.File(getFilesDir(), "salve-improvement-reports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void refreshReports() {
        java.io.File[] files = reportDirectory().listFiles((dir, name) ->
                name.startsWith("informe-") && name.endsWith(".pdf"));
        reportFiles.clear();
        if (files != null) {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            reportFiles.addAll(java.util.Arrays.asList(files));
        }
        List<String> labels = new ArrayList<>();
        for (java.io.File file : reportFiles) labels.add(file.getName());
        if (labels.isEmpty()) labels.add("Todavía no hay informes");
        reportSelector.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void createReport() {
        ExecutionJournal.Task chosen = null;
        for (ExecutionJournal.Task task : tasks) if (task.id.equals(selected)) chosen = task;
        if (chosen == null) {
            Toast.makeText(this, "Selecciona primero una tarea del registro", Toast.LENGTH_SHORT).show();
            return;
        }
        final String evidence = ExecutionJournal.transcript(chosen);
        final String type = chosen.type;
        Toast.makeText(this, "Salve está preparando el informe…", Toast.LENGTH_SHORT).show();
        worker.execute(() -> {
            try {
                salve.core.SalveLLM llm = salve.core.SalveLLM.getInstance(getApplicationContext());
                String analysis = "No se ha generado análisis del modelo: modelo no disponible.";
                if (!llm.isLoading() && llm.activeModelPath() != null) {
                    String prompt = "Eres Salve. Redacta TU informe técnico en español sobre mejoras posibles "
                            + "basándote ÚNICAMENTE en este registro de ejecución. No inventes inspecciones "
                            + "de código, experimentos, resultados, archivos ni fuentes. Distingue hechos "
                            + "observados, hipótesis, mejoras sugeridas, riesgos y pruebas necesarias. "
                            + "Si no hay evidencia de una mejora, dilo. No ejecutes cambios.\n\n"
                            + evidence.substring(0, Math.min(8500, evidence.length()));
                    salve.core.ModelResult result = llm.generateResult(prompt, salve.core.SalveLLM.Role.PLANIFICADOR);
                    if (result != null && result.isSuccess() && result.getText() != null
                            && !result.getText().trim().isEmpty()) analysis = result.getText();
                    else analysis = "El modelo no pudo producir un análisis verificable.";
                }
                String content = "INFORME DE SALVE: " + type + "\n\n"
                        + "Fuente: Registro de ejecución local\n"
                        + "Estado: Propuesta no implementada ni verificada.\n\n"
                        + "ANÁLISIS Y PROPUESTAS REDACTADAS POR SALVE\n\n" + analysis
                        + "\n\nEVIDENCIA ORIGINAL (sin modificaciones)\n\n" + evidence;
                java.io.File generated = new salve.core.PdfGenerator(this)
                        .crearPdfDesdeTexto(content, "informe-salve");
                java.io.File dest = new java.io.File(reportDirectory(),
                        "informe-" + System.currentTimeMillis() + ".pdf");
                java.nio.file.Files.copy(generated.toPath(), dest.toPath());
                generated.delete();
                main.post(() -> {
                    refreshReports();
                    Toast.makeText(this, "Informe de Salve guardado en la biblioteca", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                main.post(() -> Toast.makeText(this, "No se pudo crear el informe: "
                        + error.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void previewReport() {
        int index = reportSelector.getSelectedItemPosition();
        if (index < 0 || index >= reportFiles.size()) return;
        java.io.File file = reportFiles.get(index);
        try {
            android.os.ParcelFileDescriptor fd = android.os.ParcelFileDescriptor.open(file,
                    android.os.ParcelFileDescriptor.MODE_READ_ONLY);
            android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(fd);
            final int[] pageIndex = {0};
            ImageView image = new ImageView(this);
            image.setAdjustViewBounds(true);
            image.setBackgroundColor(Color.WHITE);
            ScrollView scroll = new ScrollView(this);
            scroll.addView(image);
            Runnable show = () -> {
                try (android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(pageIndex[0])) {
                    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                            page.getWidth() * 2, page.getHeight() * 2,
                            android.graphics.Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    page.render(bitmap, null, null,
                            android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    image.setImageBitmap(bitmap);
                }
            };
            show.run();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(file.getName()).setView(scroll)
                    .setNegativeButton("Cerrar", null)
                    .setPositiveButton("Siguiente", null).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        pageIndex[0] = (pageIndex[0] + 1) % renderer.getPageCount();
                        show.run();
                    }));
            dialog.setOnDismissListener(d -> {
                renderer.close();
                try { fd.close(); } catch (Exception ignored) { }
            });
            dialog.show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo previsualizar el PDF", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(LinearLayout parent, String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.rgb(230, 241, 247)); parent.addView(view); return view;
    }
    @Override protected void onResume() { super.onResume(); visible = true; refresh(); }
    @Override protected void onPause() { visible = false; main.removeCallbacks(poll); super.onPause(); }
    @Override protected void onDestroy() { main.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("selected", selected); super.onSaveInstanceState(state); }
    private void refresh() {
        if (!visible || loading || isFinishing() || worker.isShutdown()) return;
        loading = true; long version = revision;
        worker.execute(() -> {
            ExecutionJournal journal = ExecutionTrace.journal(this);
            List<ExecutionJournal.Task> read = journal.recent(); String warning = journal.notice();
            main.post(() -> {
                loading = false;
                if (isDestroyed()) return;
                if (version == revision) {
                    List<String> labels = new ArrayList<>(); int position = 0;
                    boolean changed = tasks.size() != read.size();
                    for (int i = 0; i < read.size(); i++) {
                        ExecutionJournal.Task task = read.get(i);
                        labels.add(task.type + " · " + ExecutionJournal.date(task.started));
                        if (task.id.equals(selected)) position = i;
                        if (i >= tasks.size() || !tasks.get(i).id.equals(task.id)) changed = true;
                    }
                    tasks.clear(); tasks.addAll(read);
                    selected = read.isEmpty() ? "" : read.get(position).id;
                    if (changed) {
                        selector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
                        selector.setSelection(position);
                    }
                    notice.setText(warning); render();
                }
                if (visible) { main.removeCallbacks(poll); main.postDelayed(poll, 1500); }
            });
        });
    }
    private void render() {
        String next = "";
        for (ExecutionJournal.Task task : tasks) if (task.id.equals(selected)) next = ExecutionJournal.transcript(task);
        if (!next.equals(shown) || next.isEmpty()) { shown = next; log.setText(next.isEmpty() ? "Todavía no hay tareas registradas." : next); }
    }
}
