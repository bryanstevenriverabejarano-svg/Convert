package salve.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class AutonomousToolRuntimeTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final String CHALLENGE = "{\"schema\":1,\"id\":\"consulta\",\"family\":\"dependencies\","
            + "\"input\":{\"nodes\":3,\"edges\":[[0,1],[1,2]]}}";

    @Test public void directCommandRunsRealLabAndPersistsVerifiedTool() throws Exception {
        File file = new File(folder.getRoot(), "tools.json");
        String result = new AutonomousToolRuntime(file).respond("resuelve reto: " + CHALLENGE, () -> false);
        assertTrue(result, result.startsWith("Resultado verificado"));
        assertTrue(result, result.contains("quedó registrada"));
        assertTrue(file.isFile());
        String reloaded = new AutonomousToolRuntime(file).respond("resuelve reto: " + CHALLENGE, () -> false);
        assertTrue(reloaded, reloaded.startsWith("Resultado verificado"));
    }

    @Test public void pauseAndResumeControlRealSolves() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        assertTrue(runtime.respond("pausa el laboratorio", () -> false).contains("pausado"));
        assertTrue(runtime.respond("resuelve reto: " + CHALLENGE, () -> false).contains("está pausado"));
        assertFalse(file.exists());
        assertTrue(runtime.respond("reanuda el laboratorio", () -> false).contains("reanudado"));
        assertTrue(runtime.respond("resuelve reto: " + CHALLENGE, () -> false).startsWith("Resultado verificado"));
    }

    @Test public void cancelledTurnDoesNotPersistATool() {
        File file = new File(folder.getRoot(), "tools.json");
        String result = new AutonomousToolRuntime(file).respond("resuelve reto: " + CHALLENGE, () -> true);
        assertTrue(result, result.contains("cancelado"));
        assertFalse(file.exists());
    }

    @Test public void immediatePauseDuringSolveRemainsCancelledAfterResume() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        AtomicBoolean once = new AtomicBoolean();
        String result = runtime.respond("resuelve reto: " + CHALLENGE, () -> {
            if (once.compareAndSet(false, true)) {
                runtime.pause();
                runtime.respond("reanuda el laboratorio", () -> false);
            }
            return false;
        });
        assertTrue(result, result.contains("cancelado"));
        assertFalse(file.exists());
    }

    @Test public void structuredModelCallShowsTheInterpretedInputAndScope() {
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(new File(folder.getRoot(), "tools.json"));
        String result = runtime.respondToModel("{\"tool\":\"SOLVE_CHALLENGE\",\"challenge\":" + CHALLENGE + "}", () -> false);
        assertTrue(result, result.startsWith("Resultado verificado"));
        assertTrue(result, result.contains("Datos interpretados: " + CHALLENGE));
        assertTrue(result, result.contains("no que el modelo haya interpretado correctamente"));
    }

    @Test public void malformedModelToolCannotExecuteOrCreateState() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        String valid = "{\"tool\":\"SOLVE_CHALLENGE\",\"challenge\":" + CHALLENGE + "}";
        for (String invalid : new String[] {"Propuesta: " + valid, "```json\n" + valid + "\n```",
                valid + "{}", valid.substring(0, valid.length() - 1) + ",\"tool\":\"TAP\"}",
                "{\"tool\":\"TAP\",\"challenge\":" + CHALLENGE + "}",
                "{\"tool\":\"SOLVE_CHALLENGE\",\"challenge\":\"shell\"}"}) {
            assertTrue(runtime.respondToModel(invalid, () -> false).contains("No lo ejecuté"));
            assertFalse(file.exists());
        }
    }

    @Test public void corruptStateIsPreservedAndDoesNotCrashConversation() throws Exception {
        File file = folder.newFile("tools.json");
        Files.write(file.toPath(), "corrupt-preserve".getBytes(StandardCharsets.UTF_8));
        String result = new AutonomousToolRuntime(file).respond("resuelve reto: " + CHALLENGE, () -> false);
        assertTrue(result, result.contains("no está disponible"));
        assertEquals("corrupt-preserve", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test public void rawUserToolJsonIsNotACommandAndOrdinaryChatIsUntouched() {
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(new File(folder.getRoot(), "tools.json"));
        assertNull(runtime.respond("{\"tool\":\"SOLVE_CHALLENGE\",\"challenge\":" + CHALLENGE + "}", () -> false));
        assertNull(runtime.respond("¿Cuál fue tu primer recuerdo?", () -> false));
        assertFalse(AutonomousToolRuntime.handles("Explícame qué es el laboratorio"));
    }

    @Test public void toolContractSurvivesRealPromptBudgetBeforeOptionalPersonality() {
        String input = "Ruta más corta con 3 nodos: 0 a 1 peso 2, 1 a 2 peso 3. Origen 0 destino 2.";
        String contract = AutonomousToolRuntime.instructionFor(input);
        String prompt = salve.core.conversation.GroundedConversationPrompt.build(
                contract + "PERSONALIDAD: configuración opcional\n", java.util.Collections.emptyList(),
                input, "", "Hora del dispositivo: 12:00 Europe/Madrid", "", 3200);
        assertTrue(prompt, prompt.contains("SOLVE_CHALLENGE"));
        assertTrue(prompt, prompt.contains("no tu interpretación"));
        assertFalse(contract, contract.contains("knapsack"));
        assertEquals("", AutonomousToolRuntime.instructionFor("¿Qué hora es?"));
    }

    @Test public void proceduralMemorySurvivesRestartWithoutExposingChallengeData() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        assertTrue(runtime.respond("resuelve reto: " + CHALLENGE.replace("consulta", "private_user_canary"),
                () -> false).startsWith("Resultado verificado"));
        String query = "Ordena las dependencias de 3 nodos: 0 a 1 y 1 a 2.";
        String metadata = runtime.proceduralContextFor(query);
        assertFalse(metadata.isEmpty());
        assertTrue(metadata, metadata.contains("dependencies"));
        assertFalse(metadata, metadata.contains("private_user_canary"));
        assertFalse(metadata, metadata.contains("\"input\""));
        assertFalse(metadata, metadata.contains("\"edges\""));
        assertFalse(metadata, metadata.contains("\"result\""));
        assertEquals(metadata, new AutonomousToolRuntime(file).proceduralContextFor(query));
        assertTrue(runtime.respond("¿Qué herramientas has aprendido?", () -> false).contains("dependencies"));
    }

    @Test public void proceduralMemoryIsNotRetrievedForUnrelatedQuestionsOrOtherFamilies() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        assertEquals("", runtime.proceduralContextFor("¿Cuál fue tu primer recuerdo?"));
        assertFalse(file.exists());
        runtime.respond("resuelve reto: " + CHALLENGE, () -> false);
        assertEquals("", runtime.proceduralContextFor("¿Qué hora es?"));
        assertEquals("", runtime.proceduralContextFor("Ruta más corta con 3 nodos: 0 a 1 peso 2."));
    }

    @Test public void proceduralMemoryReadFailureMakesNoEmptyMemoryClaimAndPreservesFile() throws Exception {
        File file = folder.newFile("tools.json");
        Files.write(file.toPath(), "preserve-corrupt-state".getBytes(StandardCharsets.UTF_8));
        assertEquals("", new AutonomousToolRuntime(file).proceduralContextFor("Ordena dependencias de 3 nodos"));
        assertEquals("preserve-corrupt-state", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test public void relevantProceduralMemoryAndToolContractFitTheSmallPromptTogether() {
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(new File(folder.getRoot(), "tools.json"));
        runtime.respond("resuelve reto: " + CHALLENGE, () -> false);
        String input = "Ordena las dependencias de 3 nodos: 0 a 1 y 1 a 2.";
        String metadata = runtime.proceduralContextFor(input);
        String prompt = salve.core.conversation.GroundedConversationPrompt.build(
                AutonomousToolRuntime.instructionFor(input), java.util.Collections.emptyList(), input, "",
                metadata + "\nHora del dispositivo: 12:00 Europe/Madrid", "", 3200);
        assertFalse(metadata.isEmpty());
        assertTrue(prompt, prompt.contains("SOLVE_CHALLENGE"));
        assertTrue(prompt, prompt.contains("dependencies"));
        assertTrue(prompt, prompt.contains("MEMORIA PROCEDIMENTAL"));
        assertTrue(prompt, prompt.contains("programa_sha256"));
        assertTrue(prompt, prompt.contains("evidencia finita"));
        assertTrue(prompt, prompt.contains("no tu interpretación"));
        assertFalse(prompt, prompt.contains("[Contenido truncado]"));
    }
}
