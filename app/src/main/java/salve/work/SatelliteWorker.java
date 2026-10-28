package salve.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Mantiene vivo a un satélite: cada poco late hacia la Salve del dueño (a través de su nodo) y, si hay
 * un trabajo de cómputo asignado, lo corre y devuelve el resultado. Solo se programa en una instalación
 * que es satélite y ya está unida a una Salve; en la Salve del dueño no se programa nunca.
 *
 * <p>Solo con red. No exige carga ni reposo: un satélite debe estar disponible para servir.</p>
 */
public class SatelliteWorker extends Worker {
    private static final String TAG = "Salve/Satelite";
    public static final String PERIODIC_NAME = "salve_satellite_serve";
    private static final int ROUNDS = 4; // unas pocas vueltas por despertar, sin quedarse colgado

    public SatelliteWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull
    @Override
    public Result doWork() {
        salve.core.satellite.SatelliteStore store = new salve.core.satellite.SatelliteStore(getApplicationContext());
        if (store.role().fullSalve() || store.parentEndpoint() == null) return Result.success();
        salve.core.satellite.SatelliteLink link = new salve.core.satellite.SatelliteLink(getApplicationContext());
        link.register();
        for (int i = 0; i < ROUNDS && !isStopped(); i++) {
            try {
                if (!link.serveOnce()) break; // no hay trabajo: hasta el próximo despertar
            } catch (RuntimeException failure) {
                Log.w(TAG, "Vuelta de satélite fallida: " + failure.getClass().getSimpleName());
                return Result.retry();
            }
        }
        return Result.success();
    }

    public static void schedule(Context context) {
        salve.core.satellite.SatelliteStore store = new salve.core.satellite.SatelliteStore(context);
        if (store.role().fullSalve()) return; // la Salve del dueño no sirve de satélite
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SatelliteWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }
}
