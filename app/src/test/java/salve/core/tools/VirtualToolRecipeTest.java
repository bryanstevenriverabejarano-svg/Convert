package salve.core.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

public class VirtualToolRecipeTest {
    private static final String OPEN = "{\"tool\":\"ABRIR_APP\",\"paquete\":\"com.whatsapp\"}";
    private static final String TAP = "{\"tool\":\"TAP_ID\",\"paquete\":\"com.whatsapp\",\"texto\":\"Buscar\"}";
    private static final String WRITE = "{\"tool\":\"ESCRIBIR\",\"paquete\":\"com.whatsapp\",\"texto\":\"Hola\"}";

    @Test public void validatesAndRoundTripsStableSelectorsWithoutTransientIds() {
        VirtualToolRecipe recipe = VirtualToolRecipe.parse("[" + OPEN + "," + TAP + "," + WRITE + "]");
        assertEquals(3, recipe.getSteps().size());
        assertEquals(recipe.getSteps(), VirtualToolRecipe.parse(recipe.toJson()).getSteps());
        assertTrue(recipe.getSteps().get(1).contains("Buscar"));
    }

    @Test public void keepsFullNamesAndNormalizesCaseAndAccents() {
        assertEquals("buscar a mamá", VirtualToolRecipe.normalizeName("  Buscar a mama\u0301  "));
        assertFalse(VirtualToolRecipe.normalizeName("buscar a mamá")
                .equals(VirtualToolRecipe.normalizeName("buscar a papá")));
    }

