package salve.core;
import java.nio.file.Files;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExperimentalLearningRuntimeTest {
    @Test public void exactCommandsAreReachableAndSettingSurvivesRestart() throws Exception {
        File file = new File(Files.createTempDirectory("salve-learning-").toFile(), "state.json");
        try {
            assertTrue(AutonomousToolRuntime.handles("desactiva aprendizaje experimental"));
            AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
            assertTrue(runtime.respond("desactiva aprendizaje experimental", () -> false).contains("desactivado"));
            assertTrue(new AutonomousToolRuntime(file).respond("estado aprendizaje experimental", () -> false).contains("desactivado"));
            assertTrue(runtime.respond("activa aprendizaje experimental", () -> true).contains("cancelada"));
        } finally { file.delete(); file.getParentFile().delete(); }
    }
}
