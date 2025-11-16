package salve.services;

import android.app.Application;

import salve.core.TTSManager;

public class SalveApplication extends Application {
    private static TTSManager ttsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ttsManager = new TTSManager(this);
    }

    public static TTSManager getTTS() {
        return ttsManager;
    }
}