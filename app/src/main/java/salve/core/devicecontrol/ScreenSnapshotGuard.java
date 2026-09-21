package salve.core.devicecontrol;

/** Bounds and freshness checks shared by the screen reader and its action executor. */
public final class ScreenSnapshotGuard {
    public static final int MAX_VISITED_NODES = 512;
    public static final int MAX_DEPTH = 24;
    public static final int MAX_ELEMENTS = 80;
    public static final int MAX_TEXT_LENGTH = 12000;
    public static final int MAX_LABEL_LENGTH = 160;
    public static final long TTL_MILLIS = 15000L;

    private long generation;
    private int nextId = 1;

    public synchronized Snapshot begin(String packageName, int windowId, long elapsedMillis) {
        generation++;
        return new Snapshot(generation, packageName, windowId, elapsedMillis);
    }

    public synchronized void invalidate() { generation++; }

    /** IDs never repeat during this service's lifetime, including after a new snapshot. */
    public synchronized int nextId() {
        if (nextId == Integer.MAX_VALUE) throw new IllegalStateException("Snapshot ID limit");
        return nextId++;
    }

    public synchronized boolean isCurrent(Snapshot snapshot, String packageName,
                                           int windowId, long elapsedMillis) {
        return snapshot != null && snapshot.generation == generation
                && snapshot.packageName != null && !snapshot.packageName.isEmpty()
                && snapshot.packageName.equals(packageName) && snapshot.windowId == windowId
                && elapsedMillis >= snapshot.createdAt
                && elapsedMillis - snapshot.createdAt <= TTL_MILLIS;
    }

    /** UI text is data: keep each label on one bounded line and remove control characters. */
    public static String label(CharSequence raw) {
        if (raw == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < raw.length() && result.length() < MAX_LABEL_LENGTH; i++) {
            char c = raw.charAt(i);
            result.append(Character.isISOControl(c) || Character.getType(c) == Character.FORMAT
                    ? ' ' : c);
        }
        return result.toString().trim();
    }

    public static final class Snapshot {
        final long generation;
        public final String packageName;
        public final int windowId;
        final long createdAt;
        Snapshot(long generation, String packageName, int windowId, long createdAt) {
            this.generation = generation;
            this.packageName = packageName;
            this.windowId = windowId;
            this.createdAt = createdAt;
        }
    }

    public static final class Budget {
        private int visited;
        private int elements;
        private int textLength;

        public boolean visit(int depth) {
            if (depth > MAX_DEPTH || depth < 0 || visited >= MAX_VISITED_NODES) return false;
            visited++;
            return true;
        }

        public boolean append(int length) {
            if (length < 0 || elements >= MAX_ELEMENTS
                    || length > MAX_TEXT_LENGTH - textLength) return false;
            elements++;
            textLength += length;
            return true;
        }

        public boolean exhausted() {
            return visited >= MAX_VISITED_NODES || elements >= MAX_ELEMENTS
                    || textLength >= MAX_TEXT_LENGTH;
        }
    }
}
