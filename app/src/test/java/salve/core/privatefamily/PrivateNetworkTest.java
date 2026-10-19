package salve.core.privatefamily;

import org.junit.Test;
import static org.junit.Assert.*;

public class PrivateNetworkTest {
    @Test public void gpsExpiresAndCoordinatesAreValidated() {
        PrivateLocation gps = new PrivateLocation(4.6, -74.0, "Ciudad", "Casa", "gps", 1000, 12, 1000);
        assertTrue(gps.fresh(1899)); assertFalse(gps.fresh(1901));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(Double.NaN, 0, "", "", "gps", 1000, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(91, 0, "", "", "static", 1000, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(0, 0, "", "", "ip", 1000, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(0, 0, "", "", "gps", 1061, 0, 1000));
    }
}
