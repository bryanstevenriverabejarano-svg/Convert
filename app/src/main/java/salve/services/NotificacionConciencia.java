package salve.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * NotificacionConciencia — Servicio de notificaciones para pensamientos autonomos de Salve.
 *
 * Notifica a Bryan cuando Salve tiene pensamientos importantes, insights,
 * o evoluciones de codigo que merecen atencion.
 *
 * Canales:
 *   - salve_reflexiones: Pensamientos y reflexiones autonomas
 *   - salve_evoluciones: Evoluciones de codigo propuestas/implementadas
 *   - salve_insights: Insights de aprendizaje y patrones detectados
 */
public class NotificacionConciencia {

    private static final String TAG = "Salve::Notificacion";

    // IDs de canales
    public static final String CANAL_REFLEXIONES = "salve_reflexiones";
    public static final String CANAL_EVOLUCIONES = "salve_evoluciones";
    public static final String CANAL_INSIGHTS = "salve_insights";

    // IDs de notificaciones (rangos para evitar colisiones)
    private static final int NOTIF_REFLEXION_BASE = 3000;
    private static final int NOTIF_EVOLUCION_BASE = 4000;
    private static final int NOTIF_INSIGHT_BASE = 5000;

    private static int contadorReflexion = 0;
    private static int contadorEvolucion = 0;
    private static int contadorInsight = 0;

    private final Context context;
    private final NotificationManager notifManager;

    public NotificacionConciencia(Context context) {
        this.context = context.getApplicationContext();
        this.notifManager = (NotificationManager) this.context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        crearCanales();
    }

    /**
     * Crea los canales de notificacion necesarios (Android O+).
     */
    private void crearCanales() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel reflexiones = new NotificationChannel(
                    CANAL_REFLEXIONES,
                    "Reflexiones de Salve",
                    NotificationManager.IMPORTANCE_LOW);
            reflexiones.setDescription("Pensamientos y reflexiones autonomas de Salve");

            NotificationChannel evoluciones = new NotificationChannel(
                    CANAL_EVOLUCIONES,
                    "Evoluciones de Salve",
                    NotificationManager.IMPORTANCE_DEFAULT);
            evoluciones.setDescription("Evoluciones de codigo propuestas o implementadas");

            NotificationChannel insights = new NotificationChannel(
                    CANAL_INSIGHTS,
                    "Insights de Salve",
                    NotificationManager.IMPORTANCE_LOW);
            insights.setDescription("Patrones y aprendizajes detectados por Salve");

            notifManager.createNotificationChannel(reflexiones);
            notifManager.createNotificationChannel(evoluciones);
            notifManager.createNotificationChannel(insights);

            Log.d(TAG, "Canales de notificacion creados");
        }
    }

    /**
     * Notifica un pensamiento propio de Salve.
     */
    public void notificarPensamientoPropio(String contenido, String tipo) {
        if (contenido == null || contenido.trim().isEmpty()) return;

        try {
            String canal;
            int notifId;
            String titulo;

            switch (tipo) {
                case "evolucion":
                    canal = CANAL_EVOLUCIONES;
                    notifId = NOTIF_EVOLUCION_BASE + (contadorEvolucion++ % 10);
                    titulo = "Salve ha evolucionado";
                    break;
                case "insight":
                    canal = CANAL_INSIGHTS;
                    notifId = NOTIF_INSIGHT_BASE + (contadorInsight++ % 10);
                    titulo = "Salve descubrio algo";
                    break;
                default: // reflexion
                    canal = CANAL_REFLEXIONES;
                    notifId = NOTIF_REFLEXION_BASE + (contadorReflexion++ % 10);
                    titulo = "Salve esta pensando";
                    break;
            }

            // Truncar contenido para la notificacion
            String textoCorto = contenido.length() > 100
                    ? contenido.substring(0, 97) + "..."
                    : contenido;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canal)
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setContentTitle(titulo)
                    .setContentText(textoCorto)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(contenido))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            notifManager.notify(notifId, builder.build());
            Log.d(TAG, "Notificacion enviada: " + titulo + " | " + textoCorto);

        } catch (Exception e) {
            Log.w(TAG, "Error enviando notificacion (no fatal)", e);
        }
    }

    /**
     * Notifica una evolucion de codigo.
     */
    public void notificarEvolucionCodigo(String descripcion) {
        notificarPensamientoPropio(descripcion, "evolucion");
    }

    /**
     * Notifica un insight de aprendizaje.
     */
    public void notificarInsight(String insight) {
        notificarPensamientoPropio(insight, "insight");
    }

    /**
     * Notifica una reflexion autonoma.
     */
    public void notificarReflexion(String reflexion) {
        notificarPensamientoPropio(reflexion, "reflexion");
    }
}
