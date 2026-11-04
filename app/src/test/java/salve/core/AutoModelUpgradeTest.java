package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class AutoModelUpgradeTest {
    private static final long GIB = AutoModelUpgrade.GIB;

    @Test public void uncensoredGemmaIsFetchedOnceOnBigPhones() {
        long s24Ultra = (long) (10.82 * GIB);
        assertTrue(AutoModelUpgrade.shouldGetUncensored(s24Ultra, LocalModelPolicy.FALLBACK, false));
        assertTrue(AutoModelUpgrade.shouldGetUncensored(s24Ultra, LocalModelPolicy.GEMMA_E4B, false));
        assertFalse("una sola vez", AutoModelUpgrade.shouldGetUncensored(s24Ultra, LocalModelPolicy.FALLBACK, true));
        assertFalse("ya activo", AutoModelUpgrade.shouldGetUncensored(s24Ultra, LocalModelPolicy.UNCENSORED, false));
        assertFalse("el pequeño sin censura también cuenta", AutoModelUpgrade.shouldGetUncensored(s24Ultra, LocalModelPolicy.UNCENSORED_E2B, false));
        assertFalse("móvil de 8 GB", AutoModelUpgrade.shouldGetUncensored((long) (7.3 * GIB), LocalModelPolicy.FALLBACK, false));
    }
}
