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
}
