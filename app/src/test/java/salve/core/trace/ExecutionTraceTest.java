package salve.core.trace;

import android.app.Application;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=28, application=Application.class)
public class ExecutionTraceTest {
    @Before public void reset() { ExecutionTrace.finish("TERMINADA"); ExecutionTrace.journal(RuntimeEnvironment.getApplication()).clear(); }
    @After public void stop() { ExecutionTrace.finish("TERMINADA"); }
    @Test public void backgroundThreadsDoNotInheritTheConversationBuffer() throws Exception {
        ExecutionTrace.begin(RuntimeEnvironment.getApplication(), "Dibujo", "sombrero");
        Thread background = new Thread(() -> ExecutionTrace.note("OTRA TAREA", "NO MEZCLAR"));
        background.start(); background.join();
        ExecutionTrace.note("NOTA", "nota exacta\n  segunda línea"); ExecutionTrace.finish("TERMINADA");
        ExecutionTrace.note("TARDÍA", "NO MEZCLAR");
        String trace = ExecutionJournal.transcript(ExecutionTrace.journal(RuntimeEnvironment.getApplication()).recent().get(0));
        assertTrue(trace.contains("nota exacta\n  segunda línea")); assertFalse(trace.contains("NO MEZCLAR"));
    }
    @Test public void journalLivesOutsideNormalBackupAndSyncFiles() {
        Application app = RuntimeEnvironment.getApplication(); ExecutionTrace.begin(app, "Dibujo", "sombrero");
        assertTrue(new java.io.File(app.getNoBackupFilesDir(), "execution-trace/execution.json").isFile());
        assertFalse(new java.io.File(app.getFilesDir(), "execution-trace/execution.json").exists());
    }
}
