package salve.core.autonomy;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Frozen programs tested on an evaluator-owned distribution never used for training. */
public class BlindSkillTransferExperimentTest {
    @Test public void sevenUnknownCompositionsTransferBeyondTrainingDistribution() throws Exception {
        int verified = 0;
        for (BlindSkillTransferExperiment.Challenge kind : BlindSkillTransferExperiment.Challenge.values()) {
            BlindSkillTransferExperiment.Result result = BlindSkillTransferExperiment.examine(
                    BlindSkillTransferExperiment.independentOracle(kind),
                    BlindSkillTransferExperiment.NUMBER_LISTS,
                    0xC010A6E2L + kind.ordinal() * 1009L, () -> false);
            assertTrue(result.report().toString(), result.attempts >= 1 && result.attempts <= 4);
            assertFalse(result.report().toString().contains("final_answer_inputs"));
            assertFalse(result.report().get("skill_installed").getAsBoolean());
            assertFalse(result.report().get("final_exam_reused_for_training").getAsBoolean());
            if ("TRANSFER_VERIFIED".equals(result.status)) {
                verified++;
                assertNotNull(result.frozenProgram);
                assertEquals(96, result.finalCorrect);
                assertTrue(result.baselineCorrect <= 84);
            } else {
                // Real failures are published. No hidden reattempt after the final.
                assertTrue(result.status.equals("TRANSFER_FAILED")
                        || result.status.equals("DEVELOPMENT_FAILED"));
            }
        }
        assertTrue("At least four of seven new compositions must generalize; actual="
                + verified, verified >= 4);
    }

    @Test public void newExternalOracleNotNamedInChallengeCatalogCanBeLearnt() throws Exception {
        // Reverse original order, square each number, retain first occurrence.
        // Independent implementation deliberately does not call the DSL.
        BlindSkillTransferExperiment.Oracle unknown = raw -> {
            ArrayList<Integer> numbers = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) numbers.add(raw.get(i).getAsInt());
            Collections.reverse(numbers);
            LinkedHashSet<Integer> distinctSquares = new LinkedHashSet<>();
            for (int value : numbers) distinctSquares.add(value * value);
            JsonArray output = new JsonArray();
            for (int number : distinctSquares) output.add(number);
            return output;
        };
        BlindSkillTransferExperiment.Result report = BlindSkillTransferExperiment.examine(
                unknown, BlindSkillTransferExperiment.NUMBER_LISTS, 0x57800929L, () -> false);
        assertEquals(report.report().toString(), "TRANSFER_VERIFIED", report.status);
        assertEquals(96, report.finalCorrect);
        assertNotNull(report.frozenProgram);
        assertEquals(3, report.frozenProgram.size());
    }

    @Test public void failedDevelopmentCanRetryWithFreshTrainingWithoutExamLeakage() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        BlindSkillTransferExperiment.Sampler staged = (random, shifted) -> {
            if (shifted) return BlindSkillTransferExperiment.NUMBER_LISTS.sample(random, true);
            int n = calls.getAndIncrement();
            JsonArray sample = new JsonArray();
            if (n < 6) { // Training looks like mere sorting+dedup.
                sample.add(n + 1); sample.add(20 - n); sample.add(n + 1); sample.add(17 - n);
            } else { // New development case exposes the missing abs transform.
                // Sign-paired values force abs BEFORE dedup; avoids a
                // training-only shortcut that would fail on unseen +/- pairs.
                sample.add(-(n + 5)); sample.add(n + 5);
                sample.add(-(n + 5)); sample.add(n % 11 + 1);
            }
            return sample;
        };
        BlindSkillTransferExperiment.Result result = BlindSkillTransferExperiment.examine(
                BlindSkillTransferExperiment.independentOracle(
                        BlindSkillTransferExperiment.Challenge.ABS_SORT_UNIQUE),
                staged, 73595L, () -> false);
        assertTrue(result.report().toString(), result.developmentFailures >= 1);
        assertTrue(result.report().toString(), result.attempts >= 2);
        assertEquals("TRANSFER_VERIFIED", result.status);
        assertEquals(96, result.finalCorrect);
    }

    @Test public void failingFinalNeverGetsRetriedOrUsedAsTraining() throws Exception {
        // Intentionally nonstationary task: looks like identity in development,
        // changes at longer lengths in unseen transfer distribution.
        BlindSkillTransferExperiment.Oracle trap = input -> {
            if (input.size() < 10) return input.deepCopy();
            JsonArray result = new JsonArray();
            result.add(9876);
            return result;
        };
        BlindSkillTransferExperiment.Result result = BlindSkillTransferExperiment.examine(
                trap, BlindSkillTransferExperiment.NUMBER_LISTS, 52025L, () -> false);
        assertEquals("TRANSFER_FAILED", result.status);
        assertEquals(1, result.attempts); // frozen after development passed
        assertEquals(0, result.finalCorrect);
        assertNotNull(result.frozenProgram);
        assertFalse(result.report().get("final_exam_reused_for_training").getAsBoolean());
    }

    @Test public void impossibleSkillStopsAfterBoundedAttemptsWithoutFinalExam() throws Exception {
        BlindSkillTransferExperiment.Oracle impossible = input -> {
            JsonArray output = new JsonArray(); output.add(7777); return output;
        };
        BlindSkillTransferExperiment.Result result = BlindSkillTransferExperiment.examine(
                impossible, BlindSkillTransferExperiment.NUMBER_LISTS, 345021L, () -> false);
        assertEquals("DEVELOPMENT_FAILED", result.status);
        assertEquals(4, result.attempts);
        assertEquals(4, result.developmentFailures);
        assertNull(result.frozenProgram);
        assertEquals(0, result.finalCorrect);
    }

    @Test public void cancelPreventsFurtherExperimentWithoutInstalling() {
        AtomicInteger tick = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> BlindSkillTransferExperiment.examine(
                BlindSkillTransferExperiment.independentOracle(
                        BlindSkillTransferExperiment.Challenge.ODD_SQUARE_SORT),
                BlindSkillTransferExperiment.NUMBER_LISTS, 80166L,
                () -> tick.incrementAndGet() > 50));
    }
}
