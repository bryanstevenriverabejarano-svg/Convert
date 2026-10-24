package salve.work;

import android.content.Context;
import android.os.Build;
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
 * Sueño de Salve: una vez al día, con el móvil cargando y en reposo.
 *
 * <p>Poda vectores de recuerdos borrados, indexa por significado los nuevos y consolida las
 * experiencias recientes en el modelo de sí misma. No depende del reconocimiento facial:
 * consolidar memoria es mantenimiento interno, no una acción que deba autenticarse.</p>
 */
public class DreamWorker extends Worker {
    private static final String TAG = "Salve/DreamWorker";
    public static final String PERIODIC_NAME = "salve_dream_nightly";
    public static final String ONE_TIME_NAME = "salve_dream_now";

    public DreamWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (isStopped()) return Result.success();
        salve.core.mind.BackgroundMindGate.Lease lease = salve.core.mind.BackgroundMindGate.get().acquire(
                salve.core.mind.BackgroundMindGate.Task.DREAM,
                salve.core.GoalAutonomyRuntime.get(getApplicationContext()).hasActiveConversation(),
                salve.core.MindRuntime.get(getApplicationContext()).journal().isPaused(),
                salve.core.SalveLLM.getInstance(getApplicationContext()).isLoading());
        if (lease == null) return Result.success();
        try {
            salve.core.synergy.SynergyRuntime synergy=salve.core.synergy.SynergyRuntime.get(getApplicationContext());
            synergy.record(salve.core.synergy.SynergyCore.Event.NONE);
            if (!synergy.allowReflection(salve.core.MindRuntime.get(getApplicationContext()).journal().isPaused(),
                    salve.core.GoalAutonomyRuntime.get(getApplicationContext()).hasActiveConversation()))
                return Result.success();

            String outcome = MindRuntime.get(getApplicationContext()).dream(this::isStopped);
            Log.i(TAG, "Sueño finalizado: " + (outcome == null ? "" : outcome.length() > 160 ? outcome.substring(0, 160) : outcome));
            lease.complete();
            return Result.success();
        } catch (Exception failure) {
            if (isStopped()) return Result.success();
            Log.e(TAG, "Fallo del sueño: " + failure.getClass().getSimpleName());
            return Result.retry();
        } finally { lease.close(); }
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DreamWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(constraints(true))
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    /** Sueño solicitado por Bryan: no exige reposo del dispositivo, sí carga. */
    public static void enqueueNow(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DreamWorker.class)
                .setConstraints(constraints(false))
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                ONE_TIME_NAME, ExistingWorkPolicy.KEEP, request);
    }

    private static Constraints constraints(boolean idle) {
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true);
        if (idle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) builder.setRequiresDeviceIdle(true);
        return builder.build();
    }
}
