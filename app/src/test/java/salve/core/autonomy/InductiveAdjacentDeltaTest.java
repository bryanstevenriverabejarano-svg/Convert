package salve.core.autonomy;

import static org.junit.Assert.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Random;
import org.junit.Test;
import salve.data.sync.SkillCloudArchive;

/** New skill on top of the existing aggregation DSL from PR #307. */
public class InductiveAdjacentDeltaTest {
    static final class Store implements InductiveSkillLab.Store {
        String json;
        public String read() { return json; }
        public void write(String value) { json = value; }
    }

    private static JsonArray vector(int... values) {
        JsonArray out = new JsonArray();
        for (int value : values) out.add(value);
        return out;
    }

    private static JsonArray independent(int[] input) {
        JsonArray expected = new JsonArray();
        for (int i = 1; i < input.length; i++)
            expected.add(Math.subtractExact(input[i], input[i - 1]));
        return expected;
    }

    @Test public void verifiedDifferenceTransfersAcrossRestartAndCloudArchive() throws Exception {
        Store store = new Store();
        InductiveSkillLab lab = new InductiveSkillLab(store);
        InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.ADJACENT_DELTA;
        InductiveSkillProposal.Evaluation result = InductiveSkillProposal.evaluate(lab, kind, () -> false);
        assertEquals(InductiveSkillLab.MAX_CANDIDATES, result.candidatesChecked);
        assertEquals(2954, result.candidatesChecked);
        assertEquals("[\"adjacent_delta\"]", result.program.toString());
        assertTrue(result.promotable());
        assertEquals(96, result.candidateSuccesses);
        assertNull("A proposal must never install a skill", store.json);

        JsonArray forged = new JsonArray(); forged.add("prefix_sum");
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillProposal.promote(lab, kind, forged, () -> false));
        assertNull(store.json);

        JsonObject approved = InductiveSkillProposal.promote(lab, kind, result.program, () -> false);
        assertTrue(approved.get("certified").getAsBoolean());
        assertFalse(store.json.contains("\"in\""));
        assertFalse(store.json.contains("\"out\""));
        InductiveSkillLab restarted = new InductiveSkillLab(store);
        Random unseen = new Random(20261030L);
        for (int caseNo = 0; caseNo < 96; caseNo++) {
            int[] numbers = new int[1 + unseen.nextInt(23)];
            for (int i = 0; i < numbers.length; i++) numbers[i] = unseen.nextInt(161) - 80;
            assertEquals("Unseen reference " + caseNo, independent(numbers),
                    restarted.apply(InductiveSkillProposal.skillName(kind), vector(numbers)));
        }
        String pack = SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE, store.json, 20261030L);
        SkillCloudArchive.Snapshot snapshot = SkillCloudArchive.unpack(pack, null);
        assertEquals(store.json, snapshot.payload);
        assertEquals(SkillCloudArchive.INDUCTIVE, snapshot.kind);
        assertEquals(vector(2, -3, 7),
                new InductiveSkillLab(new InductiveSkillLab.Store() {
                    public String read() { return snapshot.payload; }
                    public void write(String value) { throw new AssertionError("Read-only cloud"); }
                }).apply(InductiveSkillProposal.skillName(kind), vector(5, 7, 4, 11)));
    }

    @Test public void preservesPreexistingAggregationsAndRejectsOverflowAndScripts() {
        assertEquals(vector(), InductiveSkillLab.simulate(
                JsonParser.parseString("[\"adjacent_delta\"]"), vector()));
        assertEquals(vector(), InductiveSkillLab.simulate(
                JsonParser.parseString("[\"adjacent_delta\"]"), vector(3)));
        assertEquals(vector(3, -1), InductiveSkillLab.simulate(
                JsonParser.parseString("[\"adjacent_delta\"]"), vector(2, 5, 4)));
        assertEquals(vector(2, 5, 4), InductiveSkillLab.simulate(
                JsonParser.parseString("[\"prefix_sum\"]"), vector(2, 3, -1)));
        assertEquals(vector(4), InductiveSkillLab.simulate(
                JsonParser.parseString("[\"sum\"]"), vector(-1, 5)));
        assertEquals(vector(2), InductiveSkillLab.simulate(
                JsonParser.parseString("[\"count\"]"), vector(7, 8)));
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(
                        JsonParser.parseString("[\"adjacent_delta\"]"),
                        vector(10000, -10000)));
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(
                        JsonParser.parseString("[\"python.exec\"]"), vector(1)));
    }

    @Test public void failedReservedCaseAndCancellationCannotPersist() throws Exception {
        Store store = new Store();
        InductiveSkillLab lab = new InductiveSkillLab(store);
        JsonObject request = InductiveSkillProposal.request(InductiveSkillProposal.Kind.ADJACENT_DELTA);
        request.getAsJsonArray("holdout").get(0).getAsJsonObject().add("out", vector(999));
        assertEquals("HOLDOUT_FAILED", lab.learn(request, () -> false).get("status").getAsString());
        assertNull(store.json);
        final int[] ticks = {0};
        assertThrows(IllegalStateException.class,
                () -> InductiveSkillProposal.evaluate(lab, InductiveSkillProposal.Kind.ADJACENT_DELTA,
                        () -> ++ticks[0] > 20));
        assertNull(store.json);
    }
}
