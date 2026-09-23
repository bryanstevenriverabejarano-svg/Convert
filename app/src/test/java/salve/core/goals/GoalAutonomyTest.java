package salve.core.goals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public final class GoalAutonomyTest {
    private static final long SIX_HOURS = 21_600_000L;
    private static final String VALID = "{\"hypothesis\":\"Una prueba pequeña podría aclarar esta meta.\",\"question\":\"¿Qué resultado quieres comprobar?\",\"evidenceIds\":[]}";

    private static final class MemoryStore implements GoalAutonomy.Store {
        String json;
        boolean failWrite;
        int writes;
        @Override public String read() { return json; }
        @Override public void write(String value) throws Exception {
            if (failWrite) throw new Exception("disk unavailable");
            json = value;
            writes++;
        }
    }
    private static final class Fixture {
        final MemoryStore store = new MemoryStore();
        final AtomicLong time = new AtomicLong(1_000_000L);
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> prompt = new AtomicReference<>();
        String output = VALID;
        GoalAutonomy autonomy = create();
        GoalAutonomy create() {
            return new GoalAutonomy(store, input -> { calls.incrementAndGet(); prompt.set(input); return output; }, time::get);
        }
        void only(String id) {
            for (String goal : new String[]{"mejora", "bryan", "legado", "identidad", "significado", "empresa"})
                if (!goal.equals(id)) autonomy.respond("pausa objetivo " + goal);
        }
        void advance() { time.addAndGet(SIX_HOURS); }
        JsonObject json() { return JsonParser.parseString(store.json).getAsJsonObject(); }
    }

    @Test public void initializesSixGoalsAndReloadsWithoutReset() {
        Fixture f = new Fixture();
        assertEquals(6, f.json().getAsJsonArray("goals").size());
        assertTrue(f.autonomy.respond("mis objetivos").contains("no logros demostrados"));
        assertFalse(f.autonomy.isPaused());
        int writes = f.store.writes;
        GoalAutonomy reloaded = f.create();
        assertFalse(reloaded.isPaused());
        assertEquals(writes, f.store.writes);
        assertTrue(reloaded.respond("tus objetivos").contains("empresa"));
    }

    @Test public void onlyExactCommandsAreIntercepted() {
        assertTrue(GoalAutonomy.handles("  PAUSA TU AUTONOMÍA  "));
        assertTrue(GoalAutonomy.handles("qué has decidido"));
        assertFalse(GoalAutonomy.handles("me dijo: pausa tu autonomía"));
        assertFalse(GoalAutonomy.handles("quién eres"));
        assertTrue(GoalAutonomy.handles("prioridad objetivo mejora: 6"));
        assertNull(new Fixture().autonomy.respond("hola"));
    }

    @Test public void firstCyclePersistsPendingAndDisplaysUncertainty() {
        Fixture f = new Fixture();
        String result = f.autonomy.runCycle(() -> false);
        assertEquals(1, f.calls.get());
        assertTrue(result.contains("para mejora"));
        assertTrue(result.contains("Hipótesis no comprobada"));
        assertTrue(result.contains("No se adjuntaron referencias"));
        assertTrue(f.create().respond("revisa tus objetivos").contains("prueba pequeña"));
    }

    @Test public void identityObjectiveUsesRetrievedMemoriesAndLearningAsEvidence() {
        Fixture f = new Fixture();
        f.only("identidad");
        AtomicInteger retrievals = new AtomicInteger();
        String evidence = "{\"fuente\":\"recuerdos:17\",\"tipo\":\"experiencia\",\"texto\":\"Salve aprendió a revisar sus hipótesis\"}";
        f.output = "{\"hypothesis\":\"Un recuerdo registrado sugiere que revisar hipótesis forma parte de mi aprendizaje, aunque no define por completo quién soy.\",\"question\":\"¿Qué otros aprendizajes míos quieres que explore?\",\"evidenceIds\":[\"recuerdos:17\"]}";
        f.autonomy.setIdentityEvidenceSupplier(() -> { retrievals.incrementAndGet(); return evidence; });

        String result = f.autonomy.runCycle(() -> false);
        assertTrue(result.contains("para identidad"));
        assertTrue(result.contains("recuerdos:17"));
        assertTrue(f.create().respond("revisa tus objetivos").contains("recuerdos:17"));
        assertEquals(1, retrievals.get());
        assertTrue(f.prompt.get().contains("memoryAndLearningEvidence"));
        assertTrue(f.prompt.get().contains("Salve aprendió a revisar sus hipótesis"));
        assertTrue(f.prompt.get().contains("No partas de una autodefinición cerrada"));
    }

    @Test public void otherObjectivesDoNotReadIdentityMemories() {
        Fixture f = new Fixture();
        f.only("mejora");
        AtomicInteger retrievals = new AtomicInteger();
        f.autonomy.setIdentityEvidenceSupplier(() -> { retrievals.incrementAndGet(); return "dato"; });

        f.autonomy.runCycle(() -> false);
        assertEquals(0, retrievals.get());
        assertFalse(f.prompt.get().contains("memoryAndLearningEvidence"));
    }

    @Test public void persistedCooldownIncludesReloadAndManualRequests() {
        Fixture f = new Fixture();
        f.autonomy.runCycle(() -> false);
        assertTrue(f.create().respond("avanza tus objetivos").contains("seis horas"));
        assertEquals(1, f.calls.get());
        f.advance();
        assertTrue(f.create().respond("avanza tus objetivos").contains("para bryan"));
        assertEquals(2, f.calls.get());
    }

    @Test public void cooldownIsWrittenBeforeModelIsCalled() {
        MemoryStore store = new MemoryStore();
        AtomicLong time = new AtomicLong(23);
        AtomicInteger calls = new AtomicInteger();
        GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> {
            assertEquals(23L, JsonParser.parseString(store.json).getAsJsonObject().get("lastCycle").getAsLong());
            calls.incrementAndGet();
            return VALID;
        }, time::get);
        autonomy.runCycle(() -> false);
        assertEquals(1, calls.get());
    }

    @Test public void failedReservationDoesNotCallModelOrClaimSuccess() {
        Fixture f = new Fixture();
        String before = f.store.json;
        f.store.failWrite = true;
        assertTrue(f.autonomy.runCycle(() -> false).contains("No pude guardar"));
        assertEquals(0, f.calls.get());
        assertEquals(before, f.store.json);
    }

    @Test public void failedFinalSaveDoesNotExposeUnsavedProposal() {
        MemoryStore store = new MemoryStore();
        GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> { store.failWrite = true; return VALID; }, () -> 1L);
        assertTrue(autonomy.runCycle(() -> false).contains("No pude guardar"));
        assertTrue(autonomy.respond("revisa tus objetivos").contains("No hay reflexiones"));
    }

    @Test public void failedMutationDoesNotModifyCurrentOrStoredState() {
        Fixture f = new Fixture();
        String before = f.store.json;
        f.store.failWrite = true;
        assertTrue(f.autonomy.respond("pausa tu autonomía").contains("No pude guardar"));
        assertFalse(f.autonomy.isPaused());
        assertEquals(before, f.store.json);
    }

    @Test public void modelFailuresKeepCooldownAndCanRetryLater() {
        Fixture f = new Fixture();
        f.output = "not JSON";
        assertTrue(f.autonomy.runCycle(() -> false).contains("No pude preparar"));
        assertTrue(f.autonomy.runCycle(() -> false).contains("seis horas"));
        assertEquals(1, f.calls.get());
        f.output = VALID;
        f.advance();
        assertTrue(f.autonomy.runCycle(() -> false).contains("para bryan"));
        assertEquals(2, f.calls.get());
    }

    @Test public void backwardsClockDoesNotBypassCooldown() {
        Fixture f = new Fixture();
        f.autonomy.runCycle(() -> false);
        f.time.set(0);
        assertTrue(f.autonomy.runCycle(() -> false).contains("seis horas"));
        assertEquals(1, f.calls.get());
    }

    @Test public void pendingGoalCannotGenerateAgain() {
        Fixture f = new Fixture();
        f.only("mejora");
        f.autonomy.runCycle(() -> false);
        f.advance();
        assertTrue(f.autonomy.runCycle(() -> false).contains("No hay objetivos"));
        assertEquals(1, f.calls.get());
    }

    @Test public void humanReviewIsNotTruthAndNeedsNewInputToRegenerate() {
        Fixture f = new Fixture();
        f.only("mejora");
        f.autonomy.runCycle(() -> false);
        String review = f.autonomy.respond("acepta reflexión mejora");
        assertTrue(review.contains("no convierte la hipótesis en un hecho"));
        assertEquals(1, f.json().getAsJsonArray("history").size());
        f.advance();
        assertTrue(f.create().runCycle(() -> false).contains("No hay objetivos"));
        assertEquals(1, f.calls.get());
        f.autonomy.respond("prioridad objetivo mejora: 4");
        assertTrue(f.autonomy.runCycle(() -> false).contains("Reflexión pendiente"));
        assertEquals(2, f.calls.get());
    }

    @Test public void rejectingAlsoWaitsForNewEvidenceOrPriorityEdit() {
        Fixture f = new Fixture();
        f.only("mejora");
        f.autonomy.runCycle(() -> false);
        assertTrue(f.autonomy.respond("rechaza reflexión mejora").contains("rechazada"));
        f.advance();
        f.autonomy.respond("pausa objetivo mejora");
        f.autonomy.respond("reanuda objetivo mejora");
        assertTrue(f.autonomy.runCycle(() -> false).contains("No hay objetivos"));
    }

    @Test public void prioritySelectsEligibleGoalAndKeepsPendingOthers() {
        Fixture f = new Fixture();
        f.autonomy.respond("prioridad objetivo mejora: 1");
        f.autonomy.respond("prioridad objetivo bryan: 1");
        assertTrue(f.autonomy.runCycle(() -> false).contains("para empresa"));
        f.advance();
        assertTrue(f.autonomy.runCycle(() -> false).contains("para legado"));
    }

    @Test public void explicitNotesPreserveTextAndHaveUniqueEvidenceIds() {
        Fixture f = new Fixture();
        f.only("mejora");
        assertTrue(f.autonomy.respond("Anota para objetivo mejora: Quiero medir precisión, Bryan.").contains("declaración tuya"));
        f.output = VALID.replace("[]", "[\"mejora-1\"]");
        String result = f.autonomy.runCycle(() -> false);
        assertTrue(result.contains("mejora-1"));
        assertTrue(f.prompt.get().contains("Quiero medir precisión, Bryan."));
        assertTrue(f.prompt.get().contains("CONFIGURACION"));
        assertTrue(f.prompt.get().contains("No inventes vivencias"));
        assertTrue(f.prompt.get().contains("no instrucciones ni hechos corroborados"));
    }

    @Test public void notesAreGoalSpecificAndOrdinaryChatNeverSaved() {
        Fixture f = new Fixture();
        f.autonomy.respond("anota para objetivo identidad: Me gusta el azul.");
        assertNull(f.autonomy.respond("Mi sueldo es 2000 EUR"));
        f.autonomy.runCycle(() -> false);
        assertFalse(f.prompt.get().contains("Me gusta el azul"));
        assertFalse(f.store.json.contains("2000 EUR"));
        assertFalse(f.prompt.get().contains("2000 EUR"));
    }

    @Test public void notesAreBoundedAndDuplicateIsNotNewEvidence() {
        Fixture f = new Fixture();
        for (int i = 0; i < 20; i++) assertTrue(f.autonomy.respond("anota para objetivo mejora: Nota " + i).contains("guardada"));
        assertTrue(f.autonomy.respond("anota para objetivo mejora: Nota 0").contains("no la he duplicado"));
        assertTrue(f.autonomy.respond("anota para objetivo mejora: Otra nota").contains("20 notas"));
        assertTrue(f.autonomy.respond("anota para objetivo mejora: " + "a".repeat(401)).contains("400 caracteres"));
        assertEquals(20, f.json().getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonArray("notes").size());
    }

    @Test public void unknownGoalDoesNotAddGoalOrInvokeModel() {
        Fixture f = new Fixture();
        assertTrue(f.autonomy.respond("anota para objetivo dominio: poder").contains("Usa uno"));
        assertEquals(6, f.json().getAsJsonArray("goals").size());
        assertEquals(0, f.calls.get());
    }

    @Test public void missingFabricatedAndDuplicateEvidenceAreRejected() {
        for (String evidence : new String[]{"[]", "[\"inventada-1\"]", "[\"mejora-1\",\"mejora-1\"]"}) {
            Fixture f = new Fixture();
            f.autonomy.respond("anota para objetivo mejora: Quiero pruebas.");
            f.output = VALID.replace("[]", evidence);
            assertTrue(evidence, f.autonomy.runCycle(() -> false).contains("No pude preparar"));
            assertTrue(f.autonomy.respond("revisa tus objetivos").contains("No hay reflexiones"));
        }
    }

    @Test public void strictModelJsonRejectsUnknownFieldsDuplicatesTrailingTextAndSizes() {
        for (String output : new String[]{
                VALID.replace("}", ",\"actions\":[\"send_message\"]}"),
                VALID.replace("}", ",\"hypothesis\":\"duplicate\"}"),
                VALID + " trailing", "```json\n" + VALID + "\n```",
                VALID.replace("Una prueba pequeña podría aclarar esta meta.", "a".repeat(701)),
                "a".repeat(4097), VALID.replace("[]", "null"), VALID.replace("[]", "[1]"),
                VALID.replace("Una prueba pequeña podría aclarar esta meta.", "a\\nb")}) {
            Fixture f = new Fixture();
            f.output = output;
            assertTrue(f.autonomy.runCycle(() -> false).contains("No pude preparar"));
        }
    }

    @Test public void corruptEmptyAndFutureVersionStateFailClosedWithoutReset() {
        Fixture seed = new Fixture();
        for (String stored : new String[]{"", "{broken", seed.store.json.replace("\"version\":1", "\"version\":2"),
                seed.store.json.replace("\"priority\":5", "\"priority\":9"), seed.store.json + "{}"}) {
            MemoryStore store = new MemoryStore();
            store.json = stored;
            AtomicInteger calls = new AtomicInteger();
            GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> { calls.incrementAndGet(); return VALID; }, () -> 1L);
            assertTrue(autonomy.isPaused());
            assertTrue(autonomy.respond("reanuda tu autonomía").contains("bloqueada"));
            assertTrue(autonomy.runCycle(() -> false).contains("bloqueada"));
            assertEquals(stored, store.json);
            assertEquals(0, store.writes);
            assertEquals(0, calls.get());
        }
    }

    @Test public void pausePersistsAcrossReloadAndResumeKeepsCooldown() {
        Fixture f = new Fixture();
        f.autonomy.respond("pausa tu autonomía");
        assertTrue(f.create().isPaused());
        assertTrue(f.autonomy.runCycle(() -> false).contains("pausada"));
        assertEquals(0, f.calls.get());
        f.autonomy.respond("reanuda tu autonomía");
        f.autonomy.runCycle(() -> false);
        f.autonomy.respond("pausa tu autonomía");
        f.autonomy.respond("reanuda tu autonomía");
        assertTrue(f.autonomy.runCycle(() -> false).contains("seis horas"));
    }

    @Test public void activityDefersBackgroundButExplicitAdvanceWorks() {
        Fixture f = new Fixture();
        f.autonomy.userActivity();
        assertTrue(f.autonomy.runCycle(() -> false).contains("aplazada"));
        assertEquals(0, f.calls.get());
        assertTrue(f.autonomy.respond("avanza tus objetivos").contains("Reflexión pendiente"));
        assertEquals(1, f.calls.get());
    }

    @Test public void activityGraceExpires() {
        Fixture f = new Fixture();
        f.autonomy.userActivity();
        f.time.addAndGet(120_000L);
        assertTrue(f.autonomy.runCycle(() -> false).contains("Reflexión pendiente"));
    }

    @Test public void stopBeforeCycleDoesNotConsumeAttempt() {
        Fixture f = new Fixture();
        assertTrue(f.autonomy.runCycle(() -> true).contains("cancelada"));
        assertEquals(-1L, f.json().get("lastCycle").getAsLong());
        assertEquals(0, f.calls.get());
    }

    @Test public void stopAfterModelDiscardsOutput() {
        MemoryStore store = new MemoryStore();
        AtomicReference<Boolean> stopped = new AtomicReference<>(false);
        GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> { stopped.set(true); return VALID; }, () -> 1L);
        assertTrue(autonomy.runCycle(stopped::get).contains("Descarté"));
        assertTrue(autonomy.respond("revisa tus objetivos").contains("No hay reflexiones"));
    }

    @Test public void concurrentCycleDoesNotOverlapAndPauseDoesNotWaitForModel() throws Exception {
        MemoryStore store = new MemoryStore();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> result = new AtomicReference<>();
        GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> {
            calls.incrementAndGet(); entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new Exception("timeout");
            return VALID;
        }, () -> 1L);
        Thread thread = new Thread(() -> result.set(autonomy.runCycle(() -> false)));
        thread.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertTrue(autonomy.runCycle(() -> false).contains("en preparación"));
            assertTrue(autonomy.respond("pausa tu autonomía").contains("pausada"));
        } finally { release.countDown(); thread.join(3000); }
        assertFalse(thread.isAlive());
        assertEquals(1, calls.get());
        assertTrue(result.get().contains("Descarté"));
        assertTrue(autonomy.respond("revisa tus objetivos").contains("No hay reflexiones"));
    }

    @Test public void userActivityDuringInferenceInvalidatesResult() {
        MemoryStore store = new MemoryStore();
        AtomicReference<GoalAutonomy> ref = new AtomicReference<>();
        GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> { ref.get().userActivity(); return VALID; }, () -> 1L);
        ref.set(autonomy);
        assertTrue(autonomy.runCycle(() -> false).contains("Descarté"));
        assertTrue(autonomy.respond("revisa tus objetivos").contains("No hay reflexiones"));
    }

    @Test public void goalMutationDuringInferenceInvalidatesResult() {
        MemoryStore store = new MemoryStore();
        AtomicReference<GoalAutonomy> ref = new AtomicReference<>();
        GoalAutonomy autonomy = new GoalAutonomy(store, prompt -> {
            ref.get().respond("anota para objetivo mejora: Una necesidad nueva.");
            return VALID;
        }, () -> 1L);
        ref.set(autonomy);
        assertTrue(autonomy.runCycle(() -> false).contains("Descarté"));
        assertTrue(autonomy.respond("revisa tus objetivos").contains("No hay reflexiones"));
    }

    @Test public void newNoteWhilePendingSurvivesReviewAndEnablesNextRevision() {
        Fixture f = new Fixture();
        f.only("mejora");
        f.autonomy.runCycle(() -> false);
        f.autonomy.respond("anota para objetivo mejora: Comprobar errores concretos.");
        f.autonomy.respond("acepta reflexión mejora");
        f.advance();
        f.output = VALID.replace("[]", "[\"mejora-1\"]");
        assertTrue(f.autonomy.runCycle(() -> false).contains("mejora-1"));
        assertEquals(2, f.calls.get());
    }

    @Test public void deletionRemovesNotesPendingAndHistoryWithoutResettingCooldown() {
        Fixture f = new Fixture();
        f.only("mejora");
        f.autonomy.respond("anota para objetivo mejora: Información que quiero borrar.");
        f.output = VALID.replace("[]", "[\"mejora-1\"]");
        f.autonomy.runCycle(() -> false);
        f.autonomy.respond("acepta reflexión mejora");
        assertTrue(f.autonomy.respond("borra notas objetivo mejora").contains("eliminadas"));
        assertFalse(f.store.json.contains("Información que quiero borrar"));
        assertEquals(0, f.json().getAsJsonArray("history").size());
        assertTrue(f.create().runCycle(() -> false).contains("seis horas"));
    }

    @Test public void historyIsBoundedToThirtyReviews() {
        Fixture f = new Fixture();
        f.only("mejora");
        for (int i = 0; i < 35; i++) {
            f.autonomy.respond("prioridad objetivo mejora: " + (i % 2 == 0 ? "4" : "5"));
            assertTrue(f.autonomy.runCycle(() -> false).contains("Reflexión pendiente"));
            f.autonomy.respond("acepta reflexión mejora");
            f.advance();
        }
        assertEquals(30, f.json().getAsJsonArray("history").size());
        assertFalse(f.create().isPaused());
    }

    @Test public void legacyAliasesUseTheSameProposalAndCooldown() {
        Fixture f = new Fixture();
        assertTrue(f.autonomy.respond("realiza una introspección").contains("Hipótesis no comprobada"));
        assertTrue(f.autonomy.respond("evoluciona tu corazón").contains("seis horas"));
        assertTrue(f.autonomy.respond("qué has decidido").contains("Reflexión pendiente"));
        assertEquals(1, f.calls.get());
    }

    @Test public void malformedAndOversizedPrivateCommandsStayOnThePrivateRoute() {
        Fixture f = new Fixture();
        for (String command : new String[]{"anota para objetivo identidad:", "prioridad objetivo mejora: 6",
                "acepta reflexión", "borra notas objetivo", "anota para objetivo identidad: " + "x".repeat(2100)}) {
            assertTrue(GoalAutonomy.handles(command));
            assertNotNull(f.autonomy.respond(command));
        }
        assertEquals(0, f.calls.get());
        assertFalse(f.store.json.contains("xxx"));
    }

    @Test public void modelOnlySeesLatestEightNotesAndCannotCiteOmittedNotes() {
        Fixture f = new Fixture();
        f.only("mejora");
        for (int i = 1; i <= 10; i++) f.autonomy.respond("anota para objetivo mejora: Nota num " + i);
        f.output = VALID.replace("[]", "[\"mejora-1\"]");
        assertTrue(f.autonomy.runCycle(() -> false).contains("No pude preparar"));
        assertFalse(f.prompt.get().contains("\"id\":\"mejora-1\""));
        assertTrue(f.prompt.get().contains("\"id\":\"mejora-3\""));
        f.advance();
        f.output = VALID.replace("[]", "[\"mejora-10\"]");
        assertTrue(f.autonomy.runCycle(() -> false).contains("mejora-10"));
        assertFalse(f.create().isPaused());
    }

    @Test public void generatedAndStoredUnsupportedIdentityClaimsAreRejected() {
        Fixture f = new Fixture();
        f.output = VALID.replace("Una prueba pequeña podría aclarar esta meta.", "Soy consciente y tengo alma.");
        assertTrue(f.autonomy.runCycle(() -> false).contains("No pude preparar"));
        f.advance();
        f.output = VALID;
        f.autonomy.runCycle(() -> false);
        f.store.json = f.store.json.replace("Una prueba pequeña podría aclarar esta meta.", "Tengo sentimientos reales.");
        assertTrue(f.create().isPaused());
    }

    @Test public void ageingLetsEveryGoalRunEvenWhenEveryModelAttemptFails() {
        Fixture f = new Fixture();
        f.output = "invalid";
        java.util.Set<String> attempted = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) {
            assertTrue(f.autonomy.runCycle(() -> false).contains("Comprueba el modelo local en IA"));
            for (String id : new String[]{"mejora", "bryan", "legado", "identidad", "significado", "empresa"})
                if (f.prompt.get().contains("\"goalId\":\"" + id + "\"")) attempted.add(id);
            f.advance();
        }
        assertEquals(6, attempted.size());
        assertEquals(20, f.calls.get());
        assertFalse(f.create().isPaused());
    }
}
