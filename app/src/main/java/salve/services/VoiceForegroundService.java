package salve.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Conversación por voz que sigue aunque Bryan salga de la app o apague la pantalla (como el modo voz de
 * otras apps): notificación fija «Salve está contigo» con el estado y «Terminar». Solo existe mientras
 * hay una conversación por voz abierta; la empieza Bryan con la app delante (Android no deja abrir el
 * micrófono desde segundo plano) y se cierra con ella. No escucha nada por su cuenta.
 */
public final class VoiceForegroundService extends Service {
    private static final String TAG = "Salve/VozFondo";
    private static final String CHANNEL = "salve_voice_live";
    private static final int NOTIFICATION = 4211;
    private static final String EXTRA_TEXT = "text";
    private static final String END = "salve.voice.END";
    /** El ciclo de voz más largo sin novedad es de unos minutos; el cerrojo se renueva en cada estado. */
    private static final long WAKE_MS = 10L * 60L * 1000L;

    /** Lo avisa la notificación cuando Bryan pulsa «Terminar». */
    public interface EndListener { void endFromNotification(); }

    private static volatile EndListener listener;
    private static volatile boolean running;
    private PowerManager.WakeLock wakeLock;

    public static boolean isRunning() { return running; }

    /** Llamar con la app en primer plano, al empezar la conversación por voz. */
    public static void start(Context context, String text, EndListener onEnd) {
        listener = onEnd;
        Intent intent = new Intent(context, VoiceForegroundService.class).putExtra(EXTRA_TEXT, text);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (RuntimeException refused) {
            // Sin servicio la voz sigue con la app delante, como antes; al salir se cerrará.
            Log.w(TAG, "Android no permitió el servicio de voz: " + refused.getClass().getSimpleName());
        }
    }

    /** Nuevo estado en la notificación («Te escucho…», «Pensando…», «Hablando…»). */
    public static void update(Context context, String text) {
        if (!running) return;
        try {
            context.startService(new Intent(context, VoiceForegroundService.class).putExtra(EXTRA_TEXT, text));
        } catch (RuntimeException ignored) { /* la notificación se queda con el estado anterior */ }
    }

    public static void stop(Context context) {
        listener = null;
        context.stopService(new Intent(context, VoiceForegroundService.class));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && END.equals(intent.getAction())) {
            EndListener l = listener;
            if (l != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(l::endFromNotification);
            else stopSelf();
            return START_NOT_STICKY;
        }
        String text = intent == null ? null : intent.getStringExtra(EXTRA_TEXT);
        Notification n = notification(text == null ? "Conversación por voz abierta" : text);
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            else startForeground(NOTIFICATION, n);
        } catch (RuntimeException refused) {
            Log.w(TAG, "Sin primer plano para el micrófono: " + refused.getClass().getSimpleName());
            stopSelf();
            return START_NOT_STICKY;
        }
        running = true;
        holdCpu();
        return START_NOT_STICKY; // si Android lo mata, no vuelve a abrir el micrófono por su cuenta
    }

    /** Con la pantalla apagada, que pensar la respuesta no se quede a medias. */
    private void holdCpu() {
        try {
            if (wakeLock == null) {
                PowerManager pm = getSystemService(PowerManager.class);
                if (pm == null) return;
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "salve:voz");
                wakeLock.setReferenceCounted(false);
            }
            wakeLock.acquire(WAKE_MS);
        } catch (RuntimeException ignored) { /* sin cerrojo: puede ir más lenta con la pantalla apagada */ }
    }

    private Notification notification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null
                && manager.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Conversación por voz", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Mientras hablas con Salve fuera de la app");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        int immutable = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (open != null) open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent content = open == null ? null
                : PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | immutable);
        PendingIntent end = PendingIntent.getService(this, 1,
                new Intent(this, VoiceForegroundService.class).setAction(END), PendingIntent.FLAG_UPDATE_CURRENT | immutable);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Salve está contigo")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_CALL)
                .addAction(new Notification.Action.Builder(null, "Terminar", end).build());
        if (content != null) b.setContentIntent(content);
        return b.build();
    }

    @Override public void onDestroy() {
        running = false;
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (RuntimeException ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
