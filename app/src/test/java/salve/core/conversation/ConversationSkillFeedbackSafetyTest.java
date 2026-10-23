package salve.core.conversation;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.mind.SkillLab;
import salve.core.mind.SelfModelJournal;

/** Being quiet, closing the chat or changing topics is not human validation. */
public class ConversationSkillFeedbackSafetyTest {
    private static final class Memory implements SelfModelJournal.Store {
        String value;
        @Override public String read() { return value; }
        @Override public void write(String s) { value = s; }
    }
    @Test public void silenceAndNextTopicNeverBecomeVerifiedSuccess() {
        long[] now = {1_000_000L};
        SkillLab lab = new SkillLab(new Memory(), () -> now[0]);
        ConversationSkillCoordinator c = new ConversationSkillCoordinator(() -> lab,
                ignored -> { });
        DialogueFocus.State focus = new DialogueFocus.State();
        DialogueFocus.observeUser(focus, "Enséñame inglés", now[0]);
        DialogueFocus.Reading reading = DialogueFocus.read(focus, "sí", "¿Gramática o vocabulario?", now[0]);
        c.responseStrategy(reading);
        c.responseFinished(true, "inglés");
        now[0] += 1000;
        c.beginTurn("Ahora quiero hablar de otra cosa");
        assertTrue("No afirmes éxito porque no hubo corrección", lab.history().isEmpty());
        c.responseStrategy(reading);
        c.responseFinished(true, "inglés");
        now[0] += 1000;
        c.beginTurn("No, te pedí gramática");
        assertEquals(1, lab.history().size());
        assertFalse(lab.history().get(0).ok);
        assertEquals("bryan", lab.history().get(0).verifier);
    }
    @Test public void explicitPraiseCanCreditAndMissingGenerationCannot() {
        long[] now = {1_000_000L};
        SkillLab lab = new SkillLab(new Memory(), () -> now[0]);
        ConversationSkillCoordinator c = new ConversationSkillCoordinator(() -> lab, ignored -> {});
        DialogueFocus.State focus = new DialogueFocus.State();
        DialogueFocus.observeUser(focus, "Enséñame inglés", now[0]);
        DialogueFocus.Reading reading = DialogueFocus.read(focus, "sí", "¿Gramática o vocabulario?", now[0]);
        c.responseStrategy(reading);
        c.responseFinished(false, "inglés");
        c.beginTurn("Excelente, me sirvió");
        assertEquals(0, lab.history().size());
        c.responseStrategy(reading);
        c.responseFinished(true, "inglés");
        c.beginTurn("Me sirvió, muy bien");
        assertEquals(1, lab.history().size());
        assertTrue(lab.history().get(0).ok);
        assertEquals("bryan", lab.history().get(0).verifier);
    }
}