package salve.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.reports.AutonomousReportWriter;
import salve.core.reports.ReportLibrary;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/**
 * Dos pestañas: «Ejecución» (texto literal del registro, nunca un resumen del modelo) y «Biblioteca»
 * (los informes PDF de Salve: mejoras de su código, investigaciones, sueño y los que pides en el chat).
 * La lectura de disco va fuera del hilo de la interfaz.
 */
public class ExecutionJournalActivity extends Activity {
    /** Pestaña con la que abrir: {@link ReportLibrary#TAB_LIBRARY} o {@link ReportLibrary#TAB_EXECUTION}. */
    public static final String EXTRA_TAB = "salve.journal.tab";

    private static final int BG = Color.rgb(15, 30, 43), CARD = Color.rgb(25, 45, 62), LINE = Color.rgb(48, 74, 94),
            TEXT = Color.rgb(230, 241, 247), MUTED = Color.rgb(150, 175, 190), ACCENT = Color.rgb(125, 227, 208);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable poll = this::refresh;
    private final List<ExecutionJournal.Task> tasks = new ArrayList<>();
    private Spinner selector;
    private LinearLayout executionPanel, libraryPanel, libraryList, chips, evolutionPanel, evolutionList;
    private TextView executionTab, libraryTab, evolutionTab, log, notice, libraryCount;
    private String selected = "", shown = "", tab = ReportLibrary.TAB_EXECUTION;
    private ReportLibrary.Category filter;
    private boolean visible, loading;
    private long revision;

