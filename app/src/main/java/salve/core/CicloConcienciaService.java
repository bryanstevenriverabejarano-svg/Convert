package salve.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Compatibilidad para intents del antiguo servicio continuo.
 *
 * <p>Delega la programacion en ThinkWorker y termina. No mantiene hilos, no
 * publica copias de identidad y no se reinicia para preservar su ejecucion.
 * MainActivity programa directamente el worker en las versiones actuales.</p>
 */
public class CicloConcienciaService extends Service {

    private static final String TAG = "Salve/LegacyCycle";
    private static final String CHANNEL_ID = "salve_conciencia";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        // Un intent antiguo puede usar startForegroundService. Cumplir su
        // contrato brevemente antes de delegar y detener el servicio.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager == null) throw new IllegalStateException("NotificationManager unavailable");
                manager.createNotificationChannel(new NotificationChannel(
                        CHANNEL_ID, "Tareas de Salve", NotificationManager.IMPORTANCE_LOW));
                Notification notification = new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Salve")
                        .setContentText("Programando revisión de objetivos…")
                        .setSmallIcon(android.R.drawable.ic_menu_compass)
                        .build();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (RuntimeException error) {
                Log.w(TAG, "No se pudo iniciar la notificacion temporal: "
                        + error.getClass().getSimpleName());
                stopSelf();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            ThinkWorker.schedule(this);
        } catch (RuntimeException error) {
            Log.e(TAG, "No se pudo programar el ciclo: " + error.getClass().getSimpleName());
        } finally {
            stopForeground(true);
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
