package salve.core.tasks;

import android.content.Context;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import salve.core.GoalAutonomyRuntime;
import salve.core.ModelResult;
import salve.core.SalveLLM;
import salve.core.WikipediaResearchClient;
import salve.core.research.PublicResearchCoordinator;
import salve.data.db.MemoriaDatabase;
import salve.data.sync.CloudSyncManager;
import salve.data.sync.SyncWorker;
import salve.data.tasks.RoomResearchTaskStore;
import salve.work.ResearchTaskWorker;

/** Application-scoped runtime. Background research uses public reads and local inference only. */
public final class ResearchTaskRuntime {
    public static final java.util.concurrent.ExecutorService CONTROLS = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static volatile ResearchTaskRuntime instance;
    private final Context app;
    private final RoomResearchTaskStore store;
    private final AtomicBoolean running = new AtomicBoolean();
    private ResearchTaskRuntime(Context context) {
        app = context.getApplicationContext();
        store = new RoomResearchTaskStore(MemoriaDatabase.getInstance(app), () -> CloudSyncManager.isEnabled(app));
        store.initializeControl(GoalAutonomyRuntime.get(app).isPaused());
    }
    public static ResearchTaskRuntime get(Context context) {
        if (instance == null) synchronized (ResearchTaskRuntime.class) {
            if (instance == null) instance = new ResearchTaskRuntime(context);
        }
        return instance;
    }
    public RoomResearchTaskStore store() { return store; }
    public void pause(boolean paused) {
        store.pause(paused, System.currentTimeMillis());
        if (!paused) recover(true);
    }
    public void recover() { recover(false); }
    private void recover(boolean append) {
        if (store.paused()) return;
        for (ResearchTask task : store.pending()) schedule(task, append);
    }
    private boolean schedule(ResearchTask task, boolean append) {
        try {
            long ready = Math.max(task.nextAttemptAt, task.owner.isEmpty() ? 0 : task.leaseUntil);
            ResearchTaskWorker.enqueue(app, task.id, Math.max(0, ready - System.currentTimeMillis()), append);
            return true;
        } catch (RuntimeException unavailable) { return false; }
    }
    public String respond(TaskCommand command) {
        switch (command.action) {
            case CREATE:
                ResearchTask created = store.create(command.argument, System.currentTimeMillis());
                boolean scheduled = !store.paused() && schedule(created, false);
                return "Tarea " + shortId(created) + " guardada: " + created.question
                        + (store.paused() ? "\nLas tareas están pausadas. Di ‘reanuda tus tareas’."
                        : scheduled ? "\nInvestigaré con fuentes públicas y guardaré el resultado. Puedes consultar ‘mis tareas’."
                        : "\nEstá pendiente de programación; volveré a intentarlo al abrir Salve.");
            case LIST:
                StringBuilder list = new StringBuilder(store.paused() ? "Tareas pausadas.\n" : "Tareas de investigación:\n");
                List<ResearchTask> tasks = store.recent(10);
                if (tasks.isEmpty()) return "Todavía no hay tareas. Puedes decir ‘investiga y recuerda: tema’.";
                for (ResearchTask task : tasks) list.append(shortId(task)).append(" · ").append(state(task))
                        .append(" · ").append(task.question.substring(0, Math.min(100, task.question.length()))).append('\n');
                return list.append("Di ‘resultado tarea ID’, ‘cancela tarea ID’ o ‘reanuda tarea ID’.").toString();
            case PAUSE_ALL: pause(true); return "Tareas pausadas; se conservan sus resultados y puntos de recuperación.";
            case RESUME_ALL: pause(false); salve.core.agent.AgentRuntime.get(app).pause(false); return "Tareas reanudadas; continuarán cuando Android permita ejecutarlas.";
            default:
                ResearchTask task = resolve(command.argument);
                if (task == null) return "No encuentro una tarea única con ese identificador. Consulta ‘mis tareas’.";
                if (command.action == TaskCommand.Action.CANCEL) return store.cancel(task.id, System.currentTimeMillis())
                        ? "Tarea cancelada. Los resultados tardíos no se guardarán." : "La tarea ya terminó; su resultado se conserva.";
                if (command.action == TaskCommand.Action.RESUME) {
                    boolean resumed = store.resume(task.id, System.currentTimeMillis());
                    if (resumed) recover(true);
                    return resumed ? "Tarea preparada para reanudarse."
                            : "La tarea ya está pendiente o terminada; consulta su resultado.";
                }
                String header = "Tarea " + shortId(task) + " · " + state(task) + " · intentos: " + task.attempts;
                if (task.error != null) header += "\n" + task.error;
                if (task.receipt == null) return header;
                ResearchReceipt receipt = ResearchReceipt.decode(task.receipt);
                return header + "\n" + receipt.answer + "\nInferencias: "
                        + (receipt.providers.isEmpty() ? "sin inferencia; sólo lectura de fuentes" : String.join("; ", receipt.providers));
        }
    }
    public ResearchTask resolve(String reference) {
        ResearchTask found = null;
        for (ResearchTask task : store.recent(50)) {
            if (reference.equals("ultima")) return task;
            if (task.id.startsWith(reference)) { if (found != null) return null; found = task; }
        }
        return found;
    }
    /** False means a worker should retry later, preserving the same task id. */
    public boolean run(String id, BooleanSupplier stopped) {
        if (store.paused()) return true;
        if (GoalAutonomyRuntime.get(app).hasActiveConversation() || !running.compareAndSet(false, true)) return false;
        try {
            ResearchTask task = new ResearchTaskEngine(store, this::research, System::currentTimeMillis)
                    .run(id, () -> stopped.getAsBoolean() || GoalAutonomyRuntime.get(app).hasActiveConversation());
            if (task != null && task.terminal()) {
                if (CloudSyncManager.isEnabled(app)) {
                    try { SyncWorker.enqueueWhenOnline(app); } catch (RuntimeException ignored) { }
                }
                return true;
            }
            return task == null || store.paused();
        } finally { running.set(false); }
    }
    private ResearchReceipt research(String question, BooleanSupplier stopped) throws Exception {
        SalveLLM llm = SalveLLM.getInstance(app);
        ResearchReceipt receipt = new ResearchReceipt();
        PublicResearchCoordinator.Result result = new PublicResearchCoordinator(
                new WikipediaResearchClient()::researchResult,
                (phase, prompt) -> {
                    if (stopped.getAsBoolean()) return ModelResult.failure(ModelResult.Status.CANCELLED, "Interrumpido", 0);
                    ModelResult generated = llm.generateResult(prompt, SalveLLM.Role.EVALUADOR);
                    String provider = generated.getProvider() == null ? "proveedor local no confirmado" : generated.getProvider();
                    String trace = phase + ": " + provider + " · " + generated.getStatus() + " · " + generated.getLatencyMillis() + " ms";
                    receipt.providers.add(trace.substring(0, Math.min(240, trace.length())));
                    return generated;
                }, llm.getConversationPromptBudgetChars()).runConversation(question, stopped);
        if (result.status == PublicResearchCoordinator.Status.CANCELLED) throw new CancellationException();
        if (result.sources.isEmpty()) throw new ResearchTaskEngine.Unavailable(
                result.status == PublicResearchCoordinator.Status.FETCH_FAILED || result.status == PublicResearchCoordinator.Status.BUDGET_EXHAUSTED);
        receipt.status = result.status == PublicResearchCoordinator.Status.ANSWERED ? "ANSWERED"
                : result.status == PublicResearchCoordinator.Status.PARTIAL ? "PARTIAL" : "SOURCES_ONLY";
        receipt.answer = result.toConversationText();
        for (PublicResearchCoordinator.Source source : result.sources)
            receipt.sources.add(new ResearchReceipt.Source(source.url, source.excerpt));
        return receipt;
    }
    private static String shortId(ResearchTask task) { return task.id.substring(0, 8); }
    private static String state(ResearchTask task) {
        switch (task.status) {
            case "QUEUED": return "pendiente";
            case "RUNNING": return "investigando";
            case "STAGED": return "fuentes guardadas; pendiente de archivar";
            case "SUCCEEDED": return "completada y recordada";
            case "PARTIAL": return "resultado parcial guardado";
            case "CANCELLED": return "cancelada";
            default: return "necesita revisión";
        }
    }
}
