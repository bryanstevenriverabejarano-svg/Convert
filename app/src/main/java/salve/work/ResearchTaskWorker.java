package salve.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;
import salve.core.tasks.ResearchTaskRuntime;

/** WorkManager carries only an opaque task id; content remains in the private Room database. */
public class ResearchTaskWorker extends Worker {
    public ResearchTaskWorker(@NonNull Context context, @NonNull WorkerParameters parameters) { super(context, parameters); }
    @NonNull @Override public Result doWork() {
        String id = getInputData().getString("task_id");
        if (id == null || !id.matches("[a-f0-9-]{36}")) return Result.failure();
        try {
            return ResearchTaskRuntime.get(getApplicationContext()).run(id, this::isStopped) ? Result.success() : Result.retry();
        } catch (RuntimeException unavailable) { return Result.retry(); }
    }
    public static void enqueue(Context context, String id, long delay, boolean append) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ResearchTaskWorker.class)
                .setInputData(new Data.Builder().putString("task_id", id).build())
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true).build()).build();
        // Explicit resume may race an old worker returning success after a pause. Append a
        // successor so KEEP cannot discard the wake-up; routine startup recovery stays unique.
        WorkManager.getInstance(context).enqueueUniqueWork("salve_research_" + id,
                append ? ExistingWorkPolicy.APPEND_OR_REPLACE : ExistingWorkPolicy.KEEP, work);
    }
}
