package salve.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import salve.core.GoalAutonomyRuntime;
import salve.core.MindRuntime;
import salve.core.ModelResult;
import salve.core.SalveLLM;
import salve.core.mind.BackgroundMindGate;
import salve.core.mind.CodeReviewCycle;
import salve.core.mind.CodeReviewStore;
import salve.core.mind.SelfCodeLoader;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/**
 * Mientras duerme y carga, Salve revisa su propio código: primero lo que trajo el último parche, luego
 * lo que aún no ha leído. Va por tandas cortas (Android corta un trabajo a los 10 minutos) y como mucho
 * {@link #NIGHT_BUDGET_MS} por noche. Al terminar la noche guarda su informe PDF. Nunca cambia código.
 */
public final class CodeReviewWorker extends Worker {
    private static final String TAG = "Salve/CodeReview";
    private static final String NAME = "salve_code_review";
    static final long RUN_BUDGET_MS = 8L * 60L * 1000L, NIGHT_BUDGET_MS = 60L * 60L * 1000L;

    public CodeReviewWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    private static Constraints constraints() {
        return new Constraints.Builder().setRequiresCharging(true).setRequiresBatteryNotLow(true).build();
    }

    public static void enqueue(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(CodeReviewWorker.class).setConstraints(constraints()).build());
    }

    /** La siguiente tanda, cuando termine esta. */
    private static void continueLater(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE,
                new OneTimeWorkRequest.Builder(CodeReviewWorker.class).setConstraints(constraints())
                        .setInitialDelay(1, TimeUnit.MINUTES).build());
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(NAME);
    }

    public static CodeReviewStore store(Context context) {
        return new CodeReviewStore(new java.io.File(context.getApplicationContext().getFilesDir(), "self-code-review"));
    }

    /** Una noche va de mediodía a mediodía: lo de las 2 de la madrugada es de la noche anterior. */
    static String nightKey(long nowMs) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(nowMs - 12L * 60L * 60L * 1000L);
        return String.format(java.util.Locale.ROOT, "%04d-%02d-%02d",
                c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }

    @NonNull @Override public Result doWork() {
        Context app = getApplicationContext();
        if (isStopped() || !SleepFocusWorker.active(app)) return Result.success();
        GoalAutonomyRuntime goals = GoalAutonomyRuntime.get(app);
        MindRuntime mind = MindRuntime.get(app);
        SalveLLM llm = SalveLLM.getInstance(app);
        if (llm.activeModelPath() == null) return Result.success(); // sin modelo no hay revisión que valga
        BackgroundMindGate.Lease lease = BackgroundMindGate.get().acquire(BackgroundMindGate.Task.SELF_REVIEW,
                goals.hasActiveConversation(), mind.journal().isPaused(), llm.isLoading());
        if (lease == null) return Result.success(); // otra tarea tiene el turno; el próximo ciclo de sueño la vuelve a pedir
        ExecutionJournal journal = ExecutionTrace.journal(app);
        String trace = journal.begin("Revisión de mi código", "Durante el sueño, con carga");
        String verdict = "INTERRUMPIDO";
        boolean again = false;
        try {
            CodeReviewStore store = store(app);
            long now = System.currentTimeMillis();
            store.startNight(nightKey(now));
            long left = NIGHT_BUDGET_MS - store.spentMs();
            BooleanSupplier stop = () -> isStopped() || !SleepFocusWorker.active(app)
                    || goals.hasActiveConversation() || mind.journal().isPaused();
            CodeReviewCycle.Result result = null;
            if (left > 0) {
                result = CodeReviewCycle.run(SelfCodeLoader.get(app),
                        path -> SelfCodeLoader.source(app, path), store,
                        prompt -> {
                            ModelResult r = llm.generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
                            return r != null && r.isSuccess() ? r.getText() : null;
                        },
                        stop, System::currentTimeMillis, Math.min(RUN_BUDGET_MS, left));
                journal.append(trace, "RESULTADO", result.describe());
            } else {
                journal.append(trace, "ESPERA", "Ya usé esta noche mis " + (NIGHT_BUDGET_MS / 60000) + " minutos de revisión.");
            }
            boolean nightOver = store.spentMs() >= NIGHT_BUDGET_MS || result == null
                    || result.pendingAfter == 0 || result.modelFailed;
            if (nightOver && !store.reportedNight() && !store.findings().isEmpty()) {
                int pending = result == null ? -1 : result.pendingAfter;
                java.io.File pdf = salve.core.reports.AutonomousReportWriter.saveText(app,
                        CodeReviewCycle.report(store.findings(), pending, store.reviewedCount(), nightKey(now)));
                if (pdf != null) store.markReported();
                store.save();
                journal.append(trace, "INFORME", pdf != null ? "Guardado en Informes: " + pdf.getName()
                        : "No pude escribir el PDF; lo intentaré en la próxima tanda.");
            }
            again = result != null && !result.modelFailed && !stop.getAsBoolean()
                    && result.pendingAfter > 0 && store.spentMs() < NIGHT_BUDGET_MS;
            verdict = result != null && result.modelFailed ? "MODELO_SIN_RESPUESTA" : "COMPLETADO";
            lease.complete();
            return Result.success();
        } catch (RuntimeException failed) {
            verdict = "FALLO";
            journal.append(trace, "ERROR", failed.getClass().getSimpleName() + ". No marco nada como revisado.");
            Log.w(TAG, "Revisión interrumpida: " + failed.getClass().getSimpleName());
            return Result.success();
        } finally {
            lease.close();
            journal.finish(trace, verdict);
            if (again) continueLater(app);
        }
    }
}
