package salve.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Solo existe para que Android deje a Salve ver y controlar la música que suena en otras apps
 * (MediaSessionManager.getActiveSessions exige un servicio de notificaciones con permiso concedido).
 * No lee, guarda ni envía ninguna notificación: los métodos de notificaciones no hacen nada.
 */
public final class SalveMusicListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) { /* no se leen notificaciones */ }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) { /* no se leen notificaciones */ }
}
