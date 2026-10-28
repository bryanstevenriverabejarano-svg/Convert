package salve.core.evaluation;

import org.junit.Test;
import static org.junit.Assert.*;

/** Budget survives time changes and never retries a failed holdout immediately. */
public class NightTransferTrialTest {
    @Test public void weeklyBudgetIsStrictAndNeverRunsInThePast() {
        long now = 1_850_000_000_000L;
        assertTrue(NightTransferTrial.due(0, now));
        assertFalse(NightTransferTrial.due(now, now));
        assertFalse(NightTransferTrial.due(now - NightTransferTrial.COOLDOWN_MS + 1, now));
        assertTrue(NightTransferTrial.due(now - NightTransferTrial.COOLDOWN_MS, now));
        assertFalse(NightTransferTrial.due(now + 1000, now));
        assertFalse(NightTransferTrial.due(-1, now));
        assertFalse(NightTransferTrial.due(0, 0));
    }
}
