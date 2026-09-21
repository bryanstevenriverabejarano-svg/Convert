package salve.core.devicecontrol;

import java.util.List;

/** An in-memory confirmation applies to one stable screen, not to every screen of an app. */
public final class ConfirmationContextGuard {
    public static final long TTL_MILLIS = 60000L;
    private Context context;
    private long createdAt;

    public synchronized void begin(Context context, long elapsedMillis) {
        this.context = context;
        this.createdAt = elapsedMillis;
    }

    public synchronized void invalidate() { context = null; }
    public synchronized boolean hasContext() { return context != null; }

    public synchronized boolean onTargetEvent(String eventPackage, boolean relevant) {
        if (context == null || !relevant || !context.packageName.equals(eventPackage)) return false;
        context = null;
        return true;
    }

    public synchronized boolean isCurrent(Context current, long elapsedMillis) {
        return context != null && current != null && elapsedMillis >= createdAt
                && elapsedMillis - createdAt <= TTL_MILLIS
                && context.packageName.equals(current.packageName)
                && context.windowId == current.windowId
                && context.fingerprint.equals(current.fingerprint);
    }

    /** Used only for an ACTIVE-only window event, after recapturing the input-focused app. */
    public static boolean preservesReviewedSnapshot(ScreenSnapshotGuard snapshots,
            ScreenSnapshotGuard.Snapshot snapshot, Context current, ConfirmationContextGuard review,
            ConfirmationContextGuard approved, long elapsedMillis) {
        return current != null
                && snapshots.isCurrent(snapshot, current.packageName, current.windowId, elapsedMillis)
                && (review.isCurrent(current, elapsedMillis) || approved.isCurrent(current, elapsedMillis));
    }

    /** No screen text or recipient names are retained, only a digest of the bounded context. */
    public static final class Context {
        public final String packageName;
        public final int windowId;
        private final String fingerprint;

        public Context(String packageName, int windowId, String fingerprint) {
            if (packageName == null || packageName.isEmpty() || fingerprint == null || fingerprint.isEmpty())
                throw new IllegalArgumentException("A complete screen context is required");
            this.packageName = packageName;
            this.windowId = windowId;
            this.fingerprint = fingerprint;
        }
    }

    public enum WindowKind { APPLICATION, INPUT_METHOD, ACCESSIBILITY_OVERLAY, SYSTEM }

    public static final class Window {
        public final int id;
        public final WindowKind kind;
        public final String packageName;
        public final boolean focused, active, pictureInPicture;
        public Window(int id, WindowKind kind, String packageName, boolean focused,
                      boolean active, boolean pictureInPicture) {
            this.id = id; this.kind = kind; this.packageName = packageName;
            this.focused = focused; this.active = active; this.pictureInPicture = pictureInPicture;
        }
    }

    /** The touched confirmation overlay is not the destination; the input-focused app is. */
    public static int selectApplicationWindow(List<Window> windows, String expectedPackage, String ownPackage) {
        if (windows == null || windows.size() > 32) return -1;
        Window selected = null;
        for (Window window : windows) {
            if (window.focused) {
                if (window.kind != WindowKind.APPLICATION || window.pictureInPicture || selected != null
                        || window.packageName == null || window.packageName.isEmpty()) return -1;
                selected = window;
            }
            if (window.active && (window.kind == WindowKind.SYSTEM
                    || (window.kind == WindowKind.ACCESSIBILITY_OVERLAY
                        && (ownPackage == null || !ownPackage.equals(window.packageName))))) return -1;
        }
        if (selected == null || (expectedPackage != null && !expectedPackage.equals(selected.packageName))) return -1;
        for (Window window : windows) {
            if (window.active && window.kind == WindowKind.APPLICATION && window.id != selected.id) return -1;
        }
        return selected.id;
    }
}
