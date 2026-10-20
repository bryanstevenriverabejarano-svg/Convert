package salve.core.compute;

import com.google.gson.JsonParser;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class NodeTelemetryTest {
    static ComputeRouter.Health health() { return new ComputeRouter.Health("m", 1, 12000, 1024); }

    @Test public void readsGpuVramAndRoles() {
        ComputeRouter.Health h = health();
        ComputeHttpTransport.telemetry(h, JsonParser.parseString("{\"roles\":[\"inferencia\",\"guardian\"],"
                + "\"gpu\":{\"name\":\"RTX 4070\",\"vram_used_gb\":6.2,\"vram_total_gb\":12.0,\"load_pct\":35}}").getAsJsonObject());
        assertEquals(Arrays.asList("inferencia", "guardian"), h.roles);
        assertEquals("RTX 4070 · 6,2 / 12,0 GB de VRAM · 35 %", h.telemetry());
    }

    @Test public void malformedTelemetryNeverBreaksTheNode() {
        ComputeRouter.Health h = health();
        ComputeHttpTransport.telemetry(h, JsonParser.parseString("{\"gpu\":{\"name\":\"X\",\"vram_used_gb\":50,\"vram_total_gb\":8,\"load_pct\":900},"
                + "\"roles\":[\"<script>\",3]}").getAsJsonObject());
        assertEquals("X", h.telemetry());
        assertTrue(h.roles.isEmpty());
        ComputeRouter.Health none = health();
        ComputeHttpTransport.telemetry(none, JsonParser.parseString("{\"gpu\":null,\"roles\":\"x\"}").getAsJsonObject());
        assertEquals("", none.telemetry());
    }
}
