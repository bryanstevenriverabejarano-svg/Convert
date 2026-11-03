package salve.core;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Cargar un modelo grande «sí o sí» dentro de lo que Android permite: ninguna app puede cerrar a las
 * demás (Android 14+), pero el sistema va soltando caché cuando hace falta memoria. Si la carga falla,
 * se espera un poco (dejando que Android libere) y se vuelve a intentar, unas pocas veces. Nunca se
 * reintenta lo que Bryan canceló. Lógica pura para pruebas JVM (la espera es inyectable).
 */
public final class ModelActivationRetry {
    public interface Sleeper { void sleep(long millis) throws InterruptedException; }

    /** Intentos y pausa para los modelos grandes (Qwen 3.5 9B): ~25 s en total como mucho de espera. */
    public static final int ATTEMPTS = 4;
    public static final long PAUSE_MS = 6000;

    private ModelActivationRetry() { }

    public static ModelResult run(Supplier<ModelResult> attempt, BooleanSupplier cancelled) {
        return run(attempt, ATTEMPTS, PAUSE_MS, cancelled, Thread::sleep);
    }

    public static ModelResult run(Supplier<ModelResult> attempt, int attempts, long pauseMs,
                                  BooleanSupplier cancelled, Sleeper sleeper) {
        ModelResult last = null;
        for (int i = 0; i < Math.max(1, attempts); i++) {
            if (i > 0) {
                if (cancelled.getAsBoolean()) break;
                Runtime.getRuntime().gc(); // lo único que Salve puede soltar: su propia memoria
                try { sleeper.sleep(pauseMs); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
            }
            last = attempt.get();
            if (last == null) continue;
            if (last.isSuccess() || last.getStatus() == ModelResult.Status.CANCELLED || cancelled.getAsBoolean()) return last;
        }
        return last != null ? last : ModelResult.failure(ModelResult.Status.ERROR, "No se pudo cargar el modelo", 0L);
    }
}
