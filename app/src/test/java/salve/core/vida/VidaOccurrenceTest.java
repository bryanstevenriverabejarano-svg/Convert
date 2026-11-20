package salve.core.vida;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.junit.Assert.*;

public class VidaOccurrenceTest {
    private final LocalDate friday = LocalDate.of(2026, 11, 20);

    @Test public void oneFridayMovesButNextFridayStaysUnchanged() {
        VidaLog.Block recurring = new VidaLog.Block("Editar vídeo", 5, "08:00", 90);
        VidaLog log = new VidaLog().withBlocks(Arrays.asList(recurring));
        log = VidaPlanner.moveOccurrence(log, recurring, friday, friday, "15:00");
        assertEquals("15:00", VidaPlanner.blocksOn(log, friday).get(0).time);
        assertEquals("08:00", VidaPlanner.blocksOn(log, friday.plusWeeks(1)).get(0).time);
        assertEquals("08:00", log.blocks.get(0).time);
        assertEquals(1, VidaLog.parse(log.serialize()).dayOverrides.size());
    }

    @Test public void movingToSaturdayPreservesCompletion() {
        VidaLog.Block recurring = new VidaLog.Block("Editar vídeo", 5, "08:00", 90);
        VidaLog log = VidaPlanner.toggleBlock(new VidaLog().withBlocks(Arrays.asList(recurring)), recurring, friday);
        log = VidaPlanner.moveOccurrence(log, recurring, friday, friday.plusDays(1), "16:00");
        assertTrue(VidaPlanner.blocksOn(log, friday).isEmpty());
        VidaLog.Block saturday = VidaPlanner.blocksOn(log, friday.plusDays(1)).get(0);
        assertEquals("16:00", saturday.time);
        assertTrue(VidaPlanner.blockDone(log, saturday, friday.plusDays(1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void collisionOnDestinationIsRejected() {
        VidaLog.Block a = new VidaLog.Block("Vídeo", 5, "08:00", 60);
        VidaLog.Block b = new VidaLog.Block("Estudio", 6, "16:00", 60);
        VidaPlanner.moveOccurrence(new VidaLog().withBlocks(Arrays.asList(a, b)),
                a, friday, friday.plusDays(1), "16:30");
    }

    @Test public void overridesSurviveMerge() {
        VidaLog.Block a = new VidaLog.Block("Vídeo", 5, "08:00", 60);
        VidaLog base = new VidaLog().withBlocks(Arrays.asList(a));
        VidaLog moved = VidaPlanner.moveOccurrence(base, a, friday, friday, "10:00");
        assertEquals(1, VidaService.merge(base, moved).dayOverrides.size());
    }
}
