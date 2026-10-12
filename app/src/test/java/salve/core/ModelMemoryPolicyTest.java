package salve.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ModelMemoryPolicyTest {
    private ModelMemoryPolicy.Snapshot ram(long available, boolean low) {
        return new ModelMemoryPolicy.Snapshot(available, 12L * 1024 * ModelMemoryPolicy.MIB, 0, low);
    }
    @Test public void eightBRequiresAvailableRamNotJustAnExpensivePhone() {
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(8L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.LIGHT, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
    }
    @Test public void boundariesIncludeOverheadAndSystemReserve() {
        long need = ModelMemoryPolicy.required(LocalModelPolicy.PRIMARY, false, 0);
        assertTrue(need > ModelMemoryPolicy.LARGE_WEIGHTS);
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(need - 1, false), false));
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(need, false), false));
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(need, true), false));
    }
    @Test public void loadedWeightsAndAllocatedContextAreNotChargedTwice() {
        long twoGiB = 2L * 1024 * ModelMemoryPolicy.MIB;
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(twoGiB, false), true));
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(twoGiB, false), false));
        assertFalse(ModelMemoryPolicy.underPressure(ram(1024 * ModelMemoryPolicy.MIB, false)));
        assertTrue(ModelMemoryPolicy.underPressure(ram(512 * ModelMemoryPolicy.MIB, false)));
    }
    @Test public void unknownAndLowMemoryNeverAdmit8B() {
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, new ModelMemoryPolicy.Snapshot(0, 0, 0, false), false));
        assertTrue(ModelMemoryPolicy.underPressure(ram(9L * 1024 * ModelMemoryPolicy.MIB, true)));
        assertTrue(ModelMemoryPolicy.underPressure(new ModelMemoryPolicy.Snapshot(100, 50, 0, false)));
    }
    @Test public void androidThresholdOverridesTheMinimumReserve() {
        long threshold = 2L * 1024 * ModelMemoryPolicy.MIB;
        assertTrue(ModelMemoryPolicy.underPressure(new ModelMemoryPolicy.Snapshot(threshold - 1, threshold * 6, threshold, false)));
        assertEquals(ModelMemoryPolicy.LARGE_WEIGHTS + 1024 * ModelMemoryPolicy.MIB + threshold,
                ModelMemoryPolicy.required(LocalModelPolicy.PRIMARY, false, threshold));
    }
    @Test public void compactContextIsChosenOnlyInTheNarrowSupportedMemoryBand() {
        for (String id : new String[]{LocalModelPolicy.PRIMARY, LocalModelPolicy.LIGHT}) {
            long compact = ModelMemoryPolicy.required(id, false, 0, 2048);
            long standard = ModelMemoryPolicy.required(id, false, 0, 4096);
            assertTrue(compact < standard);
            assertEquals(0, ModelMemoryPolicy.selectContext(id, ram(compact - 1, false), false));
            assertEquals(2048, ModelMemoryPolicy.selectContext(id, ram(compact, false), false));
            assertEquals(2048, ModelMemoryPolicy.selectContext(id, ram(standard - 1, false), false));
            assertEquals(4096, ModelMemoryPolicy.selectContext(id, ram(standard, false), false));
            assertEquals(0, ModelMemoryPolicy.selectContext(id, ram(standard, true), false));
        }
    }
    @Test public void shrinkingContextDoesNotPretendEightBWeightsFitFourGiB() {
        assertEquals(0, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertEquals(4096, ModelMemoryPolicy.selectContext(LocalModelPolicy.LIGHT, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertEquals(2048, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, ram(1600 * ModelMemoryPolicy.MIB, false), true));
    }
    @Test public void preparationDoesNotChargeAResidentEngineTwiceButActivationStillChecksRam() {
        ModelMemoryPolicy.Snapshot current = ram(3L * 1024 * ModelMemoryPolicy.MIB, false);
        assertTrue(ModelMemoryPolicy.canPrepare(LocalModelPolicy.PRIMARY, current, true));
        assertFalse(ModelMemoryPolicy.canPrepare(LocalModelPolicy.PRIMARY, current, false));
        assertEquals(0, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, current, false));
        assertEquals(ModelMemoryPolicy.required(LocalModelPolicy.FALLBACK, false, 0, 4096),
                ModelMemoryPolicy.required(LocalModelPolicy.FALLBACK, false, 0, 2048));
    }
}
