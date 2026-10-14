package salve.core;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/** CPU llama.cpp backend. All handles are accessed under this class's monitor. */
public final class GgufLlm {
    private static boolean libraryLoaded;
    private static long model;
    private static String loadedPath;
    private static int contextTokens = GgufContextPolicy.STANDARD;
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
        return generate(prompt, "Tu nombre es Salve. Responde en español. No confundas tu identidad con la persona que te habla.", cancelled);
    }

    public static synchronized String generate(String prompt, String system, BooleanSupplier cancelled) {
        return generate(prompt, system, GgufContextPolicy.STANDARD, cancelled);
    }
    public static synchronized String generate(String prompt, String system, int requestedContext, BooleanSupplier cancelled) {
        if (model == 0) throw new IllegalStateException("GGUF no está inicializado");
        BooleanSupplier stop = cancellation(cancelled);
        byte[] input = prompt.getBytes(StandardCharsets.UTF_8), identity = system.getBytes(StandardCharsets.UTF_8);
        byte[] grammar = salve.core.cognition.CognitivePrompt.isCognitive(prompt)
                ? salve.core.cognition.CognitiveOutputConstraints.gbnf().getBytes(StandardCharsets.UTF_8) : new byte[0];
        GgufContextPolicy.Generation result = GgufContextPolicy.generate(requestedContext, selected -> {
            contextTokens = selected;
            return new String(infer(model, input, identity, grammar, selected,
                    salve.core.cognition.CognitivePrompt.isFormatRepair(prompt), stop), StandardCharsets.UTF_8).trim();
        }, stop);
        return result.text;
    }

    private static BooleanSupplier cancellation(BooleanSupplier requested) {
        Thread caller = Thread.currentThread();
        return () -> caller.isInterrupted() || requested.getAsBoolean();
    }

    public static synchronized boolean isInitialized() { return model != 0; }
    public static synchronized int getContextTokens() { return contextTokens; }
    public static synchronized void reset() {
        if (model != 0) release(model);
        model = 0;
        loadedPath = null;
        contextTokens = GgufContextPolicy.STANDARD;
    }
    private static native long load(byte[] path, BooleanSupplier cancelled);
    private static native byte[] infer(long model, byte[] prompt, byte[] system, byte[] grammar, int contextTokens, boolean repair, BooleanSupplier cancelled);
    private static native void release(long model);
}
