package salve.core.satellite;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class SatelliteProtocolTest {
    static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    @Test public void heartbeatCarriesIdAvailabilityAndCaps() {
        JsonObject h = SatelliteProtocol.heartbeat("sat-1", true, SatelliteCapabilities.all());
        assertEquals("sat-1", h.get("id").getAsString());
        assertTrue(h.get("available").getAsBoolean());
        assertEquals(3, h.getAsJsonArray("capabilities").size());
    }

    @Test public void noTaskMeansNothingToDo() {
        assertNull(SatelliteProtocol.parseAssignment(json("{}")));
        assertNull(SatelliteProtocol.parseAssignment(json("{\"task\":{}}")));
        assertNull(SatelliteProtocol.parseAssignment(json("{\"task\":{\"id\":\"t1\",\"kind\":\"teleport\"}}")));
    }

    @Test public void aValidComputeTaskIsParsed() {
        SatelliteProtocol.Assignment a = SatelliteProtocol.parseAssignment(
                json("{\"task\":{\"id\":\"t7\",\"kind\":\"compute\",\"params\":{\"prompt\":\"hola\"}}}"));
        assertNotNull(a);
        assertEquals("t7", a.taskId);
        assertEquals("compute", a.kind);
        assertEquals("hola", a.params.get("prompt").getAsString());
    }

    @Test public void aTaskIsRefusedIfThatCapabilityIsOff() {
        SatelliteProtocol.Assignment sensors = SatelliteProtocol.parseAssignment(
                json("{\"task\":{\"id\":\"t8\",\"kind\":\"sensors\"}}"));
        assertTrue(SatelliteProtocol.allowed(sensors, SatelliteCapabilities.all()));
        assertFalse(SatelliteProtocol.allowed(sensors, new SatelliteCapabilities(true, true, false)));
    }

    @Test public void resultCarriesTaskAndOutput() {
        JsonObject out = new JsonObject(); out.addProperty("text", "listo");
        JsonObject r = SatelliteProtocol.result("sat-1", "t7", out);
        assertEquals("t7", r.get("task").getAsString());
        assertEquals("listo", r.getAsJsonObject("output").get("text").getAsString());
    }
}
