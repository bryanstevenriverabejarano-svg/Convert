package salve.core.conversation;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.mind.SkillLab;
import salve.core.mind.SelfModelJournal;

/** A new seam: prove learning is coordinated without an Android conversation engine. */
public class ConversationSkillCoordinatorTest {
    private static final class Memory implements SelfModelJournal.Store {
        String saved;
        @Override public String read() { return saved; }
        @Override public void write(String value) { saved = value; }
    }
    private static final class Fixture {
        final Memory store = new Memory();
        final long[] time = {1_000_000L};
        final SkillLab lab = new SkillLab(store, () -> time[0]);
        final List<SkillLab.Decision> events = new ArrayList<>();
        final ConversationSkillCoordinator coordinator =
                new ConversationSkillCoordinator(() -> lab, events::add);
        void advance() { time[0] += 1000L; }
    }

    @Test public void coordinatorOnlyOpensExperimentAfterRepeatedVerifiedFailures() {
        Fixture f = new Fixture();
        f.coordinator.beginTurn("Necesito una referencia de animal");
        for (int i = 0; i < 3; i++) {
            f.coordinator.recordReference(f.lab.plan(SkillLab.REFERENCIAS, "animal"),
                    false, "El buscador no devolvió referencia válida");
            assertTrue(f.events.isEmpty());
            f.advance();
        }
        f.coordinator.recordReference(f.lab.plan(SkillLab.REFERENCIAS, "animal"),
                false, "El buscador volvió a fallar");
        assertEquals(1, f.events.size());
        assertEquals("experimento", f.events.get(0).verdict);
        assertEquals("wikipedia", f.lab.plan(SkillLab.REFERENCIAS, "animal").variant("fuente"));
        assertTrue(f.coordinator.describeSkills("¿En qué fallas?").contains("LABORATORIO"));
        assertFalse("las notas se consumen una sola vez",
                f.coordinator.describeSkills("hola").contains("LABORATORIO"));
    }

    @Test public void responseLearningRequiresARealCompletedAnswer() {
        Fixture f = new Fixture();
        DialogueFocus.State dialogue = new DialogueFocus.State();
        DialogueFocus.observeUser(dialogue, "Enséñame inglés", f.time[0]);
        DialogueFocus.Reading reading = DialogueFocus.read(dialogue, "sí", "¿Gramática o vocabulario?", f.time[0]);
        f.coordinator.responseStrategy(reading);
        f.coordinator.responseFinished(false, "inglés");
        f.coordinator.beginTurn("¿Y entonces?");
        assertTrue(f.lab.history().isEmpty());
        f.coordinator.responseStrategy(reading);
        f.coordinator.responseFinished(true, "inglés");
        f.coordinator.beginTurn("No, te pedí gramática");
        assertEquals(1, f.lab.history().size());
        assertFalse(f.lab.history().get(0).ok);
        assertEquals("bryan", f.lab.history().get(0).verifier);
        f.coordinator.responseFinished(true, "inglés"); // Plan already consumed
        f.coordinator.beginTurn("Otra cosa");
        assertEquals(1, f.lab.history().size());
    }

    @Test public void absenceOfVisionReceiptDoesNotTeachASuccess() {
        Fixture f = new Fixture();
        SkillLab.Plan drawing = f.lab.plan(SkillLab.DIBUJAR, "");
        f.coordinator.awaitDrawing(drawing, null, "");
        f.coordinator.beginTurn("Ahora dibuja otra cosa");
        assertTrue(f.lab.history().isEmpty());
        f.coordinator.awaitDrawing(f.lab.plan(SkillLab.DIBUJAR, ""), false, "No se reconoció el dibujo");
        f.coordinator.beginTurn("Ahora dibuja otra cosa");
        assertEquals(1, f.lab.history().size());
        assertFalse(f.lab.history().get(0).ok);
    }

    @Test public void noTaskDoesNotCreateASkillPlanOrExperience() {
        Fixture f = new Fixture();
        DialogueFocus.Reading reading = DialogueFocus.read(new DialogueFocus.State(),
                "hola", "hola", f.time[0]);
        assertEquals("", f.coordinator.responseStrategy(reading));
        f.coordinator.responseFinished(true, "hola");
        f.coordinator.beginTurn("continúa");
        assertTrue(f.lab.history().isEmpty());
        assertEquals("", f.coordinator.describeSkills("continúa"));
    }

    @Test public void storageOrRuntimeFailureDoesNotBlockTheConversation() {
        ConversationSkillCoordinator broken = new ConversationSkillCoordinator(
                () -> { throw new IllegalStateException("store unavailable"); }, d -> { });
        broken.beginTurn("hola");
        assertEquals("", broken.responseStrategy(null));
        broken.responseFinished(true, "hola");
        broken.awaitDrawing(null, null, "");
        assertEquals("", broken.describeSkills("hola"));
    }
}
