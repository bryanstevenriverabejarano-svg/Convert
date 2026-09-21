package salve.devices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.net.InetAddress;
import java.net.URI;

public class LocalServiceEndpointTest {
    @Test public void buildsSelectedPrivateHttpEndpoint() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[]{(byte)192, (byte)168, 1, 40});
        assertEquals("http://192.168.1.40:8080/", LocalServiceEndpoint.url("_http._tcp.", address, 8080));
    }

    @Test public void preservesHttpsAndSelectedPort() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[]{10, 1, 2, 3});
        assertEquals("https://10.1.2.3:8443/", LocalServiceEndpoint.url("_https._tcp", address, 8443));
    }

    @Test public void supportsIpv6UniqueLocalAddresses() throws Exception {
        byte[] bytes = new byte[16]; bytes[0] = (byte)0xfd; bytes[15] = 1;
        String url = LocalServiceEndpoint.url("_https._tcp.", InetAddress.getByAddress(bytes), 443);
        assertTrue(url.startsWith("https://[fd00:"));
        assertEquals(443, URI.create(url).getPort());
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsInternetDestinationAdvertisedAsLocal() throws Exception {
        LocalServiceEndpoint.url("_http._tcp.", InetAddress.getByAddress(new byte[]{8,8,8,8}), 80);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsLoopbackDestination() throws Exception {
        LocalServiceEndpoint.url("_http._tcp.", InetAddress.getByAddress(new byte[]{127,0,0,1}), 80);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsMulticastDestination() throws Exception {
        LocalServiceEndpoint.url("_http._tcp.", InetAddress.getByAddress(new byte[]{(byte)224,0,0,1}), 80);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsUnknownProtocol() throws Exception {
        LocalServiceEndpoint.url("_ssh._tcp.", InetAddress.getByAddress(new byte[]{10,0,0,1}), 22);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidPort() throws Exception {
        LocalServiceEndpoint.url("_http._tcp.", InetAddress.getByAddress(new byte[]{10,0,0,1}), 65536);
    }
}
