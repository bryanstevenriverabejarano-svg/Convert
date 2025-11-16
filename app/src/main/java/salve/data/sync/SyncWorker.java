package salve.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * SyncWorker
 *
 * Worker que fuerza un flush de eventos pendientes cuando haya conexión a Internet.
 * Usa WorkManager con constraints de red.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final String UNIQUE_NAME = "salve_sync_flush";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            int enviados = CloudSyncManager.flush(getApplicationContext(), 50);
            Log.d(TAG, "Flush completado. Enviados=" + enviados);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error durante flush", e);
            return Result.retry();
        }
    }

    /**
     * Programa un flush cuando haya Internet disponible.
     * Usa enqueueUniqueWork para evitar trabajos duplicados.
     */
    public static void enqueueWhenOnline(Context ctx) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(ctx).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP, // mantiene el flush si ya está programado
                req
        );
    }
}
