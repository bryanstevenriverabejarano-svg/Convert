package salve.core.vida;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class VidaCoachTest {
    private final LocalDate wed = LocalDate.of(2026, 11, 18);

    @Test public void focusSetToggleWinsAndStreak() {
        VidaLog log = VidaCoach.setFocus(new VidaLog(), wed, "Terminar escena 1");
        assertEquals("Terminar escena 1", VidaCoach.focusOn(log, wed).text);
        log = VidaCoach.toggleFocus(log, wed);
        log = VidaCoach.toggleFocus(VidaCoach.setFocus(log, wed.minusDays(1), "Mates"), wed.minusDays(1));
        assertEquals(2, VidaCoach.focusWinsThisWeek(log, wed));
        assertEquals(2, VidaCoach.focusStreak(log, wed));
        log = VidaCoach.setFocus(log, wed, "");
        assertNull(VidaCoach.focusOn(log, wed));
        assertEquals("hoy sin elegir no rompe la racha de ayer", 1, VidaCoach.focusStreak(log, wed));
    }

    @Test public void projectsLimitStepsAndStatus() {
        VidaLog log = VidaCoach.seedProjects(new VidaLog(), wed);
        assertEquals(1, log.projects.size());
        assertSame(log, VidaCoach.seedProjects(log, wed));
        log = VidaCoach.addProject(log, "Módulo Vida", Arrays.asList("Tabla", "Avisos"), wed);
        try { VidaCoach.addProject(log, "Gacha", new ArrayList<>(), wed); fail("límite de 2 activos"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("2")); }
        String id = log.projects.get(1).id;
        log = VidaCoach.toggleStep(log, id, 0);
        assertEquals("Avisos", VidaCoach.nextStep(log.projects.get(1)).text);
        assertArrayEquals(new int[]{1, 2}, VidaCoach.progress(log.projects.get(1)));
        log = VidaCoach.setStatus(log, id, VidaCoach.PAUSED);
        log = VidaCoach.addProject(log, "Gacha", new ArrayList<>(), wed);
        try { VidaCoach.setStatus(log, id, VidaCoach.ACTIVE); fail(); } catch (IllegalArgumentException expected) { }
        log = VidaCoach.addStep(log, id, "Gráficas");
        assertEquals(3, log.projects.get(1).steps.size());
        VidaLog back = VidaLog.parse(log.serialize());
        assertEquals(log.serialize(), back.serialize());
        assertTrue(VidaCoach.focusIdeas(back, wed).get(0).contains("Me dicen Bray"));
    }

    @Test public void reviewSaveAndPriority() {
        LocalDate mon = VidaCoach.monday(wed);
        VidaLog log = VidaCoach.saveReview(new VidaLog(), mon.minusWeeks(1), "4 días de gym", "dormí poco", "Publicar el vídeo");
        assertEquals("Publicar el vídeo", VidaCoach.currentPriority(log, wed));
        log = VidaCoach.saveReview(log, mon.minusWeeks(1), "a", "b", "Terminar escenas");
        assertEquals(1, log.reviews.size());
        assertTrue(VidaCoach.weekReport(VidaPlanner.seedDefaults(log), wed).contains("Gym: 0 días"));
    }

    @Test public void remindersMorningAndBlocks() {
        VidaLog log = VidaPlanner.addBlocks(new VidaLog(), "Gym", new HashSet<>(Arrays.asList(3)), "19:00", 75);
        log = VidaPlanner.addBlocks(log, "Desconectar pantallas", new HashSet<>(Arrays.asList(3)), "23:00", 30);
        LocalDateTime now = wed.atTime(7, 0);
        VidaCoach.Reminder r = VidaCoach.nextReminder(log, now, LocalTime.of(8, 0), 10);
        assertEquals(wed.atTime(8, 0), r.at);
        assertTrue(r.text.contains("19:00 Gym"));
        r = VidaCoach.nextReminder(log, wed.atTime(9, 0), LocalTime.of(8, 0), 10);
        assertEquals(wed.atTime(18, 50), r.at);
        r = VidaCoach.nextReminder(log, wed.atTime(19, 0), LocalTime.of(8, 0), 10);
        assertEquals("descanso no avisa; mañana siguiente", wed.plusDays(1).atTime(8, 0), r.at);
        assertNull(VidaCoach.nextReminder(new VidaLog(), now, null, 10));
    }

    @Test public void horarioKindsSuggestionAndCompliance() {
        assertEquals(VidaPlanner.Kind.GYM, VidaPlanner.kind("Gym"));
        assertEquals(VidaPlanner.Kind.ESTUDIO, VidaPlanner.kind("Matemáticas (1 vídeo)"));
        assertEquals(VidaPlanner.Kind.CREAR, VidaPlanner.kind("Canal: grabar y editar"));
        assertEquals(VidaPlanner.Kind.TRABAJO, VidaPlanner.kind("Trabajo (Zenitcorp)"));
        List<VidaLog.Block> week = VidaPlanner.suggestedWeek(9, 17, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)));
        int gym = 0; for (VidaLog.Block b : week) if (VidaPlanner.kind(b.title) == VidaPlanner.Kind.GYM) gym++;
        assertEquals(4, gym);
        for (VidaLog.Block a : week) for (VidaLog.Block b : week)
            if (a != b && a.day == b.day) assertFalse(a.title + "/" + b.title, VidaPlanner.overlaps(a, b));
        VidaLog log = VidaPlanner.addBlocks(new VidaLog(), "Clase de inglés", new HashSet<>(Arrays.asList(6)), "18:00", 60);
        log = VidaPlanner.addBlocks(log, "Algo que choca", new HashSet<>(Arrays.asList(1)), "18:00", 60);
        log = VidaPlanner.applySuggested(log, week);
        boolean keptEnglish = false, keptClash = false;
        for (VidaLog.Block b : log.blocks) { if (b.title.equals("Clase de inglés")) keptEnglish = true; if (b.title.equals("Algo que choca")) keptClash = true; }
        assertFalse("sábado 18:00 choca con el descanso total", keptEnglish);
        assertFalse(keptClash);
        VidaLog.Block monGym = null; for (VidaLog.Block b : log.blocks) if (b.day == 1 && b.title.equals("Gym")) monGym = b;
        log = VidaPlanner.toggleBlock(log, monGym, VidaCoach.monday(wed));
        int[] c = VidaPlanner.weekCompliance(log, wed);
        assertEquals(1, c[0]);
        assertTrue(c[1] > 5);
    }

    @Test public void gymProgressForCharts() {
        List<VidaLog.GymSet> s = new ArrayList<>();
        s.add(new VidaLog.GymSet("2026-10-01", "Push", "Press", 0, "calentamiento", 40.0, 10, null, null, "x", null));
        s.add(new VidaLog.GymSet("2026-10-01", "Push", "Press", 1, "normal", 55.0, 8, null, null, "x", null));
        s.add(new VidaLog.GymSet("2026-11-01", "Push", "Press", 0, "normal", 60.0, 7, null, null, "x", null));
        List<GymTracker.Point> p = GymTracker.progress(new VidaLog().withSets(s), "Press");
        assertEquals(2, p.size());
        assertEquals(55.0, p.get(0).maxKg, 0.0);
        assertEquals(8, p.get(0).bestReps);
        assertEquals(60 * (1 + 7 / 30.0), p.get(1).oneRm, 1e-9);
        assertEquals("Press", GymTracker.trackedExercises(new VidaLog().withSets(s)).get(0));
    }
}
