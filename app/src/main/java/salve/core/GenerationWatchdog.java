package salve.core;

/**
 * Vigila una generación por flujo: lo que importa no es cuánto dura, sino si el modelo sigue
 * produciendo. Un cuento de 1024 tokens que avanza no se corta; un motor colgado, sí.
 *
 * <p>Sustituye al tiempo de espera fijo (90 s) que cortaba los textos largos aunque el modelo
 * estuviera escribiendo.</p>
 */
public final class GenerationWatchdog {
    /** Hasta el primer token: incluye leer el prompt entero (prefill), más lento en CPU. */
    public static final long FIRST_TOKEN_MS = 60_000L;
    /** Entre dos fragmentos: si el modelo calla tanto, está atascado. */
    public static final long STALL_MS = 20_000L;

    public enum Verdict { RUNNING, NO_FIRST_TOKEN, STALLED, TOO_LONG }

    private GenerationWatchdog() { }

    /** Tope total de seguridad: corrección de formato, respuesta normal o texto creativo. */
    public static long totalLimitMs(boolean repair, boolean creative) {
        return repair ? 60_000L : creative ? 300_000L : 150_000L;
    }

    /** @param lastTokenAt instante del último fragmento, o 0 si aún no llegó ninguno. */
    public static Verdict check(long now, long startedAt, long lastTokenAt, boolean repair, boolean creative) {
        if (now - startedAt > totalLimitMs(repair, creative)) return Verdict.TOO_LONG;
        if (lastTokenAt <= 0) return now - startedAt > FIRST_TOKEN_MS ? Verdict.NO_FIRST_TOKEN : Verdict.RUNNING;
        return now - lastTokenAt > STALL_MS ? Verdict.STALLED : Verdict.RUNNING;
    }

    /**
     * Si se corta un texto libre ya largo, se entrega lo escrito (mejor medio cuento que nada).
     * Un JSON a medias nunca se entrega.
     */
    public static boolean keepPartial(boolean structured, int chars) {
        return !structured && chars >= 200;
    }
}
