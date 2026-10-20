package salve.core.trace;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ExecutionJournalTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void notesSurviveRestartWithoutRewritingWhitespaceOrUnicode() throws Exception {
        File dir = temp.newFolder(); ExecutionJournal journal = new ExecutionJournal(dir);
        String request = "Dibuja  un sombrero\npor favor";
        String note = "  línea 1\n\n\tlínea 2: «ñ» 🖉  \r\n<no ejecutar>";
        String id = journal.begin("Dibujo", request); journal.append(id, "NOTA", note); journal.finish(id, "TURNO_TERMINADO");
        ExecutionJournal.Task saved = new ExecutionJournal(dir).recent().get(0);
        assertEquals(request, saved.events.get(0).text); assertEquals(note, saved.events.get(1).text);
        assertTrue(ExecutionJournal.transcript(saved).contains(note));
        assertEquals("TURNO_TERMINADO", saved.status); assertTrue(saved.ended >= saved.started);
    }
    @Test public void lateCallbacksCannotWriteIntoAnotherTaskOrAClosedTask() throws Exception {
        ExecutionJournal journal = new ExecutionJournal(temp.newFolder());
        String a = journal.begin("Dibujo", "sombrero"); String b = journal.begin("Búsqueda", "luna");
        journal.append(a, "NOTA", "sólo del sombrero"); journal.finish(a, "CANCELADA");
        journal.append(a, "NOTA", "demasiado tarde"); journal.append(b, "NOTA", "sólo de la luna");
        assertFalse(ExecutionJournal.transcript(journal.recent().get(0)).contains("sombrero"));
        assertFalse(ExecutionJournal.transcript(journal.recent().get(1)).contains("demasiado tarde"));
        assertEquals(2, journal.recent().get(1).events.size());
    }
    @Test public void limitsOmitWholeEventsAndExplicitlyMarkIncompleteTranscript() throws Exception {
        ExecutionJournal journal = new ExecutionJournal(temp.newFolder()); String id = journal.begin("Dibujo", "sombrero");
        journal.append(id, "NOTA", new String(new char[ExecutionJournal.MAX_TASK_CHARS]).replace('\0', 'x'));
        for (int i = 0; i < ExecutionJournal.MAX_EVENTS + 1; i++) journal.append(id, "NOTA", "texto " + i);
        ExecutionJournal.Task task = journal.recent().get(0);
        assertEquals(ExecutionJournal.MAX_EVENTS, task.events.size()); assertTrue(task.omitted >= 2);
        assertTrue(ExecutionJournal.transcript(task).contains("transcripción está incompleta"));
        assertFalse(ExecutionJournal.transcript(task).contains("xxxxxxxx"));
    }
    @Test public void boundedHistoryAndClearDoNotResurrectClearedTasks() throws Exception {
        File dir = temp.newFolder(); ExecutionJournal journal = new ExecutionJournal(dir); String last = "";
        for (int i = 0; i < 23; i++) last = journal.begin("Dibujo", "petición " + i);
        assertEquals(20, journal.recent().size()); assertEquals("petición 22", journal.recent().get(0).events.get(0).text);
        journal.clear(); journal.append(last, "NOTA", "tardía"); journal.finish(last, "TERMINADA");
        assertTrue(new ExecutionJournal(dir).recent().isEmpty());
    }
    @Test public void interruptedProcessIsNotReportedAsStillRunning() throws Exception {
        File dir = temp.newFolder(); new ExecutionJournal(dir).begin("Dibujo", "sombrero");
        assertEquals("INTERRUMPIDA_AL_CERRAR", new ExecutionJournal(dir).recent().get(0).status);
    }
    @Test public void damagedFileIsPreservedUntilUserClearsIt() throws Exception {
        File dir = temp.newFolder(); File file = new File(dir, "execution.json");
        Files.write(file.toPath(), "{roto".getBytes(StandardCharsets.UTF_8));
        ExecutionJournal journal = new ExecutionJournal(dir);
        assertNull(journal.begin("Dibujo", "otro")); assertFalse(journal.notice().isEmpty());
        assertEquals("{roto", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        journal.clear(); assertNotNull(journal.begin("Dibujo", "nuevo"));
    }
    @Test public void storageFailureIsVisibleAndSnapshotsCannotMutateJournal() throws Exception {
        File blocked = temp.newFile(); ExecutionJournal journal = new ExecutionJournal(blocked);
        journal.begin("Dibujo", "sombrero"); assertFalse(journal.notice().isEmpty());
        journal.recent().get(0).events.clear(); assertEquals(1, journal.recent().get(0).events.size());
    }
}
