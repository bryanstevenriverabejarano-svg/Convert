package salve.core.evaluation;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import salve.core.mind.SkillLab;
import static org.junit.Assert.*;

public class NightPracticePolicyTest {
    private static final long NOW = 1_850_000_000_000L;
    private static SkillLab.Outcome observed(boolean successful, String verifier) {
        SkillLab.Outcome item = new SkillLab.Outcome();
        item.at = NOW - 1000;
        item.verifier = verifier;
        item.ok = successful;
        return item;
    }

    @Test public void lowPowerDefaultAndExtraPracticeOnlyForObservedFailures() {
        assertEquals(1, NightPracticePolicy.choose(Collections.emptyList(), NOW).rounds);
        List<SkillLab.Outcome> history = new ArrayList<>();
        history.add(observed(false, "independent"));
        history.add(observed(false, "independent"));
        assertEquals(1, NightPracticePolicy.choose(history, NOW).rounds);
        history.add(observed(false, "independent"));
        assertEquals(2, NightPracticePolicy.choose(history, NOW).rounds);
        history.add(observed(true, "independent"));
        history.add(observed(true, "independent"));
        history.add(observed(true, "independent"));
        assertEquals(1, NightPracticePolicy.choose(history, NOW).rounds);
        history.clear();
        history.add(observed(false, "")); // no independent verifier, never trigger exploration
        history.add(observed(false, ""));
        history.add(observed(false, ""));
        assertEquals(1, NightPracticePolicy.choose(history, NOW).rounds);
    }

    @Test public void persistedVerifiedResultIsDifferentFromAttempt() {
        String success = "Jugué 2 retos inventados por mí; resolví 1.";
        NightPracticePolicy.Outcome result = NightPracticePolicy.observed(success, 2);
        assertEquals(2, result.attempted);
        assertEquals(1, result.solved);
        assertTrue(result.persistedPractice());

        NightPracticePolicy.Outcome unsaved = NightPracticePolicy.observed(
                success + " Ojo: no pude guardar mi progreso del juego; la próxima vez seguiré desde el último guardado.", 2);
        assertEquals(2, unsaved.attempted);
        assertEquals(1, unsaved.solved);
        assertFalse(unsaved.persistedPractice());
        assertFalse(unsaved.saved);
    }

    @Test public void overBudgetUntrustedOrInconsistentSummariesAreRejected() {
        for (String message : new String[] {
                "Salve soñó que aprendió todo.",
                "Jugué 2 retos inventados por mí; resolví 3.",
                "Jugué 3 retos inventados por mí; resolví 3.",
                "Jugué 1 retos inventados por mí; resolví 1. Ojo: no pude guardar mi progreso del juego;"
        }) {
            NightPracticePolicy.Outcome r = NightPracticePolicy.observed(message, 1);
            assertFalse(r.persistedPractice());
        }
    }

    @Test public void enrichedNightReportNeverClaimsWeightUpdates() {
        NightPracticePolicy.Outcome result = NightPracticePolicy.observed(
                "Jugué 1 retos inventados por mí; resolví 1.", 1);
        JsonObject report = SleepImprovementCycle.assess(null, null, null, result, NOW);
        assertEquals(1, report.get("practice_attempted").getAsInt());
        assertEquals(1, report.get("practice_solved").getAsInt());
        assertTrue(report.get("practice_saved").getAsBoolean());
        assertFalse(report.get("model_weights_updated").getAsBoolean());
        String description = SleepImprovementCycle.describe(report);
        assertTrue(description, description.contains("No he reentrenado los pesos de Gemma"));
    }
}
