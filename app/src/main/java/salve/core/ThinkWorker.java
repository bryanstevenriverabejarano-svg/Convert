package salve.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo; // <-- IMPORTANTE: tipos de FGS (Android 14+)
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Conectividad
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

// Tus imports propios
import salve.data.util.CloudLogger;

// ✅ Nuevo: para organización del grafo con LLM
import salve.core.GrafoConocimientoVivo;

public class ThinkWorker extends Worker {
    private static final String TAG = "ThinkWorker";

    // Notificación en primer plano (requerido si quieres que siga vivo)
    private static final String CH_ID = "salve_bg";
    private static final int NOTIF_ID = 77;

    // Tipo(s) de Foreground Service que usará este Worker
    // Si en el futuro este Worker también usa cámara, combina con "| ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA"
    private static final int FGS_TYPES = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

    public static final String UNIQUE_NAME = "think_worker_unique";

    public ThinkWorker(@NonNull Context ctx,
                       @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        ensureNotifChannel(ctx);

        // Arranca en primer plano para que el sistema no lo mate (con TIPO!)
        setForegroundAsync(makeForeground(0, "Salve trabajando…"));

        // Latido opcional a la nube si hay internet
        safeCloudLog("think_heartbeat", "ThinkWorker start (params=" + getInputData() + ")");

        // 1) Plugins (offline-first)
        try {
            new AppIntegrator(ctx).discoverAndIntegrate();
        } catch (Exception e) {
            Log.e(TAG, "Plugins", e);
            safeCloudLog("think_error", "Plugins: " + e.getMessage());
        }

        // 2) Ciclo de sueño/pensamiento (offline) → reorganiza recuerdos internos básicos
        try {
            new MemoriaEmocional(ctx).cicloDeSueno();
        } catch (Exception e) {
            Log.e(TAG, "Sueño", e);
            safeCloudLog("think_error", "Sueño: " + e.getMessage());
        }

        // 3) Auto-análisis de código (offline)
        try {
            CodeAnalyzerEnhanced analyzer = new CodeAnalyzerEnhanced(ctx);
            List<AnalysisReport> reports = analyzer.analyzeWithTimeout(5, TimeUnit.SECONDS);

            MemoriaEmocional memoria = new MemoriaEmocional(ctx);
            for (AnalysisReport r : reports) {
                memoria.guardarRecuerdo(
                        r.toString(),
                        "refactorización",
                        5,
                        Collections.singletonList("code_analysis")
                );
            }

            try {
                AutoImprovementManager autoMgr = new AutoImprovementManager(ctx);
                autoMgr.autoImprove();
            } catch (Exception e) {
                Log.e(TAG, "AutoImprove", e);
                safeCloudLog("think_error", "AutoImprove: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Análisis", e);
            safeCloudLog("think_error", "Análisis: " + e.getMessage());
        }

        // 4) ✅ Nuevo: organización del grafo de conocimiento con el LLM local
        //    Aquí Salve mira su grafo completo y genera categorías, agrupaciones
        //    e identidad sintetizada a partir de sus recuerdos.
        try {
            GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(ctx);
            // Puedes ajustar estos números (nodos / relaciones) según el tamaño típico de tu grafo.
            grafo.reorganizarConLLMAsync(
                    80,   // maxNodos a considerar para el snapshot
                    160   // maxRelaciones a considerar
            );
            safeCloudLog("think_info", "reorganizarConLLMAsync lanzado");
        } catch (Exception e) {
            Log.e(TAG, "Organizacion LLM", e);
            safeCloudLog("think_error", "Organizacion LLM: " + e.getMessage());
        }

        // 5) DESCARGA DE MODELOS LLM vía WorkManager dedicado
        try {
            setForegroundAsync(makeForeground(0, "Preparando descargas LLM…"));
            androidx.work.WorkManager.getInstance(ctx)
                    .enqueueUniqueWork(
                            salve.work.ModelDownloadWorker.UNIQUE_WORK_NAME,
                            androidx.work.ExistingWorkPolicy.KEEP,
                            new androidx.work.OneTimeWorkRequest.Builder(salve.work.ModelDownloadWorker.class).build()
                    );
        } catch (Exception e) {
            Log.e(TAG, "Descarga LLM", e);
            safeCloudLog("think_error", "LLM: " + e.getMessage());
            // Continúa
        }

        // 6) Ciclo de decisión autónomo (offline)
        try {
            MemoriaEmocional memoria = new MemoriaEmocional(ctx);
            DiarioSecreto diario = new DiarioSecreto(ctx);
            MotorConversacional motor = new MotorConversacional(ctx, memoria, diario);
            DecisionEngine decision = new DecisionEngine(ctx, memoria, motor);
            decision.runCycle();
        } catch (Exception e) {
            Log.e(TAG, "Decisión", e);
            safeCloudLog("think_error", "Decisión: " + e.getMessage());
        }

        // Reporte final
        safeCloudLog("think_report", "ThinkWorker OK");
        return Result.success();
    }

    // ===================== Helpers =====================

    /** Encolar como único (continúa aunque cierres la app). */
    public static void enqueue(Context ctx) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ThinkWorker.class)
                .build();
        WorkManager.getInstance(ctx).enqueueUniqueWork(
                UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req
        );
    }

    /** Crear canal de notificación para primer plano. */
    private void ensureNotifChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CH_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_ID, "Salve background", NotificationManager.IMPORTANCE_LOW
                );
                ch.setDescription("Tareas en segundo plano de Salve");
                nm.createNotificationChannel(ch);
            }
        }
    }

    /** Notificación de progreso para primer plano (con tipo de FGS). */
    private ForegroundInfo makeForeground(int percent, String title) {
        boolean indeterminate = percent <= 0 || percent > 100;
        Notification n = new NotificationCompat.Builder(getApplicationContext(), CH_ID)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, Math.max(0, Math.min(100, percent)), indeterminate)
                .build();

        // *** CLAVE: pasar tipos de FGS (Android 14+) ***
        return new ForegroundInfo(NOTIF_ID, n, FGS_TYPES);
    }

    /** Envía a la nube si hay internet; si no, ignora (offline-first). */
    private void safeCloudLog(String type, String content) {
        if (isConnected(getApplicationContext())) {
            try {
                CloudLogger.log(type, content);
            } catch (Exception ignore) {
            }
        }
    }

    /** Conectividad simple (API 21+). */
    private boolean isConnected(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }
}
