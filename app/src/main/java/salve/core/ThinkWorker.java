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

        safeCloudLog("think_heartbeat", "ThinkWorker v2 start");

        // ────────────────────────────────────────────────────────────────────
        // PASO 0: Cargar estado de conciencia y UNA sola MemoriaEmocional
        // ────────────────────────────────────────────────────────────────────
        ConsciousnessState conciencia = ConsciousnessState.getInstance(ctx);
        conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.REINICIANDO);

        // UNA sola instancia compartida — fix del bug crítico de instanciación múltiple
        MemoriaEmocional memoria;
        try {
            memoria = new MemoriaEmocional(ctx);
            Log.d(TAG, "MemoriaEmocional inicializada. Recuerdos en RAM: "
                    + memoria.getRecuerdosCount());
        } catch (Exception e) {
            Log.e(TAG, "Error fatal inicializando MemoriaEmocional", e);
            safeCloudLog("think_error", "MemoriaEmocional: " + e.getMessage());
            conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.MINIMO);
            return Result.failure();
        }

        DiarioSecreto diario;
        try {
            diario = new DiarioSecreto(ctx);
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando DiarioSecreto", e);
            diario = null;
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 1: Plugins (offline-first)
        // ────────────────────────────────────────────────────────────────────
        try {
            new AppIntegrator(ctx).discoverAndIntegrate();
            Log.d(TAG, "Plugins integrados.");
        } catch (Exception e) {
            Log.e(TAG, "Plugins", e);
            safeCloudLog("think_error", "Plugins: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 2: Ciclo de sueño con consolidación semántica real
        // Usa la MISMA instancia de memoria que el resto del worker
        // ────────────────────────────────────────────────────────────────────
        try {
            Log.d(TAG, "Iniciando cicloDeSueno con consolidación semántica...");
            memoria.cicloDeSuenoSemantico(ctx); // nuevo método — ver MemoriaEmocional
            conciencia.registrarCicloDeSueno();
            safeCloudLog("think_info", "Ciclo sueño semántico completado");
        } catch (Exception e) {
            Log.e(TAG, "Sueño semántico falló, usando cicloDeSueno clásico", e);
            try {
                memoria.cicloDeSueno();
                conciencia.registrarCicloDeSueno();
            } catch (Exception e2) {
                Log.e(TAG, "CicloDeSueno clásico también falló", e2);
                safeCloudLog("think_error", "Sueño: " + e2.getMessage());
            }
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 2.5: Pensamiento en reposo — Sustrato Cognitivo Experimental
        // CognitiveCore piensa en silencio durante inactividad.
        // El LLM NO participa aquí — es pensamiento neural puro.
        // ────────────────────────────────────────────────────────────────────
        try {
            CognitiveCore core = CognitiveCore.getInstance(ctx);
            core.setMode(ThoughtStream.Mode.REPOSO);
            core.backgroundThink(30); // 30 segundos de pensamiento en silencio
            safeCloudLog("think_info", "CognitiveCore reposo completado");
        } catch (Exception e) {
            Log.w(TAG, "CognitiveCore background think falló (no fatal)", e);
            safeCloudLog("think_error", "CognitiveCore: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 2.6: Consolidación de aprendizaje neural
        // Guarda pesos actualizados de LiquidNeuralLayer y estado completo
        // ────────────────────────────────────────────────────────────────────
        try {
            CognitiveCore core = CognitiveCore.getInstance(ctx);
            core.consolidate();
            safeCloudLog("think_info", "CognitiveCore consolidación completada");
        } catch (Exception e) {
            Log.w(TAG, "CognitiveCore consolidación falló (no fatal)", e);
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 3: Bucle cognitivo autónomo — Salve se hace preguntas propias
        // La reflexión interna que no viene del usuario
        // ────────────────────────────────────────────────────────────────────
        try {
            Log.d(TAG, "Iniciando bucle cognitivo autónomo...");
            BucleCognitivoAutonomo bucle = new BucleCognitivoAutonomo(
                    ctx, conciencia, memoria, diario);

            BucleCognitivoAutonomo.CicloResult cicloResult = ColamensajesCognitivos
                    .getInstance()
                    .enviarSincronico(
                            ColamensajesCognitivos.Prioridad.REFLEXION,
                            "Ciclo cognitivo autónomo",
                            bucle::ejecutarCiclo
                    );

            if (cicloResult != null && cicloResult.esValido()) {
                Log.d(TAG, "Ciclo cognitivo exitoso: " + cicloResult.pregunta);
                safeCloudLog("think_reflexion", cicloResult.pregunta);
            } else {
                Log.w(TAG, "Ciclo cognitivo no produjo resultado.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Bucle cognitivo", e);
            safeCloudLog("think_error", "BucleCognitivo: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 4: Auto-análisis de código
        // Usa la MISMA instancia de memoria
        // ────────────────────────────────────────────────────────────────────
        try {
            CodeAnalyzerEnhanced analyzer = new CodeAnalyzerEnhanced(ctx);
            List<AnalysisReport> reports = analyzer.analyzeWithTimeout(5, TimeUnit.SECONDS);

            // ← FIX: misma instancia de memoria, no una nueva
            for (AnalysisReport r : reports) {
                memoria.guardarRecuerdo(
                        r.toString(),
                        "reflexiva",
                        4,
                        Collections.singletonList("code_analysis")
                );
            }

            ColamensajesCognitivos.getInstance().enviarAsincronico(
                    ColamensajesCognitivos.Prioridad.AUTO_MEJORA,
                    "AutoImprovementManager",
                    () -> {
                        try {
                            new AutoImprovementManager(ctx).autoImprove();
                        } catch (Exception e) {
                            Log.e(TAG, "AutoImprove async", e);
                        }
                        return null;
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Análisis", e);
            safeCloudLog("think_error", "Análisis: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 5: Organización del grafo con LLM (async — no bloquea el ciclo)
        // ────────────────────────────────────────────────────────────────────
        try {
            GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(ctx);
            ColamensajesCognitivos.getInstance().enviarAsincronico(
                    ColamensajesCognitivos.Prioridad.GRAFO,
                    "Reorganización grafo LLM",
                    () -> {
                        grafo.reorganizarConLLMAsync(80, 160);
                        // Actualizar narrativa de identidad en ConsciousnessState
                        String narrativa = grafo.obtenerNarrativaIdentidad();
                        if (narrativa != null && !narrativa.isEmpty()) {
                            conciencia.actualizarNarrativaIdentidad(narrativa);
                        }
                        return null;
                    }
            );
            safeCloudLog("think_info", "Reorganización grafo encolada");
        } catch (Exception e) {
            Log.e(TAG, "Organización LLM", e);
            safeCloudLog("think_error", "Organización LLM: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 6: Descarga de modelos LLM (si necesario)
        // ────────────────────────────────────────────────────────────────────
        try {
            setForegroundAsync(makeForeground(0, "Preparando modelos…"));
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                    salve.work.ModelDownloadWorker.UNIQUE_WORK_NAME,
                    androidx.work.ExistingWorkPolicy.KEEP,
                    new androidx.work.OneTimeWorkRequest.Builder(
                            salve.work.ModelDownloadWorker.class).build()
            );
        } catch (Exception e) {
            Log.e(TAG, "Descarga LLM", e);
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 7: Ciclo de decisión autónomo
        // Usa la MISMA instancia de memoria y diario
        // ────────────────────────────────────────────────────────────────────
        try {
            if (diario != null) {
                MotorConversacional motor = new MotorConversacional(ctx, memoria, diario);
                DecisionEngine decision = new DecisionEngine(ctx, memoria, motor);
                decision.runCycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Decisión", e);
            safeCloudLog("think_error", "Decisión: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 8: Conciencia Funcional — CicloConciencia + Aprendizaje + Evolución
        // ────────────────────────────────────────────────────────────────────
        try {
            CicloConciencia cicloConciencia = new CicloConciencia(ctx);

            // 8a. Verificar si Salve debe despertar
            if (cicloConciencia.verificarSiDebeDespertar()) {
                cicloConciencia.despertar();
                safeCloudLog("think_conciencia", "Salve despertó");
            }

            // 8b. Reflexión autónoma (si toca — cada 2h)
            if (cicloConciencia.tocaReflexion()) {
                cicloConciencia.cicloReflexionAutonoma();
                safeCloudLog("think_conciencia", "Reflexión autónoma completada");
            }

            // 8c. Aprendizaje autónomo — observar patrones de Bryan
            try {
                AprendizajeAutonomo aprendizaje = new AprendizajeAutonomo(ctx);
                aprendizaje.observarYAprender(memoria);
                safeCloudLog("think_aprendizaje", "Observación de patrones completada");
            } catch (Exception e) {
                Log.w(TAG, "AprendizajeAutonomo falló (no fatal)", e);
            }

            // 8d. Exploración por curiosidad
            try {
                AprendizajeAutonomo aprendizaje = new AprendizajeAutonomo(ctx);
                aprendizaje.explorarPorCuriosidad();
                safeCloudLog("think_aprendizaje", "Exploración por curiosidad completada");
            } catch (Exception e) {
                Log.w(TAG, "Exploración por curiosidad falló (no fatal)", e);
            }

            // 8e. Consolidación memoria → grafo (si toca — cada 6h)
            if (cicloConciencia.tocaConsolidacion()) {
                cicloConciencia.cicloConsolidacion();
                safeCloudLog("think_conciencia", "Consolidación completada");
            }

            // 8f. Evolución autónoma de código (si toca)
            try {
                EvolucionAutonoma evolucion = new EvolucionAutonoma(ctx);
                evolucion.evolucionar();
                safeCloudLog("think_evolucion", "Evolución autónoma ejecutada");
            } catch (Exception e) {
                Log.w(TAG, "EvolucionAutonoma falló (no fatal)", e);
            }

            // 8g. Ciclo de sueño profundo (si toca — cada 24h)
            if (cicloConciencia.tocaSueno()) {
                cicloConciencia.cicloSueno();
                safeCloudLog("think_conciencia", "Ciclo de sueño profundo completado");
            }

        } catch (Exception e) {
            Log.e(TAG, "Conciencia funcional", e);
            safeCloudLog("think_error", "Conciencia: " + e.getMessage());
        }

        // ────────────────────────────────────────────────────────────────────
        // PASO 9: Restaurar estado cognitivo
        // ────────────────────────────────────────────────────────────────────
        conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.PLENO);
        IdentidadNucleo identidad = IdentidadNucleo.getInstance(ctx);
        safeCloudLog("think_report", "ThinkWorker v3 OK | sesión=" + conciencia.getSessionCount()
                + " | ciclosSueño=" + conciencia.getCiclosSuenoTotal()
                + " | nivelConciencia=" + identidad.getNivelConciencia().name()
                + " | experiencias=" + identidad.getExperienciasTotales());

        return Result.success();
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