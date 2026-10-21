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
    @Test public void restoredSubmissionIsIdempotentEvenAfterCompletionAndReopeningDatabase() {
        String id = java.util.UUID.randomUUID().toString();
        AgentRun first = store.createWithId(id, "Recuerda el origen de Salve", false, "", now);
        execute(first.id, model(), tools("SUCCESS"));
        db.close(); open();
        AgentRun retry = store.createWithId(id, first.goal, false, "https://new.example.org", now + 1000);
        assertEquals("SUCCEEDED", retry.status); assertEquals("", retry.bridge);
        assertEquals(1, store.recent().size()); assertEquals(1, memories());
        assertEquals(3, retry.modelCalls); assertEquals(1, retry.observations.size());
        assertThrows(IllegalArgumentException.class, () -> store.createWithId(id, "Objetivo distinto", false, "", now));
        assertThrows(IllegalArgumentException.class, () -> store.createWithId(id, first.goal, true, "", now));
    }
    @Test public void retryDoesNotConsumeAnotherPendingSlot() {
        AgentRun first = create();
        for (int i = 1; i < 20; i++) create();
        assertEquals(first.id, store.createWithId(first.id, first.goal, false, "", now).id);
        assertEquals(20, store.pending().size());
        assertThrows(IllegalArgumentException.class, this::create);
    }
    @Test public void restartedInferenceReceivesSavedOutlineAndRemainingBudgets() {
        AgentRun r = create(); AgentRun claimed = store.claim(r.id, "old", now);
        claimed.outline.add("Consultar el registro original"); claimed.modelCalls = 2;
        claimed.observations.add(new AgentRun.Observation("existing", "memory.lookup", "SUCCESS", "Registro de 2025"));
        assertTrue(store.save(claimed, now)); store.release(r.id, "old", now);
        db.close(); open();
        AtomicInteger calls = new AtomicInteger();
        AgentRun done = execute(r.id, p -> {
            JsonObject data = JsonParser.parseString(p.substring(p.indexOf("DATOS=") + 6)).getAsJsonObject();
            assertEquals("Consultar el registro original", data.getAsJsonArray("plan_actual").get(0).getAsString());
            assertEquals(7, data.get("herramientas_restantes").getAsInt());
            assertEquals(10 - calls.getAndIncrement(), data.get("inferencias_restantes").getAsInt());
            return model().generate(p);
        }, tools("SUCCESS"));
        assertEquals("SUCCEEDED", done.status); assertEquals(1, done.observations.size());
    }
    @Test public void twoPublicReadStepsAreVerifiedAndPersistBothControllerReceipts() {
        String first = "https://example.org/alpha-unseen";
        String second = "https://example.net/beta-unseen";
        AgentRun created = store.create("Lee " + first + " y " + second, false, "", now);
        AgentEngine.Model model = prompt -> {
            JsonObject data = JsonParser.parseString(
                    prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (data.has("respuesta"))
                return response("{\"verdict\":\"pass\",\"issue\":\"\"}");
            com.google.gson.JsonArray observations = data.getAsJsonArray("observaciones");
            if (observations.size() == 0)
                return response("{\"tool\":\"web.read\",\"input\":\"" + first + "\"}");
            if (observations.size() == 1)
                return response("{\"tool\":\"web.read\",\"input\":\"" + second + "\"}");
            String one = observations.get(0).getAsJsonObject().get("id").getAsString();
            String two = observations.get(1).getAsJsonObject().get("id").getAsString();
            return response("{\"answer\":\"He leído ambas fuentes públicas.\",\"evidence\":[\""
                    + one + "\",\"" + two + "\"]}");
        };
        AgentEngine.Tools tools = new AgentEngine.Tools() {
            public String capabilities(AgentRun run) { return "web.read"; }
            public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call,
                                                java.util.function.BooleanSupplier stop) {
                assertEquals("web.read", call.tool);
                assertTrue(call.input.equals(first) || call.input.equals(second));
                AgentRun.Observation observation = new AgentRun.Observation(call.id, call.tool,
                        "SUCCESS", "Páginas públicas recibidas");
                observation.sources.add(call.input);
                return observation;
            }
        };
        AgentRun result = execute(created.id, model, tools);
        assertEquals("SUCCEEDED", result.status);
        assertEquals("VERIFIED", result.goalVerification);
        assertEquals("PUBLIC_READ_SET", result.verificationCriterion);
        assertEquals(2, result.verificationEvidenceIds.size());
        assertEquals(result.observations.get(0).id, result.verificationEvidenceIds.get(0));
        assertEquals(result.observations.get(1).id, result.verificationEvidenceIds.get(1));
        assertEquals(2, result.observations.size());
        db.close(); open();
        AgentRun restored = store.get(created.id);
        assertEquals("VERIFIED", restored.goalVerification);
        assertEquals(result.verificationEvidenceIds, restored.verificationEvidenceIds);
        assertEquals(1, memories());
    }

    @Test public void arithmeticOutputOracleSurvivesRoomAndCannotBeOverriddenByModelPass() {
        String goal = "Calcula -491 * 37";
        AgentRun created = store.create(goal, false, "", now);
        AgentEngine.Tools math = new AgentEngine.Tools() {
            @Override public String capabilities(AgentRun run) { return "math.compute: local"; }
            @Override public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call,
                    java.util.function.BooleanSupplier stop) {
                assertEquals("math.compute", call.tool);
                assertEquals(goal, call.input);
                long result = AgentArithmeticTask.parse(run.goal).calculateLocally();
                return new AgentRun.Observation(call.id, call.tool, "SUCCESS",
                        "RESULTADO_ARITMETICO=" + result);
            }
        };
        AgentEngine.Model model = prompt -> {
            JsonObject data = JsonParser.parseString(
                    prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (data.has("respuesta"))
                return response("{\"verdict\":\"pass\",\"issue\":\"\"}");
            com.google.gson.JsonArray observations = data.getAsJsonArray("observaciones");
            if (observations.isEmpty())
                return response("{\"tool\":\"math.compute\",\"input\":\"" + goal + "\"}");
            String receipt = observations.get(0).getAsJsonObject().get("id").getAsString();
            return response("{\"answer\":\"-18167\",\"evidence\":[\"" + receipt + "\"]}");
        };
        AgentRun done = execute(created.id, model, math);
        assertEquals("SUCCEEDED", done.status);
        assertEquals("VERIFIED", done.goalVerification);
        assertEquals("ARITHMETIC_EXACT", done.verificationCriterion);
        assertEquals(done.observations.get(0).id, done.verificationEvidence);
        db.close(); open();
        AgentRun restored = store.get(created.id);
        assertEquals("VERIFIED", restored.goalVerification);
        assertEquals(done.verificationEvidence, restored.verificationEvidence);
    }

    @Test public void modelPassCannotMarkWrongArithmeticAnswerSucceeded() {
        String goal = "Calcula 125 + 87";
        AgentRun created = store.create(goal, false, "", now);
        AgentEngine.Model wrong = prompt -> {
            JsonObject data = JsonParser.parseString(
                    prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (data.has("respuesta"))
                return response("{\"verdict\":\"pass\",\"issue\":\"\"}");
            com.google.gson.JsonArray observations = data.getAsJsonArray("observaciones");
            if (observations.isEmpty())
                return response("{\"tool\":\"math.compute\",\"input\":\"" + goal + "\"}");
            String receipt = observations.get(0).getAsJsonObject().get("id").getAsString();
            return response("{\"answer\":\"211\",\"evidence\":[\"" + receipt + "\"]}");
        };
        AgentEngine.Tools math = new AgentEngine.Tools() {
            @Override public String capabilities(AgentRun run) { return "math.compute"; }
            @Override public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call,
                    java.util.function.BooleanSupplier stop) {
                return new AgentRun.Observation(call.id, call.tool, "SUCCESS",
                        "RESULTADO_ARITMETICO=212");
            }
        };
        AgentRun finished = execute(created.id, wrong, math);
        assertEquals("PARTIAL", finished.status);
        assertEquals("NOT_VERIFIED", finished.goalVerification);
        assertTrue(finished.error.contains("aritmética independiente"));
        assertEquals(1, finished.observations.size());
    }

    @Test public void directPublicReadIsIndependentlyVerifiedAndSurvivesRestart() {
        String url = "https://example.org/manual";
        AgentRun created = store.create("Lee " + url, false, "", now);
        AgentEngine.Model model = prompt -> {
            JsonObject data = JsonParser.parseString(
                    prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (data.has("respuesta"))
                return response("{\"verdict\":\"pass\",\"issue\":\"\"}");
            if (data.getAsJsonArray("observaciones").isEmpty())
                return response("{\"tool\":\"web.read\",\"input\":\"" + url + "\"}");
            String id = data.getAsJsonArray("observaciones").get(0).getAsJsonObject()
                    .get("id").getAsString();
            return response("{\"answer\":\"He leído el documento.\",\"evidence\":[\"" + id + "\"]}");
        };
        AgentEngine.Tools tools = new AgentEngine.Tools() {
            public String capabilities(AgentRun run) { return "web.read"; }
            public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call,
                                                java.util.function.BooleanSupplier stopped) {
                assertEquals("web.read", call.tool);
                assertEquals(url, call.input);
                AgentRun.Observation o = new AgentRun.Observation(call.id, call.tool,
                        "SUCCESS", "Página recibida");
                o.sources.add(url);
                return o;
            }
        };
        AgentRun finished = execute(created.id, model, tools);
        assertEquals("SUCCEEDED", finished.status);
        assertEquals("VERIFIED", finished.goalVerification);
        assertEquals("PUBLIC_READ", finished.verificationCriterion);
        assertEquals(finished.evidence.get(0), finished.verificationEvidence);
        db.close(); open();
        AgentRun restored = store.get(created.id);
        assertEquals("VERIFIED", restored.goalVerification);
        assertEquals(finished.verificationEvidence, restored.verificationEvidence);
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
        assertEquals(1, r.observations.size()); assertEquals("BLOCKED", r.observations.get(0).status); assertEquals("PARTIAL", r.status);
        assertEquals(11, r.skippedRepeats); assertEquals(1, db.agentDao().stat("code.python").failures);
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
    @Test public void agentReceivesControllerOwnedAutonomousChoiceAtEachStep() {
        AgentRun job = create(); AtomicInteger steps = new AtomicInteger();
        AgentEngine.Model selected = prompt -> {
            JsonObject data = JsonParser.parseString(
                    prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (!data.has("respuesta")) {
                JsonObject choice = data.getAsJsonObject("eleccion_autonoma");
                assertNotNull("El contexto normal debe incluir la elección", choice);
                assertEquals(steps.getAndIncrement() == 0
                                ? "GATHER_EVIDENCE" : "VERIFY_AND_SYNTHESIZE",
                        choice.get("next").getAsString());
                assertFalse(choice.toString().contains("origen de Salve"));
            }
            return model().generate(prompt);
        };
        AgentRun done = execute(job.id, selected, tools("SUCCESS"));
        assertEquals("SUCCEEDED", done.status);
        assertEquals(2, steps.get());
        assertEquals(1, done.observations.size());
    }

    @Test public void failedResearchAutonomouslyTestsOnlyAuthorizedPublicUrlAndKeepsProof() {
        String goal = "Investiga https://example.org/study";
        AgentRun plan = store.create(goal, false, "", now);
        AtomicInteger searches = new AtomicInteger(), reads = new AtomicInteger();
        AgentEngine.Model model = prompt -> {
            JsonObject data = JsonParser.parseString(
                    prompt.substring(prompt.indexOf("DATOS=") + 6)).getAsJsonObject();
            if (data.has("respuesta")) return response("{\"verdict\":\"pass\",\"issue\":\"\"}");
            for (com.google.gson.JsonElement item : data.getAsJsonArray("observaciones")) {
                JsonObject observation = item.getAsJsonObject();
                if ("web.read".equals(observation.get("tool").getAsString())
                        && "SUCCESS".equals(observation.get("status").getAsString())) {
                    return response("{\"answer\":\"He leído la fuente pública indicada.\",\"evidence\":[\""
                            + observation.get("id").getAsString() + "\"]}");
                }
            }
            return response("{\"tool\":\"web.search\",\"input\":\"" + goal + "\"}");
        };
        AgentEngine.Tools tools = new AgentEngine.Tools() {
            public String capabilities(AgentRun run) { return "web.search, web.read"; }
            public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call,
                                                 java.util.function.BooleanSupplier stopped) {
                if ("web.search".equals(call.tool)) {
                    searches.incrementAndGet();
                    return new AgentRun.Observation(call.id, call.tool, "ERROR", "Buscador no disponible");
                }
                assertEquals("web.read", call.tool);
                assertEquals("https://example.org/study", call.input);
                reads.incrementAndGet();
                AgentRun.Observation observed = new AgentRun.Observation(
                        call.id, call.tool, "SUCCESS", "Contenido público recibido");
                observed.sources.add(call.input);
                return observed;
            }
        };
        AgentRun result = execute(plan.id, model, tools);
        assertEquals("SUCCEEDED", result.status);
        assertEquals(2, searches.get());
        assertEquals(1, reads.get());
        assertEquals(3, result.observations.size());
        assertEquals(1, result.skippedRepeats);
        assertEquals("web.read", result.observations.get(2).tool);
        assertEquals(1, memories());
    }

    @Test public void smallFallbackContextStillProducesCompleteJsonWithinBudget() {
        AgentRun r = create(); AgentRun claimed = store.claim(r.id, "old", now);
        for (int i = 0; i < 6; i++) claimed.outline.add("\"\\\n".repeat(50));
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
    @Test public void experienceSurvivesReopenAndActuallyEntersTheNextLocalInference() {
        AgentRun first = create(); execute(first.id, model(), tools("SUCCESS"));
        AgentRun next = create(); assertFalse(next.experience.isEmpty());
        String saved = next.experience; db.close(); open();
        assertEquals(saved, store.get(next.id).experience);
        AgentRun done = execute(next.id, p -> {
            JsonObject data = JsonParser.parseString(p.substring(p.indexOf("DATOS=") + 6)).getAsJsonObject();
            assertTrue(data.has("experiencia_operativa"));
            assertEquals(first.id, data.getAsJsonObject("experiencia_operativa").getAsJsonArray("planes").get(0).getAsString());
            return model().generate(p);
        }, tools("SUCCESS"));
        assertEquals("SUCCEEDED", done.status);
        assertEquals("", store.create(first.goal, true, "", now).experience);
    }
    @Test public void corruptHistoricalJournalDoesNotPreventNewPlanOrDestroyValidExperience() {
        AgentRun first = create(); execute(first.id, model(), tools("SUCCESS"));
        AgentRunEntity bad = new AgentRunEntity(); bad.id = "damaged-history"; bad.status = "SUCCEEDED";
        bad.createdAt = bad.updatedAt = now; bad.journal = "{broken"; db.agentDao().insert(bad);
        AgentRun next = create(); assertFalse(next.experience.isEmpty());
        assertTrue(next.experience.contains(first.id)); assertFalse(next.experience.contains(bad.id));
        assertEquals("{broken", db.agentDao().get(bad.id).journal);
    }
    @Test public void repeatedSuccessfulCallReusesEvidenceWithoutExecutionOrInflatedStatistics() {
        AgentRun r = create(); AtomicInteger calls = new AtomicInteger(), executions = new AtomicInteger();
        AgentRun done = execute(r.id, p -> {
            if (calls.getAndIncrement() < 2) return response("{\"tool\":\"memory.lookup\",\"input\":\"origen\"}");
            return model().generate(p);
        }, new AgentEngine.Tools() {
            public String capabilities(AgentRun r) { return "memory.lookup"; }
            public AgentRun.Observation execute(AgentRun r, AgentRun.Pending call, java.util.function.BooleanSupplier stopped) {
                executions.incrementAndGet();
                AgentRun.Observation o = new AgentRun.Observation("forged-id", "web.read", "SUCCESS", "2025");
                o.fingerprint = "forged"; return o;
            }
        });
        assertEquals("SUCCEEDED", done.status); assertEquals(1, executions.get()); assertEquals(1, done.observations.size());
        assertEquals(1, done.skippedRepeats); assertEquals(1, db.agentDao().stat("memory.lookup").successes);
        assertEquals(AgentExperience.fingerprint("memory.lookup", "origen"), done.observations.get(0).fingerprint);
        assertEquals(1, memories());
    }
    @Test public void transientFailureIsRetriedOnceAndThenChangesAreRequested() {
        AgentRun done = execute(create().id, p -> response("{\"tool\":\"memory.lookup\",\"input\":\"origen\"}"), tools("ERROR"));
        assertEquals("PARTIAL", done.status); assertEquals(2, done.observations.size());
        assertEquals(10, done.skippedRepeats); assertEquals(2, db.agentDao().stat("memory.lookup").failures);
        assertEquals(0, memories());
    }
    @Test public void interruptionDoesNotForgetDuplicateGuardButExplicitResumeAllowsFailedRetry() {
        AgentRun r = create(); AgentRun claimed = store.claim(r.id, "old", now);
        AgentRun.Observation success = new AgentRun.Observation("one", "memory.lookup", "SUCCESS", "2025");
        success.fingerprint = AgentExperience.fingerprint("memory.lookup", "origen"); claimed.observations.add(success);
        claimed.pending = new AgentRun.Pending("next", "memory.lookup", "origen");
        assertTrue(store.save(claimed, now)); store.release(r.id, "old", now); db.close(); open();
        AgentRun done = execute(r.id, model(), new AgentEngine.Tools() {
            public String capabilities(AgentRun r) { return "memory.lookup"; }
            public AgentRun.Observation execute(AgentRun r, AgentRun.Pending call, java.util.function.BooleanSupplier stopped) {
                throw new AssertionError("A saved successful request must not execute again after restart");
            }
        });
        assertEquals("SUCCEEDED", done.status); assertEquals(1, done.skippedRepeats);
        AgentRun retry = create(); claimed = store.claim(retry.id, "owner", now);
        claimed.observations.add(success);
        AgentRun.Observation failure = new AgentRun.Observation("failed", "web.search", "ERROR", "offline");
        failure.fingerprint = AgentExperience.fingerprint("web.search", retry.goal); claimed.observations.add(failure);
        claimed.error = "offline"; assertTrue(store.save(claimed, now)); assertTrue(store.cancel(retry.id, now));
        assertTrue(store.resume(retry.id, now)); AgentRun resumed = store.get(retry.id);
        assertNull(resumed.observations.get(1).fingerprint); assertEquals(success.fingerprint, resumed.observations.get(0).fingerprint);
        assertEquals("", resumed.error); assertFalse(resumed.codeAllowed);
    }
    @Test public void localOnlyResultsDoNotEnterCloudOutbox() {
        sync = false; execute(create().id, model(), tools("SUCCESS")); assertEquals(1, memories()); assertTrue(db.syncEventDao().getPending(10).isEmpty());
    }
}
