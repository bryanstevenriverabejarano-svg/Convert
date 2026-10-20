package salve.core;

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

/**
 * Entrada de WorkManager a la reflexión interna y al ciclo de objetivos de Salve.
 *
 * <p>El runtime selecciona como maximo un paso y aplica su propio intervalo entre
 * ciclos. El worker no abre otra conversacion, no lanza tareas independientes y
 * no transforma una reflexion en un cambio de identidad. La cancelacion se pasa
 * al runtime para comprobarla tambien entre generacion y persistencia.</p>
 */
public class ThinkWorker extends Worker {

    private static final String TAG = "Salve/ThinkWorker";
    public static final String UNIQUE_NAME = "think_worker_unique";
    /** Conserva el nombre instalado por versiones anteriores de MainActivity. */
    public static final String PERIODIC_NAME = "salve_think_job";

    public ThinkWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (isStopped()) return Result.success();
        salve.core.mind.BackgroundMindGate.Lease lease = salve.core.mind.BackgroundMindGate.get().acquire(
                salve.core.mind.BackgroundMindGate.Task.REFLECTION_AND_GOALS,
                salve.core.GoalAutonomyRuntime.get(getApplicationContext()).hasActiveConversation(),
                salve.core.MindRuntime.get(getApplicationContext()).journal().isPaused(),
                salve.core.SalveLLM.getInstance(getApplicationContext()).isLoading());
        if (lease == null) return Result.success();
        try {

            GoalAutonomyRuntime runtime = GoalAutonomyRuntime.get(getApplicationContext());
            if (isStopped()) return Result.success();

            // Pensamiento interno a partir de lo vivido: no requiere notas de Bryan ni aprobación,
            // porque no produce efectos fuera de Salve. Queda en su modelo de sí misma.
            try {
                String reflection = MindRuntime.get(getApplicationContext()).reflectIfDue(this::isStopped);
                Log.i(TAG, "Reflexión: " + (reflection.length() > 120 ? reflection.substring(0, 120) : reflection));
            } catch (RuntimeException reflectionFailure) {
                Log.w(TAG, "Reflexión fallida: " + reflectionFailure.getClass().getSimpleName());
            }
            if (isStopped() || runtime.isPaused()) return Result.success();

            // Un resultado normal indica que termino esta invocacion, no que el
            // objetivo se cumplio: puede estar pausado, pendiente o bloqueado.
            // El runtime conserva el resultado semantico; no se deduce del texto.
            runtime.runCycle(this::isStopped);
            if (isStopped()) return Result.success();
            Log.i(TAG, "Invocacion del ciclo de objetivos finalizada.");
            lease.complete();
            return Result.success();
        } catch (Exception error) {
            if (isStopped()) return Result.success();
            // Ni el resultado ni mensajes de excepcion pueden filtrar objetivos.
            Log.e(TAG, "Fallo del ciclo de objetivos: " + error.getClass().getSimpleName());
            return Result.failure();
        } finally { lease.close(); }
    }

    /**
     * Programa una comprobacion cada hora. UPDATE conserva el trabajo periodico
     * existente y migra sus restricciones, sin crear un segundo programador.
     * El runtime mantiene el intervalo real entre pasos de objetivos.
     */
    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ThinkWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(backgroundConstraints())
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    /** Entrada compatible con llamadores antiguos; no reinicia un paso activo. */
    public static void enqueue(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ThinkWorker.class)
                .setConstraints(backgroundConstraints())
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                UNIQUE_NAME, ExistingWorkPolicy.KEEP, request);
    }

    private static Constraints backgroundConstraints() {
        return new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .build();
    }
}
