package salve.core.agent;

import android.content.Context;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import salve.core.GoalAutonomyRuntime;
import salve.core.SalveLLM;
import salve.core.tasks.ResearchTaskRuntime;
import salve.data.db.AgentSubscriptionEntity;
import salve.data.db.MemoriaDatabase;
import salve.data.sync.CloudSyncManager;
import salve.data.sync.SyncWorker;
import salve.data.tasks.RoomAgentStore;
import salve.work.AgentWorker;

/** App-scoped orchestrator. Autonomous inference stays local, and conversation has priority. */
public final class AgentRuntime {
    private static volatile AgentRuntime instance;
    private final Context app;
    private final RoomAgentStore store;
    private final AtomicBoolean running = new AtomicBoolean();
    private AgentRuntime(Context context) {
        app = context.getApplicationContext(); ResearchTaskRuntime.get(app); // Inherit persisted pause once.
        store = new RoomAgentStore(MemoriaDatabase.getInstance(app), () -> CloudSyncManager.isEnabled(app));
    }
    public static AgentRuntime get(Context context) {
        if (instance == null) synchronized (AgentRuntime.class) { if (instance == null) instance = new AgentRuntime(context); }
        return instance;
    }
    private String bridge() { ToolBridgeConfig config = ToolBridgeConfig.load(app); return config == null ? "" : config.endpoint; }
    public void pause(boolean paused) { store.pause(paused, System.currentTimeMillis()); if (!paused) recover(true); }
    public void recover() { recover(false); }
    private void recover(boolean append) {
        if (!store.paused()) for (AgentRun run : store.pending()) schedule(run, append);
        AgentWorker.scheduleEvents(app, !store.subscriptions().isEmpty());
    }
    private boolean schedule(AgentRun run, boolean append) {
        try { AgentWorker.enqueue(app, run.id, Math.max(0, run.leaseUntil - System.currentTimeMillis()), append); return true; }
        catch (RuntimeException unavailable) { return false; }
    }
    public void signal(String event) {
        for (AgentRun run : store.signal(event, System.currentTimeMillis())) schedule(run, false);
        recover();
    }
    public boolean run(String id, BooleanSupplier stopped) {
        if (store.paused()) return true;
        if (GoalAutonomyRuntime.get(app).hasActiveConversation() || !running.compareAndSet(false, true)) return false;
        try {
            SalveLLM llm = SalveLLM.getInstance(app);
            AgentRun result = new AgentEngine(store, new AgentEngine.Model() {
                        public salve.core.ModelResult generate(String prompt) { return llm.generateResult(prompt, SalveLLM.Role.EVALUADOR); }
                        public int promptBudget() { return llm.getConversationPromptBudgetChars(); }
                    },
                    new AndroidAgentTools(app, store::statistics), System::currentTimeMillis)
                    .run(id, () -> stopped.getAsBoolean() || GoalAutonomyRuntime.get(app).hasActiveConversation());
            if (result != null && result.terminal() && CloudSyncManager.isEnabled(app)) {
                try { SyncWorker.enqueueWhenOnline(app); } catch (RuntimeException ignored) { }
            }
            return result == null || result.terminal() || store.paused();
        } finally { running.set(false); }
    }
    public String respond(AgentCommand command) {
        switch (command.action) {
            case CONFIGURE:
                app.startActivity(new android.content.Intent(app, salve.presentation.ui.ToolBridgeActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                return "He abierto la configuración de herramientas remotas.";
            case CREATE:
                AgentRun created = store.create(command.argument, command.code, bridge(), System.currentTimeMillis());
                boolean queued = !store.paused() && schedule(created, false);
                return "Plan " + shortId(created.id) + " guardado: " + created.goal
                        + (store.paused() ? "\nAutonomía pausada. Di ‘reanuda tu autonomía’."
                        : queued ? "\nConsultaré herramientas, evaluaré resultados y guardaré la respuesta. Di ‘mis planes’."
                        : "\nPendiente de programación; se recuperará al abrir Salve.");
            case SUBSCRIBE:
                AgentSubscriptionEntity subscription = store.subscribe(command.argument, command.event, command.interval, bridge(), System.currentTimeMillis());
                AgentWorker.scheduleEvents(app, true);
                return "Vigilancia " + shortId(subscription.id) + " guardada: " + subscription.goal
                        + ". Creará planes al cumplirse el evento; Android puede aplazar su ejecución. Di ‘mis vigilancias’ para revisarla o cancelarla.";
            case SUBSCRIPTIONS:
                StringBuilder subscriptions = new StringBuilder("Vigilancias autorizadas:\n");
                for (AgentSubscriptionEntity s : store.subscriptions()) subscriptions.append(shortId(s.id)).append(" · ").append(s.event)
                        .append(" · intervalo mínimo ").append(s.intervalMs / 3_600_000L).append(" h · ").append(s.goal).append('\n');
                return subscriptions.append("Cancela con ‘cancela vigilancia ID’. La pausa global también las detiene.").toString();
            case UNSUBSCRIBE:
                String id = null;
                for (AgentSubscriptionEntity s : store.subscriptions()) if (s.id.startsWith(command.argument)) {
                    if (id != null) return "El identificador es ambiguo."; id = s.id;
                }
                if (id == null) return "No encuentro esa vigilancia.";
                store.unsubscribe(id); recover(); return "Vigilancia cancelada. Los planes ya creados se conservan y pueden cancelarse por su ID.";
            case STATUS:
                return "Agente " + (store.paused() ? "pausado" : "habilitado") + ". Planes pendientes: " + store.pending().size()
                        + ". Memoria local, lectura pública y APIs HTTPS disponibles; "
                        + (bridge().isEmpty() ? "navegador/código externos sin configurar. Di ‘configura herramientas’."
                        : "puente configurado; cada ejecución comprueba su disponibilidad.")
                        + "\nModelos: " + SalveLLM.getInstance(app).getStatusDescription() + "\nResultados operativos: " + store.statistics();
            case LIST:
                StringBuilder list = new StringBuilder(store.paused() ? "Planes pausados:\n" : "Planes:\n");
                List<AgentRun> all = store.recent();
                for (int i = 0; i < Math.min(10, all.size()); i++) {
                    AgentRun r = all.get(i); list.append(shortId(r.id)).append(" · ").append(r.status).append(" · ")
                            .append(r.goal.substring(0, Math.min(100, r.goal.length()))).append('\n');
                }
                return list.append("Consulta ‘resultado de mi último plan’, ‘cancela plan ID’ o ‘reanuda plan ID’.").toString();
            default:
                AgentRun run = resolve(command.argument);
                if (run == null) return "No encuentro un plan único con ese identificador.";
                if (command.action == AgentCommand.Action.CANCEL) return store.cancel(run.id, System.currentTimeMillis())
                        ? "Plan cancelado; las respuestas tardías no se archivarán." : "El plan ya terminó.";
                if (command.action == AgentCommand.Action.RESUME) {
                    boolean resumed = store.resume(run.id, System.currentTimeMillis()); if (resumed) recover(true);
                    return resumed ? "Plan preparado para continuar con sus pasos y permisos originales." : "El plan ya está pendiente o finalizó; consulta su resultado.";
                }
                return describe(run);
        }
    }
    public AgentRun resolve(String reference) {
        AgentRun result = null;
        for (AgentRun run : store.recent()) {
            if (reference.equals("ultimo")) return run;
            if (run.id.startsWith(reference)) { if (result != null) return null; result = run; }
        }
        return result;
    }
    private String describe(AgentRun run) {
        StringBuilder text = new StringBuilder("Plan ").append(shortId(run.id)).append(" · ").append(run.status).append('\n');
        if (!run.outline.isEmpty()) text.append("Plan actual: ").append(String.join(" → ", run.outline)).append('\n');
        for (AgentRun.Observation o : run.observations) text.append(o.tool).append(" · ").append(o.status).append(" · ")
                .append(o.text.substring(0, Math.min(run.answer.isEmpty() ? 700 : 120, o.text.length()))).append('\n');
        if (!run.answer.isEmpty()) text.append("Resultado:\n").append(run.answer).append('\n');
        for (AgentRun.Observation o : run.observations) if (run.evidence.contains(o.id))
            text.append("Fuente ").append(o.id).append(": ").append(String.join(" ", o.sources)).append('\n');
        if (!run.error.isEmpty()) text.append(run.error).append('\n');
        return text.append("Inferencias: ").append(String.join("; ", run.providers)).toString();
    }
    private static String shortId(String id) { return id.substring(0, 8); }
}
