package salve.core.mind;

import static org.junit.Assert.*;
import static salve.core.mind.MindTestSupport.list;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ReflectionAndSurpriseTest {
    private final MindTestSupport.MemoryStore store = new MindTestSupport.MemoryStore();
    private final MindTestSupport.Clock clock = new MindTestSupport.Clock();
    private final SelfModelJournal journal = MindTestSupport.journal(store, clock);
    private final MindTestSupport.ScriptedGenerator model = new MindTestSupport.ScriptedGenerator();

    @Test public void reflectsOnNewExperienceWithoutNotesFromBryan() {
        ReflectionCycle cycle = new ReflectionCycle(journal, model);
        assertTrue("Experiencia nueva basta para pensar", cycle.shouldRun(clock.get() - 1000, clock.get()));
        assertFalse("Sin experiencia ni preguntas no hay nada que pensar", cycle.shouldRun(-1, clock.get()));
    }

    @Test public void reflectionIsRateLimitedButNotBlockedByApproval() {
        ReflectionCycle cycle = new ReflectionCycle(journal, model);
        model.then("{\"thought\":\"Bryan habló dos veces de facturas esta semana; puede que esté cerrando el trimestre.\","
                + "\"sources\":[\"recuerdos:3\",\"recuerdos:4\"],\"question\":null,\"resolves\":null}");
        String out = cycle.run(episodes(), () -> false);
        assertTrue(out, out.startsWith("Pensamiento registrado"));
        SelfModel.Thought t = journal.thoughts().get(0);
        assertEquals(list("recuerdos:3", "recuerdos:4"), t.sources);
        assertTrue(journal.render(2000).contains("cerrando el trimestre"));
        clock.advanceHours(0);
        assertFalse(cycle.shouldRun(clock.get(), clock.get() + 30 * 60_000L));
        assertTrue(cycle.shouldRun(clock.get() + 1, clock.get() + 61 * 60_000L));
    }

    @Test public void openQuestionsDriveThinkingWhenIdle() {
        journal.apply(new MindPatch("sueño").question("¿Qué le preocupa de la empresa?", 0.8, list("recuerdos:1")));
        ReflectionCycle cycle = new ReflectionCycle(journal, model);
        String qid = journal.questions().get(0).id;
        model.then("{\"thought\":\"Lo que más repite es la liquidez.\",\"sources\":[\"recuerdos:3\"],\"resolves\":\"" + qid + "\"}");
        cycle.run(episodes(), () -> false);
        assertTrue(model.prompts.get(0).contains("¿Qué le preocupa de la empresa?"));
        assertTrue("La pregunta respondida se cierra", journal.questions().isEmpty());
        assertEquals(1, journal.metrics().questionsResolved);
    }

    @Test public void inventedSourcesAreDroppedFromThoughts() {
        model.then("{\"thought\":\"Algo\",\"sources\":[\"recuerdos:999\"]}");
        new ReflectionCycle(journal, model).run(episodes(), () -> false);
        assertTrue(journal.thoughts().get(0).sources.isEmpty());
    }

    @Test public void failedModelLeavesTimestampSoItRetries() {
        model.then("<FAIL>");
        new ReflectionCycle(journal, model).run(episodes(), () -> false);
        assertEquals(0, journal.metrics().lastReflectionAt);
    }

    @Test public void predictionErrorBecomesAQuestionOnlyWhenAtypical() {
        // Embeddings de juguete: el texto decide la dirección del vector.
        MindEmbedder embedder = text -> text.contains("factura") ? new float[]{1, 0, 0}
                : text.contains("gracias") ? new float[]{0.95f, 0.31f, 0} : new float[]{0, 0, 1};
        PredictionLoop loop = new PredictionLoop(journal, model, embedder, clock::get);
        for (int i = 0; i < 10; i++) {
            model.then("Bryan dará las gracias");
            assertNotNull(loop.predict("¿cuánto debo?", "Debes 40 euros"));
            double s = loop.observe("gracias, Salve");
            assertTrue(s >= 0 && s < 0.35);
        }
        assertTrue(journal.questions().isEmpty());
        model.then("Bryan dará las gracias");
        loop.predict("¿cuánto debo?", "Debes 40 euros");
        double surprise = loop.observe("mi madre está en el hospital");
        assertEquals(1.0, surprise, 1e-6);
        assertEquals(1, journal.questions().size());
        assertTrue(journal.questions().get(0).text.contains("No anticipé"));
        assertEquals(11, journal.metrics().predictions);
    }

    @Test public void stalePredictionsAreNotCompared() {
        MindEmbedder embedder = text -> new float[]{1, 0};
        PredictionLoop loop = new PredictionLoop(journal, model, embedder, clock::get);
        model.then("algo");
        loop.predict("hola", "hola Bryan");
        clock.advanceHours(3);
        assertEquals(-1, loop.observe("otra cosa"), 0);
    }

    @Test public void surpriseScaleSeparatesParaphraseFromUnrelated() {
        assertEquals(0, PredictionLoop.surprise(new float[]{1, 0}, new float[]{1, 0}), 1e-9);
        double paraphrase = PredictionLoop.surprise(new float[]{1, 0}, new float[]{0.92f, (float) Math.sqrt(1 - 0.92 * 0.92)});
        double unrelated = PredictionLoop.surprise(new float[]{1, 0}, new float[]{0.72f, (float) Math.sqrt(1 - 0.72 * 0.72)});
        assertTrue(paraphrase < 0.35);
        assertTrue(unrelated > 0.8);
    }

    @Test public void mindCommandsAreExact() {
        assertEquals(MindCommand.Action.DESCRIBE, MindCommand.parse("Salve, ¿en qué has pensado?"));
        assertEquals(MindCommand.Action.ACCEPT_NARRATIVE, MindCommand.parse("Acepta tu nueva identidad"));
        assertEquals(MindCommand.Action.DREAM_NOW, MindCommand.parse("sueña ahora"));
        assertNull(MindCommand.parse("¿qué crees?"));
        assertNull(MindCommand.parse("he pensado en mi mente esta mañana"));
    }

    private List<Episode> episodes() {
        List<Episode> e = new ArrayList<>();
        e.add(new Episode("recuerdos:3", "[declaracion_usuario] Tengo que cuadrar las facturas", clock.get() - 5000));
        e.add(new Episode("recuerdos:4", "[declaracion_usuario] Otra vez con facturas pendientes", clock.get() - 1000));
        return e;
    }
}
