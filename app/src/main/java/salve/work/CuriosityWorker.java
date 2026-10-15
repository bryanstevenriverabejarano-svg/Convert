package salve.work;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import salve.core.MindRuntime;

/**
 * Curiosidad de Salve cuando está sola: cada pocas horas, con internet y batería, investiga una
 * de sus preguntas abiertas. No carga el modelo de lenguaje (solo red) y se aparta si Bryan
 * está conversando o el modelo se está cargando, para no competir con él.
 */
public class CuriosityWorker extends Worker {
    private static final String TAG = "Salve/Curiosity";
    private static final String PERIODIC = "salve_curiosity";
    private static final String NOW = "salve_curiosity_now";
    private static final String IDLE = "salve_curiosity_idle";
    private static final String CHANNEL = "salve_curiosity";
    private static final String FORCE = "force";

    public CuriosityWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        Context app = getApplicationContext();
        try {
            if (salve.core.GoalAutonomyRuntime.get(app).hasActiveConversation()
                    || salve.core.SalveLLM.getInstance(app).isLoading()) {
                Log.i(TAG, "Bryan está conversando o el modelo se carga; lo dejo para luego.");
                return Result.success();
            }
            boolean force = getInputData().getBoolean(FORCE, false);
            String outcome = MindRuntime.get(app).exploreCuriosity(this::isStopped, force);
            Log.i(TAG, outcome.length() > 160 ? outcome.substring(0, 160) : outcome);
            if (outcome.startsWith("Investigué «")) {
                notifyFinding(app, outcome);
                // Pensar sobre lo que encontró (puede abrir una pregunta nueva o cerrar la que tenía).
                // Usa el modelo local, así que solo si está cargando y nadie conversa.
                if (charging(app) && !isStopped() && !salve.core.GoalAutonomyRuntime.get(app).hasActiveConversation()) {
                    String reflection = MindRuntime.get(app).reflectIfDue(this::isStopped);
                    Log.i(TAG, "Reflexión tras explorar: " + (reflection.length() > 120 ? reflection.substring(0, 120) : reflection));
                }
            }
            return Result.success();
        } catch (Exception failure) {
            Log.w(TAG, "Curiosidad interrumpida: " + failure.getClass().getSimpleName());
            return Result.success();
        }
    }

    private static boolean charging(Context context) {
        try {
            android.os.BatteryManager battery = context.getSystemService(android.os.BatteryManager.class);
            return battery != null && battery.isCharging();
        } catch (RuntimeException unknown) { return false; }
    }

    /** Aviso discreto, solo si Android permite notificaciones a Salve. */
    private static void notifyFinding(Context context, String text) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL, "Lo que Salve investiga por su cuenta", NotificationManager.IMPORTANCE_LOW));
            Intent open = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent tap = open == null ? null : PendingIntent.getActivity(context, 7, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_menu_search)
                    .setContentTitle("Salve ha estado investigando")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setAutoCancel(true);
            if (tap != null) builder.setContentIntent(tap);
            NotificationManagerCompat.from(context).notify(4207, builder.build());
        } catch (RuntimeException ignored) { /* El hallazgo ya está guardado en su mente. */ }
    }

    private static Constraints constraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build();
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(CuriosityWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints()).setInitialDelay(30, TimeUnit.MINUTES).build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    /**
     * Bryan ha dejado el móvil (la app sigue abierta o la pantalla se ha apagado): un intento en
     * 10 minutos. Respeta el intervalo de 3 h y no sustituye un intento ya programado. Android
     * puede retrasarlo si el móvil entra en reposo profundo; cargando, suele ser puntual.
     */
    public static void enqueueWhenIdle(Context context) {
        try {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CuriosityWorker.class).setConstraints(constraints())
                    .setInitialDelay(10, TimeUnit.MINUTES).build();
            WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(IDLE, ExistingWorkPolicy.KEEP, request);
        } catch (RuntimeException unavailable) { /* El ciclo periódico sigue activo. */ }
    }

    /** «Investiga por tu cuenta»: ahora, aunque haya investigado hace poco. */
    public static void enqueueNow(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CuriosityWorker.class).setConstraints(constraints())
                .setInputData(new androidx.work.Data.Builder().putBoolean(FORCE, true).build()).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request);
    }
}
