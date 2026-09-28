package salve.core.autonomy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.autonomy.VerifiedStrategyPolicy;
import static org.junit.Assert.*;

public class ToolLearningIntegrationTest {
    private static final class Store implements AutonomousToolLab.StateStore {
        String value; boolean fail; int writes;
        public String read() { return value; }
        public void write(String json) throws Exception {
            if (fail) throw new IOException("unavailable");
            value = json; writes++;
        }
    }
    private static JsonObject challenge(int cost) {
        return JsonParser.parseString("{\"schema\":1,\"id\":\"learning-" + cost
                + "\",\"family\":\"route\",\"input\":{\"nodes\":3,\"source\":0,\"target\":2,\"edges\":[[0,2,"
                + (cost + 10) + "],[0,1,1],[1,2," + cost + "]]}}").getAsJsonObject();
    }
    @Test public void liveSolvePersistsVerifiedObservationsAndSeparatesProbeCost() {
        Store store = new Store();
        JsonObject report = new AutonomousToolLab(store).solve(challenge(2), () -> false);
        assertTrue(report.get("verified").getAsBoolean());
        assertTrue(report.get("promoted").getAsBoolean());
        assertTrue(report.get("experimentalLearning").getAsBoolean());
        assertTrue(report.get("learningOperations").getAsInt() <= ToolLearningSupport.PROBE_OPERATIONS);
        int total = report.get("learningOperations").getAsInt();
        for (com.google.gson.JsonElement item : report.getAsJsonArray("attempts"))
            total += item.getAsJsonObject().get("operations").getAsInt();
        assertEquals(total, report.get("operations").getAsInt());
        assertEquals(1, store.writes);
        JsonObject state = JsonParser.parseString(store.value).getAsJsonObject();
        assertTrue(ToolLearningSupport.policy(state).size() >= 2);
        assertTrue(new AutonomousToolLab(store).learningStatus().contains("activo"));
    }
    @Test public void disablingPersistsAndDoesNotSilentlyTrain() {
        Store store = new Store();
        AutonomousToolLab lab = new AutonomousToolLab(store);
        lab.setExperimentalLearning(false);
        JsonObject report = new AutonomousToolLab(store).solve(challenge(2), () -> false);
        assertTrue(report.get("verified").getAsBoolean());
        assertFalse(report.get("experimentalLearning").getAsBoolean());
        assertFalse(report.has("learningProbe"));
        assertEquals(0, report.get("learningOperations").getAsInt());
        assertFalse(JsonParser.parseString(store.value).getAsJsonObject().has("learningPolicy"));
    }
    @Test public void storageFailureCannotPersistNewPolicyOrDiscardPrevious() {
        Store store = new Store(); AutonomousToolLab lab = new AutonomousToolLab(store);
        lab.solve(challenge(1), () -> false);
        String before = store.value;
        store.fail = true;
        JsonObject report = lab.solve(challenge(2), () -> false);
        assertTrue(report.get("verified").getAsBoolean());
        assertFalse(report.get("promoted").getAsBoolean());
        assertEquals(before, store.value);
    }
    @Test public void cancellationInProbeCannotCommitAnOtherwiseSuccessfulCandidate() {
        Store measurement = new Store(); AutonomousToolLab baseline = new AutonomousToolLab(measurement);
        baseline.setExperimentalLearning(false);
        JsonObject done = baseline.solve(challenge(2), () -> false);
        int mainOperations = done.get("operations").getAsInt();
        Store store = new Store(); AtomicInteger ticks = new AtomicInteger();
        JsonObject report = new AutonomousToolLab(store).solve(challenge(2),
                () -> ticks.incrementAndGet() > mainOperations + 3);
        assertEquals("cancelled", report.get("status").getAsString());
        assertFalse(report.get("promoted").getAsBoolean());
        assertNull(store.value);
    }
    @Test public void legacyStateRemainsReadableAndCorruptPolicyIsNotErased() {
        Store store = new Store();
        store.value = "{\"schema\":1,\"revision\":0,\"tools\":{},\"regressions\":{},\"receipts\":[]}";
        assertTrue(new AutonomousToolLab(store).solve(challenge(2), () -> false).get("verified").getAsBoolean());
        JsonObject corrupt = JsonParser.parseString(store.value).getAsJsonObject();
        corrupt.addProperty("learningPolicy", "invalid"); store.value = corrupt.toString();
        String before = store.value;
        assertThrows(IllegalStateException.class, () -> new AutonomousToolLab(store));
        assertEquals(before, store.value);
    }
    @Test public void sameInputWithReorderedObjectKeysHasSameProvenance() {
        JsonObject a = JsonParser.parseString("{\"x\":1,\"y\":[2,3]}").getAsJsonObject();
        JsonObject b = JsonParser.parseString("{\"y\":[2,3],\"x\":1}").getAsJsonObject();
        assertEquals(ToolLearningSupport.fingerprint(a), ToolLearningSupport.fingerprint(b));
        assertNotEquals(ToolLearningSupport.trialId(a, null),
                ToolLearningSupport.trialId(a, JsonParser.parseString("[{\"x\":1}]").getAsJsonArray()));
    }
    @Test public void ablationReportsTotalCostsWithoutAssumingTheExperimentWins() {
        Store off = new Store(), on = new Store();
        AutonomousToolLab baseline = new AutonomousToolLab(off), experiment = new AutonomousToolLab(on);
        baseline.setExperimentalLearning(false); experiment.setExperimentalLearning(true);
        long baselineOps = 0, experimentOps = 0, probeOps = 0; int reordered = 0;
        for (int i = 1; i <= 24; i++) {
            JsonObject a = baseline.solve(challenge(i), () -> false);
            JsonObject b = experiment.solve(challenge(i), () -> false);
            assertTrue(a.get("verified").getAsBoolean()); assertTrue(b.get("verified").getAsBoolean());
            assertEquals(a.getAsJsonObject("result").get("cost"), b.getAsJsonObject("result").get("cost"));
            baselineOps += a.get("operations").getAsInt(); experimentOps += b.get("operations").getAsInt();
            probeOps += b.get("learningOperations").getAsInt();
            if (b.get("policyReordered").getAsBoolean()) reordered++;
        }
        System.out.println("SALVE_LEARNING_ABLATION tasks=24 baseline_ops=" + baselineOps
                + " experimental_total_ops=" + experimentOps + " probe_ops=" + probeOps
                + " reordered=" + reordered + " (finite DSL tasks, not LLM intelligence)");
        assertTrue(ToolLearningSupport.policy(JsonParser.parseString(on.value).getAsJsonObject()).size()
                <= VerifiedStrategyPolicy.MAX_OBSERVATIONS);
    }
}
