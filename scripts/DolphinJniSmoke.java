import salve.core.GgufLlm;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exercises the exact Android JNI adapter on a desktop JVM with real, verified weights. */
public final class DolphinJniSmoke {
    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        try {
            try { GgufLlm.init(args[0], () -> true); throw new AssertionError("Load ignored cancellation"); }
            catch (CancellationException expected) { }
            GgufLlm.init(args[0], () -> false);
            if (!GgufLlm.isInitialized()) throw new AssertionError("Model not loaded");
            String greeting = GgufLlm.generate("Hola, me llamo Bryan. Salúdame por mi nombre en una sola frase.", () -> false);
            System.out.println("GREETING: " + greeting);
            if (!greeting.toLowerCase(java.util.Locale.ROOT).contains("bryan")) throw new AssertionError("Greeting missed name");
            String arithmetic = GgufLlm.generate("¿Cuánto es 17 por 23? Responde solo con el número.", () -> false);
            System.out.println("ARITHMETIC: " + arithmetic);
            if (!arithmetic.contains("391")) throw new AssertionError("Wrong arithmetic");
            AtomicBoolean cancelled = new AtomicBoolean();
            Thread timer = new Thread(() -> {
                try { Thread.sleep(150); cancelled.set(true); } catch (InterruptedException ignored) { }
            });
            timer.start();
            try { GgufLlm.generate("Escribe una historia larga sobre un viaje por España.", cancelled::get); throw new AssertionError("Inference ignored cancellation"); }
            catch (CancellationException expected) { }
            finally { timer.join(); }
            try { GgufLlm.generate("Una palabra. ".repeat(12000), () -> false); throw new AssertionError("Missing context limit"); }
            catch (IllegalArgumentException expected) { }
            String recovered = GgufLlm.generate("Responde solo con la palabra listo.", () -> false);
            if (recovered.isEmpty()) throw new AssertionError("No recovery after cancellation/overflow");
            System.out.println("RECOVERED: " + recovered);
            System.out.println("PASSED: real inference, UTF-8, cancellation, context limit and recovery in "
                    + (System.nanoTime() - start) / 1_000_000 + " ms (desktop CPU, not Android benchmark)");
        } finally { GgufLlm.reset(); }
        if (GgufLlm.isInitialized()) throw new AssertionError("Native model leaked");
    }
}
