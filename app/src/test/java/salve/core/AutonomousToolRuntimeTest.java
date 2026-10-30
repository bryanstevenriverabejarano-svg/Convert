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

    @Test public void pCloudSkillRestorationNeedsExplicitOwnerAndPausedLaboratory() throws Exception {
        File state = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(state);
        File inbox = new File(new File(folder.getRoot(), "cloud-inbox"), "inductive-skills.json");
        String learned = "{\"schema\":1,\"skills\":{\"sorted_values\":"
                + "{\"program\":[\"sort\"],\"training\":3,\"holdout\":3}}}";
        String envelope = salve.data.sync.SkillCloudArchive.pack(
                salve.data.sync.SkillCloudArchive.INDUCTIVE, learned, 1_000L);
        String remote = salve.data.sync.SkillCloudArchive.unpack(envelope, null).remotePath();
        File target = new File(folder.getRoot(), "inductive-skills.json");
        assertTrue(salve.data.sync.SkillCloudArchive.stageIfMissing(target, inbox, envelope, remote));
        assertTrue(AutonomousToolRuntime.handles("estado habilidades pcloud"));
        assertTrue(AutonomousToolRuntime.handles("restaura habilidades pcloud"));
        assertFalse(target.exists());
        assertTrue(runtime.restoreStagedForVerifiedOwner(false).contains("denegada"));
        assertTrue(runtime.restoreStagedForVerifiedOwner(true).contains("pausa"));
        runtime.pause();
        assertTrue(runtime.restoreStagedForVerifiedOwner(true).contains("Restauré 1"));
        assertEquals(learned, new salve.core.autonomy.FileToolStateStore(target).read());
        assertTrue(runtime.respond("habilidades aprendidas", () -> false).contains("pausado"));
        assertFalse(runtime.restoreStagedForVerifiedOwner(true).contains("Restauré 1"));
        assertTrue(inbox.exists());
    }

    @Test public void explicitBlindLearningExperimentDoesNotInstallOrWritePersonalHistory() {
        File state = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(state);
        assertTrue(AutonomousToolRuntime.handles("experimento de aprendizaje"));
        assertFalse(AutonomousToolRuntime.handles("puedes experimentar con algo"));
        String report = runtime.respond("experimento de aprendizaje", () -> false);
        assertTrue(report, report.contains("EXPERIMENTO DE APRENDIZAJE LOCAL"));
        assertTrue(report, report.contains("\"skill_installed\":false"));
        assertTrue(report, report.contains("\"final_exam_reused_for_training\":false"));
        assertFalse("no instalar ninguna habilidad desde el experimento",
                new File(folder.getRoot(), "inductive-skills.json").exists());
        assertFalse(state.exists());
        assertFalse(new File(folder.getRoot(), "proposed-inductive-skill.json").exists());
        runtime.pause();
        assertTrue(runtime.respond("experimento de aprendizaje", () -> false)
                .contains("laboratorio pausado"));
        assertFalse(new File(folder.getRoot(), "inductive-skills.json").exists());
    }

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

    @Test public void threeStageSkillIsLearnedThroughRealUserCommandAndSurvivesRestart() {
        File file = new File(folder.getRoot(), "tools.json");
        String training = "{\"name\":\"sorted_unique_odds\",\"train\":["
                + "{\"in\":[7,3,7,4,1,3],\"out\":[1,3,7]},"
                + "{\"in\":[-1,2,-3,7,-1,7],\"out\":[-3,-1,7]},"
                + "{\"in\":[9,8,3,9,2,-5,3],\"out\":[-5,3,9]}],"
                + "\"holdout\":["
                + "{\"in\":[5,-7,5,2,-1],\"out\":[-7,-1,5]},"
                + "{\"in\":[1,8,7,1,-7,3],\"out\":[-7,1,3,7]},"
                + "{\"in\":[4,6,2,0,5],\"out\":[5]}]}";
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        String learned = runtime.respond("aprende habilidad: " + training, () -> false);
        assertTrue(learned, learned.contains("CERTIFIED_ON_PROVIDED_HOLDOUT"));
        assertTrue(learned, learned.contains("candidates_tested"));
        assertTrue(learned, learned.contains("selected_length"));
        String request = "usa habilidad: {\"name\":\"sorted_unique_odds\",\"input\":[5,3,5,2,1]}";
        String output = new AutonomousToolRuntime(file).respond(request, () -> false);
        assertTrue(output, output.contains("[1,3,5]"));
        assertTrue(new File(folder.getRoot(), "inductive-skills.json").isFile());
        assertFalse("los ejemplos no deben guardarse en el estado",
                new File(folder.getRoot(), "inductive-skills.json").length() > 1500);
    }

    @Test public void proposalRequiresExactConsentAndPersistsOnlySafeMetadata() throws Exception {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime proposer = new AutonomousToolRuntime(file);
        assertTrue(AutonomousToolRuntime.handles("propón habilidad: ordenar impares sin repetir"));
        assertTrue(AutonomousToolRuntime.handles("propuestas de habilidades"));
        assertFalse(AutonomousToolRuntime.handles("quiero que modifiques mi teléfono"));
        String proposed = proposer.respond("propón habilidad: ordenar impares sin repetir", () -> false);
        assertTrue(proposed, proposed.contains("Propuesta NO instalada"));
        assertTrue(proposed, proposed.contains("96/96"));
        File pending = new File(folder.getRoot(), "proposed-inductive-skill.json");
        File installed = new File(folder.getRoot(), "inductive-skills.json");
        assertTrue(pending.isFile());
        assertFalse(installed.exists()); // No promotion without user approval.
        String metadata = new String(Files.readAllBytes(pending.toPath()), StandardCharsets.UTF_8);
        assertFalse(metadata.contains("\"train\""));
        assertFalse(metadata.contains("\"holdout\""));
        String id = com.google.gson.JsonParser.parseString(metadata)
                .getAsJsonObject().get("id").getAsString();
        AutonomousToolRuntime afterRestart = new AutonomousToolRuntime(file);
        assertTrue(afterRestart.respond("propuestas de habilidades", () -> false).contains(id));
        assertTrue(afterRestart.respond("aprueba habilidad 00000000-0000-0000-0000-000000000000",
                () -> false).contains("no corresponde"));
        assertFalse(installed.exists());
        String success = afterRestart.respond("aprueba habilidad " + id, () -> false);
        assertTrue(success, success.contains("aprobada, verificada y guardada"));
        assertTrue(installed.isFile());
        assertFalse(pending.exists());
        String use = new AutonomousToolRuntime(file).respond(
                "usa habilidad: {\"name\":\"auto_odd_sorted\",\"input\":[7,3,7,2,1]}", () -> false);
        assertTrue(use, use.contains("[1,3,7]"));
        assertTrue(afterRestart.respond("aprueba habilidad " + id, () -> false)
                .contains("No existe ninguna propuesta"));
    }

    @Test public void sequenceSkillProposalsNeedOwnerApprovalAndSurviveRestart() throws Exception {
        File state = new File(folder.getRoot(), "tools.json");
        File installed = new File(folder.getRoot(), "inductive-skills.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(state);
        String[] needs = {"sumas acumuladas", "diferencias consecutivas"};
        String[] names = {"auto_running_total", "auto_adjacent_delta"};
        String[] outputs = {"[1,3,6]", "[2,3]"};
        for (int i = 0; i < needs.length; i++) {
            String proposal = runtime.respond("propón habilidad: " + needs[i], () -> false);
            assertTrue(proposal, proposal.contains("Propuesta NO instalada"));
            assertTrue(proposal, proposal.contains("96/96"));
            String pending = new String(java.nio.file.Files.readAllBytes(new File(
                    folder.getRoot(), "proposed-inductive-skill.json").toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            String id = com.google.gson.JsonParser.parseString(pending).getAsJsonObject()
                    .get("id").getAsString();
            assertFalse(pending.contains("holdout"));
            assertTrue(runtime.respond("aprueba habilidad " + id, () -> false)
                    .contains("aprobada, verificada y guardada"));
            assertTrue(installed.exists());
            com.google.gson.JsonObject call = new com.google.gson.JsonObject();
            call.addProperty("name", names[i]);
            com.google.gson.JsonArray values = new com.google.gson.JsonArray();
            values.add(1); values.add(3); values.add(6);
            call.add("input", values);
            String request = "usa habilidad: " + call.toString();
            String output = new AutonomousToolRuntime(state).respond(request, () -> false);
            assertTrue(output, output.contains(outputs[i]));
            // No changes to the already installed catalog when the same skill is proposed again.
            String again = runtime.respond("propón habilidad: " + needs[i], () -> false);
            assertFalse(again.contains("Propuesta NO instalada"));
        }
        String saved = new String(java.nio.file.Files.readAllBytes(installed.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(saved.contains("auto_running_total"));
        assertTrue(saved.contains("auto_adjacent_delta"));
    }

    @Test public void proposalsCanBeDiscardedOrCancelledWithoutWritingInstalledSkills() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        assertTrue(runtime.respond("propón habilidad: ordenar pares sin repetir", () -> true)
                .contains("cancelada"));
        assertFalse(new File(folder.getRoot(), "inductive-skills.json").exists());
        String proposed = runtime.respond("propón habilidad: ordenar pares sin repetir", () -> false);
        assertTrue(proposed, proposed.contains("Propuesta NO instalada"));
        String id = com.google.gson.JsonParser.parseString(
                readPending(new File(folder.getRoot(), "proposed-inductive-skill.json")))
                .getAsJsonObject().get("id").getAsString();
        assertTrue(runtime.respond("descarta habilidad " + id, () -> false)
                .contains("Propuesta descartada"));
        assertFalse(new File(folder.getRoot(), "inductive-skills.json").exists());
        runtime.pause();
        assertTrue(runtime.respond("propón habilidad: ordenar pares sin repetir", () -> false)
                .contains("pausado"));
        assertFalse(new File(folder.getRoot(), "inductive-skills.json").exists());
    }
    private static String readPending(File file) {
        try { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
        catch (java.io.IOException error) { throw new AssertionError(error); }
    }

    private static salve.core.agent.AgentRun failedPlan(String id, String goal) {
        salve.core.agent.AgentRun run = new salve.core.agent.AgentRun();
        run.id = id; run.goal = goal; run.status = "PARTIAL";
        run.updatedAt = System.currentTimeMillis() - 2000;
        run.observations.add(new salve.core.agent.AgentRun.Observation("tool-" + id,
                "web.search", "BLOCKED", "No hay estrategia adecuada"));
        return run;
    }

    @Test public void repeatedRealPlanFailuresCreateReadOnlySuggestionAndRequireApproval() throws Exception {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        java.util.List<salve.core.agent.AgentRun> plans = java.util.Arrays.asList(
                failedPlan("first", "ordena impares sin repetir"),
                failedPlan("second", "ordenar impares sin repetir"));
        assertTrue(runtime.observeFailedPlanHistory(plans, () -> false));
        File pending = new File(folder.getRoot(), "proposed-inductive-skill.json");
        File installed = new File(folder.getRoot(), "inductive-skills.json");
        File cooldown = new File(folder.getRoot(), "auto-skill-suggestion-cooldown.json");
        assertTrue(pending.isFile());
        assertTrue(cooldown.isFile());
        assertFalse(installed.exists());
        String marker = new String(Files.readAllBytes(cooldown.toPath()), StandardCharsets.UTF_8);
        assertFalse(marker.contains("ordena"));
        assertFalse(marker.contains("first"));
        assertTrue(runtime.pendingSkillSuggestion().contains("NO instalada"));
        String id = com.google.gson.JsonParser.parseString(
                new String(Files.readAllBytes(pending.toPath()), StandardCharsets.UTF_8))
                .getAsJsonObject().get("id").getAsString();
        AutonomousToolRuntime rebooted = new AutonomousToolRuntime(file);
        assertFalse(rebooted.observeFailedPlanHistory(plans, () -> false));
        assertTrue(rebooted.pendingSkillSuggestion().contains("ordenar impares sin repetir"));
        assertTrue(rebooted.respond("propuestas de habilidades", () -> false).contains(id));
        assertTrue(rebooted.respond("aprueba habilidad " + id, () -> false)
                .contains("aprobada, verificada y guardada"));
        assertTrue(installed.isFile());
        assertFalse(pending.exists());
        assertFalse(new AutonomousToolRuntime(file).observeFailedPlanHistory(plans, () -> false));
    }

    @Test public void dismissedSuggestionStaysSuppressedAcrossRestartsAndPause() throws Exception {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        java.util.List<salve.core.agent.AgentRun> plans = java.util.Arrays.asList(
                failedPlan("one", "ordena pares sin repetir"),
                failedPlan("two", "ordenar pares sin repetir"));
        assertTrue(runtime.observeFailedPlanHistory(plans, () -> false));
        File pending = new File(folder.getRoot(), "proposed-inductive-skill.json");
        String id = com.google.gson.JsonParser.parseString(
                new String(Files.readAllBytes(pending.toPath()), StandardCharsets.UTF_8))
                .getAsJsonObject().get("id").getAsString();
        assertTrue(runtime.respond("descarta habilidad " + id, () -> false)
                .contains("descartada"));
        assertFalse(pending.exists());
        AutonomousToolRuntime reopened = new AutonomousToolRuntime(file);
        assertFalse(reopened.observeFailedPlanHistory(plans, () -> false));
        assertEquals("", reopened.pendingSkillSuggestion());
        reopened.pause();
        assertFalse(reopened.observeFailedPlanHistory(plans, () -> false));
        assertFalse(new File(folder.getRoot(), "inductive-skills.json").exists());
    }

    @Test public void observerRefusesSingleFailureAndStopWithoutWritingFiles() {
        File file = new File(folder.getRoot(), "tools.json");
        AutonomousToolRuntime runtime = new AutonomousToolRuntime(file);
        java.util.List<salve.core.agent.AgentRun> plans = java.util.Arrays.asList(
                failedPlan("one", "ordena pares sin repetir"),
                failedPlan("two", "ordena pares sin repetir"));
        assertFalse(runtime.observeFailedPlanHistory(plans.subList(0, 1), () -> false));
        assertFalse(runtime.observeFailedPlanHistory(plans, () -> true));
        assertFalse(new File(folder.getRoot(), "proposed-inductive-skill.json").exists());
        assertFalse(new File(folder.getRoot(), "auto-skill-suggestion-cooldown.json").exists());
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
