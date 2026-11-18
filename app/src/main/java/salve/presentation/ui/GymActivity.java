package salve.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.vida.GymTracker;
import salve.core.vida.VidaLog;
import salve.core.vida.VidaRuntime;
import salve.core.vida.VidaService;

/**
 * El gym de Salve, inspirado en Hevy: rutinas sacadas de tu historial, entreno en vivo con la
 * columna «anterior», tipos de serie, temporizador de descanso automático, récords en vivo y
 * resumen al terminar. Los datos son los del módulo Vida (mismo archivo, misma copia en pCloud).
 */
public final class GymActivity extends Activity {
    private static final int BG = Color.BLACK, CARD = Color.rgb(28, 28, 30), ROW_ALT = Color.rgb(36, 36, 38),
            BLUE = Color.rgb(30, 136, 229), MUTED = Color.rgb(158, 158, 158), DONE = Color.rgb(27, 94, 32);
    private static final int[] RESTS = {0, 60, 90, 120, 150, 180, 240};
    private static final int PICK_CSV = 41;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private VidaService vida;
    private volatile VidaLog log = new VidaLog();
    private File draftFile;

    // Entreno en curso (null = pantalla de inicio)
    private List<GymTracker.LiveExercise> workout;
    private String title = "Entreno";
    private long startMillis;
    private TextView clock, restBar;
    private LinearLayout restPanel;
    private long restEndsAt;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (workout == null) return;
            long now = System.currentTimeMillis();
            if (clock != null) clock.setText(duration((now - startMillis) / 1000));
            if (restPanel != null) {
                if (restEndsAt > 0) {
                    long left = (restEndsAt - now + 999) / 1000;
                    if (left <= 0) { restEndsAt = 0; restPanel.setVisibility(View.GONE); buzz(); }
                    else { restPanel.setVisibility(View.VISIBLE); restBar.setText("Descanso  " + duration(left)); }
                } else restPanel.setVisibility(View.GONE);
            }
            ui.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Gym");
        vida = VidaRuntime.get(this);
        draftFile = new File(getNoBackupFilesDir(), "vida/entreno_en_curso.tsv");
        loading();
        io.execute(() -> {
            VidaLog loaded;
            String error = null;
            try { loaded = vida.read(); } catch (Exception e) { loaded = new VidaLog(); error = "No pude leer tu registro; no se ha cambiado nada."; }
            GymTracker.Draft draft = readDraft();
            final VidaLog l = loaded; final String err = error;
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                log = l;
                if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                if (draft != null && !draft.exercises.isEmpty()) {
                    title = draft.title; startMillis = draft.startMillis; workout = draft.exercises;
                    showWorkout();
                    Toast.makeText(this, "He recuperado tu entreno en curso.", Toast.LENGTH_SHORT).show();
                } else showHome();
            });
        });
    }

    @Override protected void onPause() { super.onPause(); saveDraft(); }
    @Override protected void onDestroy() { ui.removeCallbacksAndMessages(null); io.shutdown(); super.onDestroy(); }

    @Override public void onBackPressed() {
        if (workout != null) {
            saveDraft();
            Toast.makeText(this, "Tu entreno sigue en curso; vuelve cuando quieras.", Toast.LENGTH_SHORT).show();
        }
        super.onBackPressed();
    }

    // ================================================================ inicio

    private void showHome() {
        workout = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root = page();
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.addView(root);

        text(root, "Gym", 28, Color.WHITE, true);
        List<GymTracker.Session> sessions = GymTracker.sessions(log);
        LocalDate today = LocalDate.now();
        int month = 0, week = 0;
        for (GymTracker.Session s : sessions) {
            LocalDate d = LocalDate.parse(s.date);
            if (d.getYear() == today.getYear() && d.getMonth() == today.getMonth()) month++;
            if (!d.isBefore(today.minusDays(6))) week++;
        }
        text(root, sessions.size() + " entrenos · " + month + " este mes · " + week + " en 7 días", 15, MUTED, false);

        Button empty = button("Empezar entreno vacío", BLUE);
        empty.setOnClickListener(v -> startWorkout("Entreno", new ArrayList<>()));
        root.addView(empty, wide(16));

        List<GymTracker.Routine> routines = GymTracker.routines(log);
        if (!routines.isEmpty()) {
            text(root, "Rutinas", 20, Color.WHITE, true);
            for (GymTracker.Routine r : routines) {
                LinearLayout card = card(root);
                text(card, r.title, 17, BLUE, true);
                text(card, r.exercises.size() + " ejercicios · " + r.sessions + " veces · última " + pretty(r.lastDate), 13, MUTED, false);
                text(card, String.join(", ", r.exercises), 13, Color.LTGRAY, false);
                card.setOnClickListener(v -> startWorkout(r.title, GymTracker.start(log, r)));
            }
        }

        text(root, "Historial", 20, Color.WHITE, true);
        if (sessions.isEmpty()) text(root, "Aún no hay entrenos. Empieza uno o importa el CSV de Hevy.", 14, MUTED, false);
        for (int i = 0; i < Math.min(20, sessions.size()); i++) {
            GymTracker.Session s = sessions.get(i);
            LinearLayout card = card(root);
            text(card, s.title, 16, Color.WHITE, true);
            text(card, pretty(s.date) + " · " + s.sets + " series · " + kg(s.volume) + " kg", 13, MUTED, false);
            text(card, String.join(", ", s.exercises), 13, Color.LTGRAY, false);
        }

        Button importer = button("Importar CSV de Hevy", CARD);
        importer.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "application/csv", "text/plain"});
            try { startActivityForResult(pick, PICK_CSV); }
            catch (RuntimeException none) { Toast.makeText(this, "No hay selector de archivos disponible.", Toast.LENGTH_LONG).show(); }
        });
        root.addView(importer, wide(20));
        setContentView(scroll);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_CSV || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        Toast.makeText(this, "Importando…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            String message;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                message = vida.importHevy(sb.toString());
                log = vida.read();
            } catch (Exception e) { message = "No pude leer el CSV; tu registro anterior se conserva."; }
            final String m = message;
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this, m, Toast.LENGTH_LONG).show();
                if (workout == null) showHome();
            });
        });
    }

    // ================================================================ entreno en curso

    private void startWorkout(String name, List<GymTracker.LiveExercise> exercises) {
        title = name; startMillis = System.currentTimeMillis(); workout = exercises;
        saveDraft();
        showWorkout();
    }

    private void showWorkout() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout frame = new FrameLayout(this); frame.setBackgroundColor(BG);
        LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL);
        frame.addView(column);

        // Barra superior: título editable, cronómetro, terminar
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(12); top.setPadding(p, dp(28), p, p); top.setBackgroundColor(CARD);
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        EditText titleField = new EditText(this); titleField.setText(title); titleField.setTextColor(Color.WHITE);
        titleField.setTextSize(18); titleField.setSingleLine(true); titleField.setBackground(null);
        titleField.addTextChangedListener(after(s -> { title = s.trim().isEmpty() ? "Entreno" : s.trim(); saveDraft(); }));
        clock = new TextView(this); clock.setTextColor(BLUE); clock.setTextSize(14);
        titles.addView(titleField); titles.addView(clock);
        top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        Button finish = button("Terminar", BLUE);
        finish.setOnClickListener(v -> askFinish());
        top.addView(finish);
        column.addView(top);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = page();
        scroll.addView(list);
        column.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        for (GymTracker.LiveExercise ex : workout) exerciseCard(list, ex);

        Button add = button("+ Añadir ejercicio", BLUE);
        add.setOnClickListener(v -> askExercise(list, add));
        list.addView(add, wide(12));
        Button discard = button("Descartar entreno", Color.rgb(120, 30, 30));
        discard.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("¿Descartar el entreno?")
                .setMessage("Se perderán las series de esta sesión. Tu historial no cambia.")
                .setPositiveButton("Descartar", (d, w) -> { deleteDraft(); showHome(); })
                .setNegativeButton("Seguir", null).show());
        list.addView(discard, wide(8));

        // Barra de descanso
        restPanel = new LinearLayout(this); restPanel.setGravity(Gravity.CENTER_VERTICAL);
        restPanel.setBackgroundColor(Color.rgb(13, 71, 161)); restPanel.setPadding(p, p, p, p);
        restBar = new TextView(this); restBar.setTextColor(Color.WHITE); restBar.setTextSize(18);
        restPanel.addView(restBar, new LinearLayout.LayoutParams(0, -2, 1));
        restPanel.addView(small("−15", v -> restEndsAt = Math.max(System.currentTimeMillis(), restEndsAt - 15_000)));
        restPanel.addView(small("+15", v -> restEndsAt += 15_000));
        restPanel.addView(small("Saltar", v -> restEndsAt = 0));
        restPanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        frame.addView(restPanel, bottom);

        setContentView(frame);
        ui.removeCallbacks(tick); ui.post(tick);
    }

    private void exerciseCard(LinearLayout list, GymTracker.LiveExercise ex) {
        LinearLayout card = card(list);
        TextView name = text(card, ex.name, 18, BLUE, true);
        name.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this).setTitle(ex.name).setItems(new String[]{"Quitar del entreno"}, (d, w) -> {
                workout.remove(ex); list.removeView(card); saveDraft();
            }).show();
            return true;
        });
        TextView rest = text(card, restLabel(ex.restSeconds), 14, MUTED, false);
        rest.setOnClickListener(v -> {
            int i = 0; while (i < RESTS.length && RESTS[i] != ex.restSeconds) i++;
            ex.restSeconds = RESTS[(i + 1) % RESTS.length];
            rest.setText(restLabel(ex.restSeconds)); saveDraft();
        });
        LinearLayout header = row();
        header.addView(cell("SERIE", 0.7f, MUTED)); header.addView(cell("ANTERIOR", 1.6f, MUTED));
        header.addView(cell("KG", 1.1f, MUTED)); header.addView(cell("REPS", 1.0f, MUTED)); header.addView(cell("✓", 0.7f, MUTED));
        card.addView(header);
        List<VidaLog.GymSet> previous = GymTracker.previous(log, ex.name);
        LinearLayout rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);
        card.addView(rows);
        for (int i = 0; i < ex.sets.size(); i++) setRow(rows, ex, ex.sets.get(i), previous);
        TextView addSet = text(card, "+ Añadir serie", 15, Color.WHITE, false);
        addSet.setGravity(Gravity.CENTER);
        addSet.setOnClickListener(v -> {
            GymTracker.LiveSet s = new GymTracker.LiveSet();
            if (!ex.sets.isEmpty()) { GymTracker.LiveSet last = ex.sets.get(ex.sets.size() - 1); s.kg = last.kg; s.reps = last.reps; }
            ex.sets.add(s); setRow(rows, ex, s, previous); saveDraft();
        });
    }

    private void setRow(LinearLayout rows, GymTracker.LiveExercise ex, GymTracker.LiveSet s, List<VidaLog.GymSet> previous) {
        LinearLayout row = row();
        int index = ex.sets.indexOf(s);
        row.setBackgroundColor(s.done ? DONE : (index % 2 == 0 ? CARD : ROW_ALT));
        TextView type = cell("", 0.7f, Color.WHITE);
        type.setTypeface(Typeface.DEFAULT_BOLD);
        Runnable paintType = () -> {
            int working = 0;
            for (GymTracker.LiveSet o : ex.sets) { if (!"calentamiento".equals(o.type)) working++; if (o == s) break; }
            switch (s.type) {
                case "calentamiento": type.setText("W"); type.setTextColor(Color.rgb(255, 167, 38)); break;
                case "fallo": type.setText("F"); type.setTextColor(Color.rgb(239, 83, 80)); break;
                case "dropset": type.setText("D"); type.setTextColor(Color.rgb(171, 71, 188)); break;
                default: type.setText(String.valueOf(working)); type.setTextColor(Color.WHITE);
            }
        };
        paintType.run();
        type.setOnClickListener(v -> {
            String[] order = {"normal", "calentamiento", "fallo", "dropset"};
            int i = 0; while (i < order.length && !order[i].equals(s.type)) i++;
            s.type = order[(i + 1) % order.length]; paintType.run(); saveDraft();
        });
        type.setOnLongClickListener(v -> {
            ex.sets.remove(s); rows.removeView(row); saveDraft(); return true;
        });
        VidaLog.GymSet prev = index >= 0 && index < previous.size() ? previous.get(index) : null;
        TextView before = cell(previousLabel(prev), 1.6f, MUTED);
        EditText kgField = number(s.kg == null ? "" : kg(s.kg), true, 1.1f);
        EditText repsField = number(s.reps == null ? "" : String.valueOf(s.reps), false, 1.0f);
        before.setOnClickListener(v -> {
            if (prev == null) return;
            if (prev.kg != null) kgField.setText(kg(prev.kg));
            if (prev.reps != null) repsField.setText(String.valueOf(prev.reps));
        });
        kgField.addTextChangedListener(after(t -> { s.kg = parseKg(t); saveDraft(); }));
        repsField.addTextChangedListener(after(t -> { s.reps = parseReps(t); saveDraft(); }));
        TextView check = cell(s.done ? "✔" : "○", 0.7f, Color.WHITE);
        check.setTextSize(20);
        check.setOnClickListener(v -> {
            if (!s.done && !s.hasData()) {
                Toast.makeText(this, "Pon el peso o las repeticiones.", Toast.LENGTH_SHORT).show(); return;
            }
            s.done = !s.done;
            check.setText(s.done ? "✔" : "○");
            row.setBackgroundColor(s.done ? DONE : (ex.sets.indexOf(s) % 2 == 0 ? CARD : ROW_ALT));
            if (s.done) {
                String record = GymTracker.liveRecord(log, ex.name, s);
                if (record != null) { Toast.makeText(this, "🏆 " + record, Toast.LENGTH_LONG).show(); buzz(); }
                if (ex.restSeconds > 0) restEndsAt = System.currentTimeMillis() + ex.restSeconds * 1000L;
            }
            saveDraft();
        });
        row.addView(type); row.addView(before); row.addView(kgField); row.addView(repsField); row.addView(check);
        rows.addView(row);
    }

    private void askExercise(LinearLayout list, View addButton) {
        AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setHint("Busca o escribe un ejercicio");
        input.setThreshold(1);
        input.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, GymTracker.exercises(log)));
        new AlertDialog.Builder(this).setTitle("Añadir ejercicio").setView(input)
                .setPositiveButton("Añadir", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    for (String known : GymTracker.exercises(log)) if (known.equalsIgnoreCase(name)) name = known;
                    GymTracker.LiveExercise ex = GymTracker.prefilled(log, name);
                    workout.add(ex);
                    int at = list.indexOfChild(addButton);
                    LinearLayout holder = new LinearLayout(this); holder.setOrientation(LinearLayout.VERTICAL);
                    list.addView(holder, at);
                    exerciseCard(holder, ex);
                    saveDraft();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void askFinish() {
        int done = 0;
        for (GymTracker.LiveExercise ex : workout) for (GymTracker.LiveSet s : ex.sets) if (s.done) done++;
        if (done == 0) {
            new AlertDialog.Builder(this).setTitle("No has marcado ninguna serie")
                    .setMessage("Marca ✓ en las series que hiciste, o descarta el entreno.")
                    .setPositiveButton("Seguir entrenando", null)
                    .setNegativeButton("Descartar", (d, w) -> { deleteDraft(); showHome(); }).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("¿Terminar " + title + "?")
                .setMessage(done + " series completadas. Las que no tienen ✓ no se guardan.")
                .setPositiveButton("Terminar y guardar", (d, w) -> finishWorkout())
                .setNegativeButton("Seguir", null).show();
    }

    private void finishWorkout() {
        long seconds = (System.currentTimeMillis() - startMillis) / 1000;
        String date = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        List<VidaLog.GymSet> sets = GymTracker.toSets(date, String.valueOf(startMillis), title, workout);
        String name = title;
        io.execute(() -> {
            GymTracker.Summary summary = null; String error = null;
            try { summary = vida.saveWorkout(sets); log = vida.read(); deleteDraftFile(); }
            catch (Exception e) { Log.e("Salve/Gym", "No se pudo guardar el entreno", e); error = "No pude guardar el entreno. Sigue abierto para que no pierdas nada."; }
            final GymTracker.Summary s = summary; final String err = error;
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (err != null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
                StringBuilder msg = new StringBuilder()
                        .append("Duración: ").append(duration(seconds)).append('\n')
                        .append("Ejercicios: ").append(s.exercises).append('\n')
                        .append("Series: ").append(s.sets).append('\n')
                        .append("Volumen: ").append(kg(s.volume)).append(" kg");
                if (!s.records.isEmpty()) {
                    msg.append("\n\n🏆 Récords:");
                    for (String r : s.records) msg.append("\n• ").append(r);
                }
                showHome();
                new AlertDialog.Builder(this).setTitle("¡" + name + " terminado!").setMessage(msg.toString())
                        .setPositiveButton("Hecho", null).show();
            });
        });
    }

    // ================================================================ borrador

    private void saveDraft() {
        if (workout == null) return;
        String text = GymTracker.draftToText(title, startMillis, workout);
        io.execute(() -> {
            try {
                File parent = draftFile.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return;
                File tmp = new File(parent, draftFile.getName() + ".pending");
                Files.write(tmp.toPath(), text.getBytes(StandardCharsets.UTF_8));
                Files.move(tmp.toPath(), draftFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) { Log.w("Salve/Gym", "Borrador no guardado", e); }
        });
    }

    private GymTracker.Draft readDraft() {
        try {
            if (!draftFile.isFile()) return null;
            return GymTracker.draftFromText(new String(Files.readAllBytes(draftFile.toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) { return null; }
    }

    private void deleteDraft() { workout = null; io.execute(this::deleteDraftFile); }
    private void deleteDraftFile() { try { Files.deleteIfExists(draftFile.toPath()); } catch (Exception ignored) { } }

    // ================================================================ utilidades de vista

    private void loading() {
        LinearLayout root = page();
        text(root, "Cargando tu gym…", 18, Color.WHITE, false);
        setContentView(root);
    }

    private LinearLayout page() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16); root.setPadding(p, dp(32), p, dp(96)); root.setBackgroundColor(BG);
        return root;
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12); card.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(CARD); bg.setCornerRadius(dp(12));
        card.setBackground(bg);
        parent.addView(card, wide(10));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private TextView cell(String value, float weight, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), weight));
        return t;
    }

    private EditText number(String value, boolean decimal, float weight) {
        EditText e = new EditText(this); e.setText(value); e.setTextColor(Color.WHITE); e.setTextSize(16);
        e.setGravity(Gravity.CENTER); e.setSingleLine(true); e.setHint("—"); e.setHintTextColor(Color.DKGRAY);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        if (decimal) e.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        e.setSelectAllOnFocus(true);
        e.setLayoutParams(new LinearLayout.LayoutParams(0, -2, weight));
        return e;
    }

    private TextView text(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        parent.addView(t);
        return t;
    }

    private Button button(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(16);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(10));
        b.setBackground(bg);
        return b;
    }

    private TextView small(String label, View.OnClickListener click) {
        TextView t = new TextView(this); t.setText(label); t.setTextColor(Color.WHITE); t.setTextSize(16);
        t.setPadding(dp(12), dp(6), dp(12), dp(6)); t.setOnClickListener(click);
        return t;
    }

    private LinearLayout.LayoutParams wide(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private interface Text { void changed(String value); }
    private static TextWatcher after(Text listener) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { listener.changed(e.toString()); }
        };
    }

    private void buzz() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (RuntimeException ignored) { }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
    private static String previousLabel(VidaLog.GymSet p) {
        if (p == null) return "—";
        if (p.kg != null) return kg(p.kg) + " × " + (p.reps == null ? "?" : p.reps);
        if (p.reps != null) return "× " + p.reps;
        if (p.km != null || p.seconds != null)
            return (p.km == null ? "" : kg(p.km) + " km ") + (p.seconds == null ? "" : duration(p.seconds));
        return "—";
    }
    private static String restLabel(int s) { return "⏱ Descanso: " + (s == 0 ? "apagado" : (s / 60) + " min " + (s % 60) + " s"); }
    private static String duration(long s) { return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60); }
    private static String kg(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replace('.', ','); }
    private static String pretty(String iso) {
        try { return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es"))); }
        catch (RuntimeException e) { return iso; }
    }
    private static Double parseKg(String t) {
        String s = t.trim().replace(',', '.');
        if (s.isEmpty()) return null;
        try { double v = Double.parseDouble(s); return v < 0 || v > 1000 ? null : v; } catch (NumberFormatException e) { return null; }
    }
    private static Integer parseReps(String t) {
        String s = t.trim();
        if (s.isEmpty()) return null;
        try { int v = Integer.parseInt(s); return v < 0 || v > 999 ? null : v; } catch (NumberFormatException e) { return null; }
    }
}
