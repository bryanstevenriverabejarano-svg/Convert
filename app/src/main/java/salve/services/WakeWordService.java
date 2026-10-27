package salve.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;
import salve.core.voice.AssistantLaunch;
import salve.core.voice.VoiceSelectionPolicy;
import salve.core.voice.WakeWord;

/**
 * Palabra de activación «Salve». Mientras Bryan la tenga activada, escucha con el reconocimiento
 * <b>sin Internet</b> del propio móvil y, si una frase empieza llamándola («Salve…», «oye Salve…»),
 * abre la conversación por voz con lo que dijo detrás.
 *
 * <ul>
 *   <li>Solo funciona con reconocimiento local: si el móvil no lo tiene, no escucha (nunca envía
 *       audio continuo a Internet).</li>
 *   <li>Notificación fija con «Desactivar»; se para sola en modo ahorro de batería.</li>
 *   <li>Cede el micrófono mientras dura una conversación por voz y lo recupera después.</li>
 * </ul>
 */
public final class WakeWordService extends Service {
    private static final String TAG = "Salve/Palabra";
    private static final String CHANNEL = "salve_wake_word";
    private static final int NOTIFICATION = 4207;
    private static final String STOP = "salve.wake.STOP";
    private static final String PREFS = "salve_prefs";
    private static final String ENABLED = "wake_word_enabled";
    /** Si la conversación no llega a abrirse, vuelve a escuchar pasado este tiempo. */
    private static final long WAKE_GRACE_MS = 20_000L;

    private static volatile WakeWordService instance;
    private static volatile boolean conversationActive;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private int failures;
    private boolean paused;

