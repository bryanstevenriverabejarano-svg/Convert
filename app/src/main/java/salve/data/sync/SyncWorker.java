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
            CloudSyncManager.indexLocalJournal(getApplicationContext());
            CloudSyncManager.enqueueExistingProfiles(getApplicationContext());
            CloudSyncManager.enqueueArchivedPhotos(getApplicationContext());
            int enviados = CloudSyncManager.flush(getApplicationContext(), 50);
            Log.d(TAG, "Flush completado. Enviados=" + enviados);
            CloudSyncManager.RestoreBatch restored = CloudSyncManager.restoreMemoryBatch(getApplicationContext(), 200);
            boolean outboxPending = CloudSyncManager.hasPending(getApplicationContext());
            // Backoff is for network failures. Successful history pages continue immediately,
            // otherwise thousands of old memories would take days as retry delays grow.
            if (restored.retry || (outboxPending && enviados == 0)) return Result.retry();
            if (restored.more || outboxPending) enqueueNextBatch(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error durante flush", e);
            return Result.retry();
        }
    }

    private static void enqueueNextBatch(Context context) {
        if (!CloudSyncManager.isEnabled(context)) return;
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE,
                new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(constraints).build());
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
