package salve.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.util.concurrent.TimeUnit;
import salve.core.agent.AgentRuntime;

/** Content stays in Room; WorkManager only receives an opaque id or an event poll marker. */
public final class AgentWorker extends Worker {
    public AgentWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    @NonNull @Override public Result doWork() {
        try {
            AgentRuntime runtime = AgentRuntime.get(getApplicationContext());
            String id = getInputData().getString("plan_id");
            if (id == null && getInputData().getBoolean("poll", false)) {
                runtime.signal("TIMER");
                android.content.Intent battery = getApplicationContext().registerReceiver(null,
                        new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                if (battery != null && battery.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0) runtime.signal("CHARGING");
                return Result.success();
            }
            if (id == null || !id.matches("[a-f0-9-]{36}")) return Result.failure();
            return runtime.run(id, this::isStopped) ? Result.success() : Result.retry();
        } catch (RuntimeException unavailable) { return Result.retry(); }
    }
    public static void enqueue(Context context, String id, long delay, boolean append) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AgentWorker.class)
                .setInputData(new Data.Builder().putString("plan_id", id).build()).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context).enqueueUniqueWork("salve_plan_" + id,
                append ? ExistingWorkPolicy.APPEND_OR_REPLACE : ExistingWorkPolicy.KEEP, request);
    }
    public static void scheduleEvents(Context context, boolean active) {
        WorkManager manager = WorkManager.getInstance(context);
        if (!active) { manager.cancelUniqueWork("salve_agent_events"); return; }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(AgentWorker.class, 1, TimeUnit.HOURS)
                .setInputData(new Data.Builder().putBoolean("poll", true).build())
                .setConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build()).build();
        manager.enqueueUniquePeriodicWork("salve_agent_events", ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
