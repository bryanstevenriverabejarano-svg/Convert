package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CognitiveCycleFrameTest {
    private static JsonArray receipts(String... statuses) {
        JsonArray list = new JsonArray();
        for (String status : statuses) {
            JsonObject obj = new JsonObject();
            obj.addProperty("status", status);
            obj.addProperty("data", "NO DEBE MOSTRARSE; secreto de una página externa");
            list.add(obj);
        }
        return list;
    }
    @Test public void groundsInMemoryBeforeConsideringTools() {
        CognitiveCycleFrame frame = CognitiveCycleFrame.observe(
                "¿Por qué eres única?", "FOUND\nMemoria recuperada", receipts(), 2);
        assertEquals(CognitiveCycleFrame.Demand.CAUSAL_EXPLANATION, frame.demand);
        assertEquals(CognitiveCycleFrame.Phase.GROUND, frame.phase);
        assertTrue(frame.memoryContextPresent);
        assertTrue(frame.toolBudgetLeft);
        assertTrue(frame.instruction().contains("Primero interpreta"));
        assertFalse(frame.toJson().toString().contains("Memoria recuperada"));
    }
    @Test public void repliesToReceiptsWithNewPhaseNotBlindRepetition() {
        CognitiveCycleFrame success = CognitiveCycleFrame.observe(
                "Compara dos opciones", "No consultada aún", receipts("completed"), 1);
        assertEquals(CognitiveCycleFrame.Demand.COMPARISON, success.demand);
        assertEquals(CognitiveCycleFrame.Phase.EVALUATE_EVIDENCE, success.phase);
        assertEquals(1, success.completed);
        assertFalse(success.memoryContextPresent);
        CognitiveCycleFrame retry = CognitiveCycleFrame.observe(
                "¿Cómo se hace?", "Cerrada: privado", receipts("failed"), 1);
        assertEquals(CognitiveCycleFrame.Phase.RECOVER, retry.phase);
        CognitiveCycleFrame stop = CognitiveCycleFrame.observe(
                "¿Cómo se hace?", "", receipts("failed"), 0);
        assertEquals(CognitiveCycleFrame.Phase.FINISH_WITH_LIMITS, stop.phase);
        assertFalse(stop.toJson().toString().contains("secreto"));
        assertFalse(stop.toJson().toString().contains("¿Cómo se hace?"));
    }
    @Test public void unknownReceiptIsNotMadeIntoEvidence() {
        CognitiveCycleFrame blank = CognitiveCycleFrame.observe(
                "Hola", "", receipts("model_claimed_pass", ""), 2);
        assertEquals(CognitiveCycleFrame.Phase.GROUND, blank.phase);
        assertEquals(0, blank.observed);
        assertEquals(CognitiveCycleFrame.Demand.OPEN_RESPONSE, blank.demand);
    }
}