package salve.core.embeddings;

/** Admission headroom, not a claim about measured S24 RSS. Never bypass Android lowMemory. */
public final class EmbeddingMemoryPolicy {
    public static final long MIB = 1024L * 1024;
    public static final long LOAD_HEADROOM = 384 * MIB;
    public static final long ANDROID_RESERVE = 256 * MIB;
    private EmbeddingMemoryPolicy() { }
    public static boolean allows(long available, long total, long threshold, boolean low, boolean loaded) {
        return total > 0 && available > 0 && available <= total && !low
                && available >= Math.max(ANDROID_RESERVE, threshold) + (loaded ? 0 : LOAD_HEADROOM);
    }
}
