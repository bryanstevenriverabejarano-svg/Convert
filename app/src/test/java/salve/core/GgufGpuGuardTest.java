package salve.core;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GgufGpuGuardTest {
    private static LiteRtContextGuard.Store map(Map<String, Integer> m) {
        return new LiteRtContextGuard.Store() {
            @Override public int get(String key) { return m.getOrDefault(key, 0); }
            @Override public void put(String key, int value) { if (value == 0) m.remove(key); else m.put(key, value); }
        };
    }

    @Test public void triesTheGpuUntilSomethingGoesWrong() {
        Map<String, Integer> m = new HashMap<>();
        GgufGpuGuard guard = new GgufGpuGuard(map(m));
        assertTrue(guard.allowed(7));
        guard.trying();
        guard.fine();
        assertTrue("cargó y respondió: sigue con la gráfica", new GgufGpuGuard(map(m)).allowed(7));
    }

    @Test public void aCrashWhileTryingFallsBackToTheCpuUntilTheNextInstall() {
        Map<String, Integer> m = new HashMap<>();
        new GgufGpuGuard(map(m)).allowed(7);
        new GgufGpuGuard(map(m)).trying(); // y Android cierra la app aquí
        GgufGpuGuard next = new GgufGpuGuard(map(m));
        assertFalse(next.allowed(7));
        assertFalse("se recuerda", new GgufGpuGuard(map(m)).allowed(7));
        assertTrue("una versión nueva vuelve a probar", new GgufGpuGuard(map(m)).allowed(8));
    }

    @Test public void aNormalErrorDisablesTheGpu() {
        Map<String, Integer> m = new HashMap<>();
        GgufGpuGuard guard = new GgufGpuGuard(map(m));
        assertTrue(guard.allowed(3));
        guard.trying();
        guard.disable();
        assertTrue(guard.disabled());
        assertFalse(new GgufGpuGuard(map(m)).allowed(3));
    }

    @Test public void unknownInstallStampNeverResetsALearnedFailure() {
        Map<String, Integer> m = new HashMap<>();
        GgufGpuGuard guard = new GgufGpuGuard(map(m));
        guard.allowed(5);
        guard.disable();
        assertFalse(new GgufGpuGuard(map(m)).allowed(0));
    }
}
