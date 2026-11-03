package salve.core;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Reintentar la carga dejando que Android libere memoria, sin reintentar lo cancelado. */
public class ModelActivationRetryTest {
    private static ModelResult fail() { return ModelResult.failure(ModelResult.Status.ERROR, "sin memoria", 0L); }
    private static ModelResult ok() { return ModelResult.success("Hola", 1L); }

    @Test public void retriesUntilItLoads() {
        AtomicInteger tries = new AtomicInteger();
        List<Long> pauses = new ArrayList<>();
        ModelResult r = ModelActivationRetry.run(() -> tries.incrementAndGet() < 3 ? fail() : ok(),
                4, 6000, () -> false, pauses::add);
        assertTrue(r.isSuccess());
        assertEquals(3, tries.get());
        assertEquals("pausa antes de cada reintento", 2, pauses.size());
    }

    @Test public void givesUpAfterTheLastAttemptAndKeepsTheReason() {
        AtomicInteger tries = new AtomicInteger();
        ModelResult r = ModelActivationRetry.run(() -> { tries.incrementAndGet(); return fail(); },
                4, 1, () -> false, ms -> { });
        assertFalse(r.isSuccess());
        assertEquals(4, tries.get());
        assertEquals("sin memoria", r.getError());
    }

    @Test public void neverRetriesWhatBryanCancelled() {
        AtomicInteger tries = new AtomicInteger();
        ModelResult r = ModelActivationRetry.run(() -> { tries.incrementAndGet();
                return ModelResult.failure(ModelResult.Status.CANCELLED, "pausado", 0L); },
                4, 1, () -> false, ms -> { });
        assertEquals(ModelResult.Status.CANCELLED, r.getStatus());
        assertEquals(1, tries.get());
        AtomicInteger later = new AtomicInteger();
        ModelActivationRetry.run(() -> { later.incrementAndGet(); return fail(); }, 4, 1, () -> later.get() >= 2, ms -> { });
        assertEquals("se para en cuanto se cancela", 2, later.get());
    }
}
