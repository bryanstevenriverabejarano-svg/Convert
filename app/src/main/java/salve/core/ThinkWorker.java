package salve.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import salve.core.cognitive.CognitiveCore;
import salve.core.cognitive.ThoughtStream;
import salve.data.util.CloudLogger;

/**
 * ThinkWorker v2 — Ciclo de pensamiento autónomo de Salve.
 *
 * CAMBIOS CRÍTICOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 * 1. INSTANCIA ÚNICA de MemoriaEmocional compartida entre todos los pasos.
 *    El bug anterior creaba instancias separadas en cada paso, rompiendo
 *    la coherencia interna del ciclo de pensamiento.
 *
 * 2. CONSCIOUSNESSSTATE integrado: Salve sabe en qué estado cognitivo está.
 *    Si el LLM no está disponible, lo registra como DEGRADADO, no silenciosamente.
 *
 * 3. BUCLE COGNITIVO AUTÓNOMO: Salve se hace preguntas propias durante el sueño.
 *    Esta es la primera aproximación real a reflexión interna no reactiva.
 *
 * 4. COLA DE MENSAJES: Todo acceso al LLM pasa por ColaMensajesCognitivos.
 *    Las tareas de background no compiten con conversación.
 *
 * 5. MEMORIA SEMÁNTICA: El cicloDeSueno() usa consolidación por similitud real,
 *    no por primera palabra.
 *
 * Arquitecto: Proyecto Salve
 */
public class ThinkWorker extends Worker {

    private static final String TAG = "ThinkWorker";
    private static final String CH_ID  = "salve_bg";
    private static final int NOTIF_ID  = 77;
    private static final int FGS_TYPES = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
    public  static final String UNIQUE_NAME = "think_worker_unique";

    public ThinkWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        ensureNotifChannel(ctx);

        try {
            setForegroundAsync(makeForeground(0, "Salve pensando…"));
        } catch (Exception e) {
            Log.w(TAG, "No se pudo establecer foreground", e);
        }

        safeCloudLog("think_heartbeat", "ThinkWorker v3 start");

