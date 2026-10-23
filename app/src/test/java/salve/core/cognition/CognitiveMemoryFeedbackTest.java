package salve.core.cognition;

import com.google.gson.JsonArray;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.ModelResult;

public class CognitiveMemoryFeedbackTest {
    private static ModelResult text(String json) { return ModelResult.success(json, 0); }
    @Test public void secondScopedMemoryReadAllowedWithinExistingBudget() {
        AtomicInteger reads = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("memory.lookup", new CognitiveTurn.Tool("read-only memory", args -> {
            reads.incrementAndGet();
            return new CognitiveTurn.Receipt("FOUND: coincidencia en recuerdos permitidos");
        }));
        CognitiveTurn.Outcome outcome = CognitiveTurn.run((receipts, repair, remaining) -> {
            if (receipts.size() == 0) return text("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"memory.lookup\",\"arguments\":{}}}");
            if (receipts.size() == 1) return text("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"memory.lookup\",\"arguments\":{\"query\":\"hermano reloj\"}}}");
            return text("{\"mode\":\"respond\",\"message\":\"Recuerdo dos datos y los distingo de las hipótesis.\",\"action\":null}");
        }, tools, () -> false);
        assertTrue(outcome.result.isSuccess());
        assertEquals(2, reads.get());
        assertEquals(2, outcome.receipts().size());
    }
    @Test public void twoIdenticalMemoryReadsAreNeverRepeated() {
        AtomicInteger reads = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("memory.lookup", new CognitiveTurn.Tool("read", args -> {
            reads.incrementAndGet(); return new CognitiveTurn.Receipt("EMPTY");
        }));
        CognitiveTurn.Outcome outcome = CognitiveTurn.run((receipts, repair, remaining) -> {
            if (receipts.size() == 0 || remaining > 0) return text(
                    "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"memory.lookup\",\"arguments\":{\"query\":\"mi hermano\"}}}");
            return text("{\"mode\":\"respond\",\"message\":\"No encontré otros recuerdos.\",\"action\":null}");
        }, tools, () -> false);
        assertEquals(1, reads.get());
        assertEquals(1, outcome.receipts().size());
    }
}