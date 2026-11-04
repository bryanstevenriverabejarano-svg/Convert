package salve.core;

/** Admission estimates, not measured device guarantees. Uses bytes, never Java heap limits. */
public final class ModelMemoryPolicy {
    public static final long MIB = 1024L * 1024L;
    /** Margen libre para Android en los modelos GGUF (retirados) y el vigilante de presión. */
    public static final long RESERVE = 768 * MIB;
    /** Margen para Android con Gemma (LiteRT-LM), además de su propio umbral de memoria baja. */
    public static final long LITERT_RESERVE = 512 * MIB;
    public static final long LARGE_WEIGHTS = 4920749472L;
    /** Qwen 3.5 9B Q4_K_M (archivo fijado, bytes exactos). Se mapea del archivo (mmap), no se copia. */
    public static final long QWEN35_WEIGHTS = 5627044224L;
    /** Qwen 3.5 4B Q4_K_M (archivo fijado). Cabe entero en RAM: no hay que leerlo del almacenamiento a cada palabra. */
    public static final long QWEN35_4B_WEIGHTS = 2707513696L;

    /** Los Qwen 3.5 que Salve descarga (GGUF, pesos por mmap). */
    static boolean isQwen(String id) {
        return LocalModelPolicy.QWEN35_9B.equals(id) || LocalModelPolicy.QWEN35_4B.equals(id);
    }
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
                : LocalModelPolicy.QWEN35_9B.equals(id) ? QWEN35_WEIGHTS
                : LocalModelPolicy.QWEN35_4B.equals(id) ? QWEN35_4B_WEIGHTS
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
    /** Caché KV adicional al pasar de 4.096 a 7.000 tokens de contexto en LiteRT-LM. Estimación. */
    public static final long LONG_CONTEXT_OVERHEAD = 384 * MIB;
    /** Motor LiteRT-LM (buffers y activaciones) además de los pesos, que se mapean del archivo. */
    public static final long LITERT_RUNTIME_OVERHEAD = 384 * MIB;
    public static final int LITERT_STANDARD = 4096;
    public static final int LITERT_LONG = 7000;
    /**
     * Contexto ampliado para Gemma 4 E4B, que admite hasta 32.768 tokens. Con 4.096 solo le cabían unos
     * dos mensajes de historial; estos escalones dejan sitio al historial, las herramientas y la respuesta.
     */
    public static final int LITERT_8K = 8192, LITERT_16K = 16384;
    /** Escalones intermedios: mejor 5k o 6k que caer directo a 4.096 cuando 8k no cabe. */
    public static final int LITERT_6K = 6144, LITERT_5K = 5120;
    /** Escalones de LiteRT-LM del mayor al menor; los ampliados solo para modelos que los admiten. */
    static int[] liteRtTiers(String id) {
        // 16.384 se retiró (2/11/2026): en el S24 Ultra cargó, pero Android cerró la app al responder.
        // 8.192 también: funcionaba, pero el móvil se calentaba y tardaba demasiado en responder (Bryan).
        return LocalModelPolicy.GEMMA_E4B.equals(id)
                ? new int[]{LITERT_LONG, LITERT_6K, LITERT_5K, LITERT_STANDARD}
                : new int[]{LITERT_LONG, LITERT_STANDARD};
    }
    /** Móvil grande (10 GiB o más): Android puede liberar las apps en caché para el modelo. */
    static final long LARGE_PHONE_TOTAL = 10L * 1024 * MIB;
    /** Margen de RAM total que se deja al resto del sistema en un móvil grande. */
    static final long LARGE_PHONE_HEADROOM = 3L * 1024 * MIB;

    /**
     * Caché KV extra sobre 4.096 tokens. Se escala a partir de la estimación ya usada para 7.000
     * (384 MiB por 2.904 tokens más, unos 135 KB por token).
     */
    static long contextOverhead(int contextTokens) {
        if (contextTokens <= LITERT_STANDARD) return 0;
        return LONG_CONTEXT_OVERHEAD * (contextTokens - LITERT_STANDARD) / (LITERT_LONG - LITERT_STANDARD);
    }

