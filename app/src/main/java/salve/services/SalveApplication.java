package salve.services;

import android.app.Application;
import salve.core.TTSManager;
import salve.data.util.CloudLogger;

public class SalveApplication extends Application {
    private static TTSManager ttsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        CloudLogger.initialize(this);
        ttsManager = new TTSManager(this);
        salve.core.tasks.ResearchTaskRuntime.CONTROLS.execute(() -> {
            try { salve.core.tasks.ResearchTaskRuntime.get(this).recover(); }
            catch (RuntimeException unavailable) {
                android.util.Log.w("Salve/Tasks", "El diario no está disponible; no se modifica ni se borra.");
            }
        });
    }

    public static TTSManager getTTS() {
        return ttsManager;
    }
}
