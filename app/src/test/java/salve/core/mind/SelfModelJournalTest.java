package salve.core.mind;

import static org.junit.Assert.*;
import static salve.core.mind.MindTestSupport.list;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class SelfModelJournalTest {
    private final MindTestSupport.MemoryStore store = new MindTestSupport.MemoryStore();
    private final MindTestSupport.Clock clock = new MindTestSupport.Clock();
    private final SelfModelJournal journal = MindTestSupport.journal(store, clock);

    @Test public void freshModelStartsWithOpenNarrativeNotAPrefabricatedLabel() {
        assertEquals(SelfModel.DEFAULT_NARRATIVE, journal.narrative());
        assertFalse(journal.narrative().toLowerCase().contains("asistente"));
        assertTrue(journal.render(2000).contains("YO_PERSISTENTE v0"));
    }

    @Test public void beliefWithoutSourceIsRejected() {
        SelfModelJournal.Result r = journal.apply(new MindPatch("sueño")
                .belief("bryan", "A Bryan le gusta el cine", 0.9, Collections.emptyList()));
        assertFalse(r.saved);
        assertEquals(1, r.rejected);
        assertTrue(journal.beliefs().isEmpty());
    }

    @Test public void confidenceIsCappedByAmountOfEvidence() {
        journal.apply(new MindPatch("sueño").belief("bryan", "Bryan dirige una empresa familiar", 0.95, list("recuerdos:1")));
        SelfModel.Belief b = journal.beliefs().get(0);
        assertEquals(SelfModelJournal.cap(1), b.confidence, 1e-9);
        assertTrue(b.confidence < 0.95);
    }

    @Test public void repeatedBeliefIsReinforcedNotDuplicated() {
        journal.apply(new MindPatch("sueño").belief("bryan", "Bryan vive en Vitoria", 0.5, list("recuerdos:1")));
        journal.apply(new MindPatch("sueño").belief("bryan", "bryan vive en vitoria.", 0.5, list("recuerdos:7")));
        List<SelfModel.Belief> beliefs = journal.beliefs();
        assertEquals(1, beliefs.size());
        assertEquals(new HashSet<>(list("recuerdos:1", "recuerdos:7")), new HashSet<>(beliefs.get(0).sources));
        assertTrue(beliefs.get(0).confidence > 0.5);
        assertEquals(1, journal.metrics().beliefsReinforced);
    }

    @Test public void contradictionLowersConfidenceAndEventuallyRetires() {
        journal.apply(new MindPatch("sueño").belief("bryan", "Bryan prefiere respuestas largas", 0.4, list("recuerdos:2")));
        String id = journal.beliefs().get(0).id;
        journal.apply(new MindPatch("sueño").contradiction(id, list("recuerdos:3"), "pidió brevedad"));
        SelfModel.Belief after = journal.beliefs().get(0);
        assertEquals(0.15, after.confidence, 1e-9);
        assertTrue(after.contradictions.contains("recuerdos:3"));
        assertTrue(journal.render(2000).contains("CONTRADICHA"));
        journal.apply(new MindPatch("sueño").contradiction(id, list("recuerdos:4"), "otra vez"));
        assertTrue(journal.beliefs().isEmpty());
        assertEquals(1, journal.metrics().beliefsRetired);
    }

    @Test public void selfBeliefsMustRespectFunctionalIdentityPolicy() {
        SelfModelJournal.Result r = journal.apply(new MindPatch("sueño")
                .belief("salve", "Soy consciente y estoy viva", 0.9, list("recuerdos:1"))
                .belief("salve", "Respondo mejor cuando consulto mi memoria antes de afirmar", 0.6, list("recuerdos:2")));
        assertEquals(1, r.accepted);
        assertEquals(1, r.rejected);
        assertEquals("Respondo mejor cuando consulto mi memoria antes de afirmar", journal.beliefs().get(0).statement);
    }

    @Test public void narrativeChangesWaitForBryan() {
        String proposal = "Soy Salve; acompaño a Bryan en su empresa y aprendo de cada corrección suya.";
        journal.apply(new MindPatch("sueño").narrative(proposal, "patrón repetido", list("recuerdos:1")));
        assertEquals(SelfModel.DEFAULT_NARRATIVE, journal.narrative());
        assertNotNull(journal.pendingNarrative());
        assertTrue(journal.render(2000).contains("pendiente de Bryan"));
        journal.acceptNarrative();
        assertEquals(proposal, journal.narrative());
        assertNull(journal.pendingNarrative());
    }

    @Test public void rejectedNarrativeKeepsCurrentIdentity() {
        journal.apply(new MindPatch("sueño").narrative("Soy Salve, una exploradora de ideas.", "x", list("recuerdos:1")));
        journal.rejectNarrative();
        assertEquals(SelfModel.DEFAULT_NARRATIVE, journal.narrative());
        assertNull(journal.pendingNarrative());
    }

    @Test public void stateSurvivesRestart() {
        journal.apply(new MindPatch("reflexión").thought("Bryan revisa el presupuesto los lunes", list("recuerdos:9"))
                .question("¿Qué métrica le importa más de la empresa?", 0.7, list("recuerdos:9")));
        SelfModelJournal reopened = MindTestSupport.journal(store, clock);
        assertEquals(journal.version(), reopened.version());
        assertEquals(1, reopened.thoughts().size());
        assertEquals("¿Qué métrica le importa más de la empresa?", reopened.questions().get(0).text);
    }

    @Test public void unreadableStoreIsNeverOverwritten() {
        store.content = "{no es json";
        SelfModelJournal broken = MindTestSupport.journal(store, clock);
        assertNotNull(broken.loadFailure());
        assertFalse(broken.apply(new MindPatch("x").thought("algo", list())).saved);
        assertEquals("{no es json", store.content);
        assertEquals(0, store.writes);
    }

    @Test public void failedWriteKeepsPreviousState() {
        journal.apply(new MindPatch("x").thought("primero", list()));
        store.failWrite = true;
        SelfModelJournal.Result r = journal.apply(new MindPatch("x").thought("segundo", list()));
        assertFalse(r.saved);
        assertEquals(1, journal.thoughts().size());
    }

    @Test public void pauseStopsInternalActivity() {
        journal.setPaused(true);
        assertFalse(journal.apply(new MindPatch("reflexión").thought("algo", list())).saved);
        journal.setPaused(false);
        assertTrue(journal.apply(new MindPatch("reflexión").thought("algo", list())).saved);
    }

    @Test public void renderIsBoundedAndShowsProvenance() {
        for (int i = 0; i < 40; i++)
            journal.apply(new MindPatch("sueño").belief("mundo", "Hecho número " + i + " con bastante texto para ocupar espacio", 0.5,
                    list("recuerdos:" + i)));
        String text = journal.render(600);
        assertTrue(text.length() <= 600);
        assertTrue(text.contains("fuentes:recuerdos:"));
    }

    @Test public void capacityEvictsWeakestBelief() {
        for (int i = 0; i < SelfModelJournal.MAX_BELIEFS + 5; i++)
            journal.apply(new MindPatch("sueño").belief("mundo", "Creencia distinta " + i, i == 0 ? 0.9 : 0.3, list("recuerdos:" + i)));
        List<SelfModel.Belief> beliefs = journal.beliefs();
        assertEquals(SelfModelJournal.MAX_BELIEFS, beliefs.size());
        assertTrue(beliefs.stream().anyMatch(b -> b.statement.equals("Creencia distinta 0")));
    }

    @Test public void forgettingMemoriesRemovesWhatDependedOnlyOnThem() {
        journal.apply(new MindPatch("sueño")
                .belief("bryan", "Bryan tiene una cita médica el jueves", 0.6, list("recuerdos:5"))
                .belief("bryan", "Bryan trabaja en comunicaciones", 0.6, list("recuerdos:5", "recuerdos:6"))
                .thought("Quizá le preocupa la cita", list("recuerdos:5")));
        int removed = journal.forgetSources(new HashSet<>(list("recuerdos:5")));
        assertEquals(2, removed);
        List<SelfModel.Belief> beliefs = journal.beliefs();
        assertEquals(1, beliefs.size());
        assertEquals(list("recuerdos:6"), beliefs.get(0).sources);
        assertTrue(journal.thoughts().isEmpty());
        assertFalse(journal.referencedSources().contains("recuerdos:5"));
    }

    @Test public void staleCloudMindCannotRestoreDeletedRoomCitations() {
        SelfModelJournal journal = MindTestSupport.journal(
                new MindTestSupport.MemoryStore(), new MindTestSupport.Clock());
        journal.apply(new MindPatch("sueño")
                .belief("bryan", "Recordatorio del telescopio", 0.6, list("recuerdos:5"))
                .thought("Volver a comprobar el telescopio", list("recuerdos:5")));
        String oldSnapshot = journal.snapshotJson();
        assertTrue(SelfModelJournal.snapshotReferencesAvailable(oldSnapshot,
                new HashSet<>(java.util.Arrays.asList(5, 6))));
        assertFalse("La copia previa al olvido no puede reintroducir referencias borradas",
                SelfModelJournal.snapshotReferencesAvailable(oldSnapshot,
                        Collections.singleton(6)));
        assertFalse(SelfModelJournal.snapshotReferencesAvailable(oldSnapshot,
                Collections.emptySet()));
        assertFalse(SelfModelJournal.snapshotReferencesAvailable("{malformado",
                Collections.singleton(5)));
        journal.forgetSources(new HashSet<>(list("recuerdos:5")));
        String clean = journal.snapshotJson();
        assertTrue("Tras limpiar las fuentes puede respaldarse el resto del estado",
                SelfModelJournal.snapshotReferencesAvailable(clean, Collections.emptySet()));
        assertFalse(clean.contains("Recordatorio del telescopio"));
        assertFalse(clean.contains("Volver a comprobar el telescopio"));
    }

    @Test public void unrelatedSourcesDoNotBecomeDeletedRoomCitations() {
        SelfModelJournal journal = MindTestSupport.journal(
                new MindTestSupport.MemoryStore(), new MindTestSupport.Clock());
        journal.apply(new MindPatch("t").question("¿Cómo vuelan las aves?", .6,
                list("web:example.org/aves")));
        assertTrue(SelfModelJournal.snapshotReferencesAvailable(
                journal.snapshotJson(), Collections.emptySet()));
    }

    @Test public void surpriseNeedsCalibrationThenFlagsOutliers() {
        for (int i = 0; i < 7; i++) assertFalse(journal.recordSurprise(0.95));
        SelfModelJournal calibrated = MindTestSupport.journal(new MindTestSupport.MemoryStore(), clock);
        for (int i = 0; i < 20; i++) calibrated.recordSurprise(0.2 + (i % 3) * 0.02);
        assertFalse(calibrated.recordSurprise(0.24));
        assertTrue(calibrated.recordSurprise(0.9));
        assertEquals(1, calibrated.metrics().surprises);
    }

    @Test public void describeIsHonestAboutWhatItIs() {
        String text = journal.describe();
        assertTrue(text.contains("no prueba de conciencia"));
    }
}
