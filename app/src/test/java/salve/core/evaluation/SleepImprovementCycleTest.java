package salve.core.evaluation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import salve.core.mind.SkillLab;
import static org.junit.Assert.*;

public class SleepImprovementCycleTest {
    private static final long NOW = 1_850_000_000_000L;

    private static JsonObject exam(long at, String... failures) {
        JsonObject report = new JsonObject();
        report.addProperty("at", at);
        JsonArray cases = new JsonArray();
        for (String failure : failures) {
            JsonObject item = new JsonObject();
            JsonArray failed = new JsonArray();
            failed.add(failure + ": ejemplo");
            item.add("failed", failed);
            item.addProperty("question", "dato privado que no debe pasar al informe");
            item.addProperty("reply", "respuesta privada que no debe pasar al informe");
            cases.add(item);
        }
        report.add("cases", cases);
        return report;
    }

    private static SkillLab.Outcome outcome(boolean ok, String verifier) {
        SkillLab.Outcome o = new SkillLab.Outcome();
        o.at = NOW - 1000L;
        o.skill = "responder";
        o.ok = ok;
        o.verifier = verifier;
        o.evidence = "texto personal que nunca debe entrar en el informe";
        return o;
    }

    @Test public void repeatedRealFailuresQueueOnlyMetadataWithoutCodeOrPrivateText() {
        JsonObject exam = exam(NOW - 1000L, "no_repite", "no_repite", "usa");
        SkillLab.Decision adopted = new SkillLab.Decision();
        adopted.at = NOW - 1000L;
        adopted.verdict = "adoptado";
        adopted.why = "texto sensible de la estrategia";
        JsonObject report = SleepImprovementCycle.assess(exam,
                Arrays.asList(outcome(true, "bryan"), outcome(false, "independent"), outcome(true, "")),
                Collections.singletonList(adopted), true, NOW);
        assertEquals("READY_FOR_EXPLICIT_PR", report.get("stage").getAsString());
        assertEquals("no_repite", report.get("issue_id").getAsString());
        assertEquals(2, report.get("observed_failures").getAsInt());
        assertEquals(2, report.get("skill_observations").getAsInt());
        assertEquals(1, report.get("skill_successes").getAsInt());
        assertEquals(1, report.get("skill_failures").getAsInt());
        assertEquals(1, report.get("skill_adoptions").getAsInt());
        assertNotNull(SleepImprovementCycle.candidateFor(report, exam));
        for (String forbidden : Arrays.asList("dato privado", "respuesta privada", "texto personal",
                "texto sensible", "patch", "token", "code")) {
            assertFalse(forbidden, report.toString().contains(forbidden));
        }
    }

    @Test public void staleOrAbsentExamCannotTriggerCodeProposal() {
        JsonObject old = exam(NOW - SleepImprovementCycle.MAX_EXAM_AGE_MS - 2000L,
                "no_repite", "no_repite");
        JsonObject stale = SleepImprovementCycle.assess(old, null, null, false, NOW);
        assertEquals("STALE", stale.get("exam_status").getAsString());
        assertEquals("OBSERVING", stale.get("stage").getAsString());
        assertNull(SleepImprovementCycle.candidateFor(stale, old));

        JsonObject none = SleepImprovementCycle.assess(null, null, null, false, NOW);
        assertEquals("MISSING", none.get("exam_status").getAsString());
        assertFalse(none.has("source_module_candidate"));
    }

    @Test public void forgedOrChangedPriorityIsRejectedBeforeExplicitSelfPr() {
        JsonObject original = exam(NOW - 1000L, "usa", "usa");
        JsonObject report = SleepImprovementCycle.assess(original, null, null, false, NOW);
        assertNotNull(SleepImprovementCycle.candidateFor(report, original));
        assertNull(SleepImprovementCycle.candidateFor(report, exam(NOW - 500L, "usa", "usa")));
        report.addProperty("source_module_candidate", "app/src/main/java/salve/core/Fake.java");
        assertNull(SleepImprovementCycle.candidateFor(report, original));
    }

    @Test public void unrelatedOrSingleFailureCannotInventAnImprovement() {
        JsonObject report = SleepImprovementCycle.assess(exam(NOW - 1000L, "usa"),
                Collections.singletonList(outcome(false, "")), null, true, NOW);
        assertEquals("OBSERVING", report.get("stage").getAsString());
        assertEquals(0, report.get("skill_observations").getAsInt());
        assertFalse(report.has("issue_id"));
        assertTrue(SleepImprovementCycle.describe(report).contains("no señala un fallo repetido"));
    }

    @Test public void paraphrasesRouteToTheSameGroundedSleepReport() {
        for (String message : Arrays.asList(
                "¿Qué aprendiste mientras dormiste?",
                "¿Qué aprendiste mientras dormías?",
                "Salve, ¿qué has aprendido durante tu descanso?",
                "¿En qué has mejorado anoche?",
                "¿Qué investigaste cuando dormías?",
                "¿Qué descubriste durante el sueño?",
                "Dime qué lograste esta noche",
                "Cuéntame qué hiciste mientras estabas dormida",
                "Muéstrame el informe de tu sueño",
                "Tu resumen del sueño",
                "Tus mejoras nocturnas",
                "¿Qué pensaste al dormir?",
                "¿Has practicado algo durante tu descanso?")) {
            assertTrue(message, SleepImprovementCycle.asksStatus(message));
        }
    }

    @Test public void unrelatedSleepAdviceOrFictionDoesNotOpenPrivateReport() {
        for (String message : Arrays.asList(
                "¿Cómo puedo mejorar mi sueño?",
                "Quiero escribir un cuento sobre los sueños",
                "Inventa qué aprendiste mientras dormías",
                "No me digas qué aprendiste anoche",
                "¿Qué aprendiste ayer?",
                "Salve, duerme",
                "¿Qué es la fase REM?",
                "Abre un PR")) {
            assertFalse(message, SleepImprovementCycle.asksStatus(message));
        }
    }

    @Test public void statusCommandsDoNotRunDuringOrdinaryDiscussion() {
        assertTrue(SleepImprovementCycle.asksStatus("Salve, qué aprendiste mientras dormías?"));
        assertTrue(SleepImprovementCycle.asksStatus("En qué pensaste mientras dormías?"));
        assertTrue(SleepImprovementCycle.asksStatus("Tus mejoras nocturnas"));
        assertFalse(SleepImprovementCycle.asksStatus("Quiero escribir un cuento sobre los sueños"));
        assertFalse(SleepImprovementCycle.asksStatus("Salve, duerme"));
        assertFalse(SleepImprovementCycle.asksStatus("Abre un PR"));
    }
}
