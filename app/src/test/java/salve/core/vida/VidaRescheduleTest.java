package salve.core.vida;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.junit.Assert.*;

public class VidaRescheduleTest {
    private final LocalDate monday = LocalDate.of(2026, 11, 16);

    @Test public void moveToAnotherDayPreservesCompletionAndDuration() {
        VidaLog.Block original = new VidaLog.Block("Editar vídeo", 1, "08:00", 90);
        VidaLog log = new VidaLog().withBlocks(Arrays.asList(original));
        log = VidaPlanner.toggleBlock(log, original, monday);
        VidaLog moved = VidaPlanner.rescheduleBlock(log, original, 5, "15:00", monday);
        assertEquals(1, moved.blocks.size());
        assertEquals(90, moved.blocks.get(0).minutes);
        assertEquals(5, moved.blocks.get(0).day);
        assertTrue(VidaPlanner.blockDone(moved, moved.blocks.get(0), monday.plusDays(4)));
        assertFalse(VidaPlanner.blockDone(moved, original, monday));
    }

    @Test(expected = IllegalArgumentException.class)
    public void overlappingMoveIsRejected() {
        VidaLog.Block a = new VidaLog.Block("Vídeo", 1, "08:00", 60);
        VidaLog.Block b = new VidaLog.Block("Mates", 5, "15:30", 60);
        VidaPlanner.rescheduleBlock(new VidaLog().withBlocks(Arrays.asList(a, b)), a, 5, "15:00", monday);
    }

    @Test public void sameTimeRescheduleKeepsOneBlock() {
        VidaLog.Block a = new VidaLog.Block("Vídeo", 1, "08:00", 60);
        VidaLog log = VidaPlanner.rescheduleBlock(new VidaLog().withBlocks(Arrays.asList(a)), a, 1, "08:00", monday);
        assertEquals(1, log.blocks.size());
    }
}
