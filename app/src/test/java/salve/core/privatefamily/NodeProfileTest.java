package salve.core.privatefamily;

import java.util.Base64;
import org.junit.Test;
import static org.junit.Assert.*;

public class NodeProfileTest {
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyz012345_-AB";

    private static String der() {
        byte[] bytes = new byte[40]; bytes[0] = 0x30;
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String profile(String extra) {
        return "{\"salve\":\"nodo\",\"v\":2,\"n\":\"Mi PC\",\"u\":\"https://192.168.1.20:8443\",\"t\":\"" + TOKEN + "\","
                + "\"ca\":\"" + der() + "\",\"c\":\"" + der() + "\",\"k\":\"" + der() + "\"" + extra + "}";
    }

    @Test public void parsesLocalAndRemoteAddresses() {
        NodeProfile p = NodeProfile.parse(profile(",\"r\":\"https://mipc.duckdns.org:8443\""));
        assertTrue(NodeProfile.looksLikeProfile(profile("")));
        assertEquals("Mi PC", p.name);
        assertEquals("https://192.168.1.20:8443", p.local);
        assertEquals("https://mipc.duckdns.org:8443", p.remote);
        FamilyVault.Credential c = p.credential();
        assertEquals(p.remote, c.remote);
        assertArrayEquals(p.key, Base64.getDecoder().decode(c.key));
    }

    @Test public void remoteIsOptionalAndNeverDuplicatesLocal() {
        assertEquals("", NodeProfile.parse(profile("")).remote);
        assertEquals("", NodeProfile.parse(profile(",\"r\":\"https://192.168.1.20:8443\"")).remote);
    }

    @Test public void rejectsInsecureOrMalformedProfiles() {
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse(profile("").replace("\"v\":2", "\"v\":1")));
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse(profile("").replace("https://192", "http://192")));
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse(profile(",\"r\":\"https://u:p@mipc.org\"")));
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse(profile("").replace(TOKEN, "corto")));
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse(profile("").replace("\"k\":\"" + der(), "\"k\":\"AAAA")));
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse("no es json"));
        assertThrows(IllegalArgumentException.class, () -> NodeProfile.parse(null));
    }
}
