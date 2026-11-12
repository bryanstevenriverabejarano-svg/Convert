package salve.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class GgufContextPolicyTest {
    @Test public void successfulStandardContextIsNotRepeated() {
        List<Integer> calls = new ArrayList<>();
        GgufContextPolicy.Generation result = GgufContextPolicy.generate(4096, context -> { calls.add(context); return "Hola"; }, () -> false);
        assertEquals(Arrays.asList(4096), calls); assertEquals(4096, result.contextTokens); assertEquals("Hola", result.text);
    }
    @Test public void nativeAllocationFailureRetriesExactlyOnceWithCompactContext() {
        List<Integer> calls = new ArrayList<>();
        GgufContextPolicy.Generation result = GgufContextPolicy.generate(4096, context -> {
            calls.add(context);
            if (context == 4096) throw new GgufContextPolicy.AllocationException("No cabe el contexto");
            return "Respuesta con contexto reducido";
        }, () -> false);
        assertEquals(Arrays.asList(4096,2048), calls); assertEquals(2048, result.contextTokens);
    }
    @Test public void secondAllocationFailurePropagatesWithoutLoop() {
        List<Integer> calls = new ArrayList<>();
        assertThrows(GgufContextPolicy.AllocationException.class, () -> GgufContextPolicy.generate(4096, context -> {
            calls.add(context); throw new GgufContextPolicy.AllocationException("Sin memoria");
        }, () -> false));
        assertEquals(Arrays.asList(4096,2048), calls);
    }
    @Test public void cancelledOrInvalidPromptsAreNeverRetried() {
        for (RuntimeException failure : Arrays.asList(new CancellationException(), new IllegalArgumentException("Too many tokens"), new IllegalStateException("Runtime broken"))) {
            List<Integer> calls = new ArrayList<>();
            assertThrows(failure.getClass(), () -> GgufContextPolicy.generate(4096, context -> {
                calls.add(context); throw failure;
            }, () -> false));
            assertEquals(Arrays.asList(4096), calls);
        }
    }
    @Test public void cancellationBetweenAttemptsStopsBeforeCompactAllocation() {
        AtomicBoolean stopped = new AtomicBoolean(); List<Integer> calls = new ArrayList<>();
        assertThrows(CancellationException.class, () -> GgufContextPolicy.generate(4096, context -> {
            calls.add(context); stopped.set(true); throw new GgufContextPolicy.AllocationException("alloc");
        }, stopped::get));
        assertEquals(Arrays.asList(4096), calls);
    }
    @Test public void compactRetryRevalidatesPromptInsteadOfReturningTruncatedOutput() {
        List<Integer> calls = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> GgufContextPolicy.generate(4096, context -> {
            calls.add(context);
            if (context == 4096) throw new GgufContextPolicy.AllocationException("alloc");
            throw new IllegalArgumentException("El mensaje completo no cabe en 2048 tokens");
        }, () -> false));
        assertEquals(Arrays.asList(4096,2048), calls);
        assertTrue(GgufContextPolicy.promptBudget(2048) < GgufContextPolicy.promptBudget(4096));
    }

    @Test public void extendedContextStepsDownOneTierAtATime() {
        List<Integer> calls = new ArrayList<>();
        GgufContextPolicy.Generation result = GgufContextPolicy.generate(GgufContextPolicy.EXTENDED, context -> {
            calls.add(context);
            if (context == GgufContextPolicy.EXTENDED) throw new GgufContextPolicy.AllocationException("No cabe 7k");
            return "Con 4k";
        }, () -> false);
        assertEquals(Arrays.asList(7168, 4096), calls);
        assertEquals(4096, result.contextTokens);
        calls.clear();
        assertThrows(GgufContextPolicy.AllocationException.class, () -> GgufContextPolicy.generate(GgufContextPolicy.EXTENDED,
                context -> { calls.add(context); throw new GgufContextPolicy.AllocationException("Sin memoria"); }, () -> false));
        assertEquals(Arrays.asList(7168, 4096, 2048), calls);
    }
    @Test public void extendedContextLeavesRoomForHerWholeIdentityCore() throws Exception {
        assertTrue(GgufContextPolicy.supported(GgufContextPolicy.EXTENDED));
        String core = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/main/assets/prompts/salve_cognitive_core_v1.txt")), java.nio.charset.StandardCharsets.UTF_8);
        // CognitivePrompt usa el núcleo completo si núcleo + catálogo (≤1.100) + 2.600 caben en el presupuesto.
        assertTrue(core.length() + 1100 + 2600 < GgufContextPolicy.promptBudget(GgufContextPolicy.EXTENDED));
        assertFalse("con 4k nunca cabía", core.length() + 2600 < GgufContextPolicy.promptBudget(GgufContextPolicy.STANDARD));
    }
}

