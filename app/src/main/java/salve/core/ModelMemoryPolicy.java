package salve.core;

/** Admission estimates, not measured device guarantees. Uses bytes, never Java heap limits. */
public final class ModelMemoryPolicy {
    public static final long MIB = 1024L * 1024L;
    public static final long RESERVE = 768 * MIB;
    public static final long LARGE_WEIGHTS = 4920749472L;
    private ModelMemoryPolicy() { }
    public static final class Snapshot {
        public final long available, total, threshold;
        public final boolean low;
        public Snapshot(long available, long total, long threshold, boolean low) {
            this.available = available; this.total = total; this.threshold = threshold; this.low = low;
        }
        public boolean known() { return total > 0 && available > 0 && available <= total; }
    }
    public static long required(String id, boolean loaded, long threshold) {
        return required(id, loaded, threshold, GgufContextPolicy.STANDARD);
    }
    public static long required(String id, boolean loaded, long threshold, int contextTokens) {
        if (!GgufContextPolicy.supported(contextTokens)) throw new IllegalArgumentException("Contexto no admitido");
        long weights = LocalModelPolicy.PRIMARY.equals(id) ? LARGE_WEIGHTS
                : LocalModelPolicy.LIGHT.equals(id) ? 2019382400L
                : LocalModelPolicy.GEMMA_E4B.equals(id) ? 3659530240L : 2588147712L;
        long overhead = LocalModelPolicy.PRIMARY.equals(id) ? 1024 * MIB : 512 * MIB;
        if (contextTokens == GgufContextPolicy.COMPACT) {
            if (LocalModelPolicy.PRIMARY.equals(id)) overhead = 768 * MIB;
            else if (LocalModelPolicy.LIGHT.equals(id)) overhead = 384 * MIB;
        }
        return (loaded ? 0 : weights) + overhead + Math.max(RESERVE, threshold);
    }
    /** Codificador visual de Gemma (LiteRT-LM) además del modelo de texto. Estimación, no medición. */
    public static final long VISION_OVERHEAD = 384 * MIB;
    /** Caché KV adicional al pasar de 4.096 a 8.192 tokens de contexto en LiteRT-LM. Estimación. */
    public static final long LONG_CONTEXT_OVERHEAD = 512 * MIB;
    public static final int LITERT_STANDARD = 4096;
    public static final int LITERT_LONG = 8192;

    /**
     * Memoria necesaria para LiteRT-LM. Si los pesos ya están residentes (mismo modelo cargado),
     * no se vuelven a contar: antes, al pedir visión con Gemma ya cargado, se exigía la RAM de
     * los pesos otra vez y la foto fallaba con «RAM insuficiente» teniendo memoria de sobra.
     */
    public static long requiredLiteRt(String id, boolean weightsResident, long threshold, boolean vision, int contextTokens) {
        long weights = LocalModelPolicy.LIGHT.equals(id) ? 2019382400L
                : LocalModelPolicy.GEMMA_E4B.equals(id) ? 3659530240L : 2588147712L;
        return (weightsResident ? 0 : weights) + 512 * MIB + (vision ? VISION_OVERHEAD : 0)
                + (contextTokens > LITERT_STANDARD ? LONG_CONTEXT_OVERHEAD : 0) + Math.max(RESERVE, threshold);
    }

    /** Mayor contexto que cabe (8.192 o 4.096), o 0 si ni el estándar cabe. */
    public static int selectLiteRtContext(String id, Snapshot ram, boolean weightsResident, boolean vision) {
        if (!ram.known() || ram.low) return 0;
        if (ram.available >= requiredLiteRt(id, weightsResident, ram.threshold, vision, LITERT_LONG)) return LITERT_LONG;
        if (ram.available >= requiredLiteRt(id, weightsResident, ram.threshold, vision, LITERT_STANDARD)) return LITERT_STANDARD;
        return 0;
    }

    public static boolean canLoad(String id, Snapshot ram, boolean loaded) {
        return ram.known() && !ram.low && ram.available >= required(id, loaded, ram.threshold);
    }
    /** Choose the largest budget that fits. Reducing context never removes the weight/reserve cost. */
    public static int selectContext(String id, Snapshot ram, boolean loaded) {
        if (canLoad(id, ram, loaded)) return GgufContextPolicy.STANDARD;
        if ((LocalModelPolicy.PRIMARY.equals(id) || LocalModelPolicy.LIGHT.equals(id)) && ram.known() && !ram.low
                && ram.available >= required(id, loaded, ram.threshold, GgufContextPolicy.COMPACT)) return GgufContextPolicy.COMPACT;
        return 0;
    }
    /** An already resident engine will be released on activation; do not charge both models here. */
    public static boolean canPrepare(String id, Snapshot ram, boolean residentEngine) {
        return residentEngine || selectContext(id, ram, false) != 0;
    }
    /** During evaluation the KV/context is already allocated: do not charge its budget twice. */
    public static boolean underPressure(Snapshot ram) {
        return !ram.known() || ram.low || ram.available < Math.max(RESERVE, ram.threshold);
    }
    public static class PressureException extends IllegalStateException {
        public PressureException(String message) { super(message); }
    }
}
