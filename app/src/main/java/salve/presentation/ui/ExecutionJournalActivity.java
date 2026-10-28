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
        selector = new Spinner(this); root.addView(selector);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < tasks.size()) { selected = tasks.get(position).id; render(); }
            }
        });
        LinearLayout actions = new LinearLayout(this); root.addView(actions);
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
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        log = new TextView(this); log.setTextColor(Color.rgb(230, 241, 247)); log.setTextSize(15);
        log.setTextIsSelectable(true); log.setPadding(0, 12, 0, 30); scroll.addView(log);
        setContentView(root);
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
