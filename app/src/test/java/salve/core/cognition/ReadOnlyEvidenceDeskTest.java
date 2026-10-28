package salve.core.cognition;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=28, application=Application.class)
public class ReadOnlyEvidenceDeskTest {
    @Test public void usesRealJournalStatusWithoutCreatingSyntheticNightSuccess() {
        Application app = RuntimeEnvironment.getApplication();
        ExecutionJournal journal = ExecutionTrace.journal(app);
        journal.clear();
        String id = journal.begin("Sueño autónomo", "Esperando cargador");
        journal.append(id, "PLANIFICACIÓN", "La tarea no se ejecutó por falta de carga");
        journal.finish(id, "ESPERANDO_CARGA");
        String result = ReadOnlyEvidenceDesk.render(app, "both");
        assertTrue(result, result.contains("REGISTRO DE EJECUCIÓN"));
        assertTrue(result, result.contains("Sueño autónomo"));
        assertTrue(result, result.contains("ESPERANDO_CARGA"));
        assertFalse(result, result.contains("COMPLETADO"));
        assertTrue(result, result.contains("INFORME NOCTURNO"));
        assertFalse(result, result.contains("Aprendí a modificarme sola"));
        journal.clear();
    }

    @Test public void invalidScopeCannotExposeAnyRecord() {
        try {
            ReadOnlyEvidenceDesk.render(RuntimeEnvironment.getApplication(), "private-location");
            fail("Invalid scope must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("desconocido"));
        }
    }
}
