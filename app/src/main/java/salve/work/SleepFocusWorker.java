package salve.work;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.function.BooleanSupplier;

import salve.avatar.AvatarState;
import salve.core.GoalAutonomyRuntime;
import salve.core.MindRuntime;
import salve.core.SalveLLM;
import salve.core.mind.BackgroundMindGate;

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
        MindRuntime mind = MindRuntime.get(app);
        GoalAutonomyRuntime goals = GoalAutonomyRuntime.get(app);
        SalveLLM llm = SalveLLM.getInstance(app);
        BackgroundMindGate.Lease lease = BackgroundMindGate.get().acquire(
                BackgroundMindGate.Task.DREAM, goals.hasActiveConversation(),
                mind.journal().isPaused(), llm.isLoading());
        if (lease == null) return Result.success(); // The foreground or another cycle has priority.
        BooleanSupplier stop = () -> isStopped() || Thread.currentThread().isInterrupted()
                || !active(app) || goals.hasActiveConversation() || mind.journal().isPaused();
        try {
            if (stop.getAsBoolean()) return Result.success();
            salve.core.synergy.SynergyRuntime synergy = salve.core.synergy.SynergyRuntime.get(app);
            synergy.record(salve.core.synergy.SynergyCore.Event.NONE);
            if (!synergy.allowReflection(mind.journal().isPaused(), goals.hasActiveConversation()))
                return Result.success();

            mind.dream(stop);             // Consolidate sourced memories without inventing evidence.
            if (stop.getAsBoolean()) return Result.success();
            mind.reflectIfDue(stop);      // New questions, contradictions and self-assessment.
            if (stop.getAsBoolean()) return Result.success();
            mind.play(stop, 1);           // One verifiable local strategy challenge, never code deployment.
            if (stop.getAsBoolean()) return Result.success();
            lease.complete();
            Log.i(TAG, "Ciclo de concentración nocturna completado");
            return Result.success();
        } catch (RuntimeException failed) {
            if (stop.getAsBoolean()) return Result.success();
            Log.w(TAG, "Ciclo interrumpido: " + failed.getClass().getSimpleName());
            return getRunAttemptCount() < 1 ? Result.retry() : Result.failure();
        } finally {
            lease.close();
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
        // Existing public research already has its own cooldown, network and privacy safeguards.
        CuriosityWorker.enqueueWhenIdle(app);
    }

    /** User wake-up disables future concentrated cycles and cancels pending work. */
    public static void stop(Context context) {
        Context app = context.getApplicationContext();
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, false).commit();
        WorkManager work = WorkManager.getInstance(app);
        work.cancelUniqueWork(ONCE);
        work.cancelUniqueWork(PERIODIC);
    }
}