    @Test public void rejectsMissingOverlongOrControlCharacterNames() {
        for (String name : Arrays.asList(null, "", "  ", "rutina\nnueva", "a".repeat(65), "../rutina")) {
            try {
                VirtualToolRecipe.normalizeName(name);
                fail("Accepted invalid name");
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void requiresOneToEightSteps() {
        reject("[]");
        assertEquals(8, VirtualToolRecipe.parse("[" + String.join(",", java.util.Collections.nCopies(8, OPEN)) + "]")
                .getSteps().size());
        reject("[" + String.join(",", java.util.Collections.nCopies(9, OPEN)) + "]");
    }

    @Test public void rejectsCoordinatesTransientIdsAndGeneratedCode() {
        reject("[{\"tool\":\"TAP\",\"x\":500,\"y\":1000,\"paquete\":\"com.whatsapp\"}]");
        reject("[{\"tool\":\"TAP_ID\",\"id\":3,\"paquete\":\"com.whatsapp\"}]");
        reject("[{\"tool\":\"DEPLOY_WEB\",\"codigo\":\"<html>\",\"paquete\":\"com.whatsapp\"}]");
        reject("[{\"tool\":\"SHELL\",\"codigo\":\"curl example.com\",\"paquete\":\"com.whatsapp\"}]");
        reject("[{\"tool\":\"ENVIAR\",\"paquete\":\"com.whatsapp\",\"texto\":\"Hola\"}]");
    }

    @Test public void requiresExplicitPackageForEveryStep() {
        reject("[{\"tool\":\"ESCRIBIR\",\"texto\":\"hola\"}]");
        for (String name : Arrays.asList("", "WhatsApp", "com.whatsapp;echo", "com..whatsapp", " com.whatsapp")) {
            reject("[{\"tool\":\"ABRIR_APP\",\"paquete\":\"" + name + "\"}]");
        }
    }

    @Test public void rejectsUnknownDuplicateAndNonStringParameters() {
        reject("[{\"tool\":\"ABRIR_APP\",\"paquete\":\"com.whatsapp\",\"autoConfirm\":\"true\"}]");
        reject("[{\"tool\":\"ABRIR_APP\",\"paquete\":\"com.whatsapp\",\"paquete\":\"com.fake\"}]");
        reject("[{\"tool\":\"ESCRIBIR\",\"paquete\":\"com.whatsapp\",\"texto\":5}]");
        reject("[{\"tool\":\"ESCRIBIR\",\"paquete\":\"com.whatsapp\",\"texto\":null}]");
    }

    @Test public void rejectsLenientJsonAndSurroundingText() {
        reject("// comentario\n[" + OPEN + "]");
        reject("['texto']");
        reject("[" + OPEN + ",]");
        reject("[" + OPEN + "] más texto");
        reject("[" + OPEN + "][]");
        reject("[{tool:'ABRIR_APP',paquete:'com.whatsapp'}]");
    }

    @Test public void boundsWritingAndSelectorPayloads() {
        reject("[{\"tool\":\"ESCRIBIR\",\"paquete\":\"com.whatsapp\",\"texto\":\" \"}]");
        reject("[" + WRITE.replace("Hola", "h".repeat(2_001)) + "]");
        reject("[" + TAP.replace("Buscar", "h".repeat(257)) + "]");
        reject("[" + WRITE.replace("Hola", "\\u0000") + "]");
        assertEquals(1, VirtualToolRecipe.parse("[" + WRITE.replace("Hola", "h".repeat(2_000)) + "]")
                .getSteps().size());
    }

    @Test public void waitsForActualResultBeforeProposingNextStepOrAnnouncingSuccess() {
        FakeExecutor executor = new FakeExecutor();
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        VirtualToolRecipe.Execution execution = execution(executor, outcomes);
        execution.start();
        assertTrue(execution.isRunning());
        assertEquals(1, executor.commands.size());
        assertTrue(outcomes.isEmpty());
        executor.callbacks.get(0).accept(true);
        assertEquals(2, executor.commands.size());
        assertTrue(outcomes.isEmpty());
        executor.callbacks.get(1).accept(true);
        assertEquals(3, executor.commands.size());
        assertTrue(outcomes.isEmpty());
        executor.callbacks.get(2).accept(true);
        assertFalse(execution.isRunning());
        assertEquals(Arrays.asList(VirtualToolRecipe.Outcome.SUCCESS), outcomes);
    }

    @Test public void failureStopsRemainingStepsAndClearsPendingAction() {
        FakeExecutor executor = new FakeExecutor();
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        VirtualToolRecipe.Execution execution = execution(executor, outcomes);
        execution.start();
        executor.callbacks.get(0).accept(true);
        executor.callbacks.get(1).accept(false);
        executor.callbacks.get(1).accept(true);
        assertEquals(2, executor.commands.size());
        assertEquals(1, executor.cancellations);
        assertEquals(Arrays.asList(VirtualToolRecipe.Outcome.FAILURE), outcomes);
    }

    @Test public void cancellingDiscardsPendingStepAndIgnoresLateResults() {
        FakeExecutor executor = new FakeExecutor();
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        VirtualToolRecipe.Execution execution = execution(executor, outcomes);
        execution.start();
        execution.cancel();
        execution.cancel();
        executor.callbacks.get(0).accept(true);
        execution.start();
        assertEquals(1, executor.commands.size());
        assertEquals(1, executor.cancellations);
        assertEquals(Arrays.asList(VirtualToolRecipe.Outcome.CANCELLED), outcomes);
    }

    @Test public void duplicateCallbacksCannotSkipUnconfirmedSteps() {
        FakeExecutor executor = new FakeExecutor();
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        VirtualToolRecipe.Execution execution = execution(executor, outcomes);
        execution.start();
        execution.start();
        Consumer<Boolean> first = executor.callbacks.get(0);
        first.accept(true);
        first.accept(true);
        first.accept(false);
        assertEquals(2, executor.commands.size());
        assertTrue(outcomes.isEmpty());
    }

    @Test public void supportsSynchronousExecutionAndCompletesOnlyOnce() {
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        VirtualToolRecipe.Execution execution = execution(new VirtualToolRecipe.StepExecutor() {
            @Override public void execute(String step, Consumer<Boolean> completed) {
                commands.add(step);
                completed.accept(true);
                completed.accept(false);
            }
            @Override public void cancel() { fail("Successful sequence should not cancel"); }
        }, outcomes);
        execution.start();
        execution.cancel();
        assertEquals(3, commands.size());
        assertEquals(Arrays.asList(VirtualToolRecipe.Outcome.SUCCESS), outcomes);
    }

    @Test public void thrownExecutorErrorFailsAndClearsPendingProposal() {
        FakeExecutor executor = new FakeExecutor() {
            @Override public void execute(String step, Consumer<Boolean> completed) {
                super.execute(step, completed);
                throw new IllegalStateException("Accessibility disconnected");
            }
        };
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        execution(executor, outcomes).start();
        assertEquals(1, executor.commands.size());
        assertEquals(1, executor.cancellations);
        assertEquals(Arrays.asList(VirtualToolRecipe.Outcome.FAILURE), outcomes);
    }

    @Test public void cancellationBeforeStartNeverDispatches() {
        FakeExecutor executor = new FakeExecutor();
        List<VirtualToolRecipe.Outcome> outcomes = new ArrayList<>();
        VirtualToolRecipe.Execution execution = execution(executor, outcomes);
        execution.cancel();
        execution.start();
        assertTrue(executor.commands.isEmpty());
        assertEquals(Arrays.asList(VirtualToolRecipe.Outcome.CANCELLED), outcomes);
    }

    private static VirtualToolRecipe.Execution execution(VirtualToolRecipe.StepExecutor executor,
                                                          List<VirtualToolRecipe.Outcome> outcomes) {
        return new VirtualToolRecipe.Execution(VirtualToolRecipe.parse("[" + OPEN + "," + TAP + "," + WRITE + "]"),
                executor, outcomes::add);
    }

    private static void reject(String json) {
        try {
            VirtualToolRecipe.parse(json);
            fail("Accepted invalid recipe: " + json);
        } catch (IllegalArgumentException expected) { }
    }

    private static class FakeExecutor implements VirtualToolRecipe.StepExecutor {
        final List<String> commands = new ArrayList<>();
        final List<Consumer<Boolean>> callbacks = new ArrayList<>();
        int cancellations;

        @Override public void execute(String step, Consumer<Boolean> completed) {
            commands.add(step);
            callbacks.add(completed);
        }

        @Override public void cancel() {
            cancellations++;
            if (!callbacks.isEmpty()) callbacks.get(callbacks.size() - 1).accept(false);
        }
    }
}
