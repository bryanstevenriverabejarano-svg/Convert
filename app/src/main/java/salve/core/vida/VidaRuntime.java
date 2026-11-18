package salve.core.vida;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Una sola instancia de {@link VidaService} por proceso: el chat y la pantalla del gym
 * comparten el mismo candado, el mismo archivo y la misma cola de subida a pCloud.
 */
public final class VidaRuntime {
    private static VidaService instance;
    private VidaRuntime() {}

    public static synchronized VidaService get(Context context) {
        if (instance != null) return instance;
        Context app = context.getApplicationContext();
        instance = new VidaService(new File(app.getNoBackupFilesDir(), "vida/vida.tsv"), Clock.systemDefaultZone(),
                new VidaService.Cloud() {
                    // Una sola cola: una copia vieja nunca termina después de una nueva.
                    private final ExecutorService uploads = Executors.newSingleThreadExecutor();
                    @Override public String fetch() throws java.io.IOException {
                        return salve.data.sync.CloudSyncManager.fetchVida(app);
                    }
                    @Override public void push(String content) {
                        uploads.execute(() -> {
                            boolean ok = salve.data.sync.CloudSyncManager.uploadVida(app, content);
                            Log.d("Salve/Vida", "Vida → pCloud: " + (ok ? "verificada" : "pendiente"));
                        });
                    }
                });
        return instance;
    }
}
