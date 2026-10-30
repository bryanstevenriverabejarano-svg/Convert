package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Held-out examples have different inputs and are never part of candidate
 * selection; post-certification unseen cases independently check transfer.
 */
public class InductiveThreeStageGeneralizationTest {
    private static class Memory implements InductiveSkillLab.Store {
        String json; boolean reject;
        @Override public String read() { return json; }
        @Override public void write(String s) throws Exception {
            if (reject) throw new java.io.IOException("simulated persistence failure");
            json = s;
        }
    }
    private static JsonArray from(int... values) {
        JsonArray out = new JsonArray();
        for (int v : values) out.add(v);
        return out;
    }
    private static JsonArray oracle(int... input) {
        TreeSet<Integer> uniqueOdds = new TreeSet<>();
        for (int n : input) if (n % 2 != 0) uniqueOdds.add(n);
        JsonArray out = new JsonArray();
        for (Integer n : uniqueOdds) out.add(n);
        return out;
    }
    private static void example(JsonArray target, int... input) {
        JsonObject one = new JsonObject();
        one.add("in", from(input));
        one.add("out", oracle(input));
        target.add(one);
    }
    private static JsonObject request() {
        JsonObject request = new JsonObject();
        request.addProperty("name", "sorted_unique_odds");
        JsonArray train = new JsonArray();
        example(train, 7, 3, 7, 4, 1, 3);
        example(train, -1, 2, -3, 7, -1, 7);
        example(train, 9, 8, 3, 9, 2, -5, 3);
        example(train, 7, 7, -7, 4, 0, 1);
        example(train, 6, 4, 3, -9, 3, 0, 1);
        example(train, -11, 2, -11, 5, 5, 3, 3);
        JsonArray holdout = new JsonArray();
        example(holdout, 101, -21, 101, 2, -5, -5, 11);
        example(holdout, 8, 6, 4, 2, -2, 0);
        example(holdout, -1, 7, 7, 5, -3, 1);
        example(holdout, 5, -5, 9, 1, 9, 4, 2);
        request.add("train", train); request.add("holdout", holdout);
        return request;
    }
    @Test public void combinesThreeOperationsAndTransfersTo96NewInputsAfterRestart() throws Exception {
        Memory store = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(store);
        JsonObject result = lab.learn(request(), () -> false);
        assertEquals(result.toString(), "CERTIFIED_ON_PROVIDED_HOLDOUT", result.get("status").getAsString());
        assertEquals(InductiveSkillLab.MAX_CANDIDATES, result.get("candidates_tested").getAsInt());
        assertTrue(result.get("candidates_matching_training").getAsInt() >= 1);
        assertEquals(3, result.get("selected_length").getAsInt());
        assertEquals(3, result.getAsJsonArray("program").size());
        assertNotNull(store.json);
        InductiveSkillLab reloaded = new InductiveSkillLab(store);
        Random unseen = new Random(0x51A1E6);
        for (int trial = 0; trial < 96; trial++) {
            int[] input = new int[3 + unseen.nextInt(16)];
            for (int i = 0; i < input.length; i++) input[i] = unseen.nextInt(201) - 100;
            // Inject duplicates without giving the learner any held-out labels.
            if (input.length > 6) input[input.length - 1] = input[0];
            JsonArray expected = oracle(input);
            JsonArray actual = reloaded.apply("sorted_unique_odds", from(input));
            assertEquals("new case " + trial + " " + Arrays.toString(input),
                    expected, actual);
        }
        assertFalse(store.json.contains("101"));
        assertFalse(store.json.contains("sorted_unique_odds_test"));
    }

    @Test public void failedHoldoutNeverTriesAlternativeCandidateOrPersists() throws Exception {
        Memory store = new Memory();
        JsonObject request = request();
        request.getAsJsonArray("holdout").get(0).getAsJsonObject().add("out", from(999));
        JsonObject report = new InductiveSkillLab(store).learn(request, () -> false);
        assertEquals("HOLDOUT_FAILED", report.get("status").getAsString());
        assertEquals(3, report.get("selected_length").getAsInt());
        assertNull(store.json);
    }
    @Test public void cancellationAndStorageErrorsDoNotPromoteNewSkill() throws Exception {
        Memory store = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(store);
        final int[] checks = {0};
        assertThrows(IllegalStateException.class, () ->
                lab.learn(request(), () -> ++checks[0] > 120));
        assertNull(store.json);
        store.reject = true;
        assertThrows(java.io.IOException.class, () -> lab.learn(request(), () -> false));
        assertNull(store.json);
        assertTrue(lab.catalog().contains("No hay habilidades"));
    }
    @Test public void oldTwoStepSkillsStillReadAndNewThreeStepProgramIsAllowlisted() throws Exception {
        Memory previous = new Memory();
        previous.json = "{\"schema\":1,\"skills\":{\"legacy_odd\":"
                + "{\"program\":[\"unique\",\"filter_odd\"],\"training\":3,\"holdout\":3}}}";
        InductiveSkillLab existing = new InductiveSkillLab(previous);
        assertEquals(from(5, -3),
                existing.apply("legacy_odd", from(5, 5, 2, -3, -3)));
        String before = previous.json;
        // A malicious saved 4-operation program may not be interpreted.
        previous.json = before.replace("[\"unique\",\"filter_odd\"]",
                "[\"unique\",\"sort\",\"filter_odd\",\"square\"]");
        assertThrows(IllegalArgumentException.class, () -> new InductiveSkillLab(previous));
    }
}