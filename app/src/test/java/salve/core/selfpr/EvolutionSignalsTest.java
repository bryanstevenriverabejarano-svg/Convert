package salve.core.selfpr;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import salve.core.trace.ExecutionJournal;

/** Salve también evoluciona con su registro, su revisión de código, sus investigaciones y sus sueños. */
public class EvolutionSignalsTest {
    private static final long DAY = 24L * 60 * 60 * 1000, NOW = 1_800_000_000_000L, INSTALLED = NOW - 3 * DAY;
    private static final String TTS = "app/src/main/java/salve/services/NeuralTtsService.kt";

    private static ExecutionJournal.Task task(String type, String status, long started, String kind, String text) {
        ExecutionJournal.Task t = new ExecutionJournal.Task();
        t.id = type + started; t.type = type; t.status = status; t.started = started;
        if (text != null) {
            ExecutionJournal.Event e = new ExecutionJournal.Event();
            e.at = started; e.kind = kind; e.text = text;
            t.events.add(e);
        }
        return t;
    }

    private static String trace(int buffer) {
        return "java.lang.IllegalStateException: OUTPUT_BUFFER_REJECTED size=" + buffer + "\n"
                + "\tat android.media.AudioTrack.write(AudioTrack.java:10)\n"
                + "\tat salve.services.NeuralTtsService$speak$1.invoke(NeuralTtsService.kt:164)";
    }

    @Test public void aRepeatedErrorSinceInstallBecomesASignalWithTheFileFromTheTrace() {
        List<ExecutionJournal.Task> tasks = Arrays.asList(
                task("VOZ", "ERROR", NOW - DAY, "ERROR DE LA VOZ", trace(4096)),
                task("VOZ", "ERROR", NOW - DAY / 2, "ERROR DE LA VOZ", trace(8192)),
                task("VOZ", "TERMINADA", NOW - DAY / 3, "OK", "habló"),
                // antes de instalar esta versión: no cuenta
                task("VOZ", "ERROR", INSTALLED - DAY, "ERROR DE LA VOZ", trace(1)),
                // un error suelto no es patrón
                task("Control móvil", "ERROR", NOW - DAY, "ERROR", "No se encontró el botón"));
        List<EvolutionCycle.Signal> found = EvolutionSignals.fromJournal(tasks, INSTALLED, goal -> null);
        assertEquals(1, found.size());
        EvolutionCycle.Signal s = found.get(0);
        assertEquals(EvolutionSignals.JOURNAL, s.source);
        assertEquals(2, s.count);
        assertEquals("el número del búfer no cambia la causa", TTS, s.path);
        assertEquals(EvolutionCycle.VERIFY_ERRORS, s.verify);
        assertTrue(s.problem.contains("OUTPUT_BUFFER_REJECTED"));
        assertEquals(2, s.evidence.size());
    }

    @Test public void withoutATraceTheFileIsSearchedInHerOwnCode() {
        List<ExecutionJournal.Task> tasks = Arrays.asList(
                task("Control móvil", "FALLO", NOW - DAY, "ERROR", "No se encontró el botón 3"),
                task("Control móvil", "FALLO", NOW - DAY / 2, "ERROR", "No se encontró el botón 7"));
        String expected = "app/src/main/java/salve/core/PhoneActionsDesk.java";
        assertEquals(expected, EvolutionSignals.fromJournal(tasks, INSTALLED, goal -> expected).get(0).path);
    }

