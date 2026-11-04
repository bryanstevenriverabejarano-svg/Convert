package salve.core;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/** llama.cpp: Hexagon NPU (when packaged and supported), Adreno OpenCL, then CPU. */
public final class GgufLlm {
    private static boolean libraryLoaded;
    private static long model;
    private static String loadedPath;
    private static boolean loadedWithGpu;
    private static boolean loadedWithNpu;
    // volatile y sin monitor: la cabecera lo lee mientras otro hilo está generando.
    private static volatile boolean gpuActive;
    private static volatile boolean npuActive;
    private static int contextTokens = GgufContextPolicy.STANDARD;
    private GgufLlm() { }

    public static synchronized void init(String path, BooleanSupplier cancelled) {
        init(path, false, null, cancelled);
    }

    /**
     * @param allowGpu probar la gráfica; el motor vuelve solo al procesador si no hay GPU compatible
     * @param kernelCache carpeta para guardar los kernels OpenCL compilados (la 1.ª carga tarda más)
     */
    public static synchronized void init(String path, boolean allowGpu, String kernelCache, BooleanSupplier cancelled) {
        init(path, false, allowGpu, kernelCache, null, cancelled);
    }

    /** NPU first, GPU second, CPU last; native library directory is also used to find HTP skels. */
    public static synchronized void init(String path, boolean allowNpu, boolean allowGpu,
                                         String kernelCache, String nativeLibDir, BooleanSupplier cancelled) {
        if (model != 0 && path.equals(loadedPath)
                && (allowNpu || !loadedWithNpu) && (allowGpu || !loadedWithGpu)) return;
        reset();
        if (!libraryLoaded) {
            System.loadLibrary("salve_llama");
            libraryLoaded = true;
        }
        model = load(path.getBytes(StandardCharsets.UTF_8), allowNpu, allowGpu,
                kernelCache == null ? null : kernelCache.getBytes(StandardCharsets.UTF_8),
                nativeLibDir == null ? null : nativeLibDir.getBytes(StandardCharsets.UTF_8), cancellation(cancelled));
        if (model == 0) throw new IllegalStateException("No se pudo cargar el modelo GGUF");
        loadedPath = path;
        loadedWithGpu = allowGpu;
        loadedWithNpu = allowNpu;
        gpuActive = gpuActive();
        npuActive = npuActive();
    }

    public static synchronized String generate(String prompt, BooleanSupplier cancelled) {
        return generate(prompt, "Tu nombre es Salve. Responde en español. No confundas tu identidad con la persona que te habla.", cancelled);
    }

    public static synchronized String generate(String prompt, String system, BooleanSupplier cancelled) {
        return generate(prompt, system, GgufContextPolicy.STANDARD, cancelled);
    }
    public static synchronized String generate(String prompt, String system, int requestedContext, BooleanSupplier cancelled) {
        return generate(prompt, system, requestedContext, null, cancelled);
    }

    /**
     * Con {@code media} (una foto JPEG/PNG) usa la visión cargada con {@link #ensureVision}; si no hay
     * visión cargada, falla en vez de contestar sin haber mirado la foto.
     */
    public static synchronized String generate(String prompt, String system, int requestedContext, byte[] media, BooleanSupplier cancelled) {
        if (model == 0) throw new IllegalStateException("GGUF no está inicializado");
        if (media != null && vision == 0) throw new IllegalStateException("El modelo no tiene la visión cargada");
        final long eyes = media == null ? 0 : vision;
        BooleanSupplier stop = cancellation(cancelled);
        byte[] input = prompt.getBytes(StandardCharsets.UTF_8), identity = system.getBytes(StandardCharsets.UTF_8);
        byte[] grammar = salve.core.cognition.CognitivePrompt.isCognitive(prompt)
                ? salve.core.cognition.CognitiveOutputConstraints.gbnf().getBytes(StandardCharsets.UTF_8) : new byte[0];
        GgufContextPolicy.Generation result = GgufContextPolicy.generate(requestedContext, selected -> {
            contextTokens = selected;
            try {
                return ThinkingFilter.strip(new String(infer(model, input, identity, grammar, selected,
                        salve.core.cognition.CognitivePrompt.isFormatRepair(prompt), eyes, media, stop), StandardCharsets.UTF_8).trim());
            } finally {
                String measured = lastStats();
                stats = measured == null ? "" : measured;
                if (!stats.isEmpty()) android.util.Log.i("GgufLlm", stats);
            }
        }, stop);
        return result.text;
    }

