package salve.presentation.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.agent.AgentEngine;
import salve.core.agent.AgentRun;
import salve.core.agent.AgentRuntime;

/** A view of the durable agent journal. All Room/scheduler operations run off the main thread. */
public class AgentPlansActivity extends Activity {
    public static final String EXTRA_PLAN_ID = "salve.plan_id";
    private static final int INK = Color.rgb(233, 241, 247), MUTED = Color.rgb(170, 190, 201);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Backend backend;
    private boolean visible, loading, submitting, changing;
    private String selected = "", requestId = UUID.randomUUID().toString(), requestGoal = "", signature = "";
    private EditText goal;
    private Button create, pause;
    private TextView notice, summary;
    private LinearLayout list, detail;
    private ScrollView scroll;
    private final Runnable poll = () -> { if (visible) refresh(); };

    /** Small seam for lifecycle tests; production always uses the same app-scoped runtime as chat. */
    protected interface Backend {
        List<AgentRun> recent();
        boolean paused();
        AgentRun create(String id, String goal);
        boolean cancel(String id);
        boolean resume(String id);
        void pause(boolean paused);
    }
    protected Backend createBackend() {
        android.content.Context app = getApplicationContext();
        return new Backend() {
            private AgentRuntime runtime() { return AgentRuntime.get(app); }
            public List<AgentRun> recent() { return runtime().recentPlans(); }
            public boolean paused() { return runtime().paused(); }
            public AgentRun create(String id, String goal) { return runtime().createPlan(id, goal); }
            public boolean cancel(String id) { return runtime().cancelPlan(id); }
            public boolean resume(String id) { return runtime().resumePlan(id); }
            public void pause(boolean value) {
                salve.core.tasks.ResearchTaskRuntime.get(app).pause(value);
                runtime().pause(value);
            }
        };
    }
    protected Executor workExecutor() { return worker; }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        backend = createBackend();
        if (saved != null) {
            selected = saved.getString("selected", "");
            requestId = saved.getString("request", requestId);
            requestGoal = saved.getString("requestGoal", "");
        }
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(13, 24, 34));
        LinearLayout page = column(); page.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(page); setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        button(page, "Volver a Salve", this::finish);
        text(page, "Planes y resultados", 28, INK);
        text(page, "Dale un objetivo concreto. Salve conserva sus intentos, resultados y fuentes para que puedas retomarlos.", 16, MUTED);
        summary = text(page, "Cargando planes…", 15, MUTED);
        goal = new EditText(this); goal.setTextColor(INK); goal.setHintTextColor(MUTED);
        goal.setHint("Ej.: consulta mis recuerdos sobre el origen de Salve y resume lo que encuentres");
        goal.setMinLines(3); goal.setMaxLines(6); goal.setGravity(android.view.Gravity.TOP);
        goal.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        goal.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});
        goal.setContentDescription("Objetivo del nuevo plan"); page.addView(goal);
        if (saved != null) goal.setText(saved.getString("draft", ""));
        create = button(page, "Crear plan", this::submit);
        text(page, "Usa el modelo local y las herramientas configuradas. Android puede aplazar el trabajo en segundo plano.", 14, MUTED);
        notice = text(page, "", 15, INK); notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        pause = button(page, "Pausar planes y búsquedas", () -> {}); pause.setEnabled(false);
        detail = column(); page.addView(detail);
        text(page, "Planes recientes", 22, INK);
        list = column(); page.addView(list);
        if (!requestGoal.isEmpty()) { goal.setText(requestGoal); submit(); }
    }
    @Override protected void onResume() { super.onResume(); visible = true; refresh(); }
    @Override protected void onPause() { visible = false; main.removeCallbacks(poll); super.onPause(); }
    @Override protected void onDestroy() { main.removeCallbacks(poll); worker.shutdown(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("selected", selected); state.putString("draft", goal.getText().toString());
        state.putString("request", requestId); state.putString("requestGoal", requestGoal);
        super.onSaveInstanceState(state);
    }
    private void deliver(Runnable action) {
        main.post(() -> { if (!isFinishing() && !isDestroyed()) action.run(); });
    }
    private void submit() {
        if (submitting) return;
        String value = goal.getText().toString().trim();
        if (value.isEmpty()) { goal.setError("Escribe el objetivo que quieres resolver."); return; }
        if (!requestGoal.isEmpty() && !requestGoal.equals(value)) requestId = UUID.randomUUID().toString();
        requestGoal = value; String id = requestId;
        submitting = true; create.setEnabled(false); goal.setEnabled(false); notice.setText("Guardando objetivo…");
        workExecutor().execute(() -> {
            try {
                AgentRun run = backend.create(id, value);
                deliver(() -> {
                    submitting = false; create.setEnabled(true); goal.setEnabled(true); goal.setText("");
                    requestId = UUID.randomUUID().toString(); requestGoal = ""; selected = run.id; signature = "";
                    notice.setText("Plan guardado. Su estado se actualiza aquí."); refresh();
                });
            } catch (RuntimeException error) {
                deliver(() -> {
                    submitting = false; create.setEnabled(true); goal.setEnabled(true);
                    notice.setText(error instanceof IllegalArgumentException ? error.getMessage()
                            : "No pude confirmar el guardado. Puedes reintentar el mismo objetivo sin duplicarlo.");
                });
            }
        });
    }
    private void refresh() {
        main.removeCallbacks(poll);
        if (loading || isFinishing() || isDestroyed()) return;
        loading = true;
        workExecutor().execute(() -> {
            try {
                List<AgentRun> runs = backend.recent(); boolean paused = backend.paused();
                deliver(() -> {
                    loading = false; render(runs, paused);
                    if (visible) main.postDelayed(poll, 2000);
                });
            } catch (RuntimeException error) {
                deliver(() -> {
                    loading = false; summary.setText("No pude cargar los planes. Volveré a intentarlo.");
                    if (visible) main.postDelayed(poll, 4000);
                });
            }
        });
    }
    private void render(List<AgentRun> runs, boolean paused) {
        long pending = runs.stream().filter(r -> !r.terminal()).count();
        summary.setText((paused ? "Planes y búsquedas en pausa" : "Trabajo en segundo plano habilitado")
                + " · " + pending + " planes pendientes");
        pause.setText(paused ? "Reanudar planes y búsquedas" : "Pausar planes y búsquedas");
        pause.setEnabled(!changing);
        pause.setOnClickListener(v -> change(() -> { backend.pause(!paused); return true; },
                paused ? "Planes y búsquedas reanudados." : "Planes y búsquedas pausados; conservan su progreso."));
        StringBuilder fingerprint = new StringBuilder(selected).append(paused);
        AgentRun chosen = null;
        for (AgentRun run : runs) {
            fingerprint.append(run.id).append(run.status).append(run.updatedAt).append(run.modelCalls).append(run.observations.size());
            if (run.id.equals(selected)) { chosen = run; fingerprint.append(run.encode()); }
        }
        String next = fingerprint.toString(); if (signature.equals(next)) return; signature = next;
        list.removeAllViews(); detail.removeAllViews();
        if (chosen != null) renderDetail(chosen);
        else if (!selected.isEmpty()) text(detail, "Este plan ya no está en el historial reciente.", 16, MUTED);
        if (runs.isEmpty()) text(list, "Aún no hay planes. Crea el primero arriba o di: «trabaja en este objetivo: …».", 16, MUTED);
        for (AgentRun run : runs) button(list, status(run.status) + " · " + run.id.substring(0, 8) + "\n" + run.goal,
                () -> { selected = run.id; signature = ""; render(runs, paused); scroll.post(() -> scroll.smoothScrollTo(0, detail.getTop())); });
    }
    private void renderDetail(AgentRun run) {
        text(detail, "Tu objetivo", 22, INK);
        text(detail, run.goal, 18, INK);
        text(detail, status(run.status) + " · " + run.id.substring(0, 8), 16, MUTED);
        text(detail, run.observations.size() + "/" + AgentEngine.MAX_STEPS + " intentos de herramientas · "
                + run.modelCalls + "/" + AgentEngine.MAX_MODEL_CALLS + " consultas al modelo", 14, MUTED);
        if (run.pending != null) text(detail, "Pendiente: " + run.pending.tool, 16, MUTED);
        if (!run.outline.isEmpty()) text(detail, "Pasos propuestos\n• " + String.join("\n• ", run.outline), 16, INK);
        if (!run.answer.isEmpty()) {
            text(detail, run.status.equals("SUCCEEDED") ? "Resultado revisado por el modelo" : "Respuesta disponible · revisión incompleta", 18, INK);
            text(detail, run.answer, 16, INK).setTextIsSelectable(true);
        }
        if (!run.error.isEmpty()) text(detail, run.error, 16, Color.rgb(255, 205, 145));
        for (AgentRun.Observation observation : run.observations) {
            text(detail, observation.tool + " · " + status(observation.status), 16, INK);
            text(detail, observation.text, 15, MUTED).setTextIsSelectable(true);
            if (!observation.sources.isEmpty()) text(detail, "Fuentes consultadas\n" + String.join("\n", observation.sources), 14, MUTED).setTextIsSelectable(true);
        }
        if (!run.providers.isEmpty()) text(detail, "Motor y respuesta de cada consulta\n" + String.join("\n", run.providers), 14, MUTED);
        if (!run.answer.isEmpty()) button(detail, "Llevar resultado al chat", () -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_PLAN_ID, run.id)); finish();
        });
        if (!run.terminal() || run.status.equals("BLOCKED")) button(detail, "Cancelar este plan",
                () -> change(() -> backend.cancel(run.id), "Plan cancelado. Su historial se conserva."));
        if ((run.status.equals("CANCELLED") || run.status.equals("BLOCKED")) && run.modelCalls < AgentEngine.MAX_MODEL_CALLS)
            button(detail, "Continuar este plan", () -> change(() -> backend.resume(run.id), "Plan preparado para continuar; si hay una pausa, reanuda arriba."));
    }
    private interface Change { boolean apply(); }
    private void change(Change action, String success) {
        if (changing) return; changing = true; pause.setEnabled(false);
        workExecutor().execute(() -> {
            String result;
            try { result = action.apply() ? success : "El estado del plan cambió. Revisa el resultado actualizado."; }
            catch (RuntimeException error) { result = "No pude aplicar el cambio. Puedes volver a intentarlo."; }
            String message = result;
            deliver(() -> { changing = false; signature = ""; notice.setText(message); refresh(); });
        });
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private TextView text(LinearLayout parent, String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(10), 0, dp(6)); parent.addView(view); return view;
    }
    private Button button(LinearLayout parent, String value, Runnable action) {
        Button view = new Button(this); view.setText(value); view.setAllCaps(false); view.setMinHeight(dp(48));
        view.setTextColor(INK); view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(37, 68, 78)));
        view.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(8);
        parent.addView(view, params); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    static String status(String status) {
        switch (status) {
            case "QUEUED": return "En espera";
            case "RUNNING": return "En ejecución";
            case "SUCCEEDED": return "Completado";
            case "SUCCESS": return "Resultado obtenido";
            case "PARTIAL": return "Resultado parcial";
            case "BLOCKED": return "Necesita atención";
            case "CANCELLED": return "Cancelado";
            case "ERROR": return "Error";
            default: return status;
        }
    }
}
