package salve.core.devicecontrol;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfirmationContextGuardTest {
    private ConfirmationContextGuard.Context context(String pkg, int window, String fingerprint) {
        return new ConfirmationContextGuard.Context(pkg, window, fingerprint);
    }

    @Test public void unchangedDestinationCanBeConfirmed() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A-field-1"), 1000);
        assertTrue(guard.isCurrent(context("com.whatsapp", 2, "chat-A-field-1"), 2000));
    }

    @Test public void anotherChatInTheSameAppAndWindowIsRejected() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertFalse(guard.isCurrent(context("com.whatsapp", 2, "chat-B"), 2000));
    }

    @Test public void changingInputFocusRevokesTheReviewedContext() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A-focus-search"), 1000);
        assertFalse(guard.isCurrent(context("com.whatsapp", 2, "chat-A-focus-message"), 2000));
    }

    @Test public void sameContentInAnotherWindowOrAppIsRejected() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "same"), 1000);
        assertFalse(guard.isCurrent(context("com.whatsapp", 3, "same"), 2000));
        assertFalse(guard.isCurrent(context("com.other", 2, "same"), 2000));
    }

    @Test public void destinationEventRevokesEvenIfContentLaterLooksIdentical() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertTrue(guard.onTargetEvent("com.whatsapp", true));
        assertFalse(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 2000));
    }

    @Test public void overlayAndUnrelatedEventsDoNotCancelStableReview() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertFalse(guard.onTargetEvent("salve.app", true));
        assertFalse(guard.onTargetEvent(null, true));
        assertFalse(guard.onTargetEvent("com.whatsapp", false));
        assertTrue(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 2000));
    }

    @Test public void invalidatedApprovalCannotBeReused() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertTrue(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 2000));
        guard.invalidate();
        assertFalse(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 2001));
    }

    @Test public void expiresAndRejectsClockRollback() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertTrue(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 61000));
        assertFalse(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 61001));
        assertFalse(guard.isCurrent(context("com.whatsapp", 2, "chat-A"), 999));
    }

    @Test public void incompleteCaptureCannotConfirm() {
        ConfirmationContextGuard guard = new ConfirmationContextGuard();
        guard.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertFalse(guard.isCurrent(null, 2000));
    }

    @Test(expected = IllegalArgumentException.class) public void emptyFingerprintCannotCreateApproval() {
        context("com.whatsapp", 2, "");
    }
    private ConfirmationContextGuard.Window window(int id, ConfirmationContextGuard.WindowKind kind,
            String pkg, boolean focused, boolean active) {
        return new ConfirmationContextGuard.Window(id, kind, pkg, focused, active, false);
    }

    @Test public void touchingOwnOverlayStillSelectsTheInputFocusedApplication() {
        java.util.List<ConfirmationContextGuard.Window> windows = java.util.Arrays.asList(
                window(2, ConfirmationContextGuard.WindowKind.APPLICATION, "com.whatsapp", true, false),
                window(5, ConfirmationContextGuard.WindowKind.ACCESSIBILITY_OVERLAY, "salve.app", false, true));
        assertEquals(2, ConfirmationContextGuard.selectApplicationWindow(windows, "com.whatsapp", "salve.app"));
    }

    @Test public void keyboardIsNeverSelectedAsTheDestination() {
        java.util.List<ConfirmationContextGuard.Window> windows = java.util.Arrays.asList(
                window(2, ConfirmationContextGuard.WindowKind.APPLICATION, "com.whatsapp", true, false),
                window(5, ConfirmationContextGuard.WindowKind.INPUT_METHOD, "com.keyboard", false, true));
        assertEquals(2, ConfirmationContextGuard.selectApplicationWindow(windows, "com.whatsapp", "salve.app"));
    }

    @Test public void systemDialogCannotSelectAnApplicationBehindIt() {
        java.util.List<ConfirmationContextGuard.Window> windows = java.util.Arrays.asList(
                window(2, ConfirmationContextGuard.WindowKind.APPLICATION, "com.whatsapp", false, false),
                window(5, ConfirmationContextGuard.WindowKind.SYSTEM, "android", true, true));
        assertEquals(-1, ConfirmationContextGuard.selectApplicationWindow(windows, "com.whatsapp", "salve.app"));
    }

    @Test public void anActiveSystemPanelBlocksEvenIfInputFocusRemainsInTheApp() {
        java.util.List<ConfirmationContextGuard.Window> windows = java.util.Arrays.asList(
                window(2, ConfirmationContextGuard.WindowKind.APPLICATION, "com.whatsapp", true, false),
                window(5, ConfirmationContextGuard.WindowKind.SYSTEM, "android", false, true));
        assertEquals(-1, ConfirmationContextGuard.selectApplicationWindow(windows, "com.whatsapp", "salve.app"));
    }

    @Test public void anotherFocusedAppCannotAuthorizeABackgroundTarget() {
        java.util.List<ConfirmationContextGuard.Window> windows = java.util.Arrays.asList(
                window(2, ConfirmationContextGuard.WindowKind.APPLICATION, "com.whatsapp", false, false),
                window(5, ConfirmationContextGuard.WindowKind.APPLICATION, "com.other", true, true));
        assertEquals(-1, ConfirmationContextGuard.selectApplicationWindow(windows, "com.whatsapp", "salve.app"));
    }

    @Test public void anUnknownActiveOverlayCannotAuthorizeTheUnderlyingApp() {
        java.util.List<ConfirmationContextGuard.Window> windows = java.util.Arrays.asList(
                window(2, ConfirmationContextGuard.WindowKind.APPLICATION, "com.whatsapp", true, false),
                window(5, ConfirmationContextGuard.WindowKind.ACCESSIBILITY_OVERLAY, null, false, true));
        assertEquals(-1, ConfirmationContextGuard.selectApplicationWindow(windows, "com.whatsapp", "salve.app"));
    }

    @Test public void activeOnlyOverlayTransitionPreservesOnlyTheReviewedSnapshot() {
        ScreenSnapshotGuard snapshots = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = snapshots.begin("com.whatsapp", 2, 1000);
        ConfirmationContextGuard review = new ConfirmationContextGuard();
        ConfirmationContextGuard approved = new ConfirmationContextGuard();
        review.begin(context("com.whatsapp", 2, "chat-A"), 1000);
        assertTrue(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                context("com.whatsapp", 2, "chat-A"), review, approved, 2000));
        assertFalse(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                context("com.whatsapp", 2, "chat-B"), review, approved, 2000));
        assertFalse(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                context("com.whatsapp", 3, "chat-A"), review, approved, 2000));
        assertFalse(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                null, review, approved, 2000));
    }

    @Test public void activeOnlyEventCannotPreserveExpiredOrUnreviewedIds() {
        ScreenSnapshotGuard snapshots = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = snapshots.begin("com.whatsapp", 2, 1000);
        ConfirmationContextGuard review = new ConfirmationContextGuard();
        ConfirmationContextGuard approved = new ConfirmationContextGuard();
        ConfirmationContextGuard.Context current = context("com.whatsapp", 2, "chat-A");
        assertFalse(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                current, review, approved, 2000));
        approved.begin(current, 1000);
        assertTrue(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                current, review, approved, 2000));
        assertFalse(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                current, review, approved, 16001));
        snapshots.invalidate();
        assertFalse(ConfirmationContextGuard.preservesReviewedSnapshot(snapshots, snapshot,
                current, review, approved, 2000));
    }

}
