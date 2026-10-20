package salve.data.continuity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;
import salve.core.compute.ComputeNode;
import salve.core.compute.ComputeNodeStore;

/** Copia nocturna de la memoria de Salve en su Guardián: solo cargando y con Wi-Fi. */
public final class ContinuityWorker extends Worker {
    private static final String NAME = "salve-continuity-nightly";

    public ContinuityWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.UNMETERED).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(ContinuityWorker.class, 24, TimeUnit.HOURS).setConstraints(constraints)
                        .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES).build());
    }

    public static void cancel(Context context) { WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(NAME); }

    @NonNull @Override public Result doWork() {
        Context app = getApplicationContext();
        SalveContinuity continuity = new SalveContinuity(app);
        if (!continuity.prefs().getBoolean("auto", false)) return Result.success();
        String nodeId = continuity.prefs().getString("node", null);
        ComputeNode guardian = null;
        for (ComputeNode n : new ComputeNodeStore(app).load()) if (n.id.equals(nodeId)) guardian = n;
        ContinuitySecret secret = new ContinuitySecret(app);
        if (guardian == null || !secret.exists()) return Result.success();
        try {
            // Misma puerta que sueño, reflexión, curiosidad y juego: nunca a mitad de otro ciclo ni durante una charla.
            SalveContinuity.Result done = continuity.backupInBackground(guardian, secret.load(), this::isStopped,
                    salve.core.GoalAutonomyRuntime.get(app).hasActiveConversation(),
                    salve.core.MindRuntime.get(app).journal().isPaused(),
                    salve.core.SalveLLM.getInstance(app).isLoading());
            return done != null || getRunAttemptCount() >= 6 ? Result.success() : Result.retry();
        } catch (Exception failure) {
            return getRunAttemptCount() < 2 ? Result.retry() : Result.success();   // el error queda en el estado
        }
    }
}
