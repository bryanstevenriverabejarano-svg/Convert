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
                nativeLibDir == null ? null : nativeLibDir.getBytes(StandardCharsets.UTF_8), false, cancellation(cancelled));
        if (model == 0) throw new IllegalStateException("No se pudo cargar el modelo GGUF");
        loadedPath = path;
        loadedWithGpu = allowGpu;
        loadedWithNpu = allowNpu;
        gpuActive = gpuActive();
        npuActive = npuActive();
    }

    /**
     * Solo Hexagon: carga y calienta el modelo en la NPU; si no puede, devuelve false SIN cargar nada
     * en la gráfica ni en el procesador. Así cada guardia (NPU / gráfica) se apunta solo mientras de
     * verdad se prueba su acelerador, y un cierre en la NPU no apaga también la gráfica.
     */
    public static synchronized boolean tryInitNpu(String path, String dspDir, BooleanSupplier cancelled) {
        reset();
        if (!libraryLoaded) {
            System.loadLibrary("salve_llama");
            libraryLoaded = true;
        }
        long handle = load(path.getBytes(StandardCharsets.UTF_8), true, false, null,
                dspDir == null ? null : dspDir.getBytes(StandardCharsets.UTF_8), true, cancellation(cancelled));
        if (handle == 0) return false;
        model = handle;
        loadedPath = path;
        loadedWithNpu = true;
        loadedWithGpu = false;
        gpuActive = false;
        npuActive = npuActive();
        return true;
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

    public static synchronized String generate(String prompt, String system, int requestedContext, byte[] media, BooleanSupplier cancelled) {
        return generate(prompt, system, requestedContext, media, cancelled, null);
    }

    /**
     * Con {@code media} (una foto JPEG/PNG) usa la visión cargada con {@link #ensureVision}; si no hay
     * visión cargada, falla en vez de contestar sin haber mirado la foto.
     *
     * @param onChunk recibe el texto según se escribe (sin el pensamiento), para que la voz empiece antes
     *                de terminar. Solo en texto libre: la salida JSON de un turno cognitivo es interna y
     *                nunca se transmite. Puede ser null.
     */
    public static synchronized String generate(String prompt, String system, int requestedContext, byte[] media,
                                               BooleanSupplier cancelled, java.util.function.Consumer<String> onChunk) {
        if (model == 0) throw new IllegalStateException("GGUF no está inicializado");
        if (media != null && vision == 0) throw new IllegalStateException("El modelo no tiene la visión cargada");
        // Los ojos se pasan siempre que estén cargados (aunque no haya foto): el motor usa entonces un
        // único contexto para texto, fotos y audio y no pierde lo ya leído al alternar.
        final long eyes = vision;
        streamed = false;
        BooleanSupplier stop = cancellation(cancelled);
        boolean cognitive = salve.core.cognition.CognitivePrompt.isCognitive(prompt);
        boolean repair = salve.core.cognition.CognitivePrompt.isFormatRepair(prompt);
        // Las reglas fijas van al turno de sistema, detrás de la identidad (también fija): así el motor
        // conserva leído en su caché todo ese principio y en cada respuesta solo lee lo que cambió.
        String[] split = salve.core.cognition.CognitivePrompt.splitStableRules(prompt);
        String user = split == null ? prompt : split[1];
        String instructions = split == null ? system : system + "\n" + split[0];
        byte[] input = user.getBytes(StandardCharsets.UTF_8), identity = instructions.getBytes(StandardCharsets.UTF_8);
        byte[] grammar = cognitive
                ? salve.core.cognition.CognitiveOutputConstraints.gbnf().getBytes(StandardCharsets.UTF_8) : new byte[0];
        GgufContextPolicy.Generation result = GgufContextPolicy.generate(requestedContext, selected -> {
            contextTokens = selected;
            LiveText live = onChunk == null || cognitive || media != null ? null : new LiveText(onChunk);
            try {
                return ThinkingFilter.strip(new String(infer(model, input, identity, grammar, selected,
                        repair, eyes, media, stop, live), StandardCharsets.UTF_8).trim());
            } finally {
                String measured = lastStats();
                stats = measured == null ? "" : measured;
                if (!stats.isEmpty()) android.util.Log.i("GgufLlm", stats);
                if (live != null) streamed = live.emitted();
            }
        }, stop);
        return result.text;
    }

    private static boolean streamed;
    /** ¿La última respuesta ya se entregó por trozos a quien la pidió? */
    public static synchronized boolean lastStreamed() { return streamed; }

    /**
     * Recibe los bytes que escribe el motor y entrega solo texto nuevo y limpio: sin el bloque de
     * pensamiento ni marcas de turno. Si la limpieza cambia algo ya entregado, espera al final.
     */
    static final class LiveText implements java.util.function.Consumer<Object> {
        private final java.util.function.Consumer<String> target;
        private final StringBuilder raw = new StringBuilder();
        private String sent = "";
        LiveText(java.util.function.Consumer<String> target) { this.target = target; }
        @Override public void accept(Object chunk) {
            raw.append(chunk instanceof byte[] ? new String((byte[]) chunk, StandardCharsets.UTF_8) : String.valueOf(chunk));
            String clean = ThinkingFilter.strip(raw.toString());
            if (clean == null || clean.length() <= sent.length() || !clean.startsWith(sent)) return;
            String delta = clean.substring(sent.length());
            sent = clean;
            target.accept(delta);
        }
        boolean emitted() { return !sent.isEmpty(); }
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
                                    byte[] kernelCache, byte[] nativeLibDir, boolean acceleratorOnly, BooleanSupplier cancelled);
    private static native boolean npuActive();
    private static native boolean gpuActive();
    private static native String lastStats();
    private static volatile String stats = "";

    /** Dónde se fue el tiempo de la última respuesta (leer el mensaje / escribir), y si fue por gráfica. */
    public static String getLastStats() { return stats; }
    private static native byte[] infer(long model, byte[] prompt, byte[] system, byte[] grammar, int contextTokens, boolean repair,
                                       long vision, byte[] media, BooleanSupplier cancelled,
                                       java.util.function.Consumer<Object> sink);
    private static native void releaseCache();

    /**
     * Memoria baja: suelta el contexto que el motor conserva entre respuestas (y lo ya leído).
     * El modelo sigue cargado; la próxima respuesta crea el contexto de nuevo y relee el prompt.
     */
    public static synchronized void trimCache() {
        if (model != 0) releaseCache();
    }
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
