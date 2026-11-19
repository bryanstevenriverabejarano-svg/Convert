package salve.core.vida;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

/** Regression tests for sleep-aware, non-punitive daily planning. */
public class VidaRestAwarePlanTest {
    private final LocalDate day = LocalDate.of(2026, 11, 19);

    private VidaLog withSleep(int minutes, LocalDate date) {
        VidaLog log = VidaCoach.seedProjects(VidaPlanner.seedDefaults(new VidaLog()), day);
        java.util.List<VidaLog.SleepEntry> nights = new java.util.ArrayList<>();
        nights.add(new VidaLog.SleepEntry(date.toString(), "01:00", "04:00", minutes));
        return log.withSleep(nights);
    }

    @Test public void threeHoursPrioritizesRecoveryAndSuppressesGym() {
        List<VidaCoach.PlanItem> plan = VidaCoach.dayPlan(withSleep(180, day), day);
        assertFalse(plan.isEmpty());
        assertEquals("descanso", plan.get(0).kind);
        for (VidaCoach.PlanItem item : plan) {
            assertFalse("No gym on severely short sleep", item.kind.equals("gym"));
            if (item.kind.equals("proyecto")) assertTrue(item.minutes <= 20);
        }
    }

    @Test public void staleSleepDoesNotForceRecoveryMode() {
        List<VidaCoach.PlanItem> plan = VidaCoach.dayPlan(withSleep(180, day.minusDays(5)), day);
        assertFalse(plan.isEmpty());
        assertEquals("proyecto", plan.get(0).kind);
        assertEquals(90, plan.get(0).minutes);
    }

    @Test public void adequateSleepKeepsRegularProjectSession() {
        List<VidaCoach.PlanItem> plan = VidaCoach.dayPlan(withSleep(480, day), day);
        assertEquals("proyecto", plan.get(0).kind);
        assertEquals(90, plan.get(0).minutes);
    }
}
