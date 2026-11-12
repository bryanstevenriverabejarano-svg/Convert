package salve.core.selfpr;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Las cinco puertas: Salve solo se cambia sola con datos reales, sin bucles y pudiendo deshacerlo. */
public class EvolutionCycleTest {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 1_800_000_000_000L;
    private static final String FILE = "app/src/main/java/salve/core/conversation/ReplyReviewer.java";

    private static JsonObject exam(long at, int repeatedFailures, int total) {
        JsonObject exam = new JsonObject();
        exam.addProperty("at", at);
        JsonArray cases = new JsonArray();
        for (int i = 0; i < total; i++) {
            JsonObject c = new JsonObject();
            c.addProperty("question", "pregunta " + i);
            c.addProperty("reply", "respuesta " + i);
            JsonArray failed = new JsonArray();
            if (i < repeatedFailures) failed.add("no_repite: repitió la respuesta anterior");
            c.add("failed", failed);
            cases.add(c);
        }
        exam.add("cases", cases);
        return exam;
    }

    private static JsonObject candidate(String path) {
        JsonObject c = new JsonObject();
        c.addProperty("issue_id", "no_repite");
        c.addProperty("source_module_candidate", path);
        c.addProperty("observed_failures", 3);
        return c;
    }

    private static JsonObject change(long t, String origin, String goal, String issue, int failures) {
        JsonObject e = SelfLineage.entry(t, origin, goal, FILE, "abc", issue, failures);
        e.add("cambios", EvolutionCycle.record(Collections.singletonList(new SelfPatch.Edit("int a = 1;", "int a = 2;"))));
        return e;
    }

    private static JsonObject verdict(String issue, int before, int now, String result) {
        JsonObject v = new JsonObject();
        v.addProperty("issue", issue);
        v.addProperty("antes", before);
        v.addProperty("ahora", now);
        v.addProperty("resultado", result);
        return v;
    }

    private static EvolutionCycle.Plan plan(JsonObject exam, JsonObject night, List<JsonObject> lineage, List<JsonObject> verdicts) {
        return EvolutionCycle.decide(exam, night, candidate(FILE), lineage, verdicts, NOW - 5 * DAY, true, NOW);
    }

    @Test public void allFivePhasesWithRealEvidenceMakeHerReady() {
        EvolutionCycle.Plan p = plan(exam(NOW - DAY, 3, 10), candidate(FILE), new ArrayList<>(), new ArrayList<>());
        assertTrue(p.ready);
        assertEquals("Lista para proponer", p.status);
        for (int i = 1; i <= 4; i++) assertEquals(EvolutionCycle.State.DONE, p.phase(i).state);
        assertEquals(EvolutionCycle.State.IDLE, p.phase(5).state);
        assertTrue(p.phase(1).reasoning.contains("3 de 10"));
        assertTrue(p.phase(2).reasoning.contains("«pregunta 0»"));
        assertNotNull(p.candidate);
    }

    @Test public void withoutExamNothingMoves() {
        EvolutionCycle.Plan p = plan(null, null, new ArrayList<>(), new ArrayList<>());
        assertFalse(p.ready);
        assertEquals("Sin examen", p.status);
        assertEquals(EvolutionCycle.State.WAITING, p.phase(1).state);
        assertEquals(1, p.current);
    }

