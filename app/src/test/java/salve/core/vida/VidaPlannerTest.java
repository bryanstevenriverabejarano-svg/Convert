package salve.core.vida;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class VidaPlannerTest {
    private final LocalDate wed = LocalDate.of(2026, 11, 18);

    @Test public void seedsOnceAndNeverOverwrites() {
        VidaLog log = VidaPlanner.seedDefaults(new VidaLog());
        assertFalse(log.habits.isEmpty());
        assertFalse(log.goals.isEmpty());
        VidaLog again = VidaPlanner.seedDefaults(log);
        assertSame("sin cambios devuelve el mismo registro", log, again);
        VidaLog custom = new VidaLog().withHabits(java.util.Collections.singletonList(new VidaLog.Habit("x", "Leer", 3)));
        assertEquals(1, VidaPlanner.seedDefaults(custom).habits.size());
    }

    @Test public void habitsToggleCountAndStreak() {
        VidaLog log = VidaPlanner.seedDefaults(new VidaLog());
        log = VidaPlanner.toggle(log, "historia", wed);
        assertTrue(VidaPlanner.checked(log, "historia", wed));
        assertEquals(1, VidaPlanner.doneThisWeek(log, "historia", wed));
        log = VidaPlanner.toggle(log, "historia", wed.minusWeeks(1));
        log = VidaPlanner.toggle(log, "historia", wed.minusWeeks(2));
        VidaLog.Habit h = null; for (VidaLog.Habit x : log.habits) if (x.id.equals("historia")) h = x;
        assertEquals(3, VidaPlanner.weekStreak(log, h, wed));
        log = VidaPlanner.toggle(log, "historia", wed);
        assertFalse(VidaPlanner.checked(log, "historia", wed));
        assertEquals("la semana actual sin cumplir no rompe la racha", 2, VidaPlanner.weekStreak(log, h, wed));
    }

    @Test public void gymAndSleepMarkHabitsAutomatically() {
        VidaLog log = VidaPlanner.seedDefaults(new VidaLog());
        List<VidaLog.GymSet> sets = new ArrayList<>();
        sets.add(new VidaLog.GymSet("2026-11-17", "Push", "Press", 0, "normal", 50.0, 10, null, null, "salve", null));
        log = VidaPlanner.syncGymHabit(log.withSets(sets));
        assertTrue(VidaPlanner.checked(log, "gym", wed.minusDays(1)));
        assertSame(log, VidaPlanner.syncGymHabit(log));
        log = VidaPlanner.logSleep(log, wed, "23:30", "07:00");
        assertTrue(VidaPlanner.checked(log, "sueno", wed));
        log = VidaPlanner.logSleep(log, wed.minusDays(1), "01:30", "06:00");
        assertFalse(VidaPlanner.checked(log, "sueno", wed.minusDays(1)));
        assertEquals((450 + 270) / 2, VidaPlanner.averageMinutes(VidaPlanner.lastNights(log, 7)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void absurdSleepRejected() { VidaPlanner.logSleep(new VidaLog(), LocalDate.of(2026, 1, 1), "07:00", "07:10"); }

    @Test public void mealPlanSetCopyAndShopping() {
        VidaLog log = VidaPlanner.setPlanned(new VidaLog(), 1, "comida", "Lentejas con arroz");
        log = VidaPlanner.setPlanned(log, 1, "cena", "Tortilla");
        log = VidaPlanner.copyDay(log, 1, 2);
        assertEquals("Tortilla", VidaPlanner.planned(log, 2, "cena"));
        log = VidaPlanner.setPlanned(log, 2, "cena", "");
        assertNull(VidaPlanner.planned(log, 2, "cena"));
        assertEquals(2, VidaPlanner.shoppingList(log).size());
        log = VidaPlanner.logMeal(log, wed, "14:00", "comida", "Lentejas");
        assertEquals(1, VidaPlanner.mealsOn(log, wed).size());
    }

    @Test public void goalsDoneSortAndMergeKeepsDone() {
        VidaLog log = VidaPlanner.addGoal(new VidaLog(), "Publicar vídeo", "2026-11-30", "YouTube");
        String id = log.goals.get(0).id;
        VidaLog done = VidaPlanner.setGoalDone(log, id, true);
        assertTrue(done.goals.get(0).done);
        VidaLog merged = VidaService.merge(log, done);
        assertTrue("hecho en otro móvil sigue hecho", merged.goals.get(0).done);
    }

    @Test public void savedRoutinesEditDuplicateStartAndPersist() {
        List<VidaLog.GymSet> sets = new ArrayList<>();
        sets.add(new VidaLog.GymSet("2026-11-12", "Push", "Press de banca", 0, "normal", 50.0, 10, null, null, "hevy", null));
        sets.add(new VidaLog.GymSet("2026-11-12", "Push", "Press de banca", 1, "normal", 50.0, 9, null, null, "hevy", null));
        VidaLog log = new VidaLog().withSets(sets);
        VidaLog.Routine r = VidaPlanner.fromHistory(log, GymTracker.routines(log).get(0));
        assertEquals(2, r.items.get(0).sets);
        log = VidaPlanner.saveRoutine(log, r);
        log = VidaPlanner.duplicateRoutine(log, r.id);
        assertEquals(2, log.routines.size());
        List<VidaLog.Routine.Item> items = new ArrayList<>(r.items);
        items.set(0, new VidaLog.Routine.Item("Press de banca", 4, 120));
        log = VidaPlanner.saveRoutine(log, new VidaLog.Routine(r.id, "Push A", items));
        VidaLog back = VidaLog.parse(log.serialize());
        VidaLog.Routine saved = null; for (VidaLog.Routine x : back.routines) if (x.id.equals(r.id)) saved = x;
        assertEquals("Push A", saved.title);
        assertEquals(4, saved.items.get(0).sets);
        assertEquals(120, saved.items.get(0).restSeconds);
        List<GymTracker.LiveExercise> w = VidaPlanner.start(back, saved);
        assertEquals(4, w.get(0).sets.size());
        assertEquals(120, w.get(0).restSeconds);
        assertEquals(Integer.valueOf(9), w.get(0).sets.get(1).reps);
        assertEquals("series extra copian la última", Integer.valueOf(9), w.get(0).sets.get(3).reps);
        assertEquals(1, VidaPlanner.removeRoutine(back, r.id).routines.size());
    }

    @Test public void fullLogRoundTrip() {
        VidaLog log = VidaPlanner.seedDefaults(new VidaLog());
        log = VidaPlanner.setPlanned(log, 3, "desayuno", "Avena\tcon plátano");
        log = VidaPlanner.toggle(log, "mates", wed);
        assertEquals(log.serialize(), VidaLog.parse(log.serialize()).serialize());
    }
}
