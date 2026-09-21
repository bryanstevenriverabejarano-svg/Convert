package salve.core.sensors;

import org.junit.Test;

import static org.junit.Assert.*;

public class SessionPolicyTest {
    @Test public void startsInactiveAndExpiresAtDeadline() {
        SessionPolicy policy = new SessionPolicy();
        assertFalse(policy.isActive(100));
        assertTrue(policy.start(100, 30_000));
        assertTrue(policy.isActive(30_099));
        assertFalse(policy.isActive(30_100));
        assertEquals(0, policy.remainingMs(30_100));
    }

    @Test public void repeatedRequestsDoNotExtendAuthorization() {
        SessionPolicy policy = new SessionPolicy();
        policy.start(1_000, 30_000);
        assertFalse(policy.start(15_000, 60_000));
        assertEquals(16_000, policy.remainingMs(15_000));
        assertFalse(policy.isActive(31_000));
    }

    @Test public void cancellationIsImmediateAndIdempotent() {
        SessionPolicy policy = new SessionPolicy();
        policy.start(1_000, 60_000);
        policy.stop();
        policy.stop();
        assertFalse(policy.isActive(1_001));
        assertFalse(policy.acceptsSample(1_001, 1_002));
        assertEquals(0, policy.remainingMs(1_001));
    }

    @Test public void resumedSessionRejectsQueuedOldSamples() {
        SessionPolicy policy = new SessionPolicy();
        policy.start(1_000, 30_000);
        policy.stop();
        policy.start(2_000, 30_000);
        assertFalse(policy.acceptsSample(1_999, 2_001));
        assertTrue(policy.acceptsSample(2_000, 2_001));
    }

    @Test public void rejectsInvalidDurationsAndOverflow() {
        SessionPolicy policy = new SessionPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.start(0, 0));
        assertThrows(IllegalArgumentException.class, () -> policy.start(0, -1));
        assertThrows(IllegalArgumentException.class, () -> policy.start(0, 60_001));
        assertThrows(IllegalArgumentException.class, () -> policy.start(-1, 1_000));
        assertThrows(IllegalArgumentException.class, () -> policy.start(Long.MAX_VALUE, 1_000));
    }

    @Test public void readingDeadlineRejectsOldAndFutureSamples() {
        assertTrue(SessionPolicy.isFresh(1_000, 6_000));
        assertFalse(SessionPolicy.isFresh(1_000, 6_001));
        assertFalse(SessionPolicy.isFresh(1_001, 1_000));
        assertFalse(SessionPolicy.isFresh(-1, 1_000));
    }

    @Test public void clockBeforeStartAndExpiredSessionsAcceptNoData() {
        SessionPolicy policy = new SessionPolicy();
        policy.start(1_000, 1_000);
        assertFalse(policy.isActive(999));
        assertFalse(policy.acceptsSample(1_000, 999));
        assertFalse(policy.acceptsSample(1_999, 2_000));
    }

    @Test public void maximumSessionHasOnlySixtySeconds() {
        SessionPolicy policy = new SessionPolicy();
        policy.start(0, SessionPolicy.MAX_DURATION_MS);
        assertEquals(60_000, policy.remainingMs(0));
        assertTrue(policy.isActive(59_999));
        assertFalse(policy.isActive(60_000));
    }
}
