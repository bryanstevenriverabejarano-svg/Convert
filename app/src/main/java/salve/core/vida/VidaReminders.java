package salve.core.vida;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Avisos de Vida: resumen de la mañana y cada bloque del horario unos minutos antes. Un solo
 * aviso programado cada vez (AlarmManager, sin permisos de alarma exacta): al sonar, programa el
 * siguiente. Se reprograma al cambiar el horario y al reiniciar el móvil.
 */
public final class VidaReminders {
    private static final String PREFS = "salve_vida_avisos", CHANNEL = "vida";
    private VidaReminders() {}

    public static boolean enabled(Context c) { return prefs(c).getBoolean("on", true); }
    public static String morning(Context c) { return prefs(c).getString("morning", "08:00"); }
    public static int lead(Context c) { return prefs(c).getInt("lead", 10); }

    public static void configure(Context c, boolean on, String morning, int lead) {
        prefs(c).edit().putBoolean("on", on).putString("morning", morning).putInt("lead", lead).apply();
    }

    /** Programa el siguiente aviso. Llamar fuera del hilo de la pantalla (lee el registro). */
    public static void schedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pending(app, null);
        alarms.cancel(pending);
        if (!enabled(app)) return;
        try {
            VidaLog log = VidaRuntime.get(app).read();
            LocalTime m = null;
            try { m = LocalTime.parse(morning(app)); } catch (RuntimeException ignored) { }
            VidaCoach.Reminder next = VidaCoach.nextReminder(log, LocalDateTime.now(), m, lead(app));
            if (next == null) return;
            long at = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(app, next));
        } catch (Exception e) { Log.w("Salve/Vida", "No se pudo programar el aviso", e); }
    }

    private static PendingIntent pending(Context app, VidaCoach.Reminder r) {
        Intent i = new Intent(app, Receiver.class).setAction("salve.vida.AVISO");
        if (r != null) i.putExtra("title", r.title).putExtra("text", r.text);
        return PendingIntent.getBroadcast(app, 7101, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static void notify(Context app, String title, String text) {
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || title == null) return;
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Vida: avisos del horario", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent();
        open.setClassName(app, "salve.presentation.ui.VidaActivity");
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(app, 7102, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification n = new android.app.Notification.Builder(app, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title).setContentText(text)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                .setContentIntent(tap).setAutoCancel(true).build();
        try { nm.notify((int) (System.currentTimeMillis() % 100000), n); }
        catch (SecurityException noPermission) { Log.w("Salve/Vida", "Sin permiso de notificaciones"); }
    }

    /** Suena el aviso (y reprograma el siguiente); también se despierta al reiniciar el móvil. */
    public static final class Receiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            PendingResult result = goAsync();
            Context app = context.getApplicationContext();
            new Thread(() -> {
                try {
                    if ("salve.vida.AVISO".equals(intent.getAction()))
                        VidaReminders.notify(app, intent.getStringExtra("title"), intent.getStringExtra("text"));
                    schedule(app);
                } finally { result.finish(); }
            }).start();
        }
    }
}
