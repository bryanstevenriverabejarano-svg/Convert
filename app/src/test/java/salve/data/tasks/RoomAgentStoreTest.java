package salve.data.tasks;

import android.app.Application;
import android.content.Context;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.ModelResult;
import salve.core.agent.*;
import salve.data.db.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class RoomAgentStoreTest {
    Context context; MemoriaDatabase db; RoomAgentStore store; long now = 10000; boolean sync = true;
    @Before public void setup() { context = RuntimeEnvironment.getApplication(); context.deleteDatabase("agent-test"); open(); }
    void open() { db = MemoriaDatabase.builder(context, "agent-test").allowMainThreadQueries().build(); store = new RoomAgentStore(db, () -> sync); }
    @After public void close() { if (db != null) db.close(); context.deleteDatabase("agent-test"); }
    AgentRun create() { return store.create("Recuerda el origen de Salve", false, "", now); }
    int memories() { return db.recuerdoDao().filtrarRecuerdos("Resultado de plan solicitado").size(); }
    ModelResult response(String text) { return ModelResult.success(text, 1).withProvider("Dolphin de prueba"); }
    AgentEngine.Tools tools(String status) {
        return new AgentEngine.Tools() {
            public String capabilities(AgentRun run) { return "memory.lookup: local"; }
            public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call, java.util.function.BooleanSupplier stop) {
                return new AgentRun.Observation(call.id, call.tool, status, "El registro original data de 2025.");
            }
        };
    }
    AgentEngine.Model model() {
        return prompt -> {
            JsonObject data = JsonParser.parseString(prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (data.has("respuesta")) return response("{\"verdict\":\"pass\",\"issue\":\"\"}");
            if (data.getAsJsonArray("observaciones").isEmpty()) return response("{\"plan\":[\"Consultar memoria\"],\"tool\":\"memory.lookup\",\"input\":\"origen de Salve\"}");
            String id = data.getAsJsonArray("observaciones").get(0).getAsJsonObject().get("id").getAsString();
            return response("{\"answer\":\"Según el registro, 2025.\",\"evidence\":[\"" + id + "\"]}");
        };
    }
    AgentRun execute(String id, AgentEngine.Model model, AgentEngine.Tools tools) { return new AgentEngine(store, model, tools, () -> now).run(id, () -> false); }
    @Test public void plansActObserveReviewAndArchiveOnceWithRealProviderTrace() {
        AgentRun r = create(); AgentRun result = execute(r.id, model(), tools("SUCCESS"));
        assertEquals("SUCCEEDED", result.status); assertEquals(3, result.modelCalls); assertEquals(1, result.observations.size());
        assertEquals(3, result.providers.size()); assertTrue(result.providers.get(0).contains("Dolphin"));
        assertEquals(1, memories()); assertEquals(1, db.syncEventDao().getPending(20).size());
        assertEquals(1, db.agentDao().stat("memory.lookup").successes);
        execute(r.id, model(), tools("SUCCESS")); assertEquals(1, memories());
        assertEquals(0, db.recuerdoDao().perfiles().size());
        assertEquals(0, db.recuerdoDao().buscarIndice("\"salve\"", true, 4).size());
    }
    @Test public void partialEvidenceCannotBecomeFullSuccessByModelVote() {
        assertEquals("PARTIAL", execute(create().id, model(), tools("PARTIAL")).status); assertEquals(1, memories());
    }
    @Test public void unavailableModelBlocksWithoutInventingMemory() {
        AgentRun r = execute(create().id, p -> ModelResult.failure(ModelResult.Status.UNAVAILABLE, "test", 0), tools("SUCCESS"));
        assertEquals("BLOCKED", r.status); assertEquals(0, memories());
    }
    @Test public void malformedModelOutputStopsAtPersistentBudget() {
        AgentRun r = execute(create().id, p -> response("not JSON"), tools("SUCCESS"));
        assertEquals("PARTIAL", r.status); assertEquals(AgentEngine.MAX_MODEL_CALLS, r.modelCalls); assertEquals(0, memories());
    }
    @Test public void fabricatedEvidenceNeverFinishes() {
        AgentRun r = execute(create().id, p -> response("{\"answer\":\"Inventado\",\"evidence\":[\"fake\"]}"), tools("SUCCESS"));
        assertEquals("PARTIAL", r.status); assertTrue(r.answer.isEmpty()); assertEquals(0, memories());
    }
    @Test public void disallowedCodeBecomesObservationWithoutExecution() {
        AgentRun r = execute(create().id, p -> response("{\"tool\":\"code.python\",\"input\":\"print(42)\"}"), new AgentEngine.Tools() {
            public String capabilities(AgentRun r) { return "none"; }
            public AgentRun.Observation execute(AgentRun r, AgentRun.Pending p, java.util.function.BooleanSupplier stop) { throw new AssertionError("Must not execute"); }
        });
        assertEquals(8, r.observations.size()); assertEquals("BLOCKED", r.observations.get(0).status); assertEquals("PARTIAL", r.status);
    }
    @Test public void restartReusesPendingCallIdAndPersistedObservation() {
        AgentRun r = create(); r = store.claim(r.id, "old", now);
        r.pending = new AgentRun.Pending("same-call", "memory.lookup", "origen"); assertTrue(store.save(r, now));
        String id = r.id; db.close(); now += AgentEngine.LEASE_MS + 1; open(); AtomicInteger calls = new AtomicInteger();
        AgentRun result = execute(id, model(), new AgentEngine.Tools() {
            public String capabilities(AgentRun r) { return "memory.lookup"; }
            public AgentRun.Observation execute(AgentRun r, AgentRun.Pending call, java.util.function.BooleanSupplier stop) {
                assertEquals("same-call", call.id); calls.incrementAndGet(); return new AgentRun.Observation(call.id, call.tool, "SUCCESS", "2025");
            }
        });
        assertEquals("SUCCEEDED", result.status); assertEquals(1, calls.get());
        assertEquals(1, db.agentDao().stat("memory.lookup").successes);
    }
    @Test public void staleWorkerCannotSaveAfterLeaseTakeoverOrSharedPause() {
        AgentRun r = create(); AgentRun old = store.claim(r.id, "old", now);
        assertNull(store.claim(r.id, "new", now)); now += AgentEngine.LEASE_MS + 1;
        assertNotNull(store.claim(r.id, "new", now)); assertFalse(store.save(old, now));
        new RoomResearchTaskStore(db, () -> false).pause(true, now); store.pause(false, now);
        assertFalse(store.save(old, now)); assertNotNull(store.claim(r.id, "third", now));
    }
    @Test public void cancelDuringToolDropsLateResultAndExplicitResumeKeepsGrant() {
        AgentRun r = create();
        AgentRun result = execute(r.id, model(), new AgentEngine.Tools() {
            public String capabilities(AgentRun r) { return "memory.lookup"; }
            public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call, java.util.function.BooleanSupplier stop) {
                store.cancel(run.id, now); assertTrue(stop.getAsBoolean()); return new AgentRun.Observation(call.id, call.tool, "SUCCESS", "late");
            }
        });
        assertEquals("CANCELLED", result.status); assertTrue(result.observations.isEmpty()); assertEquals(0, memories());
        assertTrue(store.resume(r.id, now)); assertFalse(store.get(r.id).codeAllowed);
        assertEquals("SUCCEEDED", execute(r.id, model(), tools("SUCCESS")).status);
    }
    @Test public void outboxFailureRollsBackMemoryAndFinalState() {
        AgentRun r = store.claim(create().id, "owner", now); r.answer = "Resultado"; r.status = "SUCCEEDED";
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER reject_outbox BEFORE INSERT ON sync_events BEGIN SELECT RAISE(ABORT, 'test'); END");
        assertThrows(RuntimeException.class, () -> store.finish(r, now)); assertEquals(0, memories()); assertEquals("RUNNING", store.get(r.id).status);
        db.getOpenHelper().getWritableDatabase().execSQL("DROP TRIGGER reject_outbox"); assertTrue(store.finish(r, now)); assertEquals(1, memories());
    }
    @Test public void observedStatisticsAreNotCountedTwiceOnCheckpoint() {
        AgentRun r = store.claim(create().id, "owner", now);
        r.observations.add(new AgentRun.Observation("call", "web.search", "ERROR", "sin red"));
        assertTrue(store.save(r, now)); assertTrue(store.save(r, now)); assertEquals(1, db.agentDao().stat("web.search").failures);
    }
    @Test public void eventsAreDurableDeduplicatedPausedAndBackpressured() {
        AgentSubscriptionEntity s = store.subscribe("investigar Salve", "APP_OPEN", 3_600_000, "", now);
        assertEquals(1, store.signal("APP_OPEN", now).size()); assertTrue(store.signal("APP_OPEN", now).isEmpty());
        db.close(); open(); assertEquals(s.id, store.subscriptions().get(0).id);
        now += 3_600_000; store.pause(true, now); assertTrue(store.signal("APP_OPEN", now).isEmpty());
        store.pause(false, now); for (int i = 1; i < 20; i++) create();
        assertTrue(store.signal("APP_OPEN", now).isEmpty()); store.cancel(store.pending().get(0).id, now);
        assertEquals(1, store.signal("APP_OPEN", now).size());
        assertTrue(store.unsubscribe(s.id)); assertTrue(store.signal("APP_OPEN", now + 4_000_000).isEmpty());
    }
    @Test public void timerDoesNotFireEarlyOrReplayAllMissedHours() {
        store.subscribe("ciencia", "TIMER", 3_600_000, "", now);
        assertTrue(store.signal("TIMER", now).isEmpty()); now += 100 * 3_600_000L;
        assertEquals(1, store.signal("TIMER", now).size()); assertTrue(store.signal("TIMER", now).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> store.subscribe("x", "TIMER", 0, "", now));
    }
    @Test public void repeatedProcessDeathIsBoundedAndExplicitRetryResetsAttempts() {
        AgentRun r = create();
        for (int i = 0; i < 8; i++) { assertNotNull(store.claim(r.id, "owner" + i, now)); now += AgentEngine.LEASE_MS + 1; }
        assertNull(store.claim(r.id, "ninth", now)); assertEquals("BLOCKED", store.get(r.id).status);
        assertTrue(store.resume(r.id, now)); assertNotNull(store.claim(r.id, "retry", now));
    }
    @Test public void reviewerCanRequestCorrectionWithoutRepeatingCompletedTools() {
        AgentRun r = create(); AtomicInteger reviews = new AtomicInteger(); AgentEngine.Model normal = model();
        AgentEngine.Model corrective = p -> p.contains("\"respuesta\":") && reviews.getAndIncrement() == 0
                ? response("{\"verdict\":\"revise\",\"issue\":\"indica la fecha\"}") : normal.generate(p);
        assertEquals("QUEUED", execute(r.id, corrective, tools("SUCCESS")).status);
        AgentRun done = execute(r.id, corrective, tools("SUCCESS")); assertEquals("SUCCEEDED", done.status);
        assertEquals(1, done.observations.size()); assertEquals(1, done.revisions);
    }
    @Test public void smallFallbackContextStillProducesCompleteJsonWithinBudget() {
        AgentRun r = create(); AgentRun claimed = store.claim(r.id, "old", now);
        for (int i = 0; i < 8; i++) claimed.observations.add(new AgentRun.Observation("12345678-1234-1234-1234-1234567890a" + i, "memory.lookup", "SUCCESS", "\"\\\n".repeat(1900)));
        assertTrue(store.save(claimed, now)); store.release(r.id, "old", now);
        AgentEngine.Model small = new AgentEngine.Model() {
            public int promptBudget() { return 3200; }
            public ModelResult generate(String p) { assertTrue(p.length() <= 3040); return model().generate(p); }
        };
        assertEquals("SUCCEEDED", execute(r.id, small, tools("SUCCESS")).status);
    }
    @Test public void oversizedGoalIsBlockedRatherThanSilentlyChangingTheTask() {
        AgentRun r = store.create("x".repeat(1800), false, "", now);
        AgentEngine.Model small = new AgentEngine.Model() {
            public int promptBudget() { return 3200; }
            public ModelResult generate(String prompt) { throw new AssertionError("Truncated goal must never reach the model"); }
        };
        assertEquals("BLOCKED", execute(r.id, small, tools("SUCCESS")).status);
        assertEquals(0, memories()); assertEquals(1800, store.get(r.id).goal.length());
    }
    @Test public void localOnlyResultsDoNotEnterCloudOutbox() {
        sync = false; execute(create().id, model(), tools("SUCCESS")); assertEquals(1, memories()); assertTrue(db.syncEventDao().getPending(10).isEmpty());
    }
}
