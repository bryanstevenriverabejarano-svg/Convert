package salve.core.mind;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

public class SkillLabTest {
    static final class Memory implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }

    @Test public void repairsOnlyTheFailingStepAndAdoptsWhatWorksBetter() {
        Memory m = new Memory();
        long[] now = {1_000_000L};
        SkillLab lab = new SkillLab(m, () -> now[0]);
        // Commons falla con animales una y otra vez.
        SkillLab.Decision opened = null;
        for (int i = 0; i < 4; i++) {
            SkillLab.Plan p = lab.plan(SkillLab.REFERENCIAS, "animal");
            assertEquals("commons", p.variant("fuente"));
            SkillLab.Decision d = lab.record(p, false, "fuente", "busqueda", "0 imágenes");
            if (d != null) opened = d;
            now[0] += 1000;
        }
        assertNotNull("abre un experimento tras fallar a menudo", opened);
        assertEquals("experimento", opened.verdict);
        assertEquals("wikipedia", opened.to);
        // Ahora prueba la variante candidata… sólo en ese contexto.
        assertEquals("wikipedia", lab.plan(SkillLab.REFERENCIAS, "animal").variant("fuente"));
        assertEquals("commons", lab.plan(SkillLab.REFERENCIAS, "objeto").variant("fuente"));
        SkillLab.Decision verdict = null;
        for (int i = 0; i < 3 && verdict == null; i++) {
            verdict = lab.record(lab.plan(SkillLab.REFERENCIAS, "animal"), true, null, "busqueda", "silueta usable");
        }
        assertNotNull(verdict);
        assertEquals("adoptado", verdict.verdict);
        // Persiste: una instancia nueva usa lo adoptado.
        SkillLab again = new SkillLab(m, () -> now[0]);
        assertEquals("wikipedia", again.plan(SkillLab.REFERENCIAS, "animal").variant("fuente"));
        assertTrue(again.render(900).contains("adoptado"));
    }

    @Test public void revertsAChallengerThatIsNotBetterAndWaitsBeforeRetrying() {
        long[] now = {5_000_000L};
        SkillLab lab = new SkillLab(new Memory(), () -> now[0]);
        for (int i = 0; i < 4; i++) lab.record(lab.plan(SkillLab.DIBUJAR, ""), false, "estrategia", "vision", "no se reconoce");
        assertEquals(1, lab.trials().size());
        SkillLab.Decision d = null;
        for (int i = 0; i < 3 && d == null; i++) d = lab.record(lab.plan(SkillLab.DIBUJAR, ""), false, "estrategia", "vision", "tampoco");
        assertNotNull(d);
        assertEquals("revertido", d.verdict);
        assertEquals("referencia_primero", lab.plan(SkillLab.DIBUJAR, "").variant("estrategia"));
        // En enfriamiento: otro fallo no reabre el mismo experimento enseguida.
        lab.record(lab.plan(SkillLab.DIBUJAR, ""), false, "estrategia", "vision", "otra vez");
        assertTrue(lab.trials().isEmpty());
    }

    @Test public void creditGoesOnlyToTheStepThatFailed() {
        java.util.List<SkillLab.Skill> two = java.util.Collections.singletonList(new SkillLab.Skill("x", "x",
                new SkillLab.Step("a", "a", "a1", "a2"), new SkillLab.Step("b", "b", "b1", "b2")));
        SkillLab lab = new SkillLab(new Memory(), () -> 1L, two);
        lab.record(lab.plan("x", ""), false, "b", "test", "");
        assertNull("el paso que funcionó no carga con el fallo", lab.stat("x", "a", "a1", ""));
        assertEquals(0, lab.stat("x", "b", "b1", "").ok);
        assertEquals(1, lab.stat("x", "b", "b1", "").n);
    }

    @Test public void predictsFromItsHistoryAndLearnsOnlyWithEvidence() {
        long[] now = {9_000_000L};
        SkillLab lab = new SkillLab(new Memory(), () -> now[0]);
        assertEquals(0.5f, lab.plan(SkillLab.RESPONDER, "tarea").predicted, 1e-4);
        // Sin evidencia (sin verificador provisional y Bryan no opina) no aprende nada.
        lab.await(lab.plan(SkillLab.RESPONDER, "tarea"), null, "", "");
        assertTrue(lab.observe("¿y mañana qué tiempo hará?").isEmpty());
        assertTrue(lab.history().isEmpty());
        // Las correcciones de Bryan cuentan como fallo, aunque el verificador provisional dijera bien.
        for (int i = 0; i < 5; i++) {
            lab.await(lab.plan(SkillLab.RESPONDER, "tarea"), Boolean.TRUE, "sin_correccion", "");
            now[0] += 1000;
            lab.observe("No, te pedí la gramática, no un relato");
        }
        SkillLab.Plan p = lab.plan(SkillLab.RESPONDER, "tarea");
        assertTrue(p.predicted < 0.4f);
        assertTrue(p.lowConfidence());
        assertTrue(lab.confidenceNote(p).contains("CONFIANZA"));
        assertEquals(SkillLab.Feedback.PRAISE, SkillLab.feedback("¡Me encanta, así sí!"));
        assertEquals(SkillLab.Feedback.CORRECTION, SkillLab.feedback("parece un perro, no un gato"));
        assertEquals(SkillLab.Feedback.NONE, SkillLab.feedback("me parece un buen plan para mañana"));
        assertTrue(SkillLab.asksAbout("¿Qué has aprendido de tus errores?"));
        assertTrue(SkillLab.asksAbout("en qué fallas más"));
        assertFalse(SkillLab.asksAbout("dibuja un gato"));
        List<SkillLab.Outcome> h = lab.history();
        assertEquals("bryan", h.get(h.size() - 1).verifier);
    }
}
