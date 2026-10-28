package salve.work;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

import salve.avatar.AvatarState;
import salve.core.GoalAutonomyRuntime;
import salve.core.MindRuntime;
import salve.core.SalveLLM;
import salve.core.mind.BackgroundMindGate;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;
import salve.core.evaluation.SleepImprovementRuntime;
import salve.core.evaluation.SleepImprovementCycle;

/**
 * "Dormir" is a persistent, user-controlled focus mode, not an endless LLM loop.
 * The character stays asleep visually while a bounded cycle reviews personal memories,
 * reflects and tests one local skill. Android may defer work while the phone is unplugged.
 */
public final class SleepFocusWorker extends Worker {
    private static final String TAG = "Salve/SleepFocus";
    private static final String PREFS = "salve_sleep_focus";
    private static final String ENABLED = "enabled";
    private static final String PERIODIC = "salve_sleep_focus_periodic";
    private static final String ONCE = "salve_sleep_focus_once";
    private static final ExecutorService TRACE_WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "salve-sleep-trace"); t.setDaemon(true); return t;
    });

    /** An event of scheduling is not a completed sleep session. Never copy private memories to the trace. */
    private static void scheduledTrace(Context app, String description, String status) {
        TRACE_WRITER.execute(() -> {
            ExecutionJournal journal = ExecutionTrace.journal(app);
            String id = journal.begin("Sueño autónomo", "Control del modo de sueño");
            if (id != null) {
                journal.append(id, "PLANIFICACIÓN", description);
                journal.finish(id, status);
            }
        });
    }

    public SleepFocusWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Use persisted avatar state, not the UI singleton (which belongs to the main thread). */
    private static boolean active(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String avatar = app.getSharedPreferences("salve_avatar", Context.MODE_PRIVATE)
                .getString("state", null);
        return SleepFocusPolicy.active(prefs.getBoolean(ENABLED, false), AvatarState.decode(avatar));
    }

    @NonNull @Override public Result doWork() {
        Context app = getApplicationContext();
        if (isStopped() || !active(app)) return Result.success();
        ExecutionJournal journal = ExecutionTrace.journal(app);
        String traceId = journal.begin("Sueño autónomo", "Intento de concentración (WorkManager)");
        MindRuntime mind = MindRuntime.get(app);
        GoalAutonomyRuntime goals = GoalAutonomyRuntime.get(app);
        SalveLLM llm = SalveLLM.getInstance(app);
        BackgroundMindGate.Lease lease = BackgroundMindGate.get().acquire(
                BackgroundMindGate.Task.DREAM, goals.hasActiveConversation(),
                mind.journal().isPaused(), llm.isLoading());
        if (lease == null) {
            journal.append(traceId, "ESPERA", "No se obtuvo el turno cognitivo: conversación, pausa, carga del modelo u otra tarea activa.");
            journal.finish(traceId, "APLAZADO");
            return Result.success(); // The foreground or another cycle has priority.
        }
        BooleanSupplier stop = () -> isStopped() || Thread.currentThread().isInterrupted()
                || !active(app) || goals.hasActiveConversation() || mind.journal().isPaused();
        String verdict = "INTERRUMPIDO";
        try {
            if (stop.getAsBoolean()) return Result.success();
            salve.core.synergy.SynergyRuntime synergy = salve.core.synergy.SynergyRuntime.get(app);
            synergy.record(salve.core.synergy.SynergyCore.Event.NONE);
            if (!synergy.allowReflection(mind.journal().isPaused(), goals.hasActiveConversation())) {
                journal.append(traceId, "ESPERA", "La política de recursos no autorizó la reflexión nocturna.");
                verdict = "APLAZADO";
                return Result.success();
            }

            journal.append(traceId, "INICIO", "Permisos de batería y puerta cognitiva superados. No se publica código.");
            String dream = mind.dream(stop);
            journal.append(traceId, "MEMORIA", dream != null && dream.startsWith("Sueño completado")
                    ? "Se consolidaron experiencias mediante el ciclo existente."
                    : "No hay confirmación de nuevas experiencias consolidadas.");
            if (stop.getAsBoolean()) return Result.success();
            mind.reflectIfDue(stop);
            journal.append(traceId, "REFLEXIÓN", "Se consultó el ciclo de reflexión; puede aplazarse por intervalo o ausencia de novedades.");
            if (stop.getAsBoolean()) return Result.success();
            salve.core.evaluation.NightPracticePolicy.Plan plan =
                    salve.core.evaluation.NightPracticePolicy.choose(
                            mind.skills().history(), System.currentTimeMillis());
            journal.append(traceId, "PLAN DE PRÁCTICA",
                    plan.reason + "; máximo " + plan.rounds + " retos acotados.");
            String practice = mind.play(stop, plan.rounds);
            salve.core.evaluation.NightPracticePolicy.Outcome observed =
                    salve.core.evaluation.NightPracticePolicy.observed(practice, plan.rounds);
            journal.append(traceId, "PRÁCTICA LOCAL",
                    "Ejecutados: " + observed.attempted + ", soluciones verificadas: "
                    + observed.solved + ", persistencia: " + observed.saved
                    + ". Motivo: " + observed.reason + ".");
            if (stop.getAsBoolean()) return Result.success();
            boolean saved = SleepImprovementRuntime.record(app, mind, observed, stop);
            if (stop.getAsBoolean()) return Result.success();
            if (!saved) {
                journal.append(traceId, "INFORME", "No pude guardar de forma atómica el informe. No afirmo haberlo conservado.");
                verdict = "INFORME_NO_GUARDADO";
                return getRunAttemptCount() < 1 ? Result.retry() : Result.failure();
            }
            journal.append(traceId, "INFORME", SleepImprovementCycle.describe(SleepImprovementRuntime.last(app)));
            if (stop.getAsBoolean()) return Result.success();
            // Weekly local holdout, independent from the model and the practice samples.
            // Never installs learned skills or edits Android/GitHub, and yields on cancellation.
            String transfer = salve.core.evaluation.NightTransferTrial.runIfDue(app, stop);
            journal.append(traceId, "TRANSFERENCIA CIEGA", transfer);
            if (stop.getAsBoolean()) return Result.success();
            lease.complete();
            verdict = "COMPLETADO";
            Log.i(TAG, "Ciclo de concentración nocturna completado");
            return Result.success();
        } catch (RuntimeException failed) {
            if (stop.getAsBoolean()) return Result.success();
            verdict = "FALLO";
            journal.append(traceId, "ERROR", "El ciclo falló: " + failed.getClass().getSimpleName() + ". Sin afirmar aprendizaje.");
            Log.w(TAG, "Ciclo interrumpido: " + failed.getClass().getSimpleName());
            return getRunAttemptCount() < 1 ? Result.retry() : Result.failure();
        } finally {
            lease.close();
            journal.finish(traceId, verdict);
        }
    }

    /** Opt in after the avatar was actually laid down. Sessions need charging + sufficient battery. */
    public static void start(Context context) {
        Context app = context.getApplicationContext();
        String saved = app.getSharedPreferences("salve_avatar", Context.MODE_PRIVATE)
                .getString("state", null);
        if (!SleepFocusPolicy.active(true, AvatarState.decode(saved))) return;
        if (!app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(ENABLED, true).commit()) return;
        WorkManager work = WorkManager.getInstance(app);
        Constraints battery = new Constraints.Builder()
                .setRequiresCharging(true).setRequiresBatteryNotLow(true).build();
        work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(SleepFocusWorker.class, 2, TimeUnit.HOURS)
                        .setConstraints(battery).build());
        work.enqueueUniqueWork(ONCE, ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(SleepFocusWorker.class)
                        .setConstraints(battery).build());
        // Record the intention as scheduled, not as a successful dream.
        BatteryManager batteryManager = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
        boolean charging = batteryManager != null && batteryManager.isCharging();
        scheduledTrace(app, charging
                ? "Trabajo solicitado. Android puede retrasarlo por otras tareas o ahorro de energía."
                : "Esperando cargador: el sueño visual está activo, pero ningún aprendizaje nocturno está confirmado.",
                charging ? "PROGRAMADO" : "ESPERANDO_CARGA");
        // Existing public research already has its own cooldown, network and privacy safeguards.
        CuriosityWorker.enqueueWhenIdle(app);
    }

    /** User wake-up disables future concentrated cycles and cancels pending work. */
    public static void stop(Context context) {
        Context app = context.getApplicationContext();
        boolean wasEnabled = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false);
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, false).commit();
        WorkManager work = WorkManager.getInstance(app);
        work.cancelUniqueWork(ONCE);
        work.cancelUniqueWork(PERIODIC);
        if (wasEnabled) scheduledTrace(app, "Se desactivó el modo sueño y se cancelaron los trabajos pendientes.", "DESACTIVADO");
    }
}
