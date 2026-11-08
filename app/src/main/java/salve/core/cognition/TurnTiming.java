package salve.core.cognition;

import java.util.Locale;

/**
 * Texto de las mediciones de rendimiento que van al registro de ejecución. Sin Android, para
 * poder probarlo: dice qué inferencia fue (primera, tras un recibo o reparación de formato),
 * cuánto tardó y, si el motor local lo midió, cuánto leyó y cuánto sacó ya leído de su caché.
 */
public final class TurnTiming {
    private TurnTiming() { }

    public static String inference(int number, int receipts, boolean repair, long nanos, String engineStats) {
        String why = repair ? "reparación de formato" : receipts > 0 ? "tras " + receipts + " recibo(s)" : "primera";
        String text = String.format(Locale.ROOT, "Inferencia %d (%s): %s", number, why, seconds(nanos));
        return engineStats == null || engineStats.isEmpty() ? text : text + " · " + engineStats;
    }

    public static String turn(int inferences, long modelNanos, long totalNanos) {
        long rest = Math.max(0L, totalNanos - modelNanos);
        return String.format(Locale.ROOT, "%d inferencia(s): modelo %s, herramientas y resto %s, total %s",
                inferences, seconds(modelNanos), seconds(rest), seconds(totalNanos));
    }

    static String seconds(long nanos) {
        return String.format(Locale.ROOT, "%.1f s", nanos / 1_000_000_000.0);
    }
}
