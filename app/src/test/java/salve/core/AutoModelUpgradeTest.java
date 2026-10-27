package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class AutoModelUpgradeTest {
    private static final long GIB = AutoModelUpgrade.GIB;

    @Test public void bigPhoneOnE2BUpgradesOnce() {
        long s24Ultra = (long) (10.82 * GIB);
        assertTrue(AutoModelUpgrade.shouldUpgrade(s24Ultra, LocalModelPolicy.FALLBACK, false, false));
        assertFalse(AutoModelUpgrade.shouldUpgrade(s24Ultra, LocalModelPolicy.FALLBACK, true, false));
    }

    @Test public void smallPhonesOtherModelsAndManualChoiceAreRespected() {
        assertFalse(AutoModelUpgrade.shouldUpgrade((long) (7.3 * GIB), LocalModelPolicy.FALLBACK, false, false));
        assertFalse(AutoModelUpgrade.shouldUpgrade(12 * GIB, LocalModelPolicy.GEMMA_E4B, false, false));
        assertFalse(AutoModelUpgrade.shouldUpgrade(12 * GIB, null, false, false));
        assertFalse(AutoModelUpgrade.shouldUpgrade(12 * GIB, LocalModelPolicy.FALLBACK, false, true));
    }
}
