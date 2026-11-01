package salve.work;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;
import salve.avatar.AvatarStore;
import salve.core.trace.ExecutionTrace;

/**
 * Mira cada ~15 minutos (lo mínimo que permite Android) si el móvil está cargando y sin usar. No usa
 * el modelo ni la red: solo decide acostar o despertar a Salve con {@link AutoSleepPolicy}; el trabajo
 * del sueño lo sigue haciendo {@link SleepFocusWorker} con sus propias condiciones.
 */
public final class AutoSleepWorker extends Worker {
    private static final String TAG = "Salve/AutoSleep";
    private static final String NAME = "salve_auto_sleep_sampler";
    static final String PREFS = "salve_auto_sleep";
    private static final String ENABLED = "enabled", LAST_USE = "last_use", AUTO = "auto_sleeping", AUTO_BED = "auto_bed";

    public AutoSleepWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    public static void schedule(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(AutoSleepWorker.class, 15, TimeUnit.MINUTES).build());
    }

    /** Bryan está usando Salve ahora mismo (la app en primer plano cuenta como uso del móvil). */
    public static void touch(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(LAST_USE, System.currentTimeMillis()).apply();
    }

    /** Un sueño o despertar manual deja de ser «automático». */
    static void forgetAuto(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(AUTO, false).putBoolean(AUTO_BED, false).apply();
    }

    @NonNull @Override public Result doWork() {
        Context app = getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        PowerManager power = app.getSystemService(PowerManager.class);
        BatteryManager battery = app.getSystemService(BatteryManager.class);
        boolean interactive = power == null || power.isInteractive(); // sin dato: se asume en uso
        boolean charging = battery != null && battery.isCharging();
        boolean sleepActive = SleepFocusWorker.active(app);
        boolean auto = prefs.getBoolean(AUTO, false);
        if (auto && !sleepActive) { forgetAuto(app); auto = false; } // Bryan la despertó él
        long lastUse = prefs.getLong(LAST_USE, 0L);
        AutoSleepPolicy.Action action = AutoSleepPolicy.decide(prefs.getBoolean(ENABLED, true),
                interactive, charging, now, lastUse, sleepActive, auto);
        prefs.edit().putLong(LAST_USE, AutoSleepPolicy.nextLastUse(interactive, now, lastUse)).apply();
        if (action == AutoSleepPolicy.Action.SLEEP) sleep(app, prefs, now - lastUse);
        else if (action == AutoSleepPolicy.Action.WAKE) wake(app, prefs, interactive);
        return Result.success();
    }

    private static void sleep(Context app, SharedPreferences prefs, long idleMs) {
        new Handler(Looper.getMainLooper()).post(() -> {
            AvatarStore store = AvatarStore.get(app);
            boolean hadBed = store.state().hasBed();
            store.change(s -> { s.createBed(); s.sleep(); });
            prefs.edit().putBoolean(AUTO, true).putBoolean(AUTO_BED, !hadBed).commit();
            SleepFocusWorker.start(app, true);
            ExecutionTrace.note("SUEÑO AUTOMÁTICO", "Cargando y sin uso desde hace "
                    + (idleMs / 60000L) + " min: me acuesto y empiezo mi ciclo de sueño.");
            Log.i(TAG, "Sueño automático iniciado");
        });
    }

    private static void wake(Context app, SharedPreferences prefs, boolean interactive) {
        new Handler(Looper.getMainLooper()).post(() -> {
            boolean removeBed = prefs.getBoolean(AUTO_BED, false);
            SleepFocusWorker.stop(app);
            AvatarStore.get(app).change(s -> { if (removeBed) s.removeBed(); else s.wake(); });
            forgetAuto(app);
            ExecutionTrace.note("SUEÑO AUTOMÁTICO", interactive
                    ? "Bryan volvió a usar el móvil: me despierto." : "Me desenchufaron: me despierto.");
            Log.i(TAG, "Sueño automático terminado");
        });
    }
}
