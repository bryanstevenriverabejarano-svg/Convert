package salve.work;

import android.content.Context;
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

import salve.core.MindRuntime;

/**
 * Juego propio de Salve cuando está sola: se inventa retos, los resuelve con el laboratorio y
 * aprende qué estrategia le funciona. Solo CPU acotada (sin modelo ni red) y se aparta si Bryan
 * está conversando o el modelo se carga.
 */
public class PlayWorker extends Worker {
    private static final String TAG = "Salve/Play";
    private static final String PERIODIC = "salve_play";
    private static final String NOW = "salve_play_now";
    private static final String IDLE = "salve_play_idle";
    private static final String ROUNDS = "rounds";

    public PlayWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        Context app = getApplicationContext();
        salve.core.mind.BackgroundMindGate.Lease lease = salve.core.mind.BackgroundMindGate.get().acquire(
                salve.core.mind.BackgroundMindGate.Task.PLAY,
                salve.core.GoalAutonomyRuntime.get(getApplicationContext()).hasActiveConversation(),
                salve.core.MindRuntime.get(getApplicationContext()).journal().isPaused(),
                salve.core.SalveLLM.getInstance(getApplicationContext()).isLoading());
        if (lease == null) return Result.success();
        try {

            if (salve.core.GoalAutonomyRuntime.get(app).hasActiveConversation() || salve.core.SalveLLM.getInstance(app).isLoading())
                return Result.success();
            java.util.function.BooleanSupplier stop = () -> isStopped() || salve.core.GoalAutonomyRuntime.get(app).hasActiveConversation();
            String outcome = MindRuntime.get(app).play(stop, getInputData().getInt(ROUNDS, 4));
            Log.i(TAG, outcome.length() > 160 ? outcome.substring(0, 160) : outcome);
            lease.complete();
        } catch (Exception failure) {
            Log.w(TAG, "Juego interrumpido: " + failure.getClass().getSimpleName());
        } finally { lease.close(); }
        return Result.success();
    }

    private static Constraints constraints() { return new Constraints.Builder().setRequiresBatteryNotLow(true).build(); }

    public static void schedule(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(PlayWorker.class, 2, TimeUnit.HOURS).setConstraints(constraints())
                        .setInitialDelay(20, TimeUnit.MINUTES).build());
    }

    /** Al dejar el móvil: unas rondas a los 15 minutos (no sustituye un intento ya programado). */
    public static void enqueueWhenIdle(Context context) {
        try {
            WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(IDLE, ExistingWorkPolicy.KEEP,
                    new OneTimeWorkRequest.Builder(PlayWorker.class).setConstraints(constraints())
                            .setInitialDelay(15, TimeUnit.MINUTES).build());
        } catch (RuntimeException unavailable) { /* El ciclo periódico sigue activo. */ }
    }

    /** «Juega un rato»: ahora, unas rondas más. */
    public static void enqueueNow(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(PlayWorker.class)
                        .setInputData(new androidx.work.Data.Builder().putInt(ROUNDS, 6).build()).build());
    }
}