    // ── Control desde la app ────────────────────────────────────────────

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(ENABLED, false);
    }

    /** Activar (llamar con la app en primer plano: Android no deja abrir el micrófono desde segundo plano). */
    public static String enable(Context context) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return "Necesito permiso de micrófono. Abre «Hablar con Salve» una vez para concederlo y vuelve a pedírmelo.";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
            return "Este móvil no tiene reconocimiento de voz sin Internet, y no voy a enviar audio continuo a la red. No puedo activar la palabra «Salve».";
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ENABLED, true).apply();
        ensureRunning(context);
        return "Hecho: ahora te escucho esperando mi nombre, sin Internet. Di «Salve» o «oye Salve» y lo que quieras. "
                + "Verás una notificación fija con «Desactivar»; para quitarlo di «desactiva la palabra Salve». Gasta algo de batería, "
                + "y en modo ahorro de batería me pauso sola.";
    }

    public static String disable(Context context) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ENABLED, false).apply();
        context.stopService(new Intent(context, WakeWordService.class));
        return "Hecho: ya no escucho esperando mi nombre.";
    }

    /** Al abrir la app: si Bryan la tenía activada, vuelve a ponerla en marcha. */
    public static void ensureRunning(Context context) {
        if (!isEnabled(context) || instance != null) return;
        try { context.startForegroundService(new Intent(context, WakeWordService.class)); }
        catch (RuntimeException notAllowed) { Log.w(TAG, "Android no dejó iniciar la escucha ahora", notAllowed); }
    }

    /** La conversación por voz necesita el micrófono: se lo cedemos. */
    public static void pauseForConversation() {
        conversationActive = true;
        WakeWordService service = instance;
        if (service != null) service.main.post(service::release);
    }

    public static void resumeAfterConversation() {
        conversationActive = false;
        WakeWordService service = instance;
        if (service != null) service.main.postDelayed(service::listen, 800);
    }

    // ── Servicio ────────────────────────────────────────────────────────

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        showNotification("Esperando que digas «Salve»… (sin Internet)");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            disable(this);
            stopSelf();
            return START_NOT_STICKY;
        }
        listen();
        // Sin reinicio automático: Android no permite abrir el micrófono desde segundo plano.
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        release();
        instance = null;
        super.onDestroy();
    }

    private void listen() {
        if (recognizer != null || conversationActive || instance != this) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            showNotification("No puedo escuchar sin Internet en este móvil; palabra «Salve» desactivada.");
            stopSelf();
            return;
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null && power.isPowerSaveMode()) {
            if (!paused) showNotification("En pausa por ahorro de batería. Vuelvo cuando lo quites.");
            paused = true;
            main.postDelayed(this::listen, 60_000L);
            return;
        }
        if (paused) { paused = false; showNotification("Esperando que digas «Salve»… (sin Internet)"); }
        try {
            final SpeechRecognizer capture = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            recognizer = capture;
            capture.setRecognitionListener(new RecognitionListener() {
                private boolean current() { return recognizer == capture; }
                @Override public void onReadyForSpeech(Bundle params) { }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() { }
                @Override public void onPartialResults(Bundle partialResults) { }
                @Override public void onEvent(int eventType, Bundle params) { }
                @Override public void onResults(Bundle results) {
                    if (!current()) return;
                    failures = 0;
                    ArrayList<String> heard = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (heard != null) for (String phrase : heard) {
                        WakeWord.Hit hit = WakeWord.detect(phrase);
                        if (hit != null) { wake(hit.request); return; }
                    }
                    restart(250L);
                }
                @Override public void onError(int error) {
                    if (!current()) return;
                    switch (error) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            failures = 0;
                            restart(250L);
                            return;
                        case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                        case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                            showNotification("Falta el paquete de voz en español sin Internet. Instálalo en Ajustes de Android y vuelve a activarme.");
                            release();
                            stopSelf();
                            return;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            release();
                            stopSelf();
                            return;
                        default:
                            // Ocupado (otra app usa el micrófono) u otro fallo: espera creciente, hasta 30 s.
                            failures = Math.min(failures + 1, 5);
                            restart(Math.min(30_000L, 1000L << failures));
                    }
                }
            });
            String language = VoiceSelectionPolicy.speakerLanguageTag(Locale.getDefault(), "es-ES");
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            capture.startListening(intent);
        } catch (RuntimeException unavailable) {
            Log.w(TAG, "No se pudo escuchar", unavailable);
            release();
            failures = Math.min(failures + 1, 5);
            main.postDelayed(this::listen, Math.min(30_000L, 1000L << failures));
        }
    }

    private void restart(long delayMs) {
        release();
        main.postDelayed(this::listen, delayMs);
    }

    private void release() {
        SpeechRecognizer old = recognizer;
        recognizer = null;
        if (old != null) {
            try { old.cancel(); } catch (RuntimeException ignored) { }
            try { old.destroy(); } catch (RuntimeException ignored) { }
        }
    }

    /** Me han llamado: abro la conversación por voz con lo que dijo detrás de mi nombre. */
    private void wake(String request) {
        release();
        conversationActive = true;
        Intent open = new Intent(this, salve.presentation.ui.MainActivity.class)
                .setAction(AssistantLaunch.ACTION_WAKE)
                .putExtra(AssistantLaunch.EXTRA_REQUEST, request == null ? "" : request)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(open);
        } catch (RuntimeException blocked) {
            Log.w(TAG, "Android no dejó abrir la app desde segundo plano", blocked);
            showWakeNotification(open);
        }
        // Si la conversación no llega a abrirse, vuelve a escuchar.
        main.postDelayed(() -> { if (conversationActive && !salve.presentation.ui.LiveVoiceSession.isAnyShowing()) resumeAfterConversation(); },
                WAKE_GRACE_MS);
    }

    private void showWakeNotification(Intent open) {
        NotificationManager notifications = getSystemService(NotificationManager.class);
        PendingIntent tap = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        notifications.notify(NOTIFICATION + 1, new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Me has llamado")
                .setContentText("Toca para hablar con Salve. (Activa «Mostrar sobre otras apps» para que me abra sola.)")
                .setContentIntent(tap).setAutoCancel(true).build());
    }

    private void showNotification(String text) {
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "Palabra «Salve»", NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop = PendingIntent.getService(this, 0, new Intent(this, WakeWordService.class).setAction(STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, salve.presentation.ui.MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Salve te escucha esperando su nombre")
                .setContentText(text).setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Desactivar", stop).build()).build();
        try {
            if (Build.VERSION.SDK_INT >= 30) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            else startForeground(NOTIFICATION, notification);
        } catch (RuntimeException notAllowed) {
            Log.w(TAG, "Android no permitió el servicio de micrófono ahora", notAllowed);
            stopSelf();
        }
    }
}
