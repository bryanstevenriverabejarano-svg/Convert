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

    @Test public void conversationalTimingContainsOnlyAllowedMetrics() {
        Application app = RuntimeEnvironment.getApplication();
        ExecutionTrace.beginConversationMetrics(app);
        ExecutionTrace.beginConversationMetrics(app); // no duplicate journal per turn
        ExecutionTrace.note("ESTADO", "mensaje privado: NO_GUARDAR_ESTO");
        ExecutionTrace.note("VELOCIDAD", "primera palabra en 1,5 s · GPU");
        ExecutionTrace.note("RENDIMIENTO DEL MODELO", "inferencia 1 en 2 s");
        ExecutionTrace.note("TIEMPO DEL TURNO", "1 inferencia, 2 s");
        ExecutionTrace.finish("TURNO_TERMINADO");

        java.util.List<ExecutionJournal.Task> tasks = ExecutionTrace.journal(app).recent();
        assertEquals(1, tasks.size());
        String report = ExecutionJournal.transcript(tasks.get(0));
        assertTrue(report.contains("Conversación · rendimiento"));
        assertTrue(report.contains("VELOCIDAD"));
        assertTrue(report.contains("RENDIMIENTO DEL MODELO"));
        assertTrue(report.contains("TIEMPO DEL TURNO"));
        assertFalse(report.contains("NO_GUARDAR_ESTO"));
        assertEquals("TURNO_TERMINADO", tasks.get(0).status);
    }

    @Test public void specialTaskKeepsItsOwnLogAlongsideConversationMetrics() {
        Application app = RuntimeEnvironment.getApplication();
        ExecutionTrace.beginConversationMetrics(app);
        ExecutionTrace.begin(app, "Investigación web", "tema de investigación");
        ExecutionTrace.note("ESTADO", "leyendo fuentes públicas");
        ExecutionTrace.note("VELOCIDAD", "primera palabra en 2 s");
        ExecutionTrace.finish("TURNO_TERMINADO");

        java.util.List<ExecutionJournal.Task> tasks = ExecutionTrace.journal(app).recent();
        assertEquals(2, tasks.size());
        int metricReports = 0, researchReports = 0;
        for (ExecutionJournal.Task task : tasks) {
            String report = ExecutionJournal.transcript(task);
            if ("Conversación · rendimiento".equals(task.type)) {
                metricReports++;
                assertTrue(report.contains("VELOCIDAD"));
                assertFalse(report.contains("leyendo fuentes públicas"));
                assertFalse(report.contains("tema de investigación"));
            } else if ("Investigación web".equals(task.type)) {
                researchReports++;
                assertTrue(report.contains("leyendo fuentes públicas"));
                assertTrue(report.contains("VELOCIDAD"));
            }
        }
        assertEquals(1, metricReports);
        assertEquals(1, researchReports);
    }

    @Test public void newTurnAfterDiagnosticsIsNotFiltered() {
        Application app = RuntimeEnvironment.getApplication();
        ExecutionTrace.beginConversationMetrics(app);
        ExecutionTrace.finish("TURNO_TERMINADO");
        ExecutionTrace.begin(app, "Dibujo", "sombrero");
        ExecutionTrace.note("CONTEXTO", "mi siguiente dibujo");
        ExecutionTrace.finish("TERMINADA");
        String report = ExecutionJournal.transcript(ExecutionTrace.journal(app).recent().get(0));
        assertTrue(report.contains("mi siguiente dibujo"));
    }
}
