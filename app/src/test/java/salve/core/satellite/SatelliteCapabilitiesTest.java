package salve.core.satellite;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import org.junit.Test;

public class SatelliteCapabilitiesTest {
    @Test public void defaultOffersAllThreeUses() {
        SatelliteCapabilities c = SatelliteCapabilities.all();
        assertTrue(c.compute && c.screen && c.sensors);
        assertFalse(c.none());
        assertEquals(3, c.toJson().size());
    }

    @Test public void jsonRoundTripKeepsTheSelectedUses() {
        SatelliteCapabilities c = new SatelliteCapabilities(true, false, true);
        SatelliteCapabilities back = SatelliteCapabilities.fromJson(c.toJson());
        assertTrue(back.compute); assertFalse(back.screen); assertTrue(back.sensors);
    }

    @Test public void unknownCapabilityIsIgnored() {
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        a.add("compute"); a.add("teleport");
        SatelliteCapabilities c = SatelliteCapabilities.fromJson(a);
        assertTrue(c.compute); assertFalse(c.screen); assertFalse(c.sensors);
    }

    @Test public void registrationCarriesIdCapsAndRoleButNoPersonalData() {
        JsonObject r = SatelliteCapabilities.all().registration("sat-abc", "Móvil viejo");
        assertEquals("satellite", r.get("role").getAsString());
        assertEquals("sat-abc", r.get("id").getAsString());
        assertEquals("Móvil viejo", r.get("name").getAsString());
        assertEquals(3, r.getAsJsonArray("capabilities").size());
        assertFalse(r.has("memory"));
        assertFalse(r.has("recuerdos"));
    }
}
