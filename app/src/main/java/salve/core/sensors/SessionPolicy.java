package salve.core.sensors;

/** A temporary observation window, measured with a monotonic clock. */
public final class SessionPolicy {
    public static final long MAX_DURATION_MS = 60_000L;
    public static final long READING_TTL_MS = 5_000L;

    private long startedAt = -1L;
    private long expiresAt = -1L;

    /** Returns false without extending an already active observation window. */
    public boolean start(long now, long durationMs) {
        if (now < 0L || durationMs <= 0L || durationMs > MAX_DURATION_MS
                || now > Long.MAX_VALUE - durationMs) {
            throw new IllegalArgumentException("Invalid temporary sensor session");
        }
        if (isActive(now)) return false;
        startedAt = now;
        expiresAt = now + durationMs;
        return true;
    }

    public boolean isActive(long now) {
        return startedAt >= 0L && now >= startedAt && now < expiresAt;
    }

    public long remainingMs(long now) {
        return isActive(now) ? expiresAt - now : 0L;
    }

    /** Discards queued events from an earlier session as well as stale observations. */
    public boolean acceptsSample(long sampledAt, long now) {
        return isActive(now) && sampledAt >= startedAt && isFresh(sampledAt, now);
    }

    public void stop() {
        startedAt = -1L;
        expiresAt = -1L;
    }

    public static boolean isFresh(long sampledAt, long now) {
        return sampledAt >= 0L && now >= sampledAt && now - sampledAt <= READING_TTL_MS;
    }
}
