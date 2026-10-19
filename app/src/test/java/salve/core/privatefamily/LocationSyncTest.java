package salve.core.privatefamily;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LocationSyncTest {
    private final Map<String, String> locations = new HashMap<>(), versions = new HashMap<>();
    private final Map<String, Long> received = new HashMap<>();
    private static final long NOW = 2_000_000_000L;

    private static JsonObject status(String version, String locationJson) {
        return JsonParser.parseString("{\"protocol\":1,\"available\":true,\"location_version\":\"" + version + "\",\"location\":"
                + locationJson + "}").getAsJsonObject();
    }
    private static String place(String city, String source, long at) {
        return "{\"latitude\":42.85,\"longitude\":-2.67,\"city\":\"" + city + "\",\"zone\":\"Casa\",\"source\":\"" + source
                + "\",\"captured_at\":" + at + ",\"accuracy_m\":8}";
    }

    @Test public void storesTheNodeLocationAndItsVersion() {
        assertTrue(LocationSync.apply(locations, versions, received, "pc", status("0123456789abcdef", place("Vitoria", "static", NOW)), NOW));
        assertEquals("0123456789abcdef", versions.get("pc"));
        PrivateLocation current = LocationSync.current(locations, "pc", NOW);
        assertEquals("Vitoria · Casa", LocationSync.place(current));
    }

    @Test public void unchangedAnswerCarriesNoCoordinatesAndChangesNothing() {
        LocationSync.apply(locations, versions, received, "pc", status("0123456789abcdef", place("Vitoria", "static", NOW)), NOW);
        String before = locations.get("pc");
        JsonObject same = JsonParser.parseString("{\"protocol\":1,\"available\":true,\"unchanged\":true,\"location_version\":\"0123456789abcdef\"}").getAsJsonObject();
        assertFalse(LocationSync.apply(locations, versions, received, "pc", same, NOW + 10));
        assertEquals(before, locations.get("pc"));
        JsonObject bogus = JsonParser.parseString("{\"protocol\":1,\"available\":true,\"unchanged\":true,\"location_version\":\"ffffffffffffffff\"}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> LocationSync.apply(locations, versions, received, "pc", bogus, NOW));
    }

    @Test public void withdrawalAndExpiredGpsRemoveTheLocalCopy() {
        LocationSync.apply(locations, versions, received, "pc", status("0123456789abcdef", place("Vitoria", "static", NOW)), NOW);
        assertTrue(LocationSync.apply(locations, versions, received, "pc", status("none", "null"), NOW));
        assertNull(LocationSync.current(locations, "pc", NOW));
        assertEquals("none", versions.get("pc"));
        LocationSync.apply(locations, versions, received, "pc", status("aaaaaaaaaaaaaaaa", place("Bilbao", "gps", NOW)), NOW);
        assertNotNull(LocationSync.current(locations, "pc", NOW + 60));
        assertNull("un GPS caduca a los 15 min", LocationSync.current(locations, "pc", NOW + 901));
    }

    @Test public void invalidAnswersNeverTouchTheCopy() {
        for (String bad : new String[]{"{\"protocol\":2,\"available\":true}", "{\"protocol\":1,\"available\":false}",
                "{\"protocol\":1,\"available\":true,\"location_version\":\"<script>\"}"})
            assertThrows(bad, IllegalArgumentException.class,
                    () -> LocationSync.apply(locations, versions, received, "pc", JsonParser.parseString(bad).getAsJsonObject(), NOW));
        assertTrue(locations.isEmpty());
    }
}
