package salve.core.satellite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.gson.JsonObject;

/**
 * El trabajo de pantalla de un satélite: mostrar lo que la Salve del dueño quiere enseñar en este
 * móvil (un mensaje suyo). De momento lo muestra como una notificación del sistema, que es visible sin
 * tener la app delante; el espejo completo del avatar en vivo es una integración de interfaz posterior.
 */
public final class SatelliteScreen {
    private static final String TAG = "Salve/Satelite";
    private static final String CHANNEL = "salve_satelite_pantalla";

    private SatelliteScreen() { }

    public static JsonObject execute(Context context, SatelliteProtocol.Assignment task) {
        String text = SatelliteProtocol.screenText(task == null ? null : task.params);
        if (text.isEmpty()) return SatelliteProtocol.simpleOutput("error", "El trabajo de pantalla no traía nada que mostrar.");
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return SatelliteProtocol.simpleOutput("error", "Este móvil no puede mostrar notificaciones.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL, "Salve (satélite)", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Lo que tu Salve muestra en este móvil satélite.");
                manager.createNotificationChannel(channel);
            }
            Notification notification = new Notification.Builder(context, CHANNEL)
                    .setSmallIcon(context.getApplicationInfo().icon)
                    .setContentTitle("Salve")
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setAutoCancel(true)
                    .build();
            manager.notify(TAG.hashCode(), notification);
            return SatelliteProtocol.simpleOutput("ok", "Mostrado en el satélite.");
        } catch (RuntimeException failure) {
            Log.w(TAG, "No se pudo mostrar en pantalla", failure);
            return SatelliteProtocol.simpleOutput("error", "No se pudo mostrar en este móvil.");
        }
    }
}
