package salve.data.sync;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/**
 * Salve se sincroniza sola con pCloud: no hace falta pulsar «Sincronizar» ni «Recuperar».
 *
 * <ul>
 * <li>Un trabajo periódico (cada hora, con red) sube lo nuevo y trae lo que falte.</li>
 * <li>Al abrir la app, al conversar o cuando ella necesita sus recuerdos, «despierta» la sincronización:
 * si el trabajo anterior quedó aplazado por reintentos (WorkManager los espacia hasta horas), lo
 * sustituye por uno inmediato. Con un mínimo de 10 minutos entre despertares para no gastar batería.</li>
 * <li>Si la cuenta está conectada y Bryan no la pausó a propósito, la reactiva.</li>
 * </ul>
 */
public final class CloudAutoSync {
    static final String PREFS = "salve_memory_restore";
    private static final String PERIODIC = "salve_sync_periodic";
    static final long KICK_INTERVAL_MS = 10 * 60_000L;

    private CloudAutoSync() { }

    /** Decide si conviene despertar ya (puro, para pruebas). */
    static boolean shouldKick(long now, long lastKick, long lastRun, boolean complete) {
        if (now - lastKick < KICK_INTERVAL_MS) return false;
        // Si ya está todo recuperado y corrió hace poco, basta con el periódico.
        return !complete || now - lastRun > 60 * 60_000L;
    }

    /** Despierta la sincronización si toca. Seguro desde cualquier hilo. @return true si la lanzó */
    public static boolean kick(Context context, String reason) {
        if (context == null) return false;
        Context app = context.getApplicationContext();
        try {
            if (!CloudSyncManager.isPCloudConfigured(app)) return false;
            if (!CloudSyncManager.isEnabled(app)) {
                if (CloudSyncManager.pausedByUser(app)) return false;
                CloudSyncManager.setEnabled(app, true, false); // conectada pero sin activar: la activa ella
            }
            schedulePeriodic(app);
            SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            if (!shouldKick(now, prefs.getLong("kick_at", 0L), prefs.getLong("worker_run_at", 0L),
                    CloudSyncManager.memoryRestoreComplete(app))) return false;
            prefs.edit().putLong("kick_at", now).putString("kick_reason", reason == null ? "" : reason).apply();
            // Si el trabajador está corriendo ahora mismo, no lo corta: se encola detrás.
            if (now - prefs.getLong("worker_run_at", 0L) < 3 * 60_000L && prefs.getLong("worker_done_at", 0L)
                    < prefs.getLong("worker_run_at", 0L)) SyncWorker.enqueueWhenOnline(app);
            else SyncWorker.restartNow(app);
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    /** Trabajo de cada hora con red; no duplica si ya existe. */
    public static void schedulePeriodic(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(SyncWorker.class, 1, TimeUnit.HOURS).setConstraints(constraints).build());
    }

    /** Recuerdos que trajo sola desde la última vez que se lo contó a Bryan (y los marca como contados). */
    public static int takeUnannounced(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int n = prefs.getInt("restored_unannounced", 0);
        if (n > 0) prefs.edit().putInt("restored_unannounced", 0).apply();
        return n;
    }

    static void noteRestored(Context context, int restored) {
        if (restored <= 0) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt("restored_unannounced", prefs.getInt("restored_unannounced", 0) + restored).apply();
    }
}