    private static BooleanSupplier cancellation(BooleanSupplier requested) {
        Thread caller = Thread.currentThread();
        return () -> caller.isInterrupted() || requested.getAsBoolean();
    }

    public static synchronized boolean isInitialized() { return model != 0; }
    /** ¿El modelo cargado piensa en la gráfica? */
    public static boolean usesGpu() { return gpuActive; }
    /** True only if the GGUF model actually offloaded to Hexagon, never merely because a chip exists. */
    public static boolean usesNpu() { return npuActive; }
    public static String backendName() { return npuActive ? "NPU Hexagon" : gpuActive ? "GPU OpenCL" : "CPU"; }
    public static synchronized int getContextTokens() { return contextTokens; }
    /** Carga (una vez) la visión del modelo actual. La visión depende del modelo: se libera antes que él. */
    public static synchronized void ensureVision(String mmprojPath) {
        if (model == 0) throw new IllegalStateException("GGUF no está inicializado");
        if (vision != 0 && mmprojPath.equals(visionPath)) return;
        releaseVisionIfLoaded();
        vision = loadVision(model, mmprojPath.getBytes(StandardCharsets.UTF_8));
        if (vision == 0) throw new IllegalStateException("No se pudo cargar la visión del modelo");
        visionPath = mmprojPath;
    }
    public static synchronized boolean hasVision() { return vision != 0; }
    private static void releaseVisionIfLoaded() {
        if (vision != 0) releaseVision(vision);
        vision = 0;
        visionPath = null;
    }

    public static synchronized void reset() {
        releaseVisionIfLoaded();
        if (model != 0) release(model);
        model = 0;
        loadedPath = null;
        loadedWithGpu = false;
        loadedWithNpu = false;
        gpuActive = false;
        npuActive = false;
        adapterPath = null;
        contextTokens = GgufContextPolicy.STANDARD;
    }
    private static native long load(byte[] path, boolean allowNpu, boolean allowGpu,
                                    byte[] kernelCache, byte[] nativeLibDir, BooleanSupplier cancelled);
    private static native boolean npuActive();
    private static native boolean gpuActive();
    private static native String lastStats();
    private static volatile String stats = "";

    /** Dónde se fue el tiempo de la última respuesta (leer el mensaje / escribir), y si fue por gráfica. */
    public static String getLastStats() { return stats; }
    private static native byte[] infer(long model, byte[] prompt, byte[] system, byte[] grammar, int contextTokens, boolean repair,
                                       long vision, byte[] media, BooleanSupplier cancelled);
    private static native long loadVision(long model, byte[] mmprojPath);
    private static native void releaseVision(long vision);
    private static long vision;
    private static String visionPath;
    private static native void release(long model);
    private static native boolean setAdapter(long model, byte[] path, float scale);

    private static String adapterPath;

    /**
     * Activa un adaptador LoRA personal sobre el modelo cargado, o lo quita con {@code path == null}.
     * El adaptador debe haberse entrenado para este mismo modelo base; si no encaja, llama.cpp lo
     * rechaza y el modelo base sigue respondiendo sin él.
     */
    public static synchronized boolean applyAdapter(String path, float scale) {
        if (model == 0) return false;
        boolean applied = setAdapter(model, path == null ? null : path.getBytes(StandardCharsets.UTF_8), scale);
        adapterPath = applied ? path : null;
        return applied;
    }

    public static synchronized String getAdapterPath() { return adapterPath; }
}
