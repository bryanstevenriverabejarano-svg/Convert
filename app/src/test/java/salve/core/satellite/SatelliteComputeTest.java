package salve.core.satellite;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

/** El modelado puro del trabajo de cómputo (la ejecución en el modelo vive en SatelliteCompute). */
public class SatelliteComputeTest {
    static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    @Test public void readsThePromptFromTheTask() {
        assertEquals("hola", SatelliteProtocol.computePrompt(json("{\"prompt\":\" hola \"}")));
        assertEquals("", SatelliteProtocol.computePrompt(json("{}")));
        assertEquals("", SatelliteProtocol.computePrompt(null));
    }

    @Test public void longPromptsAreTrimmed() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 9000; i++) big.append('a');
        JsonObject p = new JsonObject(); p.addProperty("prompt", big.toString());
        assertEquals(8192, SatelliteProtocol.computePrompt(p).length());
    }

    @Test public void outputCarriesStatusTextAndLatency() {
        JsonObject out = SatelliteProtocol.computeOutput("ok", "respuesta", 1234L);
        assertEquals("ok", out.get("status").getAsString());
        assertEquals("respuesta", out.get("text").getAsString());
        assertEquals(1234L, out.get("latency_ms").getAsLong());
    }
}
