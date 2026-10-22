package salve.core.cognition;

import com.google.gson.JsonArray;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.ModelResult;

public class CognitiveSearchFeedbackTest {
    private static ModelResult text(String json) { return ModelResult.success(json, 0); }
    @Test public void revisedWebQueryAfterReceiptCanExecuteWithinExistingTwoActionLimit() {
        AtomicInteger searches = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("web.search", new CognitiveTurn.Tool("read-only", args -> {
            searches.incrementAndGet();
            return new CognitiveTurn.Receipt("No encuentro una fuente que responda a esa consulta");
        }));
        CognitiveTurn.Outcome outcome = CognitiveTurn.run((receipts, repair, remaining) -> {
            if (receipts.size() == 0) return text("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"web.search\",\"arguments\":{\"query\":\"geometria edificios\"}}}");
            if (receipts.size() == 1) return text("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"web.search\",\"arguments\":{\"query\":\"geometria funcionalidad arquitectura\"}}}");
            return text("{\"mode\":\"respond\",\"message\":\"No encontré fuentes concluyentes; estas son las consultas verificadas.\",\"action\":null}");
        }, tools, () -> false);
        assertFalse(outcome.handedOff);
        assertTrue(outcome.result.isSuccess());
        assertEquals(2, searches.get());
        assertEquals(2, outcome.receipts().size());
    }

    @Test public void unchangedSearchCannotRepeatAndDeviceActionNeverRunsTwice() {
        AtomicInteger searches = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("web.search", new CognitiveTurn.Tool("read", args -> {
            searches.incrementAndGet(); return new CognitiveTurn.Receipt("sin resultados");
        }));
        CognitiveTurn.Outcome loop = CognitiveTurn.run((receipts, repair, remaining) -> {
            if (receipts.size() == 0 || remaining > 0)
                return text("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"web.search\",\"arguments\":{\"query\":\"la misma consulta\"}}}");
            return text("{\"mode\":\"respond\",\"message\":\"No se pudo mejorar esta búsqueda.\",\"action\":null}");
        }, tools, () -> false);
        assertEquals(1, searches.get());
        assertTrue(loop.result.isSuccess());
        assertEquals(1, loop.receipts().size());

        AtomicInteger writes = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> effects = new LinkedHashMap<>();
        effects.put("ESCRIBIR", new CognitiveTurn.Tool("requires consent", args -> {
            writes.incrementAndGet(); return new CognitiveTurn.Receipt("simulada");
        }));
        CognitiveTurn.Outcome attempt = CognitiveTurn.run((receipts, repair, remaining) ->
                receipts.size() == 0 || remaining > 0
                        ? text("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"ESCRIBIR\",\"arguments\":{}}}")
                        : text("{\"mode\":\"respond\",\"message\":\"Fin.\",\"action\":null}"),
                effects, () -> false);
        assertEquals(1, writes.get());
        assertEquals(1, attempt.receipts().size());
    }
}
