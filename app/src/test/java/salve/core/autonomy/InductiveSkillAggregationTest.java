package salve.core.autonomy;

import static org.junit.Assert.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Random;
import org.junit.Test;

/**
 * New pure numeric capabilities stay in the original DSL, existing schema,
 * original learner and pCloud-compatible validated JSON, not an extra engine.
 */
public class InductiveSkillAggregationTest {
    private static final class Store implements InductiveSkillLab.Store {
        String value;
        public String read() { return value; }
        public void write(String value) { this.value = value; }
    }
    private static JsonArray vector(int... values) {
        JsonArray a = new JsonArray();
        for (int n : values) a.add(n);
        return a;
    }
    private static JsonArray run(String program, int... values) {
        return InductiveSkillLab.simulate(JsonParser.parseString(program), vector(values));
    }

    @Test public void sumsCountsAndRunningTotalsUseExistingBoundedListContract() {
        assertEquals(vector(4), run("[\"sum\"]", -7, 4, 7));
        assertEquals(vector(0), run("[\"sum\"]"));
        assertEquals(vector(0), run("[\"count\"]"));
        assertEquals(vector(5), run("[\"count\"]", 9, 2, -9, 5, 0));
        assertEquals(vector(2, -1, 4, 0), run("[\"prefix_sum\"]", 2, -3, 5, -4));
        assertEquals(vector(), run("[\"prefix_sum\"]"));
        assertEquals(vector(12), run("[\"abs\",\"sum\"]", -3, 4, -5));
        assertEquals(vector(3), run("[\"filter_even\",\"unique\",\"count\"]",
                2, 3, 2, -4, 1, 6, 6));
        assertEquals(vector(13, 14, 17), run("[\"plus_one\",\"prefix_sum\"]", 12, 0, 2));
    }

    @Test public void aggregationNeverOverflowsOrExecutesUnknownInstructions() {
        for (String op : new String[]{"sum", "prefix_sum"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> run("[\"" + op + "\"]", 10000, 1));
            assertThrows(IllegalArgumentException.class,
                    () -> run("[\"" + op + "\"]", -10000, -1));
        }
        assertThrows(IllegalArgumentException.class,
                () -> run("[\"sum\"]", 10001));
        assertThrows(IllegalArgumentException.class,
                () -> run("[\"sum\",\"count\",\"sum\",\"sum\"]", 1));
        assertThrows(IllegalArgumentException.class,
                () -> run("[\"network.http\"]", 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> run("[\"shell\"]", 1, 2));
        int[] oversized = new int[25];
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(JsonParser.parseString("[\"count\"]"), vector(oversized)));
    }

    @Test public void learnsRunningTotalsAndTransfersToNewCasesAfterRestart() throws Exception {
        Store store = new Store();
        JsonObject request = new JsonObject();
        request.addProperty("name", "running_totals");
        JsonArray train = new JsonArray(), holdout = new JsonArray();
        int[][] examples = {{1,2,3}, {3,-1,5,0}, {-7,9,2,-2}, {0,4,-6,3},
                {5,5,0,-10}, {-1,-2,-3,9}};
        for (int i = 0; i < examples.length; i++) {
            JsonObject one = new JsonObject();
            one.add("in", vector(examples[i]));
            int running = 0;
            JsonArray expected = new JsonArray();
            for (int n : examples[i]) { running += n; expected.add(running); }
            one.add("out", expected);
            (i < 3 ? train : holdout).add(one);
        }
        request.add("train", train);
        request.add("holdout", holdout);
        InductiveSkillLab initial = new InductiveSkillLab(store);
        JsonObject preview = initial.preview(request, () -> false);
        assertEquals("READY_FOR_APPROVAL", preview.get("status").getAsString());
        assertNull(store.value); // no promotion from preview
        JsonObject installed = initial.learn(request, () -> false);
        assertEquals("CERTIFIED_ON_PROVIDED_HOLDOUT", installed.get("status").getAsString());
        assertEquals("[\"prefix_sum\"]", installed.getAsJsonArray("program").toString());
        assertEquals(InductiveSkillLab.MAX_CANDIDATES, installed.get("candidates_tested").getAsInt());
        assertFalse(store.value.contains("\"in\""));
        InductiveSkillLab restored = new InductiveSkillLab(store);
        Random unseen = new Random(0xAB1029);
        for (int t = 0; t < 96; t++) {
            int length = 3 + unseen.nextInt(15);
            int[] values = new int[length];
            int total = 0;
            JsonArray expected = new JsonArray();
            for (int i = 0; i < length; i++) {
                values[i] = unseen.nextInt(101) - 50;
                total += values[i]; expected.add(total);
            }
            assertEquals("case " + t, expected, restored.apply("running_totals", vector(values)));
        }
    }

    @Test public void cloudSkillArchiveCanValidateNewOpsWithoutExecutingThem() throws Exception {
        Store store = new Store();
        store.value = "{\"schema\":1,\"skills\":{\"approved_total\":"
                + "{\"program\":[\"abs\",\"sum\"],\"training\":3,\"holdout\":3}}}";
        String envelope = salve.data.sync.SkillCloudArchive.pack(
                salve.data.sync.SkillCloudArchive.INDUCTIVE, store.value, 123456L);
        salve.data.sync.SkillCloudArchive.Snapshot restored =
                salve.data.sync.SkillCloudArchive.unpack(envelope, null);
        assertEquals(store.value, restored.payload);
        assertEquals(vector(9), new InductiveSkillLab(store).apply("approved_total", vector(-4, 5)));
        assertThrows(IllegalArgumentException.class, () ->
                salve.data.sync.SkillCloudArchive.pack(
                        salve.data.sync.SkillCloudArchive.INDUCTIVE,
                        store.value.replace("sum", "http"), 123456L));
    }
}
