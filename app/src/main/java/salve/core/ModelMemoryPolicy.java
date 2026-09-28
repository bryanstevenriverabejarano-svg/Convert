package salve.core;

/** Admission estimates, not measured device guarantees. Uses bytes, never Java heap limits. */
public final class ModelMemoryPolicy {
    public static final long MIB = 1024L * 1024L;
    public static final long RESERVE = 768 * MIB;
    public static final long LARGE_WEIGHTS = 4920749472L;
    private ModelMemoryPolicy() { }
    public static final class Snapshot {
        public final long available, total, threshold;
        public final boolean low;
        public Snapshot(long available, long total, long threshold, boolean low) {
            this.available = available; this.total = total; this.threshold = threshold; this.low = low;
        }
        public boolean known() { return total > 0 && available > 0 && available <= total; }
    }
    public static long required(String id, boolean loaded, long threshold) {
        long weights = LocalModelPolicy.PRIMARY.equals(id) ? LARGE_WEIGHTS
                : LocalModelPolicy.LIGHT.equals(id) ? 2019382400L : 2588147712L;
        long overhead = LocalModelPolicy.PRIMARY.equals(id) ? 1024 * MIB : 512 * MIB;
        return (loaded ? 0 : weights) + overhead + Math.max(RESERVE, threshold);
    }
    public static boolean canLoad(String id, Snapshot ram, boolean loaded) {
        return ram.known() && !ram.low && ram.available >= required(id, loaded, ram.threshold);
    }
    /** During evaluation the KV/context is already allocated: do not charge its budget twice. */
    public static boolean underPressure(Snapshot ram) {
        return !ram.known() || ram.low || ram.available < Math.max(RESERVE, ram.threshold);
    }
    public static class PressureException extends IllegalStateException {
        public PressureException(String message) { super(message); }
    }
}
