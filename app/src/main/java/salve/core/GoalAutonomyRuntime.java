package salve.core;

import android.content.Context;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import salve.core.goals.GoalAutonomy;
import salve.core.goals.GoalFileStore;
import salve.core.memory.ConversationMemoryGrounding;
import salve.data.db.MemoriaDatabase;

/** One application-scoped goal journal shared by conversation and WorkManager. */
public final class GoalAutonomyRuntime {
    private static volatile GoalAutonomyRuntime instance;
    private final GoalAutonomy goals;
    private final AtomicInteger activeTurns = new AtomicInteger();

    private GoalAutonomyRuntime(Context context) {
        Context app = context.getApplicationContext();
        MemoriaDatabase database = MemoriaDatabase.getInstance(app);
        ConversationMemoryGrounding memory = new ConversationMemoryGrounding(database.recuerdoDao(),
                database.knowledgeNodeDao(), database.knowledgeRelationDao());
        goals = new GoalAutonomy(new GoalFileStore(new File(app.getNoBackupFilesDir(),
                "autonomy/goals.json")), prompt -> {
            // These notes may be personal. Background reflection never falls back to a cloud model.
            ModelResult result = SalveLLM.getInstance(app).generateResult(prompt, SalveLLM.Role.EVALUADOR);
            if (!result.isSuccess()) throw new IllegalStateException("Modelo local no disponible o fallido.");
            return result.getText();
        }, System::currentTimeMillis);
        goals.setIdentityEvidenceSupplier(() -> memory.retrieve(
                "Salve identidad propósito capacidades configuración recuerdos aprendizajes experiencias sobre quién es")
                .getContext());
    }

    public static GoalAutonomyRuntime get(Context context) {
        if (instance == null) {
            synchronized (GoalAutonomyRuntime.class) {
                if (instance == null) instance = new GoalAutonomyRuntime(context);
            }
        }
        return instance;
    }

    public boolean isPaused() { return goals.isPaused(); }

    /** Invalidates a pending background result without retaining the user's utterance. */
    public void userActivity() { goals.userActivity(); }

    public void beginUserTurn() { activeTurns.incrementAndGet(); }

    public void endUserTurn() { activeTurns.decrementAndGet(); }

    public String respond(String input) { return goals.respond(input); }

    public String runCycle(BooleanSupplier stopped) {
        if (activeTurns.get() > 0) return "La conversación tiene prioridad; el ciclo queda pendiente.";
        return goals.runCycle(() -> stopped.getAsBoolean() || activeTurns.get() > 0);
    }
}
