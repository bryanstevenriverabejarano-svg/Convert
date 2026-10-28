package salve.core.satellite;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

/** El modelado puro de pantalla y sensores (la ejecución con permisos vive en las clases Android). */
public class SatelliteScreenSensorTest {
    static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    @Test public void screenReadsAndTrimsTheText() {
        assertEquals("hola", SatelliteProtocol.screenText(json("{\"text\":\" hola \"}")));
        assertEquals("", SatelliteProtocol.screenText(json("{}")));
    }

    @Test public void onlyKnownSensorsAreAccepted() {
        assertEquals("location", SatelliteProtocol.sensorKind(json("{\"sensor\":\"location\"}")));
        assertEquals("camera", SatelliteProtocol.sensorKind(json("{\"sensor\":\"camera\"}")));
        assertEquals("", SatelliteProtocol.sensorKind(json("{\"sensor\":\"gps_secreto\"}")));
        assertEquals("", SatelliteProtocol.sensorKind(json("{}")));
    }

    @Test public void simpleOutputCarriesStatusAndMessage() {
        JsonObject o = SatelliteProtocol.simpleOutput("permiso_requerido", "sin permiso");
        assertEquals("permiso_requerido", o.get("status").getAsString());
        assertEquals("sin permiso", o.get("message").getAsString());
    }

    @Test public void screenAndSensorsAreGatedByCapabilities() {
        SatelliteProtocol.Assignment sensors = SatelliteProtocol.parseAssignment(json("{\"task\":{\"id\":\"s1\",\"kind\":\"sensors\"}}"));
        SatelliteProtocol.Assignment screen = SatelliteProtocol.parseAssignment(json("{\"task\":{\"id\":\"p1\",\"kind\":\"screen\"}}"));
        SatelliteCapabilities onlyScreen = new SatelliteCapabilities(false, true, false);
        assertTrue(SatelliteProtocol.allowed(screen, onlyScreen));
        assertFalse(SatelliteProtocol.allowed(sensors, onlyScreen));
    }
}
