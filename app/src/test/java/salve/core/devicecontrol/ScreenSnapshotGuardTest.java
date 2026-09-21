package salve.core.devicecontrol;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenSnapshotGuardTest {
    @Test public void acceptsSameWindowWithinTtl() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = guard.begin("com.whatsapp", 8, 1000);
        assertTrue(guard.isCurrent(snapshot, "com.whatsapp", 8, 1500));
    }

    @Test public void rejectsOtherPackageEvenIfWindowIdMatches() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = guard.begin("com.whatsapp", 8, 1000);
        assertFalse(guard.isCurrent(snapshot, "com.bank", 8, 1500));
    }

    @Test public void rejectsOtherWindowInSameApp() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = guard.begin("com.whatsapp", 8, 1000);
        assertFalse(guard.isCurrent(snapshot, "com.whatsapp", 9, 1500));
    }

    @Test public void invalidationRejectsOldScreenWhileAppIsUnchanged() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = guard.begin("com.whatsapp", 8, 1000);
        guard.invalidate();
        assertFalse(guard.isCurrent(snapshot, "com.whatsapp", 8, 1500));
    }

    @Test public void newSnapshotRejectsOldIdsAndDoesNotReuseNumbers() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot first = guard.begin("com.whatsapp", 8, 1000);
        int firstId = guard.nextId();
        ScreenSnapshotGuard.Snapshot second = guard.begin("com.whatsapp", 8, 1100);
        int secondId = guard.nextId();
        assertNotEquals(firstId, secondId);
        assertFalse(guard.isCurrent(first, "com.whatsapp", 8, 1500));
        assertTrue(guard.isCurrent(second, "com.whatsapp", 8, 1500));
    }

    @Test public void expiresAtTtlAndRejectsClockRollback() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        ScreenSnapshotGuard.Snapshot snapshot = guard.begin("com.whatsapp", 8, 1000);
        assertTrue(guard.isCurrent(snapshot, "com.whatsapp", 8, 16000));
        assertFalse(guard.isCurrent(snapshot, "com.whatsapp", 8, 16001));
        assertFalse(guard.isCurrent(snapshot, "com.whatsapp", 8, 999));
    }

    @Test public void neverAcceptsAnonymousOrMissingSnapshot() {
        ScreenSnapshotGuard guard = new ScreenSnapshotGuard();
        assertFalse(guard.isCurrent(null, "com.whatsapp", 8, 1000));
        assertFalse(guard.isCurrent(guard.begin("", 8, 1000), "", 8, 1000));
        assertFalse(guard.isCurrent(guard.begin(null, 8, 1000), null, 8, 1000));
    }

    @Test public void limitsTraversalWidthAndDepth() {
        ScreenSnapshotGuard.Budget budget = new ScreenSnapshotGuard.Budget();
        assertFalse(budget.visit(ScreenSnapshotGuard.MAX_DEPTH + 1));
        assertFalse(budget.visit(-1));
        for (int i = 0; i < ScreenSnapshotGuard.MAX_VISITED_NODES; i++) assertTrue(budget.visit(1));
        assertFalse(budget.visit(1));
        assertTrue(budget.exhausted());
    }

    @Test public void limitsElementsEvenWithEmptyLabels() {
        ScreenSnapshotGuard.Budget budget = new ScreenSnapshotGuard.Budget();
        for (int i = 0; i < ScreenSnapshotGuard.MAX_ELEMENTS; i++) assertTrue(budget.append(0));
        assertFalse(budget.append(0));
        assertTrue(budget.exhausted());
    }

    @Test public void textBudgetRejectsOversizeAndIntegerOverflow() {
        ScreenSnapshotGuard.Budget budget = new ScreenSnapshotGuard.Budget();
        assertFalse(budget.append(-1));
        assertFalse(budget.append(Integer.MAX_VALUE));
        assertTrue(budget.append(ScreenSnapshotGuard.MAX_TEXT_LENGTH - 1));
        assertFalse(budget.append(2));
        assertTrue(budget.append(1));
        assertTrue(budget.exhausted());
    }

    @Test public void labelsAreBoundedAndRemoveNewlinesAndDirectionControls() {
        assertEquals("Enviar mensaje", ScreenSnapshotGuard.label("Enviar\nmensaje"));
        assertEquals("a b", ScreenSnapshotGuard.label("a\u202eb"));
        assertEquals("", ScreenSnapshotGuard.label(null));
        assertEquals(ScreenSnapshotGuard.MAX_LABEL_LENGTH,
                ScreenSnapshotGuard.label("a".repeat(100000)).length());
    }
}
