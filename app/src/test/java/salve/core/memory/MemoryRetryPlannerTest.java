package salve.core.memory;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryRetryPlannerTest {
    @Test public void explicitMissGetsOneCleanSubjectOnly() {
        String input = "¿Recuerdas lo que hablamos sobre el telescopio lunar?";
        String clean = MemoryRetryPlanner.secondQuery(input, input,
                ConversationMemoryGrounding.Status.EMPTY, true, true);
        assertNotNull(clean);
        assertTrue(clean.contains("telescopio"));
        assertTrue(clean.contains("lunar"));
        assertFalse(clean.contains("recuerdas"));
        assertFalse(clean.contains("?"));
        assertTrue(clean.length() <= 100);
    }

    @Test public void neverRetryWithoutPermissionOrExplicitRecall() {
        String q = "¿Recuerdas lo que hablamos sobre el telescopio lunar?";
        assertNull(MemoryRetryPlanner.secondQuery(q, q,
                ConversationMemoryGrounding.Status.EMPTY, true, false));
        assertNull(MemoryRetryPlanner.secondQuery(q, q,
                ConversationMemoryGrounding.Status.EMPTY, false, true));
        assertNull(MemoryRetryPlanner.secondQuery(q, q,
                ConversationMemoryGrounding.Status.ERROR, true, true));
        assertNull(MemoryRetryPlanner.secondQuery(q, q,
                ConversationMemoryGrounding.Status.PARTIAL, true, true));
        assertNull(MemoryRetryPlanner.secondQuery(q, q,
                ConversationMemoryGrounding.Status.FOUND, true, true));
    }

    @Test public void neverReinterpretChronologyOrLiveLocationAsTopic() {
        assertNull(MemoryRetryPlanner.secondQuery("¿Cuál fue tu primer recuerdo?",
                "¿Cuál fue tu primer recuerdo?",
                ConversationMemoryGrounding.Status.EMPTY, true, true));
        assertNull(MemoryRetryPlanner.secondQuery("¿Dónde estoy, mi ubicación actual GPS?",
                "ubicación actual GPS",
                ConversationMemoryGrounding.Status.EMPTY, true, true));
    }

    @Test public void noBroadeningWhenAlreadyCleanOrInsufficientSubject() {
        assertNull(MemoryRetryPlanner.secondQuery("recuérdame", "telescopio",
                ConversationMemoryGrounding.Status.EMPTY, true, true));
        assertNull(MemoryRetryPlanner.secondQuery("búscalo", "telescopio lunar",
                ConversationMemoryGrounding.Status.EMPTY, true, true));
    }

    @Test public void noUserTranscriptOrLongPromptUsedAsSearchQuery() {
        String over = new String(new char[513]).replace('\0', 'x');
        assertNull(MemoryRetryPlanner.secondQuery("¿Recuerdas el telescopio lunar?", over,
                ConversationMemoryGrounding.Status.EMPTY, true, true));
        assertNull(MemoryRetryPlanner.secondQuery(over, "telescopio lunar",
                ConversationMemoryGrounding.Status.EMPTY, true, true));
    }
}