    /**
     * Memoria necesaria para LiteRT-LM. Si los pesos ya están residentes (mismo modelo cargado),
     * no se vuelven a contar: antes, al pedir visión con Gemma ya cargado, se exigía la RAM de
     * los pesos otra vez y la foto fallaba con «RAM insuficiente» teniendo memoria de sobra.
     */
    public static long requiredLiteRt(String id, boolean weightsResident, long threshold, boolean vision, int contextTokens) {
        long weights = LocalModelPolicy.LIGHT.equals(id) ? 2019382400L
                : LocalModelPolicy.GEMMA_E4B.equals(id) ? 3659530240L : 2588147712L;
        return (weightsResident ? 0 : weights) + LITERT_RUNTIME_OVERHEAD + (vision ? VISION_OVERHEAD : 0)
                + contextOverhead(contextTokens) + Math.max(LITERT_RESERVE, threshold);
    }

    /** Mayor contexto que cabe con la RAM «disponible» medida (16.384 … 4.096), o 0 si ni el estándar cabe. */
    public static int selectLiteRtContext(String id, Snapshot ram, boolean weightsResident, boolean vision) {
        if (!ram.known() || ram.low) return 0;
        for (int tokens : liteRtTiers(id))
            if (ram.available >= requiredLiteRt(id, weightsResident, ram.threshold, vision, tokens)) return tokens;
        return 0;
    }

    /** Gemma (LiteRT-LM): sus pesos se mapean del archivo, no se reservan de golpe. */
    public static boolean isLiteRt(String id) {
        return LocalModelPolicy.FALLBACK.equals(id) || LocalModelPolicy.GEMMA_E4B.equals(id);
    }

    /**
     * Admisión para cargar Gemma de verdad. Si no caben ni 4.096 tokens con la RAM «disponible»
     * que dice Android, pero el móvil tiene RAM total de sobra, se intenta igualmente con 4.096:
     * Android cuenta como ocupada la memoria de apps cerradas que guarda en caché y la libera en
     * cuanto alguien la pide, y los pesos se leen del archivo por partes. Antes Salve se rendía y
     * Bryan tenía que «limpiar la RAM» a mano (Galaxy de 10,82 GiB con 2,71 GiB «disponibles»).
     * Sigue exigiendo libre todo lo que no son pesos (motor, visión y reserva de Android).
     */
    public static int admitLiteRt(String id, Snapshot ram, boolean weightsResident, boolean vision) {
        int strict = selectLiteRtContext(id, ram, weightsResident, vision);
        int large = largePhoneContext(id, ram, weightsResident, vision);
        if (Math.max(strict, large) != 0) return Math.max(strict, large);
        return roomyDevice(id, ram, weightsResident, vision) ? LITERT_STANDARD : 0;
    }

    /**
     * En un móvil de 10 GiB o más (el Galaxy S24 Ultra de Bryan: 10,82 GiB, con 2,7–3,1 GiB
     * «disponibles» por las apps en caché), el contexto ampliado cabe si queda libre todo lo que NO
     * son pesos (motor, caché KV, visión y reserva de Android) y la RAM total cubre el modelo entero
     * con 3 GiB de margen. Android libera la caché cuando el motor la pide. Si aun así la carga
     * fallara, {@link LiteRtContextGuard} baja un escalón la próxima vez.
     */
    static int largePhoneContext(String id, Snapshot ram, boolean weightsResident, boolean vision) {
        if (!ram.known() || ram.low || weightsResident || ram.total < LARGE_PHONE_TOTAL
                || !LocalModelPolicy.GEMMA_E4B.equals(id)) return 0; // otros modelos: como siempre
        for (int tokens : liteRtTiers(id)) {
            if (tokens <= LITERT_STANDARD) break; // el estándar ya lo cubre roomyDevice
            if (ram.available >= requiredLiteRt(id, true, ram.threshold, vision, tokens)
                    && ram.total >= requiredLiteRt(id, false, ram.threshold, vision, tokens) + LARGE_PHONE_HEADROOM)
                return tokens;
        }
        return 0;
    }

    /** ¿RAM total de sobra (el doble de lo necesario) y libre al menos lo que no son pesos? */
    static boolean roomyDevice(String id, Snapshot ram, boolean weightsResident, boolean vision) {
        if (!ram.known() || ram.low || weightsResident) return false; // con pesos residentes ya se mide bien
        long need = requiredLiteRt(id, false, ram.threshold, vision, LITERT_STANDARD);
        long withoutWeights = requiredLiteRt(id, true, ram.threshold, vision, LITERT_STANDARD);
        return ram.total >= 2 * need && ram.available >= withoutWeights;
    }

    public static boolean canLoad(String id, Snapshot ram, boolean loaded) {
        return ram.known() && !ram.low && ram.available >= required(id, loaded, ram.threshold);
    }
    /** Choose the largest budget that fits. Reducing context never removes the weight/reserve cost. */
    public static int selectContext(String id, Snapshot ram, boolean loaded) {
        if (canLoad(id, ram, loaded)) return GgufContextPolicy.STANDARD;
        if (largePhoneGguf(id, ram, loaded)) return GgufContextPolicy.STANDARD;
        if ((LocalModelPolicy.PRIMARY.equals(id) || LocalModelPolicy.LIGHT.equals(id)) && ram.known() && !ram.low
                && ram.available >= required(id, loaded, ram.threshold, GgufContextPolicy.COMPACT)) return GgufContextPolicy.COMPACT;
        // Qwen 3.5 9B: si no cabe ni con la regla de móvil grande, se intenta con contexto compacto antes de rendirse.
        if (isQwen(id) && ram.known() && ram.total >= LARGE_PHONE_TOTAL
                && ram.available >= required(id, true, ram.threshold, GgufContextPolicy.COMPACT)
                && ram.total >= required(id, false, ram.threshold, GgufContextPolicy.COMPACT) + LARGE_PHONE_HEADROOM)
            return GgufContextPolicy.COMPACT;
        return 0;
    }
    /**
     * Qwen 3.5 9B en un móvil de 10 GiB o más (el S24 Ultra: 10,82 GiB, con solo 2,7–3,1 GiB
     * «disponibles» por las apps en caché): como con Gemma E4B, sus pesos se leen del archivo y Android
     * libera la caché cuando hacen falta. Basta con que quede libre lo que NO son pesos (contexto y
     * reserva) y que la RAM total cubra el modelo entero con 3 GiB de margen. Si aun así no carga,
     * la activación falla y SalveLLM vuelve al modelo anterior.
     */
    static boolean largePhoneGguf(String id, Snapshot ram, boolean loaded) {
        if (!isQwen(id) || !ram.known() || ram.low || ram.total < LARGE_PHONE_TOTAL) return false;
        return ram.available >= required(id, true, ram.threshold)
                && ram.total >= required(id, false, ram.threshold) + LARGE_PHONE_HEADROOM;
    }

    /** An already resident engine will be released on activation; do not charge both models here. */
    public static boolean canPrepare(String id, Snapshot ram, boolean residentEngine) {
        if (residentEngine || selectContext(id, ram, false) != 0) return true;
        // Descargar no gasta RAM: el Qwen grande solo se frena si el móvil, con toda su RAM, no podría con él.
        if (isQwen(id))
            return ram.known() && ram.total >= LARGE_PHONE_TOTAL
                    && ram.total >= required(id, false, ram.threshold) + LARGE_PHONE_HEADROOM;
        // Descargar Gemma no gasta RAM y la carga vuelve a medir: solo se frena si el móvil, aun
        // con toda su RAM libre, no podría con ella. Antes bastaban unas apps en caché para no descargar.
        return isLiteRt(id) && ram.known() && ram.total >= requiredLiteRt(id, false, ram.threshold, false, LITERT_STANDARD);
    }
    /** During evaluation the KV/context is already allocated: do not charge its budget twice. */
    public static boolean underPressure(Snapshot ram) {
        return !ram.known() || ram.low || ram.available < Math.max(RESERVE, ram.threshold);
    }
    public static class PressureException extends IllegalStateException {
        public PressureException(String message) { super(message); }
    }
}