    public static Intent library(Context context) {
        return new Intent(context, ExecutionJournalActivity.class).putExtra(EXTRA_TAB, ReportLibrary.TAB_LIBRARY);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) { selected = state.getString("selected", ""); tab = state.getString("tab", tab); }
        else if (ReportLibrary.TAB_LIBRARY.equals(getIntent().getStringExtra(EXTRA_TAB))) tab = ReportLibrary.TAB_LIBRARY;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(8)); root.setBackgroundColor(BG);
        TextView title = text(root, "Registro de Salve", 23); title.setTypeface(Typeface.DEFAULT_BOLD);

        // ── Pestañas ──
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(0, dp(10), 0, 0);
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        executionTab = tab(tabs, "Ejecución", ReportLibrary.TAB_EXECUTION);
        libraryTab = tab(tabs, "Biblioteca", ReportLibrary.TAB_LIBRARY);
        evolutionTab = tab(tabs, "Evolución", "evolucion");
        View rule = new View(this); rule.setBackgroundColor(LINE);
        root.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));

        buildExecution(root);
        buildLibrary(root);
        buildEvolution(root);
        notice = text(root, "", 13); notice.setTextColor(MUTED);
        setContentView(root);
        showTab(tab);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ReportLibrary.TAB_LIBRARY.equals(intent.getStringExtra(EXTRA_TAB))) showTab(ReportLibrary.TAB_LIBRARY);
    }

    /** Una pestaña: el nombre y, debajo, una raya que se colorea cuando está activa. */
    private TextView tab(LinearLayout tabs, String label, String key) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(label); t.setTextSize(16); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(12), dp(8), dp(10));
        View underline = new View(this);
        LinearLayout.LayoutParams ul = new LinearLayout.LayoutParams(-1, dp(3));
        ul.setMargins(dp(24), 0, dp(24), 0);
        box.addView(t);
        box.addView(underline, ul);
        t.setTag(underline);
        box.setOnClickListener(v -> showTab(key));
        t.setOnClickListener(v -> showTab(key));
        tabs.addView(box, new LinearLayout.LayoutParams(0, -2, 1f));
        return t;
    }

    private void showTab(String key) {
        tab = "evolucion".equals(key) ? "evolucion" : ReportLibrary.TAB_LIBRARY.equals(key) ? ReportLibrary.TAB_LIBRARY : ReportLibrary.TAB_EXECUTION;
        boolean library = tab.equals(ReportLibrary.TAB_LIBRARY);
        styleTab(executionTab, ReportLibrary.TAB_EXECUTION.equals(tab));
        styleTab(libraryTab, library);
        styleTab(evolutionTab, "evolucion".equals(tab));
        executionPanel.setVisibility(ReportLibrary.TAB_EXECUTION.equals(tab) ? View.VISIBLE : View.GONE);
        libraryPanel.setVisibility(library ? View.VISIBLE : View.GONE);
        evolutionPanel.setVisibility("evolucion".equals(tab) ? View.VISIBLE : View.GONE);
        if (library) refreshLibrary();
        if ("evolucion".equals(tab)) refreshEvolution();
    }

    /** Pestaña activa: texto en color y raya debajo, como las de un navegador. */
    private void styleTab(TextView t, boolean active) {
        t.setTextColor(active ? ACCENT : MUTED);
        t.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        ((View) t.getTag()).setBackgroundColor(active ? ACCENT : Color.TRANSPARENT);
    }

    // ───────────────────────── Ejecución ─────────────────────────

    private void buildExecution(LinearLayout root) {
        executionPanel = new LinearLayout(this); executionPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(executionPanel, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView about = text(executionPanel, "Peticiones, operaciones y resultados de dibujos, búsquedas y sueño autónomo. "
                + "Se conservan las últimas 20 tareas. Solo actividad registrada y verificable; no es el pensamiento interno del modelo.", 13);
        about.setTextColor(MUTED); about.setPadding(0, dp(8), 0, dp(4));
        selector = new Spinner(this); executionPanel.addView(selector);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < tasks.size()) { selected = tasks.get(position).id; render(); }
            }
        });
        LinearLayout actions = new LinearLayout(this); executionPanel.addView(actions);
        button(actions, "Copiar", v -> {
            if (!shown.isEmpty()) {
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Registro de Salve", shown));
                Toast.makeText(this, "Registro copiado", Toast.LENGTH_SHORT).show();
            }
        });
        button(actions, "Informe PDF", v -> createReport());
        button(actions, "Borrar", v -> new AlertDialog.Builder(this).setMessage("¿Borrar el registro local de ejecución? Los informes de la biblioteca no se borran.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Borrar", (d, w) -> {
                    revision++; selected = ""; shown = ""; tasks.clear(); log.setText("");
                    worker.execute(() -> { ExecutionTrace.journal(this).clear(); main.post(this::refresh); });
                }).show());
        ScrollView scroll = new ScrollView(this);
        executionPanel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        log = new TextView(this); log.setTextColor(TEXT); log.setTextSize(15);
        log.setTextIsSelectable(true); log.setPadding(0, dp(8), 0, dp(24)); scroll.addView(log);
    }

    /** Pide a Salve un informe PDF de la tarea elegida; va a la biblioteca. */
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
            salve.core.SalveLLM llm = salve.core.SalveLLM.getInstance(getApplicationContext());
            String analysis = "No se ha generado análisis del modelo: modelo no disponible.";
            try {
                if (!llm.isLoading() && llm.activeModelPath() != null) {
                    String prompt = "Eres Salve. Redacta TU informe técnico en español sobre mejoras posibles "
                            + "basándote ÚNICAMENTE en este registro de ejecución. No inventes inspecciones "
                            + "de código, experimentos, resultados, archivos ni fuentes. Distingue hechos "
                            + "observados, hipótesis, mejoras sugeridas, riesgos y pruebas necesarias. "
                            + "Si no hay evidencia de una mejora, dilo. No ejecutes cambios.\n\n"
                            + evidence.substring(0, Math.min(8500, evidence.length()));
                    salve.core.ModelResult result = llm.generateResult(prompt, salve.core.SalveLLM.Role.PLANIFICADOR);
                    analysis = result != null && result.isSuccess() && result.getText() != null && !result.getText().trim().isEmpty()
                            ? result.getText() : "El modelo no pudo producir un análisis verificable.";
                }
            } catch (RuntimeException failed) { analysis = "El modelo no pudo producir un análisis verificable."; }
            String content = "INFORME DE SALVE: " + type + "\n\n"
                    + "Fuente: Registro de ejecución local\n"
                    + "Estado: Propuesta no implementada ni verificada.\n\n"
                    + "ANÁLISIS Y PROPUESTAS REDACTADAS POR SALVE\n\n" + analysis
                    + "\n\nEVIDENCIA ORIGINAL (sin modificaciones)\n\n" + evidence;
            File saved = AutonomousReportWriter.saveText(this, content);
            main.post(() -> {
                if (saved == null) { Toast.makeText(this, "No se pudo crear el informe", Toast.LENGTH_LONG).show(); return; }
                Toast.makeText(this, "Informe guardado en la Biblioteca", Toast.LENGTH_SHORT).show();
                showTab(ReportLibrary.TAB_LIBRARY);
            });
        });
    }

    // ───────────────────────── Biblioteca ─────────────────────────

    private void buildLibrary(LinearLayout root) {
        libraryPanel = new LinearLayout(this); libraryPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(libraryPanel, new LinearLayout.LayoutParams(-1, 0, 1));
        libraryCount = text(libraryPanel, "", 13); libraryCount.setTextColor(MUTED); libraryCount.setPadding(0, dp(8), 0, dp(4));
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chips = new LinearLayout(this); chipScroll.addView(chips);
        libraryPanel.addView(chipScroll);
        chip("Todos", null);
        for (ReportLibrary.Category c : ReportLibrary.Category.values()) chip(c.label, c);
        ScrollView scroll = new ScrollView(this);
        libraryPanel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        libraryList = new LinearLayout(this); libraryList.setOrientation(LinearLayout.VERTICAL);
        libraryList.setPadding(0, dp(6), 0, dp(24));
        scroll.addView(libraryList);
    }

    private void chip(String label, ReportLibrary.Category category) {
        TextView c = new TextView(this);
        c.setText(label); c.setTextSize(14); c.setPadding(dp(14), dp(7), dp(14), dp(7));
        c.setTag(category == null ? "all" : category.name());
        c.setOnClickListener(v -> { filter = category; refreshLibrary(); });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMargins(0, dp(4), dp(8), dp(4));
        chips.addView(c, p);
    }

    private void styleChips() {
        String active = filter == null ? "all" : filter.name();
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView c = (TextView) chips.getChildAt(i);
            boolean on = active.equals(c.getTag());
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(on ? ACCENT : Color.TRANSPARENT);
            bg.setStroke(dp(1), on ? ACCENT : LINE);
            c.setBackground(bg);
            c.setTextColor(on ? BG : TEXT);
        }
    }

    private File reportDirectory() {
        File dir = new File(getFilesDir(), AutonomousReportWriter.DIRECTORY);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void refreshLibrary() {
        styleChips();
        final ReportLibrary.Category only = filter;
        worker.execute(() -> {
            List<ReportLibrary.Entry> all = ReportLibrary.list(reportDirectory(), null);
            List<ReportLibrary.Entry> shownEntries = new ArrayList<>();
            for (ReportLibrary.Entry e : all) if (only == null || e.category == only) shownEntries.add(e);
            main.post(() -> { if (!isDestroyed()) renderLibrary(all.size(), shownEntries); });
        });
    }

    private void renderLibrary(int total, List<ReportLibrary.Entry> entries) {
        libraryList.removeAllViews();
        libraryCount.setText(total == 0 ? "Aquí se guardan los informes PDF de Salve."
                : total + (total == 1 ? " informe" : " informes") + " · toca uno para leerlo");
        if (entries.isEmpty()) {
            TextView empty = text(libraryList, total == 0
                    ? "Todavía no hay informes.\n\nSe crean solos cuando Salve revisa su código o investiga mientras duerme, "
                    + "o cuando se lo pides en el chat («revisa tu código y guárdalo en un PDF»). También puedes crear uno "
                    + "de cualquier tarea en la pestaña Ejecución → «Informe PDF»."
                    : "No hay informes de este tipo todavía.", 15);
            empty.setTextColor(MUTED); empty.setPadding(dp(4), dp(24), dp(4), 0);
            return;
        }
        for (ReportLibrary.Entry e : entries) libraryList.addView(card(e));
    }

    private View card(ReportLibrary.Entry e) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable(); bg.setCornerRadius(dp(14)); bg.setColor(CARD); bg.setStroke(dp(1), LINE);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);
        TextView meta = text(card, e.category.label.toUpperCase(java.util.Locale.ROOT) + "  ·  "
                + android.text.format.DateFormat.getMediumDateFormat(this).format(new java.util.Date(e.createdAt)) + " "
                + android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(e.createdAt)), 12);
        meta.setTextColor(ACCENT);
        TextView title = text(card, e.title, 17); title.setTypeface(Typeface.DEFAULT_BOLD); title.setPadding(0, dp(4), 0, dp(2));
        if (!e.preview.isEmpty()) {
            TextView preview = text(card, e.preview, 14); preview.setTextColor(MUTED);
            preview.setMaxLines(3); preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.END); card.addView(row, new LinearLayout.LayoutParams(-1, -2));
        link(row, "Leer", v -> openViewer(e));
        link(row, "Compartir", v -> share(e));
        link(row, "Borrar", v -> new AlertDialog.Builder(this).setMessage("¿Borrar «" + e.title + "»?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Borrar", (d, w) -> worker.execute(() -> { ReportLibrary.delete(e); main.post(this::refreshLibrary); }))
                .show());
        card.setOnClickListener(v -> openViewer(e));
        return card;
    }

    private void share(ReportLibrary.Entry e) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", e.pdf);
            Intent send = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, e.title)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Compartir informe"));
        } catch (RuntimeException failed) {
            Toast.makeText(this, "No se pudo compartir el PDF", Toast.LENGTH_LONG).show();
        }
    }

    /** Lector a pantalla completa: páginas del PDF con Anterior / Siguiente. */
    private void openViewer(ReportLibrary.Entry e) {
        try {
            android.os.ParcelFileDescriptor fd = android.os.ParcelFileDescriptor.open(e.pdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY);
            android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(fd);
            final int[] page = {0};
            LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setBackgroundColor(BG);
            box.setPadding(dp(12), dp(12), dp(12), dp(12));
            TextView title = text(box, e.title, 17); title.setTypeface(Typeface.DEFAULT_BOLD);
            TextView counter = text(box, "", 13); counter.setTextColor(MUTED);
            ScrollView scroll = new ScrollView(this);
            ImageView image = new ImageView(this); image.setAdjustViewBounds(true); image.setBackgroundColor(Color.WHITE);
            scroll.addView(image);
            box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout nav = new LinearLayout(this); box.addView(nav);
            Runnable show = () -> {
                try (android.graphics.pdf.PdfRenderer.Page p = renderer.openPage(page[0])) {
                    int width = Math.max(600, getResources().getDisplayMetrics().widthPixels);
                    int height = Math.round(width * (p.getHeight() / (float) p.getWidth()));
                    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    p.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    image.setImageBitmap(bitmap);
                }
                counter.setText("Página " + (page[0] + 1) + " de " + renderer.getPageCount());
                scroll.scrollTo(0, 0);
            };
            AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                    .setView(box).create();
            button(nav, "Anterior", v -> { if (page[0] > 0) { page[0]--; show.run(); } });
            button(nav, "Siguiente", v -> { if (page[0] < renderer.getPageCount() - 1) { page[0]++; show.run(); } });
            button(nav, "Compartir", v -> share(e));
            button(nav, "Cerrar", v -> dialog.dismiss());
            dialog.setOnDismissListener(d -> {
                renderer.close();
                try { fd.close(); } catch (Exception ignored) { }
            });
            show.run();
            dialog.show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el PDF", Toast.LENGTH_LONG).show();
        }
    }

    // ───────────────────────── Evolución ─────────────────────────
    /** Read-only overview built from the existing journal and PDF library; no duplicate storage. */
    private void buildEvolution(LinearLayout root) {
        evolutionPanel = new LinearLayout(this);
        evolutionPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(evolutionPanel, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView info = text(evolutionPanel,
                "Observar → Registrar → Analizar → Proponer → Verificar\n"
                + "Seguimiento basado en tareas e informes existentes. Una propuesta no equivale a una mejora verificada ni a un PR.", 13);
        info.setTextColor(MUTED);
        info.setPadding(0, dp(12), 0, dp(12));
        ScrollView scroll = new ScrollView(this);
        evolutionPanel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        evolutionList = new LinearLayout(this);
        evolutionList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(evolutionList);
    }

    private void refreshEvolution() {
        worker.execute(() -> {
            List<ReportLibrary.Entry> reports = ReportLibrary.list(reportDirectory(), null);
            List<ExecutionJournal.Task> recent = ExecutionTrace.journal(this).recent();
            List<com.google.gson.JsonObject> lineage = salve.core.selfpr.SelfLineage.read(
                    new File(new File(getFilesDir(), "selfpr"), "linaje.jsonl"), 15);
            main.post(() -> {
                if (!isDestroyed() && "evolucion".equals(tab)) renderEvolution(reports, recent, lineage);
            });
        });
    }

    private void renderEvolution(List<ReportLibrary.Entry> reports, List<ExecutionJournal.Task> recent,
                                 List<com.google.gson.JsonObject> lineage) {
        evolutionList.removeAllViews();
        int errors = 0;
        for (ExecutionJournal.Task task : recent) {
            if (task != null && ("ERROR".equals(task.status) || "FALLO".equals(task.status))) errors++;
        }
        TextView summary = text(evolutionList, "Observaciones: " + recent.size()
                + " tareas recientes · " + errors + " fallos · " + reports.size() + " informes", 15);
        summary.setTextColor(ACCENT);
        TextView note = text(evolutionList,
                "Observar y registrar: Registro de Ejecución.\n"
                + "Analizar y proponer: informes de Biblioteca.\n"
                + "Verificar: pendiente hasta disponer de resultados de pruebas vinculados.\n"
                + "PR: consulta el estado real desde las herramientas de PR de Salve.", 13);
        note.setTextColor(MUTED);
        note.setPadding(0, dp(10), 0, dp(12));
        if (reports.isEmpty() && recent.isEmpty() && lineage.isEmpty()) {
            text(evolutionList, "Aún no hay observaciones, propuestas ni informes para mostrar.", 15);
            return;
        }
        if (!lineage.isEmpty()) {
            TextView heading = text(evolutionList, "Propuestas registradas: " + lineage.size(), 17);
            heading.setTypeface(Typeface.DEFAULT_BOLD);
            for (int i = lineage.size() - 1; i >= 0; i--) {
                com.google.gson.JsonObject e = lineage.get(i);
                String goal = safeLineage(e, "objetivo");
                String file = safeLineage(e, "archivo");
                String issue = safeLineage(e, "issue");
                String revision = safeLineage(e, "revision");
                String origin = safeLineage(e, "origen");
                String failures = safeLineage(e, "fallos_examen");
                LinearLayout proposal = new LinearLayout(this);
                proposal.setOrientation(LinearLayout.VERTICAL);
                proposal.setPadding(dp(12), dp(10), dp(12), dp(10));
                GradientDrawable background = new GradientDrawable();
                background.setColor(CARD);
                background.setCornerRadius(dp(12));
                background.setStroke(dp(1), LINE);
                proposal.setBackground(background);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, dp(5), 0, dp(5));
                evolutionList.addView(proposal, params);
                TextView phase = text(proposal, "REGISTRADA · Verificación pendiente", 12);
                phase.setTextColor(ACCENT);
                TextView name = text(proposal, goal.isEmpty() ? "Propuesta sin título" : goal, 16);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                TextView details = text(proposal,
                        "Origen: " + (origin.isEmpty() ? "No indicado" : origin)
                        + "\nArchivo: " + (file.isEmpty() ? "No indicado" : file)
                        + "\nRevisión: " + (revision.isEmpty() ? "No indicada" : revision)
                        + "\nFallos del examen al registrar: " + (failures.isEmpty() || "-1".equals(failures) ? "Sin datos" : failures), 13);
                details.setTextColor(MUTED);
                if (issue.startsWith("https://github.com/")) {
                    link(proposal, "Abrir referencia en GitHub", v -> startActivity(
                            new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(issue))));
                } else {
                    TextView missing = text(proposal, "PR/issue: sin enlace registrado", 13);
                    missing.setTextColor(MUTED);
                }
            }
        }
        int count = 0;
        for (ReportLibrary.Entry entry : reports) {
            if (count++ >= 12) break;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(CARD); bg.setCornerRadius(dp(12)); bg.setStroke(dp(1), LINE);
            item.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(5), 0, dp(5));
            evolutionList.addView(item, lp);
            TextView phase = text(item, "ANALIZAR / PROPONER · Documento existente · No verificado", 12);
            phase.setTextColor(ACCENT);
            TextView title = text(item, entry.title, 16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            if (!entry.preview.isEmpty()) {
                TextView preview = text(item, entry.preview, 13);
                preview.setTextColor(MUTED);
                preview.setMaxLines(3);
            }
            link(item, "Abrir informe PDF", v -> openViewer(entry));
        }
        if (reports.isEmpty()) {
            text(evolutionList, "Todavía no hay informes. Puedes crear uno desde Ejecución.", 14);
        }
    }

    private static String safeLineage(com.google.gson.JsonObject e, String key) {
        if (e == null || !e.has(key) || !e.get(key).isJsonPrimitive()) return "";
        String value = e.get(key).getAsString();
        return value.length() > 220 ? value.substring(0, 220) : value;
    }

    // ───────────────────────── comunes ─────────────────────────

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(LinearLayout parent, String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(TEXT); parent.addView(view); return view;
    }

    private void button(LinearLayout parent, String label, View.OnClickListener click) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(click);
        parent.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void link(LinearLayout parent, String label, View.OnClickListener click) {
        TextView t = new TextView(this); t.setText(label); t.setTextSize(14); t.setTextColor(ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD); t.setPadding(dp(12), dp(10), dp(4), dp(6)); t.setOnClickListener(click);
        parent.addView(t);
    }

    @Override protected void onResume() { super.onResume(); visible = true; refresh(); if (tab.equals(ReportLibrary.TAB_LIBRARY)) refreshLibrary(); if ("evolucion".equals(tab)) refreshEvolution(); }
    @Override protected void onPause() { visible = false; main.removeCallbacks(poll); super.onPause(); }
    @Override protected void onDestroy() { main.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("selected", selected); state.putString("tab", tab); super.onSaveInstanceState(state);
    }

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
