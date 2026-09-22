package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.CancellationException;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ToolVerifierTest {
    private static final String ROUTE = "{\"nodes\":4,\"edges\":[[0,1,9],[0,2,2],[2,1,1],[1,3,2],[2,3,8]],\"source\":0,\"target\":3}";
    private static final String PACK = "{\"capacity\":10,\"items\":[{\"weight\":6,\"value\":12},{\"weight\":5,\"value\":9},{\"weight\":5,\"value\":9}]}";
    private static final String SCHEDULE = "{\"jobs\":[{\"start\":0,\"end\":8,\"value\":11},{\"start\":0,\"end\":4,\"value\":8},{\"start\":4,\"end\":8,\"value\":8}]}";
    private static final String DAG = "{\"nodes\":4,\"edges\":[[0,2],[1,2],[2,3]]}";

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static ToolVerifier.Verification check(String family, String input, String output) {
        return ToolVerifier.verify(family, json(input), json(output), new OperationBudget(1_000_000));
    }

    @Test public void routeChecksWitnessAndShortestObjectiveIndependently() {
        assertTrue(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":5,\"path\":[0,2,1,3]}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":11,\"path\":[0,1,3]}").passed);
    }

    @Test public void routeCannotForgeOptimumWithWrongWitness() {
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":5,\"path\":[0,1,3]}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":5,\"path\":[0,3]}").passed);
    }

    @Test public void routeRejectsWrongEndpointAndDuplicateVertices() {
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":3,\"path\":[0,2,1]}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":5,\"path\":[0,2,2,3]}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":2,\"path\":[1,3]}").passed);
    }

    @Test public void routeUsesSmallestParallelEdgeAndSupportsNegativeEdges() {
        String input = "{\"nodes\":3,\"edges\":[[0,1,8],[0,1,3],[1,2,-5],[0,2,0]],\"source\":0,\"target\":2}";
        assertTrue(check("route", input, "{\"status\":\"ok\",\"cost\":-2,\"path\":[0,1,2]}").passed);
        assertFalse(check("route", input, "{\"status\":\"ok\",\"cost\":3,\"path\":[0,1,2]}").passed);
    }

    @Test public void routeAcceptsAlternativeTiedOptimum() {
        String input = "{\"nodes\":4,\"edges\":[[0,1,1],[1,3,1],[0,2,1],[2,3,1]],\"source\":0,\"target\":3}";
        assertTrue(check("route", input, "{\"status\":\"ok\",\"cost\":2,\"path\":[0,1,3]}").passed);
        assertTrue(check("route", input, "{\"status\":\"ok\",\"cost\":2,\"path\":[0,2,3]}").passed);
    }

    @Test public void unreachableRequiresAbsenceOfPathAndEmptyWitness() {
        String input = "{\"nodes\":3,\"edges\":[[0,1,2]],\"source\":0,\"target\":2}";
        assertTrue(check("route", input, "{\"status\":\"unreachable\",\"path\":[]}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"unreachable\",\"path\":[]}").passed);
        assertFalse(check("route", input, "{\"status\":\"unreachable\",\"path\":[0]}").passed);
    }

    @Test public void sourceEqualsTargetRequiresZeroCostSingleton() {
        String input = "{\"nodes\":1,\"edges\":[],\"source\":0,\"target\":0}";
        assertTrue(check("route", input, "{\"status\":\"ok\",\"cost\":0,\"path\":[0]}").passed);
        assertFalse(check("route", input, "{\"status\":\"unreachable\",\"path\":[]}").passed);
    }

    @Test public void negativeCyclesAreRejectedEvenOutsideTheRequestedPath() {
        String input = "{\"nodes\":3,\"edges\":[[1,2,-1],[2,1,0]],\"source\":0,\"target\":0}";
        assertFalse(check("route", input, "{\"status\":\"ok\",\"cost\":0,\"path\":[0]}").passed);
    }

    @Test public void changedInputDoesNotReuseEarlierRouteResult() {
        JsonObject input = json(ROUTE);
        JsonObject output = json("{\"status\":\"ok\",\"cost\":5,\"path\":[0,2,1,3]}");
        assertTrue(ToolVerifier.verify("route", input, output, new OperationBudget(10_000)).passed);
        input.getAsJsonArray("edges").get(1).getAsJsonArray().set(2, JsonParser.parseString("20"));
        assertFalse(ToolVerifier.verify("route", input, output, new OperationBudget(10_000)).passed);
    }

    @Test public void knapsackRejectsFeasibleGreedyButSuboptimalSelection() {
        assertFalse(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":12,\"weight\":6,\"selected\":[0]}").passed);
        assertTrue(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":18,\"weight\":10,\"selected\":[1,2]}").passed);
    }

    @Test public void knapsackRejectsForgedValueWeightAndDuplicateIndices() {
        assertFalse(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":18,\"weight\":10,\"selected\":[0]}").passed);
        assertFalse(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":18,\"weight\":9,\"selected\":[1,2]}").passed);
        assertFalse(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":18,\"weight\":10,\"selected\":[1,1]}").passed);
    }

    @Test public void knapsackRejectsOverweightWitness() {
        assertFalse(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":21,\"weight\":11,\"selected\":[0,1]}").passed);
    }

    @Test public void knapsackAcceptsAnyOrderAndEquivalentOptimum() {
        assertTrue(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":18,\"weight\":10,\"selected\":[2,1]}").passed);
        String input = "{\"capacity\":1,\"items\":[{\"weight\":1,\"value\":5},{\"weight\":1,\"value\":5}]}";
        assertTrue(check("knapsack", input, "{\"status\":\"ok\",\"value\":5,\"weight\":1,\"selected\":[1]}").passed);
    }

    @Test public void emptyAndZeroCapacityKnapsackAreVerified() {
        assertTrue(check("knapsack", "{\"capacity\":0,\"items\":[]}", "{\"status\":\"ok\",\"value\":0,\"weight\":0,\"selected\":[]}").passed);
        assertTrue(check("knapsack", "{\"capacity\":0,\"items\":[{\"weight\":1,\"value\":9}]}", "{\"status\":\"ok\",\"value\":0,\"weight\":0,\"selected\":[]}").passed);
    }

    @Test public void knapsackRejectsInvalidWeightsAndHugeCollections() {
        String output = "{\"status\":\"ok\",\"value\":0,\"weight\":0,\"selected\":[]}";
        assertFalse(check("knapsack", "{\"capacity\":1,\"items\":[{\"weight\":0,\"value\":9}]}", output).passed);
        JsonObject input = json("{\"capacity\":1,\"items\":[]}");
        for (int i = 0; i < 15; i++) input.getAsJsonArray("items").add(json("{\"weight\":1,\"value\":0}"));
        assertFalse(ToolVerifier.verify("knapsack", input, json(output), new OperationBudget(10_000)).passed);
    }

    @Test public void scheduleFindsGlobalObjectiveRatherThanLargestSingleJob() {
        assertFalse(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":11,\"selected\":[0]}").passed);
        assertTrue(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":16,\"selected\":[1,2]}").passed);
    }

    @Test public void scheduleRejectsOverlapEvenWithCorrectClaimedObjective() {
        assertFalse(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":16,\"selected\":[0,1]}").passed);
        assertFalse(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":19,\"selected\":[0,1]}").passed);
    }

    @Test public void scheduleAllowsTouchingEndpointsAndUnorderedWitness() {
        assertTrue(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":16,\"selected\":[2,1]}").passed);
    }

    @Test public void scheduleRejectsDuplicateOrOutOfRangeJobs() {
        assertFalse(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":16,\"selected\":[1,1]}").passed);
        assertFalse(check("schedule", SCHEDULE, "{\"status\":\"ok\",\"value\":16,\"selected\":[1,3]}").passed);
    }

    @Test public void scheduleRejectsZeroLengthAndBackwardsIntervals() {
        String output = "{\"status\":\"ok\",\"value\":0,\"selected\":[]}";
        assertFalse(check("schedule", "{\"jobs\":[{\"start\":2,\"end\":2,\"value\":0}]}", output).passed);
        assertFalse(check("schedule", "{\"jobs\":[{\"start\":3,\"end\":2,\"value\":0}]}", output).passed);
    }

    @Test public void scheduleSupportsEmptyAndExtremeIntegerTimesWithoutOverflow() {
        assertTrue(check("schedule", "{\"jobs\":[]}", "{\"status\":\"ok\",\"value\":0,\"selected\":[]}").passed);
        String input = "{\"jobs\":[{\"start\":-2147483648,\"end\":0,\"value\":2},{\"start\":0,\"end\":2147483647,\"value\":3}]}";
        assertTrue(check("schedule", input, "{\"status\":\"ok\",\"value\":5,\"selected\":[0,1]}").passed);
    }

    @Test public void dependenciesVerifyOrderAndParallelLayersSeparately() {
        assertTrue(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[1,0,2,3],\"layers\":[[0,1],[2],[3]]}").passed);
        assertFalse(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[2,0,1,3],\"layers\":[[0,1],[2],[3]]}").passed);
        assertFalse(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[0,1,2,3],\"layers\":[[0,1,2],[3]]}").passed);
    }

    @Test public void dependenciesDoNotRequireMinimumDepthOrCanonicalOrdering() {
        assertTrue(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[1,0,2,3],\"layers\":[[1],[0],[2],[3]]}").passed);
    }

    @Test public void dependenciesRejectMissingAndRepeatedCoverage() {
        assertFalse(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[0,0,2,3],\"layers\":[[0,1],[2],[3]]}").passed);
        assertFalse(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[0,1,2,3],\"layers\":[[0],[2],[3]]}").passed);
        assertFalse(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[0,1,2,3],\"layers\":[[0,1],[1,2],[3]]}").passed);
    }

    @Test public void dependenciesRejectEmptyLayers() {
        assertFalse(check("dependencies", DAG, "{\"status\":\"ok\",\"order\":[0,1,2,3],\"layers\":[[0,1],[],[2],[3]]}").passed);
    }

    @Test public void dependenciesAcceptClosedCycleWitnessAndRejectFalseDag() {
        String input = "{\"nodes\":3,\"edges\":[[0,1],[1,2],[2,0]]}";
        assertTrue(check("dependencies", input, "{\"status\":\"cycle\",\"cycle\":[0,1,2,0]}").passed);
        assertFalse(check("dependencies", input, "{\"status\":\"ok\",\"order\":[0,1,2],\"layers\":[[0],[1],[2]]}").passed);
    }

    @Test public void dependenciesRejectOpenOrInventedCycle() {
        String input = "{\"nodes\":3,\"edges\":[[0,1],[1,2],[2,0]]}";
        assertFalse(check("dependencies", input, "{\"status\":\"cycle\",\"cycle\":[0,1,2]}").passed);
        assertFalse(check("dependencies", input, "{\"status\":\"cycle\",\"cycle\":[0,2,1,0]}").passed);
        assertFalse(check("dependencies", DAG, "{\"status\":\"cycle\",\"cycle\":[0,2,0]}").passed);
    }

    @Test public void dependenciesHandleSelfCycleAndParallelEdges() {
        assertTrue(check("dependencies", "{\"nodes\":1,\"edges\":[[0,0]]}", "{\"status\":\"cycle\",\"cycle\":[0,0]}").passed);
        assertTrue(check("dependencies", "{\"nodes\":2,\"edges\":[[0,1],[0,1]]}", "{\"status\":\"ok\",\"order\":[0,1],\"layers\":[[0],[1]]}").passed);
    }

    @Test public void strictNumericTypesRejectCoercionFractionsAndExponents() {
        for (String number : new String[]{"\"5\"", "5.0", "5e0", "true", "null", "999999999999999999999999"}) {
            String output = "{\"status\":\"ok\",\"cost\":" + number + ",\"path\":[0,2,1,3]}";
            assertFalse(number, check("route", ROUTE, output).passed);
        }
    }

    @Test public void unknownFieldsStatusesFamiliesAndNestedPayloadsAreRejected() {
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":5,\"path\":[0,2,1,3],\"execute\":\"shell\"}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"trusted\",\"path\":[]}").passed);
        assertFalse(check("python", "{}", "{}").passed);
        assertFalse(check("route", ROUTE, "{\"status\":\"ok\",\"cost\":5,\"path\":[{}]}").passed);
    }

    @Test public void invalidAndMissingFieldsAreFailuresRatherThanCrashes() {
        assertFalse(check("route", "{}", "{}").passed);
        assertFalse(ToolVerifier.verify("route", null, json("{}"), new OperationBudget(10)).passed);
        assertFalse(check("knapsack", PACK, "{\"status\":\"ok\",\"value\":18,\"weight\":10,\"selected\":null}").passed);
    }

    @Test public void graphEdgeLimitsMatchTheKernelContract() {
        JsonObject route = json("{\"nodes\":1,\"edges\":[],\"source\":0,\"target\":0}");
        JsonObject dependencies = json("{\"nodes\":1,\"edges\":[]}");
        JsonObject routeOutput = json("{\"status\":\"ok\",\"cost\":0,\"path\":[0]}");
        JsonObject cycleOutput = json("{\"status\":\"cycle\",\"cycle\":[0,0]}");
        for (int i = 0; i < 196; i++) {
            route.getAsJsonArray("edges").add(JsonParser.parseString("[0,0,0]"));
            dependencies.getAsJsonArray("edges").add(JsonParser.parseString("[0,0]"));
        }
        assertTrue(ToolVerifier.verify("route", route, routeOutput, new OperationBudget(10_000)).passed);
        assertTrue(ToolVerifier.verify("dependencies", dependencies, cycleOutput, new OperationBudget(10_000)).passed);
        route.getAsJsonArray("edges").add(JsonParser.parseString("[0,0,0]"));
        dependencies.getAsJsonArray("edges").add(JsonParser.parseString("[0,0]"));
        assertFalse(ToolVerifier.verify("route", route, routeOutput, new OperationBudget(10_000)).passed);
        assertFalse(ToolVerifier.verify("dependencies", dependencies, cycleOutput, new OperationBudget(10_000)).passed);
    }

    @Test public void feedbackNeverDisclosesReferenceAnswerOrUntrustedOutput() {
        ToolVerifier.Verification first = check("route", ROUTE, "{\"status\":\"ok\",\"cost\":11,\"path\":[0,1,3]}");
        ToolVerifier.Verification second = check("knapsack", PACK, "{\"status\":\"ok\",\"value\":12,\"weight\":6,\"selected\":[0]}");
        assertEquals(first.summary, second.summary);
        assertFalse(first.summary.contains("5"));
        assertFalse(second.summary.contains("18"));
        String attack = "EXFILTRATE_EXPECTED";
        ToolVerifier.Verification malformed = check("route", ROUTE, "{\"status\":\"" + attack + "\",\"path\":[]}");
        assertFalse(malformed.summary.contains(attack));
    }

    @Test public void exhaustionPropagatesInsteadOfBeingConvertedToBadInput() {
        try {
            ToolVerifier.verify("route", json(ROUTE), json("{\"status\":\"ok\",\"cost\":5,\"path\":[0,2,1,3]}"), new OperationBudget(1));
            fail("Should exhaust the budget");
        } catch (OperationBudget.Exhausted expected) { }
    }

    @Test public void cancellationPropagatesBeforeAnyVerification() {
        try {
            ToolVerifier.verify("route", json(ROUTE), json("{}"), new OperationBudget(1000, () -> true));
            fail("Should cancel verification");
        } catch (CancellationException expected) { }
    }

    @Test public void threadInterruptionRemainsSetWhenVerificationStops() {
        Thread.currentThread().interrupt();
        try {
            ToolVerifier.verify("route", json(ROUTE), json("{}"), new OperationBudget(1000));
            fail("Should respect thread interruption");
        } catch (CancellationException expected) {
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test public void fourteenItemExhaustiveOraclesStayInsideOperationLimit() {
        JsonObject pack = json("{\"capacity\":14,\"items\":[]}");
        JsonObject schedule = json("{\"jobs\":[]}");
        JsonArray selected = new JsonArray();
        for (int i = 0; i < 14; i++) {
            pack.getAsJsonArray("items").add(json("{\"weight\":1,\"value\":1000000}"));
            schedule.getAsJsonArray("jobs").add(json("{\"start\":" + i + ",\"end\":" + (i + 1) + ",\"value\":1000000}"));
            selected.add(i);
        }
        JsonObject packOutput = json("{\"status\":\"ok\",\"value\":14000000,\"weight\":14}");
        packOutput.add("selected", selected);
        JsonObject scheduleOutput = json("{\"status\":\"ok\",\"value\":14000000}");
        scheduleOutput.add("selected", selected);
        OperationBudget first = new OperationBudget(100_000);
        OperationBudget second = new OperationBudget(100_000);
        assertTrue(ToolVerifier.verify("knapsack", pack, packOutput, first).passed);
        assertTrue(ToolVerifier.verify("schedule", schedule, scheduleOutput, second).passed);
        assertTrue(first.getUsed() > 16_000);
        assertTrue(second.getUsed() > 16_000);
    }
}
