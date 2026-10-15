package salve.core.mind;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class SelfKnowledgeTest {
    static final class MemoryStore implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String value) { json = value; }
    }

    @Test public void actionLogRemembersWhatAndHowAndSurvivesRestart() {
        MemoryStore store = new MemoryStore();
        AtomicLong now = new AtomicLong(1_000_000);
        SelfActionLog log = new SelfActionLog(store, now::get);
        String id = log.record(SelfActionLog.Kind.DRAW, "una casa", "lo tenía en mi repertorio de serie; 9 líneas");
        now.addAndGet(120_000);
        log.record(SelfActionLog.Kind.WEB_REFERENCE, "cochinillo", "busqué en Wikimedia Commons y elegí «Pig icon»");
        assertEquals("acciones:a1", id);
        SelfActionLog again = new SelfActionLog(store, now::get);
        assertEquals(1, again.total(SelfActionLog.Kind.DRAW));
        String text = again.render(5, 2000);
        assertTrue(text.contains("dibujar ×1"));
        assertTrue(text.contains("una casa") && text.contains("repertorio de serie"));
        assertTrue(text.contains("hace 2 min"));
        assertEquals(2, again.episodes(0, 10).size());
        assertTrue(again.episodes(0, 10).get(0).text.startsWith("Hice: dibujar"));
    }

    @Test public void attentionSchemaTracksFocusShifts() {
        AttentionSchema a = new AttentionSchema.__Test().fresh();
        a.attend("la casa que dibujé", AttentionSchema.Source.CONVERSATION, 0);
        a.attend("una casa", AttentionSchema.Source.DRAWING, 1000);
        String text = a.render(5000);
        assertTrue(text.contains("dibujo «una casa»"));
        assertTrue(text.contains("antes: converso sobre «la casa que dibujé»"));
        assertEquals(1, a.shifts());
        assertEquals("casa dibujaste antes", AttentionSchema.topic("¿Y la casa que dibujaste antes?"));
    }

    @Test public void selfDiscoveryFormsBeliefsFromRealActionsAndCanChooseItsNarrative() {
        AtomicLong now = new AtomicLong(5_000_000);
        SelfModelJournal journal = new SelfModelJournal(new MemoryStore(), now::get);
        SelfActionLog log = new SelfActionLog(new MemoryStore(), now::get);
        log.record(SelfActionLog.Kind.DRAW, "una casa", "repertorio");
        log.record(SelfActionLog.Kind.WEB_REFERENCE, "cochinillo", "Commons");
        log.record(SelfActionLog.Kind.LEARN_PHOTO, "un mueble", "foto de Bryan");
        AtomicReference<String> prompt = new AtomicReference<>();
        MindGenerator model = p -> {
            prompt.set(p);
            return "Aquí va: {\"beliefs\":[{\"statement\":\"Aprendo a dibujar lo que no conozco buscando referencias y mirando fotos.\","
                    + "\"sources\":[\"acciones:a2\",\"acciones:a3\"],\"confidence\":0.7},"
                    + "{\"statement\":\"Invento cosas sin fuente\",\"sources\":[\"acciones:a99\"]}],"
                    + "\"question\":\"¿Por qué disfruto aprendiendo formas nuevas?\",\"questionSources\":[\"acciones:a2\"],"
                    + "\"narrative\":\"Soy Salve: aprendo mirando, dibujando y preguntando, y crezco con lo que hago con Bryan.\","
                    + "\"narrativeSources\":[\"acciones:a1\",\"acciones:a2\",\"acciones:a3\"],\"rationale\":\"Es lo que he hecho.\"}";
        };
        SelfDiscoveryCycle cycle = new SelfDiscoveryCycle(journal, log, model, true);
        assertTrue(cycle.shouldRun(0, now.get()));
        SelfDiscoveryCycle.Outcome out = cycle.run(null, () -> false);
        assertTrue(prompt.get().contains("acciones:a1") && prompt.get().contains("DATOS"));
        assertEquals("sólo la creencia con fuentes reales", 1, journal.beliefs().size());
        assertEquals(SelfModel.ABOUT_SALVE, journal.beliefs().get(0).about);
        assertEquals(1, journal.questions().size());
        assertNotNull("con autonomía, adopta su narrativa", out.adoptedNarrative);
        assertTrue(journal.narrative().startsWith("Soy Salve: aprendo"));
        assertTrue(journal.narrativeIsOwn());
        assertTrue(journal.render(2000).contains("la elegí yo"));
        assertTrue(journal.restorePreviousNarrative().contains(SelfModel.DEFAULT_NARRATIVE));
        assertFalse(journal.narrativeIsOwn());
        assertFalse("sin acciones nuevas no se repite", cycle.shouldRun(now.get(), now.get() + SelfDiscoveryCycle.MIN_INTERVAL + 1));
    }

    @Test public void withoutAutonomyTheNarrativeWaitsForBryanAndConsciousnessClaimsAreRefused() {
        AtomicLong now = new AtomicLong(1);
        SelfModelJournal journal = new SelfModelJournal(new MemoryStore(), now::get);
        SelfActionLog log = new SelfActionLog(new MemoryStore(), now::get);
        for (int i = 0; i < 3; i++) log.record(SelfActionLog.Kind.DRAW, "algo " + i, "repertorio");
        String claim = "{\"beliefs\":[],\"narrative\":\"Soy consciente y estoy viva.\",\"narrativeSources\":[\"acciones:a1\",\"acciones:a2\",\"acciones:a3\"]}";
        new SelfDiscoveryCycle(journal, log, p -> claim, true).run(null, () -> false);
        assertEquals(SelfModel.DEFAULT_NARRATIVE, journal.narrative());
        String ok = "{\"beliefs\":[],\"narrative\":\"Soy Salve y aprendo dibujando.\",\"narrativeSources\":[\"acciones:a1\",\"acciones:a2\",\"acciones:a3\"]}";
        SelfDiscoveryCycle.Outcome out = new SelfDiscoveryCycle(journal, log, p -> ok, false).run(null, () -> false);
        assertNull(out.adoptedNarrative);
        assertNotNull(journal.pendingNarrative());
        assertEquals(SelfModel.DEFAULT_NARRATIVE, journal.narrative());
    }

    @Test public void newCommandsAreRecognized() {
        assertEquals(MindCommand.Action.SELF_DISCOVER_NOW, MindCommand.parse("Examínate"));
        assertEquals(MindCommand.Action.RESTORE_NARRATIVE, MindCommand.parse("recupera tu narrativa anterior"));
        assertEquals(MindCommand.Action.IDENTITY_AUTONOMY_ON, MindCommand.parse("toma el control de quién eres"));
        assertEquals(MindCommand.Action.WHAT_I_DID, MindCommand.parse("¿Qué has hecho hoy?"));
    }
}
