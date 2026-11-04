package salve.core;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GgufNpuPolicyTest {
    @Test public void allowsQ40OnSnapdragon8Gen3Only() {
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/files/Gemma-4-E2B-Q4_0.gguf"));
        assertTrue(GgufNpuPolicy.eligible("SM8650-AC", "test-q4_0.GGUF"));
        assertFalse(GgufNpuPolicy.eligible("SM8750", "/files/Gemma-4-E2B-Q4_0.gguf"));
        assertFalse(GgufNpuPolicy.eligible("SM8650", "/files/Dolphin-Q4_K_M.gguf"));
        assertFalse(GgufNpuPolicy.eligible("SM8650", "/files/Gemma-Q4_K_P.gguf"));
        assertFalse(GgufNpuPolicy.eligible(null, "x-Q4_0.gguf"));
    }

    @Test public void npuGuardIsIndependentOfExistingGpuCrashGuard() {
        Map<String, Integer> prefs = new HashMap<>();
        LiteRtContextGuard.Store store = new LiteRtContextGuard.Store() {
            @Override public int get(String key) { return prefs.getOrDefault(key, 0); }
            @Override public void put(String key, int value) {
                if (value == 0) prefs.remove(key); else prefs.put(key, value);
            }
        };
        GgufGpuGuard npu = GgufGpuGuard.forNpu(store);
        GgufGpuGuard gpu = new GgufGpuGuard(store);
        assertTrue(npu.allowed(3));
        assertTrue(gpu.allowed(3));
        npu.trying();
        assertFalse(GgufGpuGuard.forNpu(store).allowed(3)); // process died on NPU
        assertTrue(new GgufGpuGuard(store).allowed(3)); // GPU remains available
        assertTrue(GgufGpuGuard.forNpu(store).allowed(4)); // new installation
    }
}
