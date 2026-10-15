package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

/** Caso real: Galaxy con 10,82 GiB, 3,09 GiB libres y Gemma 4 E2B ya cargado para texto (15/10/2026). */
public class LiteRtMemoryPolicyTest {
    private static final long GIB = 1024L * ModelMemoryPolicy.MIB;
    private final ModelMemoryPolicy.Snapshot ram = new ModelMemoryPolicy.Snapshot(
            (long) (3.09 * GIB), (long) (10.82 * GIB), 768 * ModelMemoryPolicy.MIB, false);

    @Test public void residentWeightsAreNotChargedTwiceWhenAddingVision() {
        String id = LocalModelPolicy.FALLBACK;
        assertTrue("Antes exigía 3,66 GiB contando otra vez los pesos ya cargados",
                ModelMemoryPolicy.requiredLiteRt(id, false, ram.threshold, true, 4096) > ram.available);
        assertNotEquals(0, ModelMemoryPolicy.selectLiteRtContext(id, ram, true, true));
    }

    @Test public void longContextOnlyWhenItFits() {
        String id = LocalModelPolicy.FALLBACK;
        ModelMemoryPolicy.Snapshot plenty = new ModelMemoryPolicy.Snapshot(6L * GIB, 12L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals(ModelMemoryPolicy.LITERT_LONG, ModelMemoryPolicy.selectLiteRtContext(id, plenty, false, true));
        ModelMemoryPolicy.Snapshot tight = new ModelMemoryPolicy.Snapshot((long) (3.8 * GIB), 12L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals(ModelMemoryPolicy.LITERT_STANDARD, ModelMemoryPolicy.selectLiteRtContext(id, tight, false, false));
        ModelMemoryPolicy.Snapshot low = new ModelMemoryPolicy.Snapshot(2L * GIB, 12L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals(0, ModelMemoryPolicy.selectLiteRtContext(id, low, false, false));
    }
}
