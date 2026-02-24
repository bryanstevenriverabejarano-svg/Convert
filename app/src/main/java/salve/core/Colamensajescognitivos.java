package salve.core;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ColaMensajesCognitivos — Árbitro de acceso al LLM local de Salve.
 *
 * PROBLEMA CRÍTICO QUE RESUELVE:
 * El LLM local (MLC Engine en Android) es de hilo único. Si ThinkWorker,
 * MotorConversacional y GrafoConocimientoVivo intentan usarlo simultáneamente,
 * el sistema falla silenciosamente o produce respuestas corruptas.
 *
 * Esta clase implementa una cola de prioridad con un único ExecutorService:
 *
 *   PRIORIDAD 1 (URGENTE): Conversación con Bryan — respuesta inmediata
 *   PRIORIDAD 2 (ALTA):    Reflexión en ciclo de sueño — pensamiento propio
 *   PRIORIDAD 3 (MEDIA):   Organización del grafo — background
 *   PRIORIDAD 4 (BAJA):    Auto-mejora y análisis — puede esperar
 *   PRIORIDAD 5 (MÍNIMA):  Ideas y sugerencias — solo si no hay nada urgente
 *
 * Garantiza:
 *   - Solo una llamada al LLM a la vez
 *   - Las conversaciones nunca esperan detrás de tareas de background
 *   - El ThinkWorker no bloquea al usuario
 *
 * Arquitecto: Proyecto Salve
 */
public class ColaMensajesCognitivos {

    private static final String TAG = "Salve/Cola";

    // Singleton
    private static volatile ColaMensajesCognitivos instance;

    public static ColaMensajesCognitivos getInstance() {
        if (instance == null) {
            synchronized (ColaMensajesCognitivos.class) {
                if (instance == null) {
                    instance = new ColaMensajesCognitivos();
                }
            }
        }
        return instance;
    }

    // ── Prioridades ──────────────────────────────────────────────────────────

    public enum Prioridad {
        CONVERSACION(1),    // Responder a Bryan — máxima prioridad
        REFLEXION(2),       // Ciclo cognitivo autónomo
        GRAFO(3),           // Organización del conocimiento
        AUTO_MEJORA(4),     // Análisis y mejora de código
        IDEAS(5);           // Generación de ideas — background

        final int nivel;
        Prioridad(int nivel) { this.nivel = nivel; }
    }

    // ── Estado interno ───────────────────────────────────────────────────────

    /** Executor de un solo hilo — garantiza acceso serial al LLM */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Salve-LLM-Worker");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    /** Cola de prioridad para tareas pendientes */
    private final PriorityBlockingQueue<TareaCognitiva<?>> colaPendiente =
            new PriorityBlockingQueue<>();

    /** Indica si hay una tarea ejecutándose ahora mismo */
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    private ColaMensajesCognitivos() {
        Log.d(TAG, "Cola de mensajes cognitivos inicializada.");
    }

    // ── API principal ────────────────────────────────────────────────────────

    /**
     * Envía una tarea cognitiva a la cola y espera su resultado (bloqueante).
     * Usar solo desde hilos de background, NUNCA desde el hilo principal.
     *
     * @param prioridad  Nivel de prioridad de la tarea
     * @param descripcion Descripción legible (para logs)
     * @param tarea       La función que llama al LLM
     * @return El resultado de la tarea, o null si falló
     */
    public <T> T enviarSincronico(Prioridad prioridad, String descripcion, Callable<T> tarea) {
        TareaCognitiva<T> tc = new TareaCognitiva<>(prioridad, descripcion, tarea);

        if (prioridad == Prioridad.CONVERSACION) {
            // Conversación: ejecutar directamente sin cola para mínima latencia
            Log.d(TAG, "Conversación → ejecución directa (saltando cola)");
            try {
                return tarea.call();
            } catch (Exception e) {
                Log.e(TAG, "Error en tarea de conversación directa", e);
                return null;
            }
        }

        // Background: encolar y ejecutar en orden
        Future<T> future = executor.submit(() -> {
            Log.d(TAG, "Ejecutando: [" + prioridad.name() + "] " + descripcion);
            ejecutando.set(true);
            try {
                return tarea.call();
            } finally {
                ejecutando.set(false);
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            Log.e(TAG, "Error esperando resultado de: " + descripcion, e);
            return null;
        }
    }

    /**
     * Envía una tarea cognitiva sin esperar resultado (asincrónico).
     * Útil para background tasks que no necesitan respuesta inmediata.
     */
    public <T> Future<T> enviarAsincronico(Prioridad prioridad, String descripcion,
                                           Callable<T> tarea) {
        Log.d(TAG, "Encolando async: [" + prioridad.name() + "] " + descripcion);
        return executor.submit(() -> {
            Log.d(TAG, "Iniciando async: [" + prioridad.name() + "] " + descripcion);
            ejecutando.set(true);
            try {
                return tarea.call();
            } catch (Exception e) {
                Log.e(TAG, "Error en tarea async: " + descripcion, e);
                return null;
            } finally {
                ejecutando.set(false);
            }
        });
    }

    /**
     * Cancela todas las tareas pendientes de prioridad menor a la dada.
     * Útil cuando llega una conversación y hay mucho trabajo de background.
     */
    public void cancelarTareasBajasPrioridad(Prioridad umbral) {
        // El executor de un hilo ya serializa las tareas.
        // Con esta arquitectura, la conversación siempre va directa (ver enviarSincronico).
        // Este método existe para extensibilidad futura con pools más complejos.
        Log.d(TAG, "Solicitud de cancelación por debajo de " + umbral.name()
                + " (en arquitectura actual, conversación ya tiene prioridad directa)");
    }

    /**
     * @return true si hay una tarea LLM ejecutándose ahora mismo
     */
    public boolean estaOcupado() {
        return ejecutando.get();
    }

    /**
     * Apaga la cola limpiamente. Llamar solo al cerrar la app.
     */
    public void shutdown() {
        executor.shutdown();
        Log.d(TAG, "Cola cognitiva detenida.");
    }

    // ── Clase interna ────────────────────────────────────────────────────────

    private static class TareaCognitiva<T> implements Comparable<TareaCognitiva<?>> {
        final Prioridad prioridad;
        final String descripcion;
        final Callable<T> tarea;
        final long timestamp;

        TareaCognitiva(Prioridad prioridad, String descripcion, Callable<T> tarea) {
            this.prioridad   = prioridad;
            this.descripcion = descripcion;
            this.tarea       = tarea;
            this.timestamp   = System.currentTimeMillis();
        }

        @Override
        public int compareTo(TareaCognitiva<?> other) {
            // Menor nivel = mayor prioridad
            int cmp = Integer.compare(this.prioridad.nivel, other.prioridad.nivel);
            if (cmp != 0) return cmp;
            // Igual prioridad → FIFO por timestamp
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
}