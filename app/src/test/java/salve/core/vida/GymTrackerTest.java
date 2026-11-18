package salve.core.vida;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GymTrackerTest {
    private static VidaLog.GymSet set(String date, String title, String ex, int i, String type, double kg, int reps) {
        return new VidaLog.GymSet(date, title, ex, i, type, kg, reps, null, null, "hevy", null);
    }

    private VidaLog history() {
        List<VidaLog.GymSet> s = new ArrayList<>();
        s.add(set("2026-10-28", "Brazos", "Curl predicador", 0, "calentamiento", 10, 10));
        s.add(set("2026-10-28", "Brazos", "Curl predicador", 1, "normal", 15, 8));
        s.add(set("2026-10-28", "Brazos", "Curl martillo", 0, "normal", 8, 9));
        s.add(set("2026-11-12", "Push", "Press de banca", 0, "normal", 50, 10));
        s.add(set("2026-11-12", "Push", "Press de banca", 1, "fallo", 50, 9));
        s.add(set("2026-10-01", "Push", "Press de banca", 0, "normal", 60, 7));
        return new VidaLog().withSets(s);
    }

    @Test public void routinesComeFromHistoryMostRecentFirst() {
        List<GymTracker.Routine> r = GymTracker.routines(history());
        assertEquals("Push", r.get(0).title);
        assertEquals(2, r.get(0).sessions);
        assertEquals("2026-11-12", r.get(0).lastDate);
        assertEquals("Brazos", r.get(1).title);
        assertEquals(2, r.get(1).exercises.size());
    }

    @Test public void routineKeepsExercisesFromFullerRecentSession() {
        List<VidaLog.GymSet> s = new ArrayList<>(history().sets);
        s.add(set("2026-11-15", "Brazos", "Curl predicador", 0, "normal", 15, 8)); // te fuiste antes
        GymTracker.Routine brazos = GymTracker.routines(new VidaLog().withSets(s)).get(0);
        assertEquals("Brazos", brazos.title);
        assertEquals(2, brazos.exercises.size());
    }

    @Test public void cardioSetsCarryDistanceAndTime() {
        List<VidaLog.GymSet> s = new ArrayList<>();
        s.add(new VidaLog.GymSet("2026-11-12", "Push", "Elíptica", 0, "normal", null, null, 0.18, 320, "hevy", null));
        GymTracker.LiveExercise ex = GymTracker.prefilled(new VidaLog().withSets(s), "Elíptica");
        assertTrue(ex.sets.get(0).hasData());
        ex.sets.get(0).done = true;
        List<GymTracker.LiveExercise> w = new ArrayList<>(); w.add(ex);
        VidaLog.GymSet saved = GymTracker.toSets("2026-11-18", "1", "Push", w).get(0);
        assertEquals(Integer.valueOf(320), saved.seconds);
    }

    @Test public void startCopiesLastSessionSetsAndTypes() {
        VidaLog log = history();
        GymTracker.Routine brazos = GymTracker.routines(log).get(1);
        List<GymTracker.LiveExercise> w = GymTracker.start(log, brazos);
        assertEquals("Curl predicador", w.get(0).name);
        assertEquals(2, w.get(0).sets.size());
        assertEquals("calentamiento", w.get(0).sets.get(0).type);
        assertEquals(15.0, w.get(0).sets.get(1).kg, 0.0);
        assertFalse(w.get(0).sets.get(1).done);
        assertEquals(1, GymTracker.prefilled(log, "Ejercicio nuevo").sets.size());
    }

    @Test public void previousColumnUsesLatestSession() {
        List<VidaLog.GymSet> prev = GymTracker.previous(history(), "Press de banca");
        assertEquals(2, prev.size());
        assertEquals(50.0, prev.get(0).kg, 0.0);
    }

    @Test public void liveRecordsIgnoreWarmupsAndFirstTimers() {
        VidaLog log = history();
        GymTracker.LiveSet heavy = new GymTracker.LiveSet(); heavy.kg = 62.5; heavy.reps = 5;
        assertTrue(GymTracker.liveRecord(log, "Press de banca", heavy).contains("peso"));
        GymTracker.LiveSet more = new GymTracker.LiveSet(); more.kg = 60.0; more.reps = 9;
        assertTrue(GymTracker.liveRecord(log, "Press de banca", more).contains("1RM"));
        GymTracker.LiveSet warm = new GymTracker.LiveSet(); warm.kg = 100.0; warm.reps = 5; warm.type = "calentamiento";
        assertNull(GymTracker.liveRecord(log, "Press de banca", warm));
        assertNull(GymTracker.liveRecord(log, "Nunca hecho", heavy));
    }

    @Test public void finishKeepsOnlyCheckedSetsAndSummarizes() {
        VidaLog log = history();
        GymTracker.LiveExercise ex = new GymTracker.LiveExercise("Press de banca");
        GymTracker.LiveSet a = new GymTracker.LiveSet(); a.kg = 62.5; a.reps = 6; a.done = true;
        GymTracker.LiveSet b = new GymTracker.LiveSet(); b.kg = 50.0; b.reps = 10; // sin ✓
        ex.sets.add(a); ex.sets.add(b);
        List<GymTracker.LiveExercise> w = new ArrayList<>(); w.add(ex);
        List<VidaLog.GymSet> done = GymTracker.toSets("2026-11-18", "123", "Push", w);
        assertEquals(1, done.size());
        assertEquals("salve", done.get(0).source);
        GymTracker.Summary s = GymTracker.summarize(log, done);
        assertEquals(1, s.sets);
        assertEquals(375.0, s.volume, 1e-9);
        assertTrue(s.records.get(0).contains("62,5"));
    }

    @Test public void draftRoundTrip() {
        GymTracker.LiveExercise ex = new GymTracker.LiveExercise("Curl\tpredicador");
        ex.restSeconds = 120;
        GymTracker.LiveSet a = new GymTracker.LiveSet(); a.kg = 17.5; a.reps = 7; a.done = true; a.type = "fallo";
        ex.sets.add(a); ex.sets.add(new GymTracker.LiveSet());
        List<GymTracker.LiveExercise> w = new ArrayList<>(); w.add(ex);
        GymTracker.Draft d = GymTracker.draftFromText(GymTracker.draftToText("Brazos", 42L, w));
        assertEquals("Brazos", d.title);
        assertEquals(42L, d.startMillis);
        assertEquals(120, d.exercises.get(0).restSeconds);
        assertEquals("fallo", d.exercises.get(0).sets.get(0).type);
        assertTrue(d.exercises.get(0).sets.get(0).done);
        assertNull(d.exercises.get(0).sets.get(1).kg);
        assertNull(GymTracker.draftFromText("basura"));
    }

    @Test public void serviceSavesWorkoutOnceAndReportsRecords() throws Exception {
        java.io.File f = new java.io.File(java.nio.file.Files.createTempDirectory("gym").toFile(), "vida.tsv");
        VidaService service = new VidaService(f);
        service.importHevy(HevyCsvImporterTest.CSV);
        GymTracker.LiveExercise ex = new GymTracker.LiveExercise("Curl predicador (máquina)");
        GymTracker.LiveSet a = new GymTracker.LiveSet(); a.kg = 20.0; a.reps = 6; a.done = true; ex.sets.add(a);
        List<GymTracker.LiveExercise> w = new ArrayList<>(); w.add(ex);
        List<VidaLog.GymSet> done = GymTracker.toSets("2026-11-18", "999", "Brazos", w);
        GymTracker.Summary s = service.saveWorkout(done);
        assertEquals(1, s.records.size());
        int before = service.read().sets.size();
        service.saveWorkout(done);
        assertEquals("guardar dos veces no duplica", before, service.read().sets.size());
    }
}
