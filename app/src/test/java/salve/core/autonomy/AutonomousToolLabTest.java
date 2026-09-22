package salve.core.autonomy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousToolLabTest {
    private static final class Store implements AutonomousToolLab.StateStore {
        String value; boolean fail; int writes;
        public String read() { return value; }
        public void write(String json) throws Exception {
            if (fail) throw new IOException("storage unavailable");
            value = json; writes++;
        }
    }
    private JsonObject challenge(String edges) {
        return JsonParser.parseString("{\"schema\":1,\"id\":\"route-test\",\"family\":\"route\",\"input\":{\"nodes\":3,\"source\":0,\"target\":2,\"edges\":"+edges+"}}").getAsJsonObject();
    }
    private JsonObject solve(AutonomousToolLab lab, String edges) { return lab.solve(challenge(edges), () -> false); }
    private String strategy(JsonObject report) { return ToolProgram.strategy(report.getAsJsonObject("program")); }

    @Test public void createsExecutableToolAndAdaptsAfterFailedGreedyCandidate() {
        Store store = new Store(); AutonomousToolLab lab = new AutonomousToolLab(store);
        JsonObject report = solve(lab, "[[0,2,10],[0,1,1],[1,2,1]]");
        assertTrue(report.get("verified").getAsBoolean()); assertTrue(report.get("promoted").getAsBoolean());
        assertTrue(report.get("adapted").getAsBoolean()); assertTrue(report.getAsJsonArray("attempts").size()>1);
        assertEquals(2, report.getAsJsonObject("result").get("cost").getAsInt());
        assertEquals("symbolic", report.get("synthesis").getAsString());
        assertEquals("salve-tools/1", report.getAsJsonObject("program").get("language").getAsString());
        assertEquals(1, store.writes);
    }
    @Test public void persistedToolIsRecheckedAgainstChangedInputsAfterRestart() {
        Store store = new Store(); AutonomousToolLab lab = new AutonomousToolLab(store);
        JsonObject first = solve(lab, "[[0,2,10],[0,1,1],[1,2,1]]");
        assertEquals("DIJKSTRA", strategy(first));
        AutonomousToolLab restarted = new AutonomousToolLab(store);
        JsonObject next = solve(restarted, "[[0,2,10],[0,1,1],[1,2,-5]]");
        assertTrue(next.get("verified").getAsBoolean()); assertTrue(next.get("adapted").getAsBoolean());
        assertTrue(next.get("previousRejected").getAsBoolean());
        assertEquals(-4, next.getAsJsonObject("result").get("cost").getAsInt());
        assertEquals("BELLMAN_FORD", strategy(next));
        assertEquals(2, next.get("version").getAsInt());
        assertTrue(next.getAsJsonArray("attempts").get(next.getAsJsonArray("attempts").size()-1)
                .getAsJsonObject().get("regressionChecks").getAsInt()>0);
    }
    @Test public void reusesProgramButNeverReusesAnOldAnswer() {
        AutonomousToolLab lab = new AutonomousToolLab();
        solve(lab, "[[0,1,1],[1,2,1]]");
        JsonObject next = solve(lab, "[[0,1,5],[1,2,7]]");
        assertTrue(next.get("reused").getAsBoolean()); assertEquals(12,next.getAsJsonObject("result").get("cost").getAsInt());
    }
    @Test public void invalidInputDoesNotPromoteOrErasePreviousVersion() {
        Store store = new Store(); AutonomousToolLab lab = new AutonomousToolLab(store);
        solve(lab,"[[0,1,1],[1,2,1]]"); String before=store.value;
        JsonObject report=solve(lab,"[[0,99,1]]");
        assertFalse(report.get("verified").getAsBoolean()); assertFalse(report.get("promoted").getAsBoolean());
        assertEquals(before,store.value);
    }
    @Test public void pauseCancelsWithoutChangingStateAndResumeWorks() {
        Store store = new Store(); AutonomousToolLab lab = new AutonomousToolLab(store);
        lab.pause(); JsonObject report=solve(lab,"[[0,2,1]]");
        assertEquals("cancelled",report.get("status").getAsString()); assertNull(store.value);
        lab.resume(); assertTrue(solve(lab,"[[0,2,1]]").get("verified").getAsBoolean());
    }
    @Test public void cancellationDuringExecutionCannotPromotePartialWork() {
        Store store=new Store(); AutonomousToolLab lab=new AutonomousToolLab(store);
        int[] ticks={0};
        JsonObject report=lab.solve(challenge("[[0,2,10],[0,1,1],[1,2,1]]"),()->++ticks[0]>8);
        assertEquals("cancelled",report.get("status").getAsString()); assertFalse(report.get("verified").getAsBoolean());
        assertNull(store.value);
        for (com.google.gson.JsonElement attempt: report.getAsJsonArray("attempts"))
            assertFalse(attempt.getAsJsonObject().get("passed").getAsBoolean());
    }
    @Test public void storageFailureKeepsVerifiedResultSeparateFromPromotion() {
        Store store=new Store(); store.fail=true;
        JsonObject report=solve(new AutonomousToolLab(store),"[[0,2,1]]");
        assertTrue(report.get("verified").getAsBoolean()); assertFalse(report.get("promoted").getAsBoolean());
        assertEquals("storage_error",report.get("status").getAsString()); assertNull(store.value);
    }
    @Test public void corruptJournalDoesNotBecomeEmptySuccessfulInstallation() {
        Store store=new Store(); store.value="{}";
        assertThrows(IllegalStateException.class,()->new AutonomousToolLab(store)); assertEquals(0,store.writes);
    }
    @Test public void rollbackRestoresOnlyAProgramAndRevalidatesIt() {
        Store store=new Store(); AutonomousToolLab lab=new AutonomousToolLab(store);
        solve(lab,"[[0,2,10],[0,1,1],[1,2,1]]");
        solve(lab,"[[0,2,10],[0,1,1],[1,2,-5]]");
        assertTrue(lab.rollback("route").contains("restaurada"));
        JsonObject result=solve(lab,"[[0,2,10],[0,1,1],[1,2,-5]]");
        assertTrue(result.get("verified").getAsBoolean()); assertEquals(-4,result.getAsJsonObject("result").get("cost").getAsInt());
    }
    @Test public void generatedProgramsCannotRemoveVerificationOrCallShell() {
        JsonObject program=ToolProgram.generate("route","DIJKSTRA");
        program.getAsJsonArray("steps").get(2).getAsJsonObject().addProperty("op","shell");
        assertThrows(IllegalArgumentException.class,()->ToolInterpreter.run(program,
                challenge("[[0,2,1]]").getAsJsonObject("input"),new OperationBudget(10000)));
    }
    @Test public void candidateBudgetAndInterruptedThreadStopExecution() {
        assertThrows(OperationBudget.Exhausted.class,()->ToolInterpreter.run(ToolProgram.generate("route","DIJKSTRA"),
                challenge("[[0,2,1]]").getAsJsonObject("input"),new OperationBudget(1)));
        Thread.currentThread().interrupt();
        try { assertThrows(CancellationException.class,()->new OperationBudget(2).tick()); }
        finally { Thread.interrupted(); }
    }
    @Test public void journalAndRegressionWindowStayBounded() {
        Store store=new Store(); AutonomousToolLab lab=new AutonomousToolLab(store);
        for(int i=1;i<=30;i++) assertTrue(solve(lab,"[[0,2,"+i+"]]").get("verified").getAsBoolean());
        JsonObject state=JsonParser.parseString(store.value).getAsJsonObject();
        assertEquals(24,state.getAsJsonArray("receipts").size());
        assertEquals(3,state.getAsJsonObject("regressions").getAsJsonArray("route").size());
        assertTrue(new AutonomousToolLab(store).describe().contains("route"));
    }
    @Test public void unknownFamilyCannotCreateCapabilities() {
        JsonObject request=challenge("[]"); request.addProperty("family","exec_shell");
        Store store=new Store(); JsonObject report=new AutonomousToolLab(store).solve(request,()->false);
        assertFalse(report.get("verified").getAsBoolean()); assertEquals(0,store.writes);
    }

    @Test public void restoreRejectsCorruptRegressionDataWithoutWritingOrErasing() {
        Store baseline = new Store();
        solve(new AutonomousToolLab(baseline), "[[0,2,1]]");
        String[] corruptInputs = {
                "null", "{}", "[]",
                "{\"nodes\":3,\"source\":0,\"target\":2,\"edges\":[[0,99,1]]}",
                "{\"nodes\":3,\"source\":0,\"target\":2,\"edges\":[[0,1,-2],[1,0,1]]}"
        };
        for (String input : corruptInputs) {
            Store corrupt = new Store();
            JsonObject journal = JsonParser.parseString(baseline.value).getAsJsonObject();
            journal.getAsJsonObject("regressions").getAsJsonArray("route").add(JsonParser.parseString(input));
            corrupt.value = journal.toString();
            String before = corrupt.value;
            assertThrows(input, IllegalStateException.class, () -> new AutonomousToolLab(corrupt));
            assertEquals(before, corrupt.value);
            assertEquals(0, corrupt.writes);
        }
    }

    @Test public void restoreValidatesEveryRegressionFamilyAndItsLimits() {
        String[] families = {"knapsack", "schedule", "dependencies"};
        String[] inputs = {
                "{\"capacity\":101,\"items\":[]}",
                "{\"jobs\":[{\"start\":1,\"end\":1,\"value\":2}]}",
                "{\"nodes\":2,\"edges\":[[0,2]]}"
        };
        for (int i = 0; i < families.length; i++) {
            Store corrupt = new Store();
            JsonObject journal = JsonParser.parseString("{\"schema\":1,\"revision\":0,\"tools\":{},\"regressions\":{},\"receipts\":[]}").getAsJsonObject();
            com.google.gson.JsonArray cases = new com.google.gson.JsonArray();
            cases.add(JsonParser.parseString(inputs[i]));
            journal.getAsJsonObject("regressions").add(families[i], cases);
            corrupt.value = journal.toString();
            assertThrows(IllegalStateException.class, () -> new AutonomousToolLab(corrupt));
            assertEquals(0, corrupt.writes);
        }
    }

    @Test public void restoredRegressionNeedNotBeSolvableByRolledBackProgram() {
        Store store = new Store();
        AutonomousToolLab lab = new AutonomousToolLab(store);
        solve(lab, "[[0,2,10],[0,1,1],[1,2,1]]");
        solve(lab, "[[0,2,10],[0,1,1],[1,2,-5]]");
        assertTrue(lab.rollback("route").contains("restaurada"));
        AutonomousToolLab restarted = new AutonomousToolLab(store);
        JsonObject report = solve(restarted, "[[0,2,10],[0,1,1],[1,2,-5]]");
        assertTrue(report.get("verified").getAsBoolean());
        assertEquals("BELLMAN_FORD", strategy(report));
    }

    @Test public void restoredCountersRequireExactNonnegativeIntegerNumbers() {
        Store baseline = new Store();
        solve(new AutonomousToolLab(baseline), "[[0,2,1]]");
        for (String invalid : new String[]{"\"1\"", "1.5", "1e0", "-1", "null", "true", "9223372036854775808"}) {
            for (String field : new String[]{"revision", "version"}) {
                Store corrupt = new Store();
                JsonObject journal = JsonParser.parseString(baseline.value).getAsJsonObject();
                JsonObject target = "revision".equals(field) ? journal
                        : journal.getAsJsonObject("tools").getAsJsonObject("route");
                target.add(field, JsonParser.parseString(invalid));
                corrupt.value = journal.toString();
                assertThrows(field + "=" + invalid, IllegalStateException.class, () -> new AutonomousToolLab(corrupt));
                assertEquals(0, corrupt.writes);
            }
        }
    }

    @Test public void exhaustedRevisionCannotWrapOrMakeJournalUnreadable() {
        Store store = new Store();
        solve(new AutonomousToolLab(store), "[[0,2,1]]");
        JsonObject journal = JsonParser.parseString(store.value).getAsJsonObject();
        journal.addProperty("revision", Long.MAX_VALUE);
        store.value = journal.toString();
        String before = store.value;
        int writes = store.writes;
        AutonomousToolLab restored = new AutonomousToolLab(store);
        JsonObject report = solve(restored, "[[0,2,2]]");
        assertEquals("storage_error", report.get("status").getAsString());
        assertFalse(report.get("promoted").getAsBoolean());
        assertEquals(before, store.value);
        assertEquals(writes, store.writes);
        assertTrue(new AutonomousToolLab(store).describe().contains("route"));
    }

    @Test public void exhaustedVersionCannotOverflowDuringPromotionOrRollback() {
        Store store = new Store();
        solve(new AutonomousToolLab(store), "[[0,2,1]]");
        JsonObject journal = JsonParser.parseString(store.value).getAsJsonObject();
        JsonObject route = journal.getAsJsonObject("tools").getAsJsonObject("route");
        route.addProperty("version", Long.MAX_VALUE);
        route.add("previous", ToolProgram.generate("route", "DIJKSTRA"));
        store.value = journal.toString();
        String before = store.value;
        int writes = store.writes;
        AutonomousToolLab restored = new AutonomousToolLab(store);
        JsonObject report = solve(restored, "[[0,2,10],[0,1,1],[1,2,1]]");
        assertEquals("storage_error", report.get("status").getAsString());
        assertFalse(report.get("promoted").getAsBoolean());
        assertTrue(restored.rollback("route").contains("límite"));
        assertEquals(before, store.value);
        assertEquals(writes, store.writes);
        assertTrue(new AutonomousToolLab(store).describe().contains("route"));
    }

    @Test public void lastRepresentableRevisionRemainsReadableAfterIncrement() {
        Store store = new Store();
        solve(new AutonomousToolLab(store), "[[0,2,1]]");
        JsonObject journal = JsonParser.parseString(store.value).getAsJsonObject();
        journal.addProperty("revision", Long.MAX_VALUE - 1L);
        store.value = journal.toString();
        JsonObject report = solve(new AutonomousToolLab(store), "[[0,2,2]]");
        assertTrue(report.get("promoted").getAsBoolean());
        assertEquals(Long.MAX_VALUE, JsonParser.parseString(store.value).getAsJsonObject().get("revision").getAsLong());
        assertTrue(new AutonomousToolLab(store).describe().contains("route"));
    }
}
