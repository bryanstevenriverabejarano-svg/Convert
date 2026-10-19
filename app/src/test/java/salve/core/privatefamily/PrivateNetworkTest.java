package salve.core.privatefamily;

import org.junit.Test;
import static org.junit.Assert.*;

public class PrivateNetworkTest {
    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private String profile(String routes, String endpoint) {
        return "[Interface]\nPrivateKey = " + KEY + "\nAddress = 10.77.0.1/32\n[Peer]\nPublicKey = " + KEY
                + "\nAllowedIPs = " + routes + "\nEndpoint = " + endpoint + "\n";
    }
    @Test public void literalPrivateEndpointsOnly() {
        assertEquals("10.77.0.2", PrivatePolicy.endpointHost("https://10.77.0.2:8766"));
        assertEquals("172.16.0.2", PrivatePolicy.endpointHost("https://172.16.0.2"));
        for (String value : new String[]{"http://10.77.0.2", "https://node.example", "https://127.0.0.1", "https://169.254.169.254", "https://8.8.8.8", "https://100.64.0.2", "https://192.168.01.2", "https://u:p@10.77.0.2", "https://10.77.0.2/x", "https://10.77.0.2?q=1", "https://10.77.0.2:0"}) {
            assertThrows(value, IllegalArgumentException.class, () -> PrivatePolicy.endpointHost(value));
        }
    }
    @Test public void profileLimitsRoutesAndForbidsDns() throws Exception {
        com.wireguard.config.Config safe = PrivateVpn.validate(profile("10.77.0.2/32", "198.51.100.2:51820"), "com.salve.app");
        assertEquals(java.util.Collections.singleton("com.salve.app"), safe.getInterface().getIncludedApplications());
        for (String routes : new String[]{"0.0.0.0/0", "10.77.0.0/24", "8.8.8.8/32", "10.77.0.1/32", "10.77.0.2/32, 10.77.0.3/32, 10.77.0.4/32, 10.77.0.5/32, 10.77.0.6/32"}) {
            assertThrows(Exception.class, () -> PrivateVpn.validate(profile(routes, "198.51.100.2:51820"), "com.salve.app"));
        }
        assertThrows(Exception.class, () -> PrivateVpn.validate(profile("10.77.0.2/32", "node.example:51820"), "com.salve.app"));
        assertThrows(Exception.class, () -> PrivateVpn.validate(profile("10.77.0.2/32", "198.51.100.2:51820").replace("Address =", "DNS = 8.8.8.8\nAddress ="), "com.salve.app"));
    }
    @Test public void gpsExpiresAndCoordinatesAreValidated() {
        PrivateLocation gps = new PrivateLocation(4.6, -74.0, "Ciudad", "Casa", "gps", 1000, 12, 1000);
        assertTrue(gps.fresh(1899)); assertFalse(gps.fresh(1901));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(Double.NaN, 0, "", "", "gps", 1000, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(91, 0, "", "", "static", 1000, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(0, 0, "", "", "ip", 1000, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new PrivateLocation(0, 0, "", "", "gps", 1061, 0, 1000));
    }
    @Test public void vpnLossAndRefreshSpamNeverCreateIncident() {
        FamilyState state = new FamilyState(); state.observe(true, true, 1000);
        for (int i = 0; i < 10; i++) assertFalse(state.observe(false, false, 2000 + i));
        assertEquals(0, state.failures);
        assertFalse(state.observe(false, true, 2200));
        for (int i = 1; i < 100; i++) assertFalse(state.observe(false, true, 2200 + i));
        assertEquals(1, state.failures);
        assertFalse(state.observe(false, true, 2500)); assertTrue(state.observe(false, true, 2800));
        assertFalse(state.observe(false, true, 3100)); assertEquals("Inaccesible", state.status);
        assertFalse(state.observe(true, true, 3200)); assertFalse(state.alerted); assertEquals(0, state.failures);
    }
    @Test public void oldSuccessIsNotShownAsLiveAvailability() {
        FamilyState state = new FamilyState(); state.observe(true, true, 1000);
        assertEquals("Disponible", state.displayStatus(true, 1100));
        assertEquals("Sin comprobación reciente", state.displayStatus(true, 2801));
        assertEquals("Sin VPN disponible; estado desconocido", state.displayStatus(false, 1100));
    }
    @Test public void neverSeenNodesDoNotRaiseAnIncident() {
        FamilyState state = new FamilyState();
        for (int i = 0; i < 5; i++) assertFalse(state.observe(false, true, 1000 + i * 900));
    }
}
