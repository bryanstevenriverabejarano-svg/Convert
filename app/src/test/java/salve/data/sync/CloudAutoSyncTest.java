package salve.data.sync;

import static org.junit.Assert.*;
import org.junit.Test;

public class CloudAutoSyncTest {
    private static final long MIN = 60_000L;

    @Test public void wakesUpWhenHistoryIsIncompleteButNotTooOften() {
        long now = 1_000_000_000L;
        assertTrue("sin recuperar todo: despierta", CloudAutoSync.shouldKick(now, now - 11 * MIN, now - 5 * MIN, false));
        assertFalse("hace menos de 10 min ya despertó", CloudAutoSync.shouldKick(now, now - 4 * MIN, 0, false));
        assertFalse("todo recuperado y corrió hace poco: basta el periódico", CloudAutoSync.shouldKick(now, 0, now - 20 * MIN, true));
        assertTrue("todo recuperado pero lleva más de una hora sin correr", CloudAutoSync.shouldKick(now, 0, now - 61 * MIN, true));
    }
}