    @Test public void examFromAnOlderVersionMustBeRepeated() {
        EvolutionCycle.Plan p = EvolutionCycle.decide(exam(NOW - 6 * DAY, 3, 10), candidate(FILE), candidate(FILE),
                new ArrayList<>(), new ArrayList<>(), NOW - 2 * DAY, true, NOW);
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.WAITING, p.phase(1).state);
        assertTrue(p.phase(1).reasoning.contains("antes de la versión"));
    }

    @Test public void aSingleFailureIsNotEnough() {
        EvolutionCycle.Plan p = EvolutionCycle.decide(exam(NOW - DAY, 1, 10), null, null,
                new ArrayList<>(), new ArrayList<>(), NOW - 5 * DAY, true, NOW);
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.WAITING, p.phase(2).state);
    }

    @Test public void analysisNeedsTheNightConfirmation() {
        EvolutionCycle.Plan p = plan(exam(NOW - DAY, 3, 10), null, new ArrayList<>(), new ArrayList<>());
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.WAITING, p.phase(3).state);
        assertTrue(p.phase(3).reasoning.contains("sospecha"));
    }

    @Test public void protectedFilesAreNeverTouched() {
        String gate = "app/src/main/java/salve/core/selfpr/SelfPatch.java";
        EvolutionCycle.Plan p = EvolutionCycle.decide(exam(NOW - DAY, 3, 10), candidate(gate), candidate(gate),
                new ArrayList<>(), new ArrayList<>(), NOW - 5 * DAY, true, NOW);
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.BLOCKED, p.phase(3).state);
    }

    @Test public void waitsForTheVerdictOfThePreviousChange() {
        List<JsonObject> lineage = new ArrayList<>();
        lineage.add(change(NOW - DAY, "examen", "Repite respuestas", "https://github.com/x/y/issues/7", 3));
        EvolutionCycle.Plan p = plan(exam(NOW - DAY / 2, 3, 10), candidate(FILE), lineage, new ArrayList<>());
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.WAITING, p.phase(4).state);
        assertEquals(EvolutionCycle.State.WAITING, p.phase(5).state);
        assertEquals("Verificando", p.status);
    }

    @Test public void atMostOneAutonomousProposalPerDay() {
        List<JsonObject> lineage = new ArrayList<>();
        String issue = "https://github.com/x/y/issues/8";
        lineage.add(change(NOW - DAY / 3, EvolutionCycle.ORIGIN_AUTONOMOUS, "Otra cosa", issue, 3));
        List<JsonObject> verdicts = Collections.singletonList(verdict(issue, 3, 2, "mejor"));
        EvolutionCycle.Plan p = plan(exam(NOW - DAY / 4, 3, 10), candidate(FILE), lineage, verdicts);
        assertFalse(p.ready);
        assertTrue(p.phase(4).reasoning.contains("24 horas"));
    }

    @Test public void stopsAfterRepeatedAttemptsWithoutImprovement() {
        List<JsonObject> lineage = new ArrayList<>();
        List<JsonObject> verdicts = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String issue = "https://github.com/x/y/issues/" + (20 + i);
            lineage.add(change(NOW - (10 - i) * DAY, "examen",
                    salve.core.evaluation.ImprovementProposals.problem("no_repite")[0], issue, 3));
            verdicts.add(verdict(issue, 3, 3, "igual"));
        }
        EvolutionCycle.Plan p = plan(exam(NOW - DAY, 3, 10), candidate(FILE), lineage, verdicts);
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.BLOCKED, p.phase(4).state);
    }

    @Test public void worseByAMarginIsRevertedAndBlocksNewProposals() {
        String issue = "https://github.com/x/y/issues/30";
        List<JsonObject> lineage = Collections.singletonList(change(NOW - 4 * DAY, EvolutionCycle.ORIGIN_AUTONOMOUS, "X", issue, 2));
        List<JsonObject> verdicts = Collections.singletonList(verdict(issue, 2, 5, "peor"));
        assertNotNull(EvolutionCycle.revertTarget(lineage, verdicts));
        EvolutionCycle.Plan p = plan(exam(NOW - DAY, 3, 10), candidate(FILE), lineage, verdicts);
        assertFalse(p.ready);
        assertEquals(EvolutionCycle.State.BLOCKED, p.phase(4).state);
        assertEquals(EvolutionCycle.State.BLOCKED, p.phase(5).state);
    }

    @Test public void smallWorseningMayBeNoiseAndIsNotRevertedAlone() {
        String issue = "https://github.com/x/y/issues/31";
        List<JsonObject> lineage = Collections.singletonList(change(NOW - 4 * DAY, "examen", "X", issue, 2));
        assertNull(EvolutionCycle.revertTarget(lineage, Collections.singletonList(verdict(issue, 2, 3, "peor"))));
    }

    @Test public void aRevertIsNeverRevertedAgain() {
        String issue = "https://github.com/x/y/issues/32";
        List<JsonObject> lineage = Collections.singletonList(change(NOW - DAY, EvolutionCycle.ORIGIN_REVERT, "Revertir: X", issue, 5));
        assertNull(EvolutionCycle.revertTarget(lineage, Collections.singletonList(verdict(issue, 5, 9, "peor"))));
    }

    @Test public void inverseEditsRestoreTheOriginalCode() throws Exception {
        String before = "class A {\n    int a = 1;\n    int b = 1;\n}\n";
        List<SelfPatch.Edit> edits = new ArrayList<>();
        edits.add(new SelfPatch.Edit("    int a = 1;", "    int a = 2;"));
        edits.add(new SelfPatch.Edit("    int b = 1;", "    int b = 3;\n    int c = 4;"));
        String after = SelfPatch.apply(before, edits);
        JsonObject entry = new JsonObject();
        entry.add("cambios", EvolutionCycle.record(edits));
        assertEquals(before, SelfPatch.apply(after, EvolutionCycle.inverse(entry)));
    }

    @Test public void aDeletionCannotBeHalfReverted() {
        List<SelfPatch.Edit> edits = new ArrayList<>();
        edits.add(new SelfPatch.Edit("int a = 1;", "int a = 2;"));
        edits.add(new SelfPatch.Edit("int b = 1;", ""));
        JsonObject entry = new JsonObject();
        entry.add("cambios", EvolutionCycle.record(edits));
        assertTrue(EvolutionCycle.inverse(entry).isEmpty());
    }
}