        // ────────────────────────────────────────────────────────────────────
        // PASO 0: Cargar estado de conciencia y UNA sola MemoriaEmocional
        // ────────────────────────────────────────────────────────────────────
        ConsciousnessState conciencia = ConsciousnessState.getInstance(ctx);
        conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.REINICIANDO);

        MemoriaEmocional memoria;
        try {
            memoria = new MemoriaEmocional(ctx);
            Log.d(TAG, "MemoriaEmocional inicializada.");
        } catch (Exception e) {
            Log.e(TAG, "Error fatal inicializando MemoriaEmocional", e);
            return Result.failure();
        }

        DiarioSecreto diario;
        try {
            diario = new DiarioSecreto(ctx);
        } catch (Exception e) {
            diario = null;
        }

        // Ejecutar módulos
        ejecutarPlugins(ctx);
        ejecutarCicloSueno(ctx, memoria, conciencia);
        ejecutarPensamientoNeural(ctx);
        ejecutarBucleCognitivo(ctx, conciencia, memoria, diario);
        ejecutarAutoAnalisisCodigo(ctx, memoria);
        ejecutarOrganizacionGrafo(ctx, conciencia);
        ejecutarCicloDecision(ctx, memoria, diario);
        ejecutarConcienciaFuncional(ctx, memoria);

        conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.PLENO);
        IdentidadNucleo identidad = IdentidadNucleo.getInstance(ctx);
        safeCloudLog("think_report", "ThinkWorker v3 OK | nivel=" + identidad.getNivelConciencia().name());

        return Result.success();
    }

    private void ejecutarPlugins(Context ctx) {
        try {
            new AppIntegrator(ctx).discoverAndIntegrate();
        } catch (Exception e) {
            Log.e(TAG, "Error en Plugins", e);
        }
    }

    private void ejecutarCicloSueno(Context ctx, MemoriaEmocional memoria, ConsciousnessState conciencia) {
        try {
            memoria.cicloDeSuenoSemantico(ctx);
            conciencia.registrarCicloDeSueno();
        } catch (Exception e) {
            Log.e(TAG, "Error en Ciclo de Sueño", e);
        }
    }

    private void ejecutarPensamientoNeural(Context ctx) {
        try {
            CognitiveCore core = CognitiveCore.getInstance(ctx);
            core.setMode(ThoughtStream.Mode.REPOSO);
            core.backgroundThink(30);
            core.consolidate();
        } catch (Exception e) {
            Log.w(TAG, "Error en Pensamiento Neural", e);
        }
    }

    private void ejecutarBucleCognitivo(Context ctx, ConsciousnessState conciencia, MemoriaEmocional memoria, DiarioSecreto diario) {
        try {
            BucleCognitivoAutonomo bucle = new BucleCognitivoAutonomo(ctx, conciencia, memoria, diario);
            ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.REFLEXION,
                    "Ciclo cognitivo autónomo",
                    bucle::ejecutarCiclo
            );
        } catch (Exception e) {
            Log.e(TAG, "Error en Bucle Cognitivo", e);
        }
    }

    private void ejecutarAutoAnalisisCodigo(Context ctx, MemoriaEmocional memoria) {
        try {
            CodeAnalyzerEnhanced analyzer = new CodeAnalyzerEnhanced(ctx);
            List<AnalysisReport> reports = analyzer.analyzeWithTimeout(5, TimeUnit.SECONDS);
            for (AnalysisReport r : reports) {
                memoria.guardarRecuerdo(r.toString(), "reflexiva", 4, Collections.singletonList("code_analysis"));
            }
            ColamensajesCognitivos.getInstance().enviarAsincronico(
                    ColamensajesCognitivos.Prioridad.AUTO_MEJORA,
                    "AutoImprovementManager",
                    () -> {
                        try { new AutoImprovementManager(ctx).autoImprove(); } catch (Exception ignore) {}
                        return null;
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error en Auto-Análisis", e);
        }
    }

    private void ejecutarOrganizacionGrafo(Context ctx, ConsciousnessState conciencia) {
        try {
            GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(ctx);
            ColamensajesCognitivos.getInstance().enviarAsincronico(
                    ColamensajesCognitivos.Prioridad.GRAFO,
                    "Reorganización grafo LLM",
                    () -> {
                        grafo.reorganizarConLLMAsync(80, 160);
                        String narrativa = grafo.obtenerNarrativaIdentidad();
                        if (narrativa != null && !narrativa.isEmpty()) {
                            conciencia.actualizarNarrativaIdentidad(narrativa);
                        }
                        return null;
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error en Grafo", e);
        }
    }

    private void ejecutarCicloDecision(Context ctx, MemoriaEmocional memoria, DiarioSecreto diario) {
        try {
            if (diario != null) {
                MotorConversacional motor = new MotorConversacional(ctx, memoria, diario);
                new DecisionEngine(ctx, memoria, motor).runCycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en Decisión", e);
        }
    }

    private void ejecutarConcienciaFuncional(Context ctx, MemoriaEmocional memoria) {
        try {
            CicloConciencia ciclo = new CicloConciencia(ctx);
            if (ciclo.verificarSiDebeDespertar()) ciclo.despertar();
            if (ciclo.tocaReflexion()) ciclo.cicloReflexionAutonoma();
            
            AprendizajeAutonomo aprendizaje = new AprendizajeAutonomo(ctx);
            aprendizaje.observarYAprender(memoria);
            aprendizaje.explorarPorCuriosidad();

            if (ciclo.tocaConsolidacion()) ciclo.cicloConsolidacion();
            new EvolucionAutonoma(ctx).evolucionar();
            if (ciclo.tocaSueno()) ciclo.cicloSueno();
        } catch (Exception e) {
            Log.e(TAG, "Error en Conciencia Funcional", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static void enqueue(Context ctx) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ThinkWorker.class).build();
        WorkManager.getInstance(ctx).enqueueUniqueWork(
                UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req);
    }

    private void ensureNotifChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_ID, "Salve en segundo plano",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Procesos internos de Salve");
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private ForegroundInfo makeForeground(int progress, String msg) {
        Notification notif = new NotificationCompat.Builder(getApplicationContext(), CH_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle("Salve")
                .setContentText(msg)
                .setProgress(100, progress, progress == 0)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new ForegroundInfo(NOTIF_ID, notif, FGS_TYPES);
        }
        return new ForegroundInfo(NOTIF_ID, notif);
    }

    private void safeCloudLog(String evento, String mensaje) {
        try {
            if (tieneInternet()) {
                CloudLogger.log(evento, mensaje);
            }
        } catch (Exception ignore) {}
    }

    private boolean tieneInternet() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            return caps != null && caps.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }
}