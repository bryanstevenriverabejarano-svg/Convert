package salve.core.mind;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

/** Correcciones del informe del 15/10/2026 (H3, H4, H7, H8) sobre su mente. */
public class InformeMindFixesTest {
    @Test public void h3SummaryAndExactUrlComeFromTheSamePage() {
        CuriosityCycle.Finding finding = CuriosityCycle.summarize(
                Arrays.asList("Menú. Inicio. Aceptar cookies.",
                        "Los pulpos tienen tres corazones: dos bombean sangre a las branquias y uno al resto del cuerpo."),
                Arrays.asList("https://primera.example/vacia", "https://es.wikipedia.org/wiki/Octopoda"));
        assertNotNull(finding);
        assertEquals("https://es.wikipedia.org/wiki/Octopoda", finding.url);
        SelfModel.Question q = new SelfModel.Question(); q.id = "q7"; q.text = "¿Por qué los pulpos tienen tres corazones?";
        MindPatch patch = CuriosityCycle.patch(q, finding, false);
        assertTrue(patch.thoughts.get(0).text.contains("es.wikipedia.org"));
        assertTrue(patch.thoughts.get(0).sources.contains("web:es.wikipedia.org/wiki/Octopoda"));
        String longRef = CuriosityCycle.sourceRef("https://example.org/" + String.join("/", Collections.nCopies(30, "tramo")));
        assertTrue(longRef.length() <= 64);
    }

    @Test public void h8RepeatingTheSameEvidenceDoesNotRaiseConfidence() {
        MindTestSupport.MemoryStore store = new MindTestSupport.MemoryStore();
        SelfModelJournal journal = MindTestSupport.journal(store, new MindTestSupport.Clock());
        journal.apply(new MindPatch("t").belief("bryan", "A Bryan le gusta la ciencia", 0.5, MindTestSupport.list("m:1")));
        double first = journal.beliefs().get(0).confidence;
        journal.apply(new MindPatch("t").belief("bryan", "A Bryan le gusta la ciencia", 0.5, MindTestSupport.list("m:1")));
        assertEquals(first, journal.beliefs().get(0).confidence, 1e-9);
        journal.apply(new MindPatch("t").belief("bryan", "A Bryan le gusta la ciencia", 0.5, MindTestSupport.list("m:2")));
        assertTrue(journal.beliefs().get(0).confidence > first);
    }

    @Test public void h8ForgettingPropagatesToWhatDependedOnIt() {
        MindTestSupport.MemoryStore store = new MindTestSupport.MemoryStore();
        SelfModelJournal journal = MindTestSupport.journal(store, new MindTestSupport.Clock());
        journal.apply(new MindPatch("t").belief("salve", "Me gusta investigar el mar", 0.6, MindTestSupport.list("m:9")));
        String beliefId = journal.beliefs().get(0).id;
        journal.apply(new MindPatch("t").thought("Por eso quiero leer sobre pulpos", MindTestSupport.list(beliefId)));
        assertEquals(1, journal.thoughts().size());
        journal.forgetSources(new HashSet<>(Collections.singletonList("m:9")));
        assertTrue(journal.beliefs().isEmpty());
        assertTrue("el pensamiento que solo citaba la creencia también se retira", journal.thoughts().isEmpty());
    }

    @Test public void h7RestoresOnlyANewerOrNonEmptyMind() {
        MindTestSupport.Clock clock = new MindTestSupport.Clock();
        MindTestSupport.MemoryStore old = new MindTestSupport.MemoryStore();
        SelfModelJournal source = MindTestSupport.journal(old, clock);
        source.apply(new MindPatch("t").question("¿Cómo navegan las aves migratorias?", 0.7, MindTestSupport.list("m:1")));
        String snapshot = source.snapshotJson();

        SelfModelJournal fresh = MindTestSupport.journal(new MindTestSupport.MemoryStore(), clock);
        assertTrue(fresh.restoreFrom(snapshot));
        assertEquals(1, fresh.questions().size());
        assertFalse("la misma versión no se vuelve a restaurar", fresh.restoreFrom(snapshot));
        assertFalse(fresh.restoreFrom("{basura"));
        assertEquals(1, fresh.questions().size());
    }
}
