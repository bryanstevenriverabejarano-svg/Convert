package salve.core.selfpr;

/**
 * Cuándo se examina Salve sola. Solo para comprobar un cambio suyo ya instalado, nunca por rutina:
 * el examen son minutos de inferencia, y de noche eso es batería y calor.
 *
 * La puerta del sueño (cargando y sin uso) ya la pasó quien llama; aquí se decide lo demás.
 */
public final class SelfExamPolicy {
    /** Si un intento se interrumpe, no se repite antes de esto: evita que cada ciclo de sueño lo reintente. */
    public static final long RETRY_AFTER_MS = 6L * 60 * 60 * 1000;
    /** Tras este número de intentos fallidos con el mismo cambio, espera a que lo pida Bryan. */
    public static final int MAX_ATTEMPTS = 3;

    private SelfExamPolicy() { }

    /**
     * @return null si debe examinarse ahora; si no, el motivo en una frase (o "" si no hay nada que comprobar).
     */
    public static String reasonNotToRun(boolean pendingChange, boolean engineAlive, boolean modelReady,
                                        long lastAttemptAt, int attempts, long now) {
        if (!pendingChange) return "";
        if (attempts >= MAX_ATTEMPTS)
            return "Ya intenté examinarme " + attempts + " veces por este cambio sin poder terminar; espero a que me lo pidas tú.";
        if (lastAttemptAt > 0 && now >= lastAttemptAt && now - lastAttemptAt < RETRY_AFTER_MS)
            return "Lo intenté hace poco y tuve que parar; lo reintento en el siguiente descanso largo.";
        if (!engineAlive)
            return "No puedo examinarme ahora: mi motor de conversación no está despierto (la app no está abierta en este proceso).";
        if (!modelReady) return "No puedo examinarme ahora: mi modelo no está cargado.";
        return null;
    }
}
