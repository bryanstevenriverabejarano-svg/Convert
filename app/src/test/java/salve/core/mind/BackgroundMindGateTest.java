package salve.core.mind;

import static org.junit.Assert.*;
import org.junit.Test;

public class BackgroundMindGateTest {
    @Test public void admitsOnlyOneTaskAndReleasesOnClose() {
        BackgroundMindGate gate = new BackgroundMindGate();
        BackgroundMindGate.Lease first = gate.acquire(BackgroundMindGate.Task.CURIOSITY, false, false, false);
        assertNotNull(first);
        assertNull(gate.acquire(BackgroundMindGate.Task.DREAM, false, false, false));
        assertEquals(BackgroundMindGate.Task.CURIOSITY, gate.snapshot().active);
        first.complete();
        assertNull(gate.snapshot().active);
        assertEquals(Integer.valueOf(1), gate.snapshot().completions.get(BackgroundMindGate.Task.CURIOSITY));
        assertEquals(BackgroundMindGate.Outcome.COMPLETED_CYCLE,
                gate.snapshot().lastOutcomes.get(BackgroundMindGate.Task.CURIOSITY));
        try (BackgroundMindGate.Lease second = gate.acquire(BackgroundMindGate.Task.DREAM, false, false, false)) {
            assertNotNull(second);
        }
        assertEquals(BackgroundMindGate.Outcome.INTERRUPTED_OR_FAILED,
                gate.snapshot().lastOutcomes.get(BackgroundMindGate.Task.DREAM));
    }

    @Test public void yieldsToConversationPauseAndLoadingWithoutMarkingSuccess() {
        BackgroundMindGate gate = new BackgroundMindGate();
        assertNull(gate.acquire(BackgroundMindGate.Task.PLAY, true, false, false));
        assertNull(gate.acquire(BackgroundMindGate.Task.PLAY, false, true, false));
        assertNull(gate.acquire(BackgroundMindGate.Task.PLAY, false, false, true));
        assertTrue(gate.snapshot().completions.isEmpty());
        assertNull(gate.snapshot().active);
    }

    @Test public void duplicateCloseCannotReleaseAnotherLease() {
        BackgroundMindGate gate = new BackgroundMindGate();
        BackgroundMindGate.Lease first = gate.acquire(BackgroundMindGate.Task.PLAY, false, false, false);
        first.close();
        BackgroundMindGate.Lease second = gate.acquire(BackgroundMindGate.Task.REFLECTION_AND_GOALS, false, false, false);
        first.complete();
        assertEquals(BackgroundMindGate.Task.REFLECTION_AND_GOALS, gate.snapshot().active);
        second.complete();
        assertNull(gate.snapshot().active);
    }
}
