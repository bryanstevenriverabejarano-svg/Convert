package salve.core;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/** Bounded context adaptation. Allocation failures are distinct from invalid input or cancellation. */
public final class GgufContextPolicy {
    public static final int STANDARD = 4096, COMPACT = 2048;
    /**
     * Contexto ampliado (7k) para Gemma sin censura en un móvil grande. Con 4.096 tokens el prompt
     * se quedaba en 7.000 caracteres y su núcleo de identidad (≈8.000) nunca cabía: iba la versión
     * compacta y a su yo persistente le tocaban unos 550 caracteres. Con 7k entra su núcleo completo.
     */
    public static final int EXTENDED = 7168;
    private GgufContextPolicy() { }
    public static boolean supported(int context) { return context == EXTENDED || context == STANDARD || context == COMPACT; }
    /**
     * Caracteres de prompt cognitivo por contexto. 12.000 deja entrar el núcleo completo (≈8.000),
     * su catálogo y la evidencia (CognitivePrompt pide núcleo + catálogo + 2.600) y queda por debajo
     * de lo que hizo perder el hilo a E2B (≈21.000). En tokens: ≈3.600 + sistema + 1.024 de respuesta.
     */
    public static int promptBudget(int context) { return context == COMPACT ? 3000 : context == EXTENDED ? 12000 : 7000; }
    public static final class AllocationException extends IllegalStateException {
        public AllocationException(String message) { super(message); }
    }
    public static final class Generation {
        public final String text;
        public final int contextTokens;
        Generation(String text, int contextTokens) { this.text = text; this.contextTokens = contextTokens; }
    }
    public static Generation generate(int context, IntFunction<String> attempt, BooleanSupplier cancelled) {
        if (!supported(context)) throw new IllegalArgumentException("Contexto GGUF no admitido");
        check(cancelled);
        try { return new Generation(attempt.apply(context), context); }
        catch (AllocationException failure) {
            if (context == COMPACT) throw failure;
            check(cancelled);
            // Tokenization runs again: never silently truncate a prompt to make it fit.
            // 7k → 4k → 2k: cada escalón solo si el anterior no pudo reservar memoria.
            return generate(context == EXTENDED ? STANDARD : COMPACT, attempt, cancelled);
        }
    }
    private static void check(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw new CancellationException("Turno cancelado");
    }
}
