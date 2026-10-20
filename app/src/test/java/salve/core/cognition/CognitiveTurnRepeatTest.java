package salve.core.cognition;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import salve.core.ModelResult;

/** Pedir dos veces memory.lookup no debe acabar en «Pasos comprobados…» delante de Bryan. */
public class CognitiveTurnRepeatTest {
    private static final String LOOKUP = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"memory.lookup\",\"arguments\":{}}}";
    private static final String ANSWER = "{\"mode\":\"respond\",\"message\":\"Me interesan la luz y cómo aprendo contigo.\",\"action\":null}";

    @Test public void repeatedLookupClosesToolsAndLetsHerAnswer() {
        int[] calls = new int[1];
        int[] lastBudget = new int[1];
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        int[] executions = new int[1];
        tools.put("memory.lookup", new CognitiveTurn.Tool("{}", args -> { executions[0]++; return new CognitiveTurn.Receipt("OK"); }));
        CognitiveTurn.Outcome out = CognitiveTurn.run((results, repair, left) -> {
            calls[0]++;
            lastBudget[0] = left;
            return ModelResult.success(calls[0] <= 2 ? LOOKUP : ANSWER, 1);
        }, tools, () -> false);
        assertTrue(out.result.isSuccess());
        assertEquals("Me interesan la luz y cómo aprendo contigo.", out.result.getText());
        assertEquals(1, executions[0]);
        assertEquals(0, lastBudget[0]);
    }

    @Test public void askingForToolsAfterClosingStillStops() {
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("memory.lookup", new CognitiveTurn.Tool("{}", args -> new CognitiveTurn.Receipt("OK")));
        CognitiveTurn.Outcome out = CognitiveTurn.run((results, repair, left) -> ModelResult.success(LOOKUP, 1), tools, () -> false);
        assertFalse(out.result.isSuccess());
    }
}
