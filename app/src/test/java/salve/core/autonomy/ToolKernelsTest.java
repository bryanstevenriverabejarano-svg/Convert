package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolKernelsTest {
    private static JsonObject json(String source) { return JsonParser.parseString(source).getAsJsonObject(); }
    private static JsonObject run(String family, String strategy, String input) {
        return ToolKernels.execute(family, strategy, json(input), new OperationBudget(100_000));
    }

    @Test public void shortestRouteRepairsFewerEdgesCounterexample() {
        String input = "{\"nodes\":3,\"source\":0,\"target\":2,\"edges\":[[0,2,20],[0,1,2],[1,2,3]]}";
        assertEquals(20, run("route", "FEWEST_EDGES", input).get("cost").getAsInt());
        for (String strategy : new String[]{"DIJKSTRA", "BELLMAN_FORD", "DAG_SHORTEST"}) {
            JsonObject result = run("route", strategy, input);
            assertEquals(5, result.get("cost").getAsInt());
            assertEquals("[0,1,2]", result.get("path").toString());
        }
    }

    @Test public void negativeEdgesSelectCompatibleExactAlgorithm() {
        String input = "{\"nodes\":4,\"source\":0,\"target\":3,\"edges\":[[0,1,4],[0,2,3],[1,2,-5],[2,3,2]]}";
        assertThrows(IllegalArgumentException.class, () -> run("route", "DIJKSTRA", input));
        for (String strategy : new String[]{"BELLMAN_FORD", "DAG_SHORTEST"}) {
            JsonObject result = run("route", strategy, input);
            assertEquals(1, result.get("cost").getAsInt());
            assertEquals("[0,1,2,3]", result.get("path").toString());
        }
    }

    @Test public void negativeCycleIsRejectedEvenWhenDisconnected() {
        String input = "{\"nodes\":4,\"source\":0,\"target\":1,\"edges\":[[0,1,5],[2,3,-2],[3,2,1]]}";
        for (String strategy : ToolKernels.strategies("route"))
            assertThrows(IllegalArgumentException.class, () -> run("route", strategy, input));
    }

    @Test public void unreachableAndIdentityRoutesAreExplicit() {
        for (String strategy : ToolKernels.strategies("route")) {
            JsonObject missing = run("route", strategy,
                    "{\"nodes\":2,\"source\":0,\"target\":1,\"edges\":[]}");
            assertEquals("unreachable", missing.get("status").getAsString());
            assertEquals("[]", missing.get("path").toString());
            assertFalse(missing.has("cost"));
            JsonObject identity = run("route", strategy,
                    "{\"nodes\":1,\"source\":0,\"target\":0,\"edges\":[]}");
            assertEquals(0, identity.get("cost").getAsInt());
            assertEquals("[0]", identity.get("path").toString());
        }
    }

    @Test public void parallelEdgesUseMinimumWeightAndZeroCycleDoesNotBreakPath() {
        String input = "{\"nodes\":3,\"source\":0,\"target\":2,\"edges\":[[0,1,7],[0,1,0],[1,0,0],[1,2,2]]}";
        for (String strategy : new String[]{"DIJKSTRA", "BELLMAN_FORD"}) {
            JsonObject result = run("route", strategy, input);
            assertEquals(2, result.get("cost").getAsInt());
            assertEquals("[0,1,2]", result.get("path").toString());
        }
        assertThrows(IllegalArgumentException.class, () -> run("route", "DAG_SHORTEST", input));
    }

    @Test public void dynamicKnapsackRepairsBothGreedyFailures() {
        String input = "{\"capacity\":10,\"items\":[{\"weight\":6,\"value\":13},{\"weight\":5,\"value\":10},{\"weight\":5,\"value\":10}]}";
        assertEquals(13, run("knapsack", "RATIO_GREEDY", input).get("value").getAsInt());
        assertEquals(13, run("knapsack", "VALUE_GREEDY", input).get("value").getAsInt());
        JsonObject result = run("knapsack", "DYNAMIC_PROGRAMMING", input);
        assertEquals(20, result.get("value").getAsInt());
        assertEquals(10, result.get("weight").getAsInt());
        assertEquals("[1,2]", result.get("selected").toString());
    }

    @Test public void knapsackNeverReusesItemAndAcceptsEmptyAndZeroCapacity() {
        JsonObject result = run("knapsack", "DYNAMIC_PROGRAMMING",
                "{\"capacity\":10,\"items\":[{\"weight\":2,\"value\":9}]}" );
        assertEquals(9, result.get("value").getAsInt());
        assertEquals("[0]", result.get("selected").toString());
        for (String strategy : ToolKernels.strategies("knapsack")) {
            assertEquals(0, run("knapsack", strategy, "{\"capacity\":100,\"items\":[]}").get("value").getAsInt());
            assertEquals(0, run("knapsack", strategy,
                    "{\"capacity\":0,\"items\":[{\"weight\":1,\"value\":1000000}]}").get("value").getAsInt());
        }
    }

    @Test public void weightedScheduleRepairsEarliestFinishAndLargestJob() {
        String earliestTrap = "{\"jobs\":[{\"start\":0,\"end\":1,\"value\":1},{\"start\":0,\"end\":4,\"value\":10}]}";
        assertEquals(1, run("schedule", "EARLIEST_FINISH", earliestTrap).get("value").getAsInt());
        assertEquals(10, run("schedule", "WEIGHTED_DP", earliestTrap).get("value").getAsInt());
        String largestTrap = "{\"jobs\":[{\"start\":0,\"end\":4,\"value\":10},{\"start\":0,\"end\":2,\"value\":6},{\"start\":2,\"end\":4,\"value\":6}]}";
        assertEquals(10, run("schedule", "GREATEST_VALUE", largestTrap).get("value").getAsInt());
        JsonObject exact = run("schedule", "WEIGHTED_DP", largestTrap);
        assertEquals(12, exact.get("value").getAsInt());
        assertEquals("[1,2]", exact.get("selected").toString());
    }

    @Test public void touchingIntervalsAndFullIntegerTimesDoNotOverflow() {
        String input = "{\"jobs\":[{\"start\":-2147483648,\"end\":0,\"value\":4},{\"start\":0,\"end\":2147483647,\"value\":5}]}";
        for (String strategy : ToolKernels.strategies("schedule")) {
            assertEquals(9, run("schedule", strategy, input).get("value").getAsInt());
            assertEquals(0, run("schedule", strategy, "{\"jobs\":[]}").get("value").getAsInt());
        }
    }

    @Test public void dependencyLayersRepairReversedInputOrderAndPreserveParallelism() {
        String input = "{\"nodes\":5,\"edges\":[[4,1],[4,2],[1,0],[2,0],[2,0]]}";
        assertEquals("[0,1,2,3,4]", run("dependencies", "INPUT_ORDER", input).get("order").toString());
        JsonObject result = run("dependencies", "KAHN_LAYERS", input);
        assertEquals("[[3,4],[1,2],[0]]", result.get("layers").toString());
        assertEquals("[3,4,1,2,0]", result.get("order").toString());
    }

    @Test public void dependencyCyclesContainClosedEdgeWitness() {
        for (String strategy : ToolKernels.strategies("dependencies")) {
            JsonObject cycle = run("dependencies", strategy, "{\"nodes\":4,\"edges\":[[0,1],[1,2],[2,1]]}");
            assertEquals("cycle", cycle.get("status").getAsString());
            assertEquals("[1,2,1]", cycle.get("cycle").toString());
            JsonObject self = run("dependencies", strategy, "{\"nodes\":1,\"edges\":[[0,0]]}");
            assertEquals("[0,0]", self.get("cycle").toString());
        }
    }

    @Test public void invalidTypesShapesAndUnsupportedStrategiesAreRejected() {
        String[] badRoutes = {
                "{\"nodes\":0,\"source\":0,\"target\":0,\"edges\":[]}",
                "{\"nodes\":15,\"source\":0,\"target\":0,\"edges\":[]}",
                "{\"nodes\":\"2\",\"source\":0,\"target\":0,\"edges\":[]}",
                "{\"nodes\":2.0,\"source\":0,\"target\":0,\"edges\":[]}",
                "{\"nodes\":2,\"source\":0,\"target\":0,\"edges\":[[0,1]]}",
                "{\"nodes\":2,\"source\":0,\"target\":2,\"edges\":[]}",
                "{\"nodes\":2,\"source\":0,\"target\":1,\"edges\":[[0,2,1]]}",
                "{\"nodes\":2,\"source\":0,\"target\":1,\"edges\":[],\"extra\":0}",
                "{\"nodes\":2,\"source\":0,\"target\":1,\"edges\":[[0,1,1000001]]}"
        };
        for (String input : badRoutes)
            assertThrows(IllegalArgumentException.class, () -> run("route", "BELLMAN_FORD", input));
        assertThrows(IllegalArgumentException.class, () -> run("knapsack", "DYNAMIC_PROGRAMMING",
                "{\"capacity\":100,\"items\":[{\"weight\":0,\"value\":5}]}"));
        assertThrows(IllegalArgumentException.class, () -> run("schedule", "WEIGHTED_DP",
                "{\"jobs\":[{\"start\":3,\"end\":3,\"value\":5}]}"));
        assertThrows(IllegalArgumentException.class, () -> ToolKernels.strategies("shell"));
        assertThrows(IllegalArgumentException.class, () -> run("dependencies", "DIJKSTRA",
                "{\"nodes\":1,\"edges\":[]}"));
        assertThrows(UnsupportedOperationException.class, () -> ToolKernels.strategies("route").add("SHELL"));
    }

    @Test public void operationBudgetAndCancellationInterruptKernelWork() {
        JsonObject input = json("{\"nodes\":14,\"source\":0,\"target\":13,\"edges\":[]}");
        OperationBudget tiny = new OperationBudget(2);
        assertThrows(OperationBudget.Exhausted.class,
                () -> ToolKernels.execute("route", "BELLMAN_FORD", input, tiny));
        assertEquals(2, tiny.getUsed());
        OperationBudget cancelled = new OperationBudget(1000, () -> true);
        assertThrows(CancellationException.class,
                () -> ToolKernels.execute("route", "BELLMAN_FORD", input, cancelled));
        assertEquals(0, cancelled.getUsed());
    }

    @Test public void exactOptimizationMatchesExhaustiveSubsetsOnIndependentRandomInputs() {
        Random random = new Random(20260922L);
        for (int trial = 0; trial < 60; trial++) {
            int n = 1 + random.nextInt(8), capacity = random.nextInt(20);
            int[] weights = new int[n], values = new int[n], starts = new int[n], ends = new int[n];
            JsonArray items = new JsonArray(), jobs = new JsonArray();
            for (int i = 0; i < n; i++) {
                weights[i] = 1 + random.nextInt(12);
                values[i] = random.nextInt(20);
                starts[i] = random.nextInt(12) - 6;
                ends[i] = starts[i] + 1 + random.nextInt(6);
                JsonObject item = new JsonObject();
                item.addProperty("weight", weights[i]); item.addProperty("value", values[i]); items.add(item);
                JsonObject job = new JsonObject();
                job.addProperty("start", starts[i]); job.addProperty("end", ends[i]);
                job.addProperty("value", values[i]); jobs.add(job);
            }
            int bestKnapsack = 0, bestSchedule = 0;
            for (int mask = 0; mask < (1 << n); mask++) {
                int weight = 0, value = 0;
                boolean compatible = true;
                for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) {
                    weight += weights[i]; value += values[i];
                    for (int j = i + 1; j < n; j++) if ((mask & (1 << j)) != 0
                            && starts[i] < ends[j] && starts[j] < ends[i]) compatible = false;
                }
                if (weight <= capacity) bestKnapsack = Math.max(bestKnapsack, value);
                if (compatible) bestSchedule = Math.max(bestSchedule, value);
            }
            JsonObject pack = new JsonObject(); pack.addProperty("capacity", capacity); pack.add("items", items);
            JsonObject schedule = new JsonObject(); schedule.add("jobs", jobs);
            assertEquals(bestKnapsack, ToolKernels.execute("knapsack", "DYNAMIC_PROGRAMMING", pack,
                    new OperationBudget(100_000)).get("value").getAsInt());
            assertEquals(bestSchedule, ToolKernels.execute("schedule", "WEIGHTED_DP", schedule,
                    new OperationBudget(100_000)).get("value").getAsInt());
        }
    }
}
