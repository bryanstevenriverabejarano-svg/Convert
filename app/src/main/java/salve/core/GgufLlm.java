package salve.core;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/** CPU llama.cpp backend. All handles are accessed under this class's monitor. */
public final class GgufLlm {
    private static boolean libraryLoaded;
    private static long model;
    private static String loadedPath;
    private GgufLlm() { }

    public static synchronized void init(String path, BooleanSupplier cancelled) {
        if (model != 0 && path.equals(loadedPath)) return;
        reset();
        if (!libraryLoaded) {
            System.loadLibrary("salve_llama");
            libraryLoaded = true;
        }
        model = load(path.getBytes(StandardCharsets.UTF_8), cancellation(cancelled));
        if (model == 0) throw new IllegalStateException("No se pudo cargar el modelo GGUF");
        loadedPath = path;
    }

    public static synchronized String generate(String prompt, BooleanSupplier cancelled) {
        if (model == 0) throw new IllegalStateException("GGUF no está inicializado");
        byte[] result = infer(model, prompt.getBytes(StandardCharsets.UTF_8), cancellation(cancelled));
        return new String(result, StandardCharsets.UTF_8).trim();
    }

    private static BooleanSupplier cancellation(BooleanSupplier requested) {
        Thread caller = Thread.currentThread();
        return () -> caller.isInterrupted() || requested.getAsBoolean();
    }

    public static synchronized boolean isInitialized() { return model != 0; }
    public static synchronized void reset() {
        if (model != 0) release(model);
        model = 0;
        loadedPath = null;
    }
    private static native long load(byte[] path, BooleanSupplier cancelled);
    private static native byte[] infer(long model, byte[] prompt, BooleanSupplier cancelled);
    private static native void release(long model);
}
