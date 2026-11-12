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
        // Cabecera: su cara (la misma que en el chat), el nombre del registro y para qué sirve.
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        ImageView face = new ImageView(this);
        face.setImageResource(com.salve.app.R.drawable.salve_chat_avatar);
        face.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.setContentDescription("Salve");
        face.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        face.setClipToOutline(true);
        LinearLayout.LayoutParams faceSize = new LinearLayout.LayoutParams(dp(56), dp(56));
        faceSize.setMargins(0, 0, dp(14), 0);
        header.addView(face, faceSize);
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        header.addView(names, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView title = text(names, "Registro de Salve", 23); title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView subtitle = text(names, "Actividad, investigaciones y evolución", 14); subtitle.setTextColor(MUTED);

        // ── Pestañas ──
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(0, dp(10), 0, 0);
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        executionTab = tab(tabs, "Ejecución", ReportLibrary.TAB_EXECUTION);
        libraryTab = tab(tabs, "Biblioteca", ReportLibrary.TAB_LIBRARY);
        evolutionTab = tab(tabs, "Evolución", "evolucion");

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
        ul.setMargins(dp(3), 0, dp(3), 0);
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
        GradientDrawable bar = new GradientDrawable();
        bar.setColor(active ? ACCENT : LINE); bar.setCornerRadius(dp(2));
        ((View) t.getTag()).setBackground(bar);
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
    // Las cinco fases con las que Salve decide si se cambia a sí misma (EvolutionCycle), sus propuestas con su
    // estado real (linaje + veredictos + lo que dijo GitHub) y los informes. Nada se guarda aquí: solo se enseña.

    private static final int PANEL = Color.rgb(17, 33, 46), SELECTED = Color.rgb(29, 78, 82),
            WAIT = Color.rgb(240, 196, 110), BAD = Color.rgb(240, 128, 120);
    private static final int[] PHASE_ICONS = {com.salve.app.R.drawable.ic_salve_phase_observe, com.salve.app.R.drawable.ic_salve_phase_record,
            com.salve.app.R.drawable.ic_salve_phase_analyze, com.salve.app.R.drawable.ic_salve_phase_propose, com.salve.app.R.drawable.ic_salve_phase_verify};
    private static final String[] PHASE_TITLES = {"Observar el problema", "Registrar las evidencias", "Analizar las causas",
            "Proponer una solución", "Verificar resultados"};
    private int selectedPhase = 0;
    private salve.core.selfpr.EvolutionCycle.Plan plan;
    private List<com.google.gson.JsonObject> lineage = new ArrayList<>(), verdicts = new ArrayList<>();
    private java.util.Map<Integer, salve.core.selfpr.GitHubSelfPr.Status> github = new java.util.HashMap<>();
    private List<ReportLibrary.Entry> evolutionReports = new ArrayList<>();
    private LinearLayout phaseRow, phaseCard;

    private void buildEvolution(LinearLayout root) {
        evolutionPanel = new LinearLayout(this);
        evolutionPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(evolutionPanel, new LinearLayout.LayoutParams(-1, 0, 1));
        ScrollView scroll = new ScrollView(this);
        evolutionPanel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        evolutionList = new LinearLayout(this);
        evolutionList.setOrientation(LinearLayout.VERTICAL);
        evolutionList.setPadding(0, dp(14), 0, dp(16));
        scroll.addView(evolutionList);
    }

    private void refreshEvolution() {
        worker.execute(() -> {
            salve.core.selfpr.EvolutionCycle.Plan cycle = salve.core.selfpr.EvolutionCycle.load(this);
            List<com.google.gson.JsonObject> changes = salve.core.selfpr.SelfLineage.read(
                    salve.core.selfpr.SelfVerification.lineageFile(this), 15);
            List<com.google.gson.JsonObject> measured = salve.core.selfpr.SelfLineage.read(
                    salve.core.selfpr.SelfVerification.verdictsFile(this), 50);
            List<ReportLibrary.Entry> reports = ReportLibrary.list(reportDirectory(), null);
            main.post(() -> {
                if (isDestroyed()) return;
                plan = cycle; lineage = changes; verdicts = measured; evolutionReports = reports;
                if (selectedPhase == 0) selectedPhase = cycle.current;
                if ("evolucion".equals(tab)) renderEvolution();
            });
            // Lo que dijo GitHub de cada propuesta (si hay token y red); sin red se queda lo local.
            String token = salve.core.selfpr.SelfPrTokenStore.token(this);
            if (token == null || changes.isEmpty()) return;
            try {
                java.util.Map<Integer, salve.core.selfpr.GitHubSelfPr.Status> found = new java.util.HashMap<>();
                for (salve.core.selfpr.GitHubSelfPr.Status st : new salve.core.selfpr.GitHubSelfPr(token).recent(15))
                    found.put(st.number, st);
                main.post(() -> {
                    if (isDestroyed()) return;
                    github = found;
                    if ("evolucion".equals(tab)) renderEvolution();
                });
            } catch (java.io.IOException | RuntimeException offline) { /* se enseña lo local */ }
        });
    }

    private void renderEvolution() {
        evolutionList.removeAllViews();
        if (plan == null) return;

        // ── Ciclo de mejora + estado ──
        LinearLayout head = row(evolutionList);
        TextView heading = text(head, "Ciclo de mejora", 21);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        pill(head, plan.status, plan.ready ? ACCENT : TEXT);

        // ── Las cinco fases ──
        phaseRow = new LinearLayout(this);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, dp(16), 0, dp(14));
        evolutionList.addView(phaseRow, rowLp);
        for (int i = 1; i <= 5; i++) phaseChip(i);

        // ── Tarjeta de la fase elegida ──
        phaseCard = new LinearLayout(this);
        phaseCard.setOrientation(LinearLayout.VERTICAL);
        phaseCard.setPadding(dp(18), dp(16), dp(18), dp(14));
        phaseCard.setBackground(box(PANEL, LINE, 18));
        evolutionList.addView(phaseCard, new LinearLayout.LayoutParams(-1, -2));
        renderPhaseCard();

        // ── Autonomía ──
        LinearLayout auto = section(evolutionList, CARD);
        LinearLayout autoRow = row(auto);
        TextView autoTitle = text(autoRow, "Evolución autónoma", 17);
        autoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        autoTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        Switch toggle = new Switch(this);
        toggle.setChecked(salve.core.selfpr.EvolutionCycle.autonomous(this));
        toggle.setOnCheckedChangeListener((b, on) -> {
            salve.core.selfpr.EvolutionCycle.setAutonomous(this, on);
            Toast.makeText(this, on ? "Salve podrá proponer y revertir sola mientras duerme"
                    : "Salve solo cambiará su código cuando se lo pidas", Toast.LENGTH_SHORT).show();
        });
        autoRow.addView(toggle);
        TextView autoText = text(auto, "Mientras duermo cargando: si cumplo las cinco fases propongo un cambio (como mucho uno al día). "
                + "GitHub ejecuta todas mis pruebas y compila Android; solo si pasa se fusiona y se publica. Al instalar la versión "
                + "nueva me examino y, si empeoro en " + salve.core.selfpr.EvolutionCycle.REVERT_MARGIN + " casos o más, me revierto sola.", 13);
        autoText.setTextColor(MUTED);
        autoText.setPadding(0, dp(6), 0, 0);

        // ── Propuestas de mejora ──
        LinearLayout proposals = section(evolutionList, CARD);
        TextView ptitle = text(proposals, "Propuestas de mejora", 20);
        ptitle.setTypeface(Typeface.DEFAULT_BOLD);
        TextView pnote = text(proposals, "Un PR solo avanza si pasa las comprobaciones y permisos establecidos; "
                + "después lo verifico con un examen en la versión instalada.", 13);
        pnote.setTextColor(MUTED);
        pnote.setPadding(0, dp(4), 0, dp(6));
        if (lineage.isEmpty()) {
            TextView none = text(proposals, "Todavía no he propuesto ningún cambio de mi código.", 15);
            none.setPadding(0, dp(8), 0, dp(4));
        }
        for (int i = lineage.size() - 1, shown = 0; i >= 0 && shown < 8; i--, shown++) proposal(proposals, lineage.get(i));

        // ── Informes ──
        if (!evolutionReports.isEmpty()) {
            TextView rtitle = text(evolutionList, "Informes", 18);
            rtitle.setTypeface(Typeface.DEFAULT_BOLD);
            rtitle.setPadding(dp(2), dp(18), 0, dp(4));
        }
        int count = 0;
        for (ReportLibrary.Entry entry : evolutionReports) {
            if (count++ >= 8) break;
            LinearLayout item = section(evolutionList, CARD);
            TextView title = text(item, entry.title, 16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            if (!entry.preview.isEmpty()) {
                TextView preview = text(item, entry.preview, 13);
                preview.setTextColor(MUTED);
                preview.setMaxLines(3);
            }
            link(item, "Abrir informe PDF", v -> openViewer(entry));
        }
    }

    private void phaseChip(int number) {
        salve.core.selfpr.EvolutionCycle.Phase phase = plan.phase(number);
        boolean on = number == selectedPhase;
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER_HORIZONTAL);
        chip.setPadding(dp(2), dp(12), dp(2), dp(9));
        chip.setBackground(box(on ? SELECTED : CARD, 0, 14));
        ImageView icon = new ImageView(this);
        icon.setImageResource(PHASE_ICONS[number - 1]);
        icon.setColorFilter(on ? ACCENT : TEXT);
        chip.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView label = new TextView(this);
        label.setText(phase.name); label.setTextSize(12.5f); label.setSingleLine(true);
        label.setGravity(Gravity.CENTER); label.setTextColor(on ? ACCENT : TEXT);
        label.setPadding(0, dp(6), 0, dp(5));
        chip.addView(label, new LinearLayout.LayoutParams(-1, -2));
        // Puntito: cumplida, esperando, bloqueada o aún no alcanzada.
        View dot = new View(this);
        GradientDrawable d = new GradientDrawable(); d.setShape(GradientDrawable.OVAL); d.setColor(stateColor(phase.state));
        dot.setBackground(d);
        chip.addView(dot, new LinearLayout.LayoutParams(dp(6), dp(6)));
        chip.setContentDescription(phase.name + ": " + stateLabel(phase.state));
        chip.setOnClickListener(v -> { selectedPhase = number; renderEvolution(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(number == 1 ? 0 : dp(3), 0, number == 5 ? 0 : dp(3), 0);
        phaseRow.addView(chip, lp);
    }

    private void renderPhaseCard() {
        int n = Math.max(1, Math.min(5, selectedPhase));
        salve.core.selfpr.EvolutionCycle.Phase phase = plan.phase(n);
        LinearLayout top = row(phaseCard);
        TextView what = text(top, "Investigación actual · " + plan.title, 18);
        what.setTextColor(ACCENT);
        what.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        pill(top, stateLabel(phase.state), stateColor(phase.state));
        TextView step = text(phaseCard, n + ". " + PHASE_TITLES[n - 1], 20);
        step.setTypeface(Typeface.DEFAULT_BOLD);
        step.setPadding(0, dp(14), 0, dp(8));
        TextView body = text(phaseCard, phase.reasoning, 15);
        body.setLineSpacing(0, 1.15f);
        View rule = new View(this); rule.setBackgroundColor(LINE);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(-1, dp(1));
        ruleLp.setMargins(0, dp(16), 0, dp(12));
        phaseCard.addView(rule, ruleLp);
        LinearLayout evidence = row(phaseCard);
        TextView label = text(evidence, "Evidencia vinculada", 14);
        label.setTextColor(MUTED);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        String issue = lineage.isEmpty() ? "" : safeLineage(lineage.get(lineage.size() - 1), "issue");
        String target = n == 1 ? "Examen" : n == 2 ? "Registro de ejecución" : n == 3 ? "Informes del sueño"
                : issue.startsWith("https://github.com/") ? "Propuesta en GitHub" : "Registro + informe PDF";
        TextView open = text(evidence, target + " ›", 14);
        open.setTypeface(Typeface.DEFAULT_BOLD);
        open.setOnClickListener(v -> {
            if (n == 1) showExam();
            else if (n == 2) showTab(ReportLibrary.TAB_EXECUTION);
            else if (n == 3 || !issue.startsWith("https://github.com/")) showTab(ReportLibrary.TAB_LIBRARY);
            else startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(issue)));
        });
    }

    private void proposal(LinearLayout parent, com.google.gson.JsonObject e) {
        View rule = new View(this); rule.setBackgroundColor(LINE);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(-1, dp(1));
        ruleLp.setMargins(0, dp(10), 0, dp(10));
        parent.addView(rule, ruleLp);
        String goal = safeLineage(e, "objetivo"), file = safeLineage(e, "archivo"), issue = safeLineage(e, "issue");
        LinearLayout top = row(parent);
        TextView name = text(top, goal.isEmpty() ? "Cambio en " + file.substring(file.lastIndexOf('/') + 1) : goal, 16);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        String[] st = proposalState(e);
        pill(top, st[0], "ok".equals(st[1]) ? ACCENT : "bad".equals(st[1]) ? BAD : "wait".equals(st[1]) ? WAIT : TEXT);
        String origin = safeLineage(e, "origen");
        String who = salve.core.selfpr.EvolutionCycle.ORIGIN_AUTONOMOUS.equals(origin) ? "Propuesta autónoma"
                : salve.core.selfpr.EvolutionCycle.ORIGIN_REVERT.equals(origin) ? "Reversión automática"
                : "bryan".equals(origin) ? "Pedida por Bryan"
                : salve.core.selfpr.SelfVerification.ADAPTER.equals(origin) ? "Adaptador de pesos" : "Del examen";
        long t = e.has("t") ? e.get("t").getAsLong() : 0;
        TextView meta = text(parent, who + " · " + file.substring(file.lastIndexOf('/') + 1)
                + (t > 0 ? " · " + android.text.format.DateFormat.format("d MMM, HH:mm", t) : "")
                + (st[2].isEmpty() ? "" : "\n" + st[2]), 13);
        meta.setTextColor(MUTED);
        meta.setPadding(0, dp(4), 0, 0);
        if (issue.startsWith("https://github.com/")) {
            View.OnClickListener go = v -> startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(issue)));
            name.setOnClickListener(go); meta.setOnClickListener(go);
        }
    }

    /** {etiqueta, tono (ok/bad/wait/neutral), detalle}: el veredicto medido manda; si no hay, lo que dijo GitHub. */
    private String[] proposalState(com.google.gson.JsonObject e) {
        String issue = safeLineage(e, "issue");
        com.google.gson.JsonObject v = salve.core.selfpr.EvolutionCycle.verdictFor(verdicts, issue);
        if (v != null) {
            String r = safeLineage(v, "resultado");
            String counts = "Examen: " + v.get("antes").getAsInt() + " → " + v.get("ahora").getAsInt() + " casos fallidos";
            switch (r) {
                case "mejor": return new String[]{"Mejoró", "ok", counts};
                case "peor": return new String[]{"Empeoró", "bad", counts};
                case "no_comparable": return new String[]{"No comparable", "neutral", "El examen cambió de tamaño"};
                default: return new String[]{"Sin cambio", "neutral", counts};
            }
        }
        int number = -1;
        int slash = issue.lastIndexOf('/');
        if (slash >= 0) try { number = Integer.parseInt(issue.substring(slash + 1)); } catch (NumberFormatException ignored) { }
        salve.core.selfpr.GitHubSelfPr.Status st = github.get(number);
        if (st != null) switch (st.outcome) {
            case "merged": return new String[]{"Fusionado", "wait", "Falta instalarlo y examinarme con él"};
            case "passed": return new String[]{"Pasó pruebas", "wait", st.detail};
            case "failed": return new String[]{"Falló pruebas", "bad", st.detail};
            case "rejected": return new String[]{"Rechazado", "bad", st.detail};
            default: return new String[]{"open".equals(st.state) ? "Probándose" : "Cerrado", "wait", ""};
        }
        long before = e.has("fallos_examen") && e.get("fallos_examen").isJsonPrimitive() ? e.get("fallos_examen").getAsLong() : -1;
        return new String[]{"Sin verificar", "neutral", before >= 0 ? "Fallos del examen al proponer: " + before : ""};
    }

    /** El último examen tal cual (ultimo.txt), sin resumir. */
    private void showExam() {
        worker.execute(() -> {
            File file = new File(new File(getFilesDir(), "exam"), "ultimo.txt");
            String text;
            try { text = file.isFile() ? new String(java.nio.file.Files.readAllBytes(file.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8) : "Aún no me he examinado. Pídeme «hazte el examen»."; }
            catch (java.io.IOException unreadable) { text = "No pude leer el examen."; }
            String shown = text.length() > 20000 ? text.substring(0, 20000) + "…" : text;
            main.post(() -> {
                if (isDestroyed()) return;
                TextView view = new TextView(this);
                view.setText(shown); view.setTextIsSelectable(true); view.setPadding(dp(18), dp(12), dp(18), dp(12));
                ScrollView sv = new ScrollView(this); sv.addView(view);
                new AlertDialog.Builder(this).setTitle("Mi último examen").setView(sv)
                        .setPositiveButton("Cerrar", null).show();
            });
        });
    }

    private static String stateLabel(salve.core.selfpr.EvolutionCycle.State state) {
        switch (state) {
            case DONE: return "Cumplida";
            case BLOCKED: return "Bloqueada";
            case WAITING: return "En estudio";
            default: return "Pendiente";
        }
    }

    private int stateColor(salve.core.selfpr.EvolutionCycle.State state) {
        switch (state) {
            case DONE: return ACCENT;
            case BLOCKED: return BAD;
            case WAITING: return WAIT;
            default: return MUTED;
        }
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(r, new LinearLayout.LayoutParams(-1, -2));
        return r;
    }

    private LinearLayout section(LinearLayout parent, int color) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(dp(18), dp(16), dp(18), dp(16));
        s.setBackground(box(color, 0, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, 0);
        parent.addView(s, lp);
        return s;
    }

    private void pill(LinearLayout parent, String label, int color) {
        TextView p = new TextView(this);
        p.setText(label); p.setTextSize(14); p.setTextColor(color); p.setSingleLine(true);
        p.setPadding(dp(14), dp(6), dp(14), dp(6));
        p.setBackground(box(Color.rgb(38, 56, 72), 0, 99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(10), 0, 0, 0);
        parent.addView(p, lp);
    }

    private GradientDrawable box(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(radius));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
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
