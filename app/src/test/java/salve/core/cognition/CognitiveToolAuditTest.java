package salve.core.cognition;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.ModelResult;

/** Tests actual controller-created receipts rather than model-asserted success. */
public class CognitiveToolAuditTest {
    private static final String LOOKUP =
            "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"memory.lookup\",\"arguments\":{}}}";
    private static final String REPLY =
            "{\"mode\":\"respond\",\"message\":\"Respuesta\",\"action\":null}";

    @Test public void completedReceiptIsVisibleButDoesNotProveGoal() {
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("memory.lookup", new CognitiveTurn.Tool("{}", args ->
                new CognitiveTurn.Receipt("Secreto personal que nunca debe aparecer en el log")));
        AtomicInteger calls = new AtomicInteger();
        CognitiveTurn.Outcome outcome = CognitiveTurn.run(
                (results, fix, remaining) -> ModelResult.success(
                        calls.getAndIncrement() == 0 ? LOOKUP : REPLY, 0),
                tools, () -> false);
        assertTrue(outcome.result.isSuccess());
        assertEquals(2, calls.get());
        assertEquals(1, outcome.receipts().size());
        JsonObject audit = CognitiveToolAudit.from(outcome).toJson();
        assertEquals(1, audit.get("tool_receipts_completed_not_goal_proof").getAsInt());
        assertFalse(audit.get("goal_verified").getAsBoolean());
        assertFalse(audit.toString().contains("Secreto"));
        assertFalse(audit.toString().contains("personal"));
    }

    @Test public void failedAttemptIsNotRecordedAsSuccess() {
        Map<String, CognitiveTurn.Tool> tools = Collections.singletonMap(
                "memory.lookup", new CognitiveTurn.Tool("{}", args -> {
                    throw new IllegalArgumentException("Credencial privada: no registrar");
                }));
        AtomicInteger calls = new AtomicInteger();
        CognitiveTurn.Outcome outcome = CognitiveTurn.run((results, fix, remaining) ->
                ModelResult.success(calls.getAndIncrement() == 0 ? LOOKUP : REPLY, 0),
                tools, () -> false);
        JsonObject report = CognitiveToolAudit.from(outcome).toJson();
        assertEquals(0, report.get("tool_receipts_completed_not_goal_proof").getAsInt());
        assertEquals(1, report.get("tool_receipts_failed").getAsInt());
        assertFalse(report.toString().contains("Credencial privada"));
        assertFalse(report.get("goal_verified").getAsBoolean());
    }

    @Test public void asynchronousApprovalIsNeverCalledCompletion() {
        Map<String, CognitiveTurn.Tool> tools = Collections.singletonMap("memory.lookup",
                new CognitiveTurn.Tool("{}", args -> CognitiveTurn.Receipt.handoff()));
        CognitiveTurn.Outcome outcome = CognitiveTurn.run(
                (results, fix, remaining) -> ModelResult.success(LOOKUP, 0), tools, () -> false);
        assertTrue(outcome.handedOff);
        JsonObject audit = CognitiveToolAudit.from(outcome).toJson();
        assertEquals(0, audit.get("tool_receipts_completed_not_goal_proof").getAsInt());
        assertEquals(1, audit.get("pending_external_approval_or_result").getAsInt());
        assertFalse(audit.get("goal_verified").getAsBoolean());
    }

    @Test public void callerCannotRewriteRecordedReceipts() {
        Map<String, CognitiveTurn.Tool> tools = Collections.singletonMap("memory.lookup",
                new CognitiveTurn.Tool("{}", args -> new CognitiveTurn.Receipt("observado")));
        AtomicInteger calls = new AtomicInteger();
        CognitiveTurn.Outcome outcome = CognitiveTurn.run((results, fix, remaining) ->
                ModelResult.success(calls.getAndIncrement() == 0 ? LOOKUP : REPLY, 0),
                tools, () -> false);
        outcome.receipts().remove(0);
        assertEquals(1, outcome.receipts().size());
        assertEquals(1, CognitiveToolAudit.from(outcome).toJson()
                .get("tool_receipts_completed_not_goal_proof").getAsInt());
    }

    @Test public void noToolsMeansNoInventedOutcome() {
        CognitiveTurn.Outcome outcome = CognitiveTurn.run(
                (results, fix, remaining) -> ModelResult.success(REPLY, 0),
                Collections.emptyMap(), () -> false);
        CognitiveToolAudit audit = CognitiveToolAudit.from(outcome);
        assertFalse(audit.hasActions());
        assertFalse(audit.toJson().get("goal_verified").getAsBoolean());
    }
}
