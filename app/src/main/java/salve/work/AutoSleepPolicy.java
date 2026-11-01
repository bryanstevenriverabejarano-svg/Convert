package salve.work;

/**
 * Sueño automático: si el móvil está cargando y lleva más de una hora sin usarse, Salve se acuesta
 * sola; si Bryan vuelve a usarlo o lo desenchufa, se despierta. Nunca despierta un sueño que pidió
 * Bryan a mano. Lógica pura (se prueba en la JVM); el muestreo lo hace {@link AutoSleepWorker}.
 */
public final class AutoSleepPolicy {
    public static final long IDLE_MS = 60L * 60L * 1000L;

    public enum Action { NONE, SLEEP, WAKE }

    private AutoSleepPolicy() { }

    /**
     * @param interactive  pantalla encendida ahora
     * @param lastUseMs    último momento en que se vio el móvil en uso (0 = desconocido)
     * @param sleepActive  el modo sueño ya está activo (por Bryan o automático)
     * @param autoSleeping el sueño activo lo empezó esta política
     */
    public static Action decide(boolean enabled, boolean interactive, boolean charging, long nowMs,
                                long lastUseMs, boolean sleepActive, boolean autoSleeping) {
        if (autoSleeping && sleepActive && (interactive || !charging)) return Action.WAKE;
        if (!enabled || interactive || !charging || sleepActive || lastUseMs <= 0) return Action.NONE;
        return nowMs - lastUseMs >= IDLE_MS ? Action.SLEEP : Action.NONE;
    }

    /** El último uso conocido tras esta muestra: se mueve a «ahora» si la pantalla está encendida. */
    public static long nextLastUse(boolean interactive, long nowMs, long lastUseMs) {
        return interactive || lastUseMs <= 0 ? nowMs : lastUseMs;
    }
}
