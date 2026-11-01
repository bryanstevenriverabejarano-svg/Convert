package salve.work;

import static org.junit.Assert.*;
import static salve.work.AutoSleepPolicy.Action.*;

import org.junit.Test;

public class AutoSleepPolicyTest {
    private static final long HOUR = AutoSleepPolicy.IDLE_MS, NOW = 10 * HOUR;

    @Test public void sleepsOnlyWhenChargingAndUnusedForAnHour() {
        assertEquals(SLEEP, AutoSleepPolicy.decide(true, false, true, NOW, NOW - HOUR, false, false));
        assertEquals(NONE, AutoSleepPolicy.decide(true, false, true, NOW, NOW - HOUR + 60_000, false, false));
        assertEquals(NONE, AutoSleepPolicy.decide(true, false, false, NOW, NOW - 3 * HOUR, false, false)); // sin cargar
        assertEquals(NONE, AutoSleepPolicy.decide(true, true, true, NOW, NOW - 3 * HOUR, false, false));   // en uso
        assertEquals(NONE, AutoSleepPolicy.decide(false, false, true, NOW, NOW - 3 * HOUR, false, false)); // desactivado
        assertEquals(NONE, AutoSleepPolicy.decide(true, false, true, NOW, 0, false, false));              // sin dato
    }

    @Test public void wakesUpWhenUsedOrUnpluggedButNeverFromAManualSleep() {
        assertEquals(WAKE, AutoSleepPolicy.decide(true, true, true, NOW, NOW - HOUR, true, true));
        assertEquals(WAKE, AutoSleepPolicy.decide(true, false, false, NOW, NOW - HOUR, true, true));
        assertEquals(NONE, AutoSleepPolicy.decide(true, true, true, NOW, NOW - HOUR, true, false)); // la acostó Bryan
        assertEquals(NONE, AutoSleepPolicy.decide(true, false, true, NOW, NOW - 3 * HOUR, true, true)); // ya duerme
    }

    @Test public void useMovesTheClockAndUnknownStartsCountingNow() {
        assertEquals(NOW, AutoSleepPolicy.nextLastUse(true, NOW, NOW - HOUR));
        assertEquals(NOW - HOUR, AutoSleepPolicy.nextLastUse(false, NOW, NOW - HOUR));
        assertEquals(NOW, AutoSleepPolicy.nextLastUse(false, NOW, 0));
    }
}