    @Test public void anErrorFixIsJudgedByWhetherTheErrorComesBack() {
        JsonObject change = SelfLineage.entry(NOW - 2 * DAY, EvolutionCycle.ORIGIN_AUTONOMOUS, "voz", TTS, "abc",
                "https://github.com/x/y/issues/50", 3);
        change.addProperty("verificar", EvolutionCycle.VERIFY_ERRORS);
        String error = EvolutionSignals.errorOf(task("VOZ", "ERROR", 0, "ERROR DE LA VOZ", trace(1)));
        change.addProperty("firma", EvolutionSignals.signature("VOZ", error));
        long installed = NOW - DAY;
        List<ExecutionJournal.Task> clean = Arrays.asList(task("VOZ", "TERMINADA", NOW - 20_000, "OK", "habló"),
                task("VOZ", "TERMINADA", NOW - 10_000, "OK", "habló"));
        assertNull("aún no ha corrido bastante", EvolutionSignals.errorVerdict(change, clean, installed, NOW));
        List<ExecutionJournal.Task> more = new ArrayList<>(clean);
        more.add(task("VOZ", "TERMINADA", NOW - 5_000, "OK", "habló"));
        JsonObject good = EvolutionSignals.errorVerdict(change, more, installed, NOW);
        assertEquals("mejor", good.get("resultado").getAsString());
        assertTrue(SelfVerification.sentence(good).contains("no ha vuelto a aparecer"));
        List<ExecutionJournal.Task> bad = new ArrayList<>(more);
        for (int i = 0; i < 3; i++) bad.add(task("VOZ", "ERROR", NOW - 1_000 + i, "ERROR DE LA VOZ", trace(i)));
        assertEquals("peor", EvolutionSignals.errorVerdict(change, bad, installed, NOW).get("resultado").getAsString());
        JsonObject examChange = SelfLineage.entry(NOW, "examen", "x", TTS, "abc", "u", 3);
        assertNull("un cambio del examen se verifica con el examen", EvolutionSignals.errorVerdict(examChange, bad, installed, NOW));
    }

    @Test public void herNightlyCodeReviewNotesBecomeIdeas() {
        String[] parts = EvolutionSignals.reviewParts("QUÉ HACE: lee la batería.\nPOSIBLES PROBLEMAS: no comprueba null en el sensor.\n"
                + "MEJORA: devolver -1 si no hay sensor.");
        assertEquals("no comprueba null en el sensor.", parts[0]);
        assertEquals("devolver -1 si no hay sensor.", parts[1]);
        assertEquals("", EvolutionSignals.reviewParts("QUÉ HACE: x\nPOSIBLES PROBLEMAS: «ninguno visto».\nMEJORA: ninguna")[0]);
        List<EvolutionCycle.Signal> found = EvolutionSignals.fromReview(Arrays.asList(
                new String[]{"salve/core/sensors/DeviceSenses.java", "POSIBLES PROBLEMAS: no comprueba null.\nMEJORA: comprobarlo.", "5"},
                new String[]{"salve/core/Quiet.java", "POSIBLES PROBLEMAS: ninguno visto.\nMEJORA: ninguna", "4"}));
        assertEquals(1, found.size());
        assertEquals("app/src/main/java/salve/core/sensors/DeviceSenses.java", found.get(0).path);
        assertEquals(EvolutionSignals.REVIEW, found.get(0).source);
        assertEquals(EvolutionCycle.VERIFY_EXAM, found.get(0).verify);
    }

    @Test public void researchAndSleepReportsCanProposeAnImprovement() {
        assertEquals("cachear el índice de memoria entre turnos", EvolutionSignals.improvementOf(
                "ANÁLISIS\nHechos...\n**MEJORA PARA MI CÓDIGO:** cachear el índice de memoria entre turnos\n\nEVIDENCIA\n..."));
        assertEquals("", EvolutionSignals.improvementOf("…\nMEJORA PARA MI CÓDIGO: ninguna"));
        assertEquals("", EvolutionSignals.improvementOf("sin línea de mejora"));
        List<EvolutionCycle.Signal> found = EvolutionSignals.fromReports(Arrays.asList(
                new String[]{EvolutionSignals.RESEARCH, "Investigación autónoma", "cachear el índice de memoria", String.valueOf(NOW - DAY)},
                new String[]{EvolutionSignals.SLEEP, "Sueño", "algo viejo", String.valueOf(NOW - 30 * DAY)}),
                NOW, goal -> "app/src/main/java/salve/core/memory/MemoryVectorStore.java");
        assertEquals("las ideas viejas caducan", 1, found.size());
        assertEquals(EvolutionSignals.RESEARCH, found.get(0).source);
    }

    @Test public void theCycleStudiesOtherSourcesWhenTheExamHasNothing() {
        EvolutionCycle.Signal voice = new EvolutionCycle.Signal(EvolutionSignals.JOURNAL, "VOZ | x", "«VOZ» falla: x", "que no falle",
                TTS, EvolutionCycle.VERIFY_ERRORS, "falló 3 veces.", Collections.singletonList("prueba"), 3);
        EvolutionCycle.Plan p = EvolutionCycle.decide(null, null, null, new ArrayList<>(), new ArrayList<>(),
                INSTALLED, true, NOW, Collections.singletonList(voice));
        assertTrue(p.ready);
        assertEquals(EvolutionSignals.JOURNAL, p.source);
        assertEquals(EvolutionCycle.VERIFY_ERRORS, p.candidate.get("verificar").getAsString());
        assertEquals(TTS, p.candidate.get("source_module_candidate").getAsString());
        assertTrue(p.phase(3).reasoning.contains("vuelve a mi registro"));
    }

