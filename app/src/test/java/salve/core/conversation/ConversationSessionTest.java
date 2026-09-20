package salve.core.conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConversationSessionTest {
    @Test
    public void preservesRolesAndOrder() {
        ConversationSession session = new ConversationSession(4);
        session.addUser("Me llamo Bryan");
        session.addAssistant("Lo tendré en cuenta");

        String transcript = session.asPromptTranscript();
        assertTrue(transcript.startsWith("USUARIO: Me llamo Bryan"));
        assertTrue(transcript.contains("SALVE: Lo tendré en cuenta"));
    }

    @Test
    public void evictsOldestMessages() {
        ConversationSession session = new ConversationSession(2);
        session.addUser("uno");
        session.addAssistant("dos");
        session.addUser("tres");

        assertEquals(2, session.snapshot().size());
        assertFalse(session.asPromptTranscript().contains("uno"));
        assertTrue(session.asPromptTranscript().contains("tres"));
    }
}
