package salve.core.privatefamily;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import com.salve.app.R;
import com.wireguard.android.backend.GoBackend;
import salve.presentation.ui.PrivateFamilyActivity;

/** Foreground subclass initializes GoBackend's VpnService before requesting the tunnel. */
public final class FamilyVpnService extends GoBackend.VpnService {
    public static final String STOP = "salve.private.family.STOP";
    public static volatile boolean running;
    private volatile boolean destroyed;
    @Override public void onCreate() {
        super.onCreate(); running = true;
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("private-vpn", "Red privada familiar", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 8201, new Intent(this, PrivateFamilyActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 8202, new Intent(this, FamilyVpnService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, "private-vpn").setSmallIcon(android.R.drawable.ic_menu_myplaces)
                .setContentTitle("Red privada de Salve").setContentText("Conexión WireGuard activa o conectando")
                .setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(new Notification.Action.Builder(null, "Desconectar", stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(8201, note, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(8201, note);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        PrivateVpn vpn = PrivateVpn.get(this);
        if (intent == null || STOP.equals(intent.getAction())) {
            vpn.invalidate(); PrivateVpn.CONTROL.execute(() -> { try { vpn.disconnect(); } catch (Exception ignored) { } stopSelf(); });
        } else PrivateVpn.CONTROL.execute(() -> {
            if (destroyed) return;
            try { vpn.connect(); if (destroyed) vpn.disconnect(); }
            catch (Exception failure) { vpn.failed(); stopSelf(); }
        });
        return START_NOT_STICKY;
    }
    @Override public void onRevoke() { PrivateVpn.get(this).invalidate(); super.onRevoke(); }
    @Override public void onDestroy() { destroyed = true; PrivateVpn.get(this).invalidate(); super.onDestroy(); running = false; }
}
