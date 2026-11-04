package salve.core;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GgufNpuPolicyTest {
    @Test public void allowsHtpQuantizationsOnSnapdragon8Gen3Only() {
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/files/Gemma-4-E2B-Q4_0.gguf"));
        assertTrue(GgufNpuPolicy.eligible("SM8650-AC", "test-q4_0.GGUF"));
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/files/Dolphin-Q8_0.gguf"));
        // Los dos Gemma 4 sin censura que descarga Salve (con el sufijo del commit fijado).
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/m/" + LocalModelPolicy.UNCENSORED_FILE));
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/m/" + LocalModelPolicy.UNCENSORED_E2B_FILE));
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/files/Dolphin-Q6_K.gguf"));
        assertFalse(GgufNpuPolicy.eligible("SM8750", "/files/Gemma-4-E2B-Q4_0.gguf"));
        assertFalse("sin kernels HTP", GgufNpuPolicy.eligible("SM8650", "/files/Gemma-IQ3_M.gguf"));
        assertFalse(GgufNpuPolicy.eligible("SM8650", "/files/Gemma-Q5_K_M.gguf"));
        assertFalse("el mmproj no es el modelo", GgufNpuPolicy.eligible("SM8650", "/m/mmproj-Gemma-f16-45b6a33.gguf"));
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
