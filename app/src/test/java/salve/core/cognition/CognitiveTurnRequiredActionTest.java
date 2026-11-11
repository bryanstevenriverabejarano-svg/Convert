package salve.core.cognition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.ModelResult;
import static org.junit.Assert.*;

public class CognitiveTurnRequiredActionTest {
    private static ModelResult spoken(String text) {
        return ModelResult.success("{\"mode\":\"respond\",\"message\":\"" + text + "\",\"action\":null}", 0);
    }
    private static ModelResult call(String tool) {
        return ModelResult.success("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\""
                + tool + "\",\"arguments\":{}}}", 0);
    }

    @Test public void retriesProseOnceAndThenExecutesAuthorizedTool() {
        AtomicInteger executed = new AtomicInteger(), calls = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("device.control", new CognitiveTurn.Tool("{}", args -> {
            executed.incrementAndGet();
            return CognitiveTurn.Receipt.handoff();
        }));
        CognitiveTurn.Outcome result = CognitiveTurn.run((receipts, repair, remaining) -> {
            int call = calls.incrementAndGet();
            if (call == 1) { assertFalse(repair); return spoken("Claro ya está"); }
            assertTrue(repair);
            return call("device.control");
        }, tools, () -> false, "device.control");
        assertEquals(2, calls.get());
        assertEquals(1, executed.get());
        assertTrue(result.handedOff);
        assertEquals(1, result.receipts().size());
    }

    @Test public void cannotPretendToolRanWhenModelOnlyRepliesInProse() {
        AtomicInteger calls = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("device.open_app", new CognitiveTurn.Tool("{}", args -> {
            fail("No tool was selected");
            return CognitiveTurn.Receipt.handoff();
        }));
        CognitiveTurn.Outcome result = CognitiveTurn.run((receipts, repair, remaining) -> {
            calls.incrementAndGet();
            return spoken("He abierto Spotify");
        }, tools, () -> false, "device.open_app");
        assertEquals(2, calls.get());
        assertFalse(result.handedOff);
        assertTrue(result.receipts().isEmpty());
        assertFalse(result.result.isSuccess());
        assertTrue(result.result.getError().contains("no he ejecutado"));
    }

    @Test public void explicitRejectionDoesNotTurnIntoFalseSuccess() {
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("device.setting", new CognitiveTurn.Tool("{}", args -> {
            throw new IllegalArgumentException("Permiso no concedido");
        }));
        CognitiveTurn.Outcome result = CognitiveTurn.run((receipts, repair, remaining) ->
                call("device.setting"), tools, () -> false, "device.setting");
        assertFalse(result.result.isSuccess());
        assertEquals("failed", result.receipts().get(0).getAsJsonObject().get("status").getAsString());
    }

    @Test public void noToolObligationLeavesOrdinaryConversationUntouched() {
        AtomicInteger calls = new AtomicInteger();
        CognitiveTurn.Outcome result = CognitiveTurn.run((receipts, repair, remaining) -> {
            calls.incrementAndGet();
            return spoken("Hola");
        }, new LinkedHashMap<>(), () -> false, null);
        assertTrue(result.result.isSuccess());
        assertEquals(1, calls.get());
    }
}
