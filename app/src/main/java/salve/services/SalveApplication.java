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
    }

    public static TTSManager getTTS() {
        return ttsManager;
    }
}
