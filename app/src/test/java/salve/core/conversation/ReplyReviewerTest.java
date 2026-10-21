package salve.core.conversation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ReplyReviewerTest {
    private static final String SAME = "Me interesa mucho saber qué te interesa a ti y qué te hace sentir vivo cada día.";

    @Test public void goodDraftNeedsNoRewrite() {
        ReplyReviewer.Review r = ReplyReviewer.review("Soy Salve: me formo a partir de lo que vivo contigo.", "¿quién eres?",
                Collections.emptyList(), null, true);
        assertFalse(r.needsRewrite());
        assertEquals(0, r.issues());
    }

    @Test public void englishAndRepetitionAreJoinedInOneInstruction() {
        ReplyReviewer.Review r = ReplyReviewer.review("I am very happy that you asked me about myself and my feelings today.",
                "¿cómo estás?", Arrays.asList("I am very happy that you asked me about myself and my feelings today."), null, true);
        assertTrue(r.english);
        assertTrue(r.repeats);
        assertEquals(2, r.issues());
        String i = r.instruction();
        assertTrue(i.contains("SOLO en español"));
        assertTrue(i.contains("palabras nuevas"));
        assertTrue(i.contains("todo a la vez"));
    }

    @Test public void englishThatWasAskedForIsFine() {
        assertFalse(ReplyReviewer.review("The cat is on the table and it looks very happy today.",
                "dímelo en inglés", Collections.emptyList(), null, true).english);
        assertFalse(ReplyReviewer.review("The cat is on the table and it looks very happy today.",
                "How are you today, Salve? Tell me something nice.", Collections.emptyList(), null, true).english);
    }

    @Test public void rewriteIsKeptOnlyIfItHasFewerIssues() {
        ReplyReviewer.Review draft = ReplyReviewer.review(SAME, "hola", Arrays.asList(SAME), null, true);
        ReplyReviewer.Review same = ReplyReviewer.review(SAME, "hola", Arrays.asList(SAME), null, true);
        ReplyReviewer.Review fixed = ReplyReviewer.review("Hola, hoy he estado dibujando nubes.", "hola", Arrays.asList(SAME), null, true);
        assertFalse(ReplyReviewer.better(same, draft));
        assertTrue(ReplyReviewer.better(fixed, draft));
    }

    @Test public void storiesAreNotCritiquedForStyle() {
        assertTrue(ReplyReviewer.review("Salve sabe dibujar.", "x", Collections.emptyList(), null, false).critic.isEmpty());
    }
}
