package salve.core;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/** Bounded context adaptation. Allocation failures are distinct from invalid input or cancellation. */
public final class GgufContextPolicy {
    public static final int STANDARD = 4096, COMPACT = 2048;
    private GgufContextPolicy() { }
    public static boolean supported(int context) { return context == STANDARD || context == COMPACT; }
    public static int promptBudget(int context) { return context == COMPACT ? 3000 : 7000; }
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
            return new Generation(attempt.apply(COMPACT), COMPACT);
        }
    }
    private static void check(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw new CancellationException("Turno cancelado");
    }
}
