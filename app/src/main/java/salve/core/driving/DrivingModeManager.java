package salve.core.driving;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.car.app.connection.CarConnection;
import androidx.lifecycle.Observer;
import salve.presentation.ui.MainActivity;
import salve.core.voice.AssistantLaunch;

/**
 * Detects Android Auto projection while the Salve process is running.
 *
 * A car connection enables a *profile*, not a microphone or a car-display app.
 * Android background-activity and microphone restrictions are respected: tapping
 * a notification brings the existing voice UI to the phone foreground.
 */
public final class DrivingModeManager {
    private static final String TAG = "Salve/Conduccion";
    private static final String PREFS = "salve_driving_mode";
    private static final String ENABLED = "auto_profile_enabled";
    private static final String CHANNEL = "salve_driving_ready";
    private static final int NOTIFICATION = 4318;
    private static volatile DrivingModeManager instance;

    private final Context app;
    private final SharedPreferences preferences;
    private CarConnection connection;
    private final Observer<Integer> observer;
    private volatile int type = -1;
    private boolean started;

    private DrivingModeManager(Context context) {
        app = context.getApplicationContext();
        preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        observer = value -> {
            int next = value == null ? -1 : value;
            int previous = type;
            type = next;
            if (previous != next) {
                Log.i(TAG, "Conexión Android for Cars: " + next);
                updateNotification();
            }
        };
    }

    public static synchronized DrivingModeManager get(Context context) {
        if (instance == null) instance = new DrivingModeManager(context);
        return instance;
    }

    /** Called on the main thread from Application.onCreate. No background service is started. */
    public synchronized void start() {
        if (started) return;
        started = true;
        try {
            connection = new CarConnection(app);
            connection.getType().observeForever(observer);
        } catch (RuntimeException unsupported) {
            type = -1;
            Log.w(TAG, "No se pudo observar Android Auto", unsupported);
        }
    }

    public boolean isEnabled() {
        return preferences.getBoolean(ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean(ENABLED, enabled).apply();
        updateNotification();
    }

    public int connectionType() {
        return type;
    }

    public boolean isActive() {
        return DrivingModePolicy.active(isEnabled(), type);
    }

    public String description() {
        return DrivingModePolicy.status(isEnabled(), type);
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (!isActive()) {
            manager.cancel(NOTIFICATION);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Salve · Modo conducción", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Acceso a la conversación por voz al conectar Android Auto");
            manager.createNotificationChannel(channel);
        }
        Intent openVoice = new Intent(app, MainActivity.class)
                .setAction(AssistantLaunch.ACTION_VOICE_COMMAND)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent launch = PendingIntent.getActivity(app, 4318, openVoice,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(app, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Salve · Modo conducción")
                .setContentText("Android Auto conectado. Toca para hablar con Salve.")
                .setContentIntent(launch)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();
        try { manager.notify(NOTIFICATION, notification); }
        catch (RuntimeException rejected) { Log.w(TAG, "Android rechazó la notificación de conducción"); }
    }
}
