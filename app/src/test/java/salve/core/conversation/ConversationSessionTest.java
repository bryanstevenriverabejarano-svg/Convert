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

    @Test public void theCodeFragmentRemainsAvailableForTheNextCorrection() {
        ConversationSession session = new ConversationSession();
        session.addUser("Crea una suma");
        session.addAssistant("Revisión del modelo; no compilado.\n```java\nint sum(int a, int b) { return a + b; }\n```");
        session.addUser("Corrige esa función para usar long");
        assertTrue(session.asPromptTranscript().contains("int sum(int a, int b)"));
        assertTrue(session.asPromptTranscript().contains("Corrige esa función"));
    }

    @Test public void characterBudgetEvictsWholeOlderMessages() {
        ConversationSession session = new ConversationSession(16, 300);
        session.addUser(new String(new char[180]).replace('\0', 'a'));
        session.addAssistant(new String(new char[180]).replace('\0', 'b'));
        assertEquals(1, session.snapshot().size());
        assertTrue(session.snapshot().get(0).getContent().startsWith("bbbb"));
    }

    @Test public void anOversizeMessageIsMarkedAndClearResetsTheBudget() {
        ConversationSession session = new ConversationSession(16, 300);
        session.addAssistant(new String(new char[500]).replace('\0', 'x'));
        assertTrue(session.snapshot().get(0).getContent().length() <= 300);
        assertTrue(session.snapshot().get(0).getContent().contains("truncado"));
        session.clear(); session.addUser("Nuevo"); session.addAssistant("Listo");
        assertEquals(2, session.snapshot().size());
    }
}
