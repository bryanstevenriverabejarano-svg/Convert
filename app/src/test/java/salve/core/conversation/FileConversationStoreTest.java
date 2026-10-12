package salve.core.conversation;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class FileConversationStoreTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private File file() { return new File(temp.getRoot(), "conversation/recent.bin"); }
    private ConversationSession session() { return new ConversationSession(new FileConversationStore(file())); }

    @Test public void restartPreservesRolesTextDatesAndFollowUpContext() throws Exception {
        ConversationSession first = session();
        first.addUser("Estoy preparando un proyecto de música 🎵");
        first.addAssistant("Podemos empezar con tres canciones.");
        long original = first.snapshot().get(0).getTimestamp();
        first.close();
        ConversationSession reopened = session(); // load orders itself after pending writes
        assertTrue(reopened.hasPriorContext());
        assertEquals(2, reopened.snapshot().size());
        assertEquals(original, reopened.snapshot().get(0).getTimestamp());
        assertEquals(ChatMessage.Role.ASSISTANT, reopened.snapshot().get(1).getRole());
        assertTrue(reopened.asPromptTranscript().contains("música 🎵"));
        assertFalse(ConversationRequestAnalyzer.analyze("continúa", reopened.hasPriorContext()).needsClarification());
    }

    @Test public void restoringTextDoesNotRestoreCurrentLocation() throws Exception {
        ConversationSession first = session(); first.addUser("Estoy en Quito"); first.close();
        ConversationSession reopened = session();
        assertTrue(reopened.asPromptTranscript().contains("Quito"));
        assertTrue(reopened.relevantLocationContext("¿Dónde estoy?").contains("no hay una ubicación"));
        reopened.addUser("Estoy en Lima");
        assertTrue(reopened.relevantLocationContext("¿Dónde estoy?").contains("Lima"));
        reopened.close(); FileConversationStore.awaitWrites();
    }

    @Test public void clearPersistsAndLateWritesAfterCloseCannotResurrectHistory() throws Exception {
        ConversationSession first = session(); first.addUser("Viejo"); first.addAssistant("Anterior");
        first.clear(); first.close(); first.addAssistant("Respuesta tardía");
        assertFalse(session().hasPriorContext());
    }

    @Test public void boundedSnapshotEvictsHistoryAcrossRestarts() throws Exception {
        ConversationSession first = session();
        for (int i = 0; i < 40; i++) first.addUser("Mensaje " + i + " " + "x".repeat(500));
        first.close(); ConversationSession second = session();
        assertTrue(second.snapshot().size() <= 16);
        assertTrue(second.snapshot().stream().mapToInt(m -> m.getContent().length()).sum() <= 8000);
        assertTrue(second.asPromptTranscript().contains("Mensaje 39"));
        assertFalse(second.asPromptTranscript().contains("Mensaje 0 "));
    }

    @Test public void corruptCheckpointIsReportedAndPreservedUntilExplicitClear() throws Exception {
        File path = file(); path.getParentFile().mkdirs();
        byte[] broken = {1, 2, 3}; Files.write(path.toPath(), broken);
        ConversationSession first = session();
        assertFalse(first.hasPriorContext()); assertFalse(first.persistenceWarning().isEmpty());
        first.addUser("Nuevo mensaje"); FileConversationStore.awaitWrites();
        assertArrayEquals(broken, Files.readAllBytes(path.toPath()));
        first.clear(); first.addUser("Después del reinicio explícito"); first.close();
        assertTrue(session().asPromptTranscript().contains("Después del reinicio explícito"));
    }

    @Test public void failedWritePreservesPreviousCheckpointAndSurfacesWarning() throws Exception {
        FileConversationStore store = new FileConversationStore(file());
        store.save(Collections.singletonList(new ChatMessage(ChatMessage.Role.USER, "Conservado", 123)));
        FileConversationStore.awaitWrites();
        store.save(Collections.singletonList(new ChatMessage(ChatMessage.Role.USER, "x".repeat(8001), 124)));
        FileConversationStore.awaitWrites(); assertFalse(store.warning().isEmpty());
        assertEquals("Conservado", new FileConversationStore(file()).load().get(0).getContent());
    }

    @Test public void diskFailureDoesNotBreakInMemoryConversation() throws Exception {
        File blocked = temp.newFile("blocked");
        ConversationSession session = new ConversationSession(new FileConversationStore(new File(blocked, "recent.bin")));
        session.addUser("Hola"); session.addAssistant("Seguimos aquí");
        FileConversationStore.awaitWrites();
        assertEquals(2, session.snapshot().size()); assertFalse(session.persistenceWarning().isEmpty());
    }

    @Test public void oversizedCheckpointIsNotLoadedOrSilentlyOverwritten() throws Exception {
        File path = file(); path.getParentFile().mkdirs(); Files.write(path.toPath(), new byte[48001]);
        ConversationSession session = session();
        assertFalse(session.hasPriorContext()); assertFalse(session.persistenceWarning().isEmpty());
        session.addUser("Hola"); FileConversationStore.awaitWrites(); assertEquals(48001, path.length());
    }
}
