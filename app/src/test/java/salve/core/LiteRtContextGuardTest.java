package salve.core;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** Contexto ampliado para Gemma 4 E4B en el Galaxy S24 Ultra de Bryan (10,82 GiB). */
public class LiteRtContextGuardTest {
    private static final long GIB = 1024L * ModelMemoryPolicy.MIB;
    private static final String E4B = LocalModelPolicy.GEMMA_E4B;

    private static ModelMemoryPolicy.Snapshot ram(double available, double total) {
        return new ModelMemoryPolicy.Snapshot((long) (available * GIB), (long) (total * GIB), 768 * ModelMemoryPolicy.MIB, false);
    }

    private static final class Prefs implements LiteRtContextGuard.Store {
        final Map<String, Integer> map = new HashMap<>();
        public int get(String key) { return map.getOrDefault(key, 0); }
        public void put(String key, int value) { if (value == 0) map.remove(key); else map.put(key, value); }
    }

    @Test public void s24UltraWithCachedAppsGetsMoreThan4096() {
        // Lo que de verdad marca su móvil: 2,71–3,09 GiB «disponibles» de 10,82.
        assertEquals(ModelMemoryPolicy.LITERT_8K, ModelMemoryPolicy.admitLiteRt(E4B, ram(2.71, 10.82), false, false));
        assertEquals(ModelMemoryPolicy.LITERT_16K, ModelMemoryPolicy.admitLiteRt(E4B, ram(3.09, 10.82), false, false));
        assertEquals("con RAM de sobra, el máximo",
                ModelMemoryPolicy.LITERT_16K, ModelMemoryPolicy.selectLiteRtContext(E4B, ram(7.0, 10.82), false, false));
    }

    @Test public void smallerPhonesAndOtherModelsKeepTheOldLimits() {
        assertEquals("móvil de 7,5 GiB con poca RAM libre: igual que antes, no carga",
                0, ModelMemoryPolicy.admitLiteRt(E4B, ram(2.71, 7.5), false, false));
        assertEquals("y con RAM libre de verdad, solo lo que cabe medido",
                ModelMemoryPolicy.LITERT_STANDARD, ModelMemoryPolicy.admitLiteRt(E4B, ram(4.7, 7.5), false, false));
        assertEquals("E2B nunca pasa de 7.000",
                ModelMemoryPolicy.LITERT_LONG, ModelMemoryPolicy.selectLiteRtContext(LocalModelPolicy.FALLBACK, ram(9.0, 12), false, false));
        assertEquals("memoria baja: nada", 0, ModelMemoryPolicy.admitLiteRt(E4B,
                new ModelMemoryPolicy.Snapshot(9 * GIB, 12 * GIB, 768 * ModelMemoryPolicy.MIB, true), false, false));
        assertEquals("7.000 cuesta lo de siempre", ModelMemoryPolicy.LONG_CONTEXT_OVERHEAD,
                ModelMemoryPolicy.contextOverhead(ModelMemoryPolicy.LITERT_LONG));
        assertTrue(ModelMemoryPolicy.contextOverhead(ModelMemoryPolicy.LITERT_16K) > ModelMemoryPolicy.contextOverhead(ModelMemoryPolicy.LITERT_8K));
    }

    @Test public void aCrashDuringLoadStepsDownAndIsRemembered() {
        Prefs prefs = new Prefs();
        LiteRtContextGuard guard = new LiteRtContextGuard(prefs);
        assertEquals(16384, guard.admit(16384));
        guard.loading(16384); // ... Android cierra la app aquí
        LiteRtContextGuard next = new LiteRtContextGuard(prefs);
        assertEquals(8192, next.admit(16384));
        next.loading(8192);
        next.loaded();
        assertEquals("aprendido: no vuelve a intentar 16k", 8192, new LiteRtContextGuard(prefs).admit(16384));
        next.loading(8192); // otro cierre con 8k
        assertEquals(7000, new LiteRtContextGuard(prefs).admit(16384));
        guard.reset();
        assertEquals(16384, new LiteRtContextGuard(prefs).admit(16384));
    }

    @Test public void normalLoadsAndStandardContextLeaveNoTrace() {
        Prefs prefs = new Prefs();
        LiteRtContextGuard guard = new LiteRtContextGuard(prefs);
        guard.loading(4096);
        assertTrue(prefs.map.isEmpty());
        guard.loading(16384);
        guard.loaded();
        assertTrue(prefs.map.isEmpty());
        assertEquals(16384, guard.admit(16384));
    }

    @Test public void promptGrowsWithTheLoadedContext() {
        assertEquals(10500, LiteRtContextGuard.promptBudget(4096));
        assertEquals(10500, LiteRtContextGuard.promptBudget(7000));
        assertEquals(16000, LiteRtContextGuard.promptBudget(8192));
        assertEquals(22000, LiteRtContextGuard.promptBudget(16384));
        // ~3,5 caracteres por token: el prompt deja sitio para pensar y responder.
        assertTrue(22000 / 3.0 < 16384 - 2048);
        assertTrue(16000 / 3.0 < 8192 - 1536);
    }
}