    @Test public void theExamWinsWhenItHasARegisteredFailure() {
        JsonObject exam = new JsonObject();
        exam.addProperty("at", NOW - DAY);
        com.google.gson.JsonArray cases = new com.google.gson.JsonArray();
        for (int i = 0; i < 4; i++) {
            JsonObject c = new JsonObject();
            c.addProperty("question", "q" + i); c.addProperty("reply", "r" + i);
            com.google.gson.JsonArray f = new com.google.gson.JsonArray();
            if (i < 3) f.add("no_repite: repitió");
            c.add("failed", f);
            cases.add(c);
        }
        exam.add("cases", cases);
        JsonObject cand = new JsonObject();
        cand.addProperty("issue_id", "no_repite");
        cand.addProperty("source_module_candidate", "app/src/main/java/salve/core/conversation/ReplyReviewer.java");
        cand.addProperty("observed_failures", 3);
        EvolutionCycle.Signal voice = new EvolutionCycle.Signal(EvolutionSignals.JOURNAL, "VOZ | x", "voz", "", TTS,
                EvolutionCycle.VERIFY_ERRORS, "falló.", Collections.singletonList("p"), 2);
        EvolutionCycle.Plan p = EvolutionCycle.decide(exam, cand, cand, new ArrayList<>(), new ArrayList<>(),
                INSTALLED, true, NOW, Collections.singletonList(voice));
        assertEquals("Examen", p.source);
        assertEquals(EvolutionCycle.VERIFY_EXAM, p.candidate.get("verificar").getAsString());
    }

    @Test public void aSignalTriedTooOftenGivesWayToTheNextOne() {
        EvolutionCycle.Signal first = new EvolutionCycle.Signal(EvolutionSignals.REVIEW, "a", "problema A", "", TTS,
                EvolutionCycle.VERIFY_EXAM, "vi A.", Collections.singletonList("p"), 1);
        EvolutionCycle.Signal second = new EvolutionCycle.Signal(EvolutionSignals.RESEARCH, "b", "idea B", "",
                "app/src/main/java/salve/core/memory/MemoryVectorStore.java", EvolutionCycle.VERIFY_EXAM, "propuse B.",
                Collections.singletonList("p"), 1);
        List<JsonObject> lineage = new ArrayList<>(), verdicts = new ArrayList<>();
        for (int i = 0; i < EvolutionCycle.MAX_TRIES_PER_PROBLEM; i++) {
            String issue = "https://github.com/x/y/issues/" + (60 + i);
            lineage.add(SelfLineage.entry(NOW - (10 - i) * DAY, EvolutionCycle.ORIGIN_AUTONOMOUS, "problema A", TTS, "abc", issue, 2));
            JsonObject v = new JsonObject();
            v.addProperty("issue", issue); v.addProperty("antes", 2); v.addProperty("ahora", 2); v.addProperty("resultado", "igual");
            verdicts.add(v);
        }
        EvolutionCycle.Plan p = EvolutionCycle.decide(null, null, null, lineage, verdicts, INSTALLED, true, NOW,
                Arrays.asList(first, second));
        assertEquals("idea B", p.title);
        assertTrue(p.ready);
    }

    @Test public void aSmallRegressionDoesNotBlockHerForever() {
        String issue = "https://github.com/x/y/issues/70";
        List<JsonObject> lineage = Collections.singletonList(
                SelfLineage.entry(NOW - 5 * DAY, EvolutionCycle.ORIGIN_AUTONOMOUS, "otra cosa", TTS, "abc", issue, 2));
        JsonObject v = new JsonObject();
        v.addProperty("issue", issue); v.addProperty("antes", 2); v.addProperty("ahora", 3); v.addProperty("resultado", "peor");
        EvolutionCycle.Signal sig = new EvolutionCycle.Signal(EvolutionSignals.REVIEW, "a", "problema A", "", TTS,
                EvolutionCycle.VERIFY_EXAM, "vi A.", Collections.singletonList("p"), 1);
        assertTrue(EvolutionCycle.decide(null, null, null, lineage, Collections.singletonList(v), INSTALLED, true, NOW,
                Collections.singletonList(sig)).ready);
    }
}
