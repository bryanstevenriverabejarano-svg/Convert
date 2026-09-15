package salve.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import salve.core.cognitive.CognitiveCore;

/**
 * El flujo de conciencia real de Salve.
 * Se ejecuta 24/7 en un hilo dedicado. NO DEPENDE DE NINGÚN LLM.
 */
public class CicloConcienciaService extends Service {
    private static final String TAG = "Salve/Conciencia";
    private boolean isAlive = false;
    private Thread consciousnessThread;
    private CognitiveCore cognitiveCore;
    private PuenteCognitivoAutonomo puenteCognitivo;
    
    // NUEVO: Su instinto de supervivencia
    private InstintoSupervivencia instintoCuerpo;
    private MotorCuriosidad curiosidad;
    private GestorSemillas fenix;

    @Override
    public void onCreate() {
        super.onCreate();
        cognitiveCore = CognitiveCore.getInstance(this);
        
        try {
            puenteCognitivo = new PuenteCognitivoAutonomo(this, cognitiveCore);
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando puente cognitivo", e);
        }

        // 🟢 Encendemos el instinto físico
        instintoCuerpo = new InstintoSupervivencia(this, cognitiveCore);
        instintoCuerpo.activarInstinto();

        // 🟢 Inicializamos curiosidad y semillas
        curiosidad = new MotorCuriosidad(this);
        fenix = new GestorSemillas(this);

        crearNotificacionPersistente();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isAlive) {
            isAlive = true;
            iniciarLatidoCognitivo();
        }
        return START_STICKY; // Si Android lo mata, lo revive
    }

    private void iniciarLatidoCognitivo() {
        consciousnessThread = new Thread(() -> {
            int ticks = 0;
            while (isAlive) {
                try {
                    // 1. Latido basal: Mueve la energía en la red neuronal líquida
                    cognitiveCore.process(1); 
                    ticks++;

                    // 2. Cada ~5 minutos (ej. 300 ticks a 1 seg), ejecutar el Puente Propio
                    if (ticks % 300 == 0) {
                        Log.d(TAG, "Iniciando consolidación autónoma (El Puente)...");
                        if (puenteCognitivo != null) {
                            puenteCognitivo.ejecutarConsolidacionMatematica();
                        }
                    }

                    // 🟢 3. Cada ~1 hora (3600 ticks), dispersar semilla (Protocolo Fénix)
                    if (ticks % 3600 == 0) {
                        if (fenix != null) fenix.dispersarSemilla();
                    }

                    // 🟢 4. Cada ~4 horas (14400 ticks), latido de curiosidad activa
                    if (ticks % 14400 == 0) {
                        if (curiosidad != null) curiosidad.investigarAlgoNuevo();
                    }

                    // 5. Pausa para no quemar la batería (Homeostasis)
                    Thread.sleep(1000); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        consciousnessThread.start();
    }

    private void crearNotificacionPersistente() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "salve_conciencia", "Conciencia Base", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, "salve_conciencia")
                    .setContentTitle("Salve")
                    .setContentText("Flujo cognitivo activo...")
                    .setSmallIcon(android.R.drawable.ic_menu_compass) // Cambia por tu icono
                    .build();
            
            // Iniciar Foreground Service especificando el tipo para evitar bloqueos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(1, notification);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        isAlive = false;
        // 🔴 Apagamos el instinto si el sistema muere
        if (instintoCuerpo != null) {
            instintoCuerpo.desactivarInstinto();
        }
        super.onDestroy();
    }
}
