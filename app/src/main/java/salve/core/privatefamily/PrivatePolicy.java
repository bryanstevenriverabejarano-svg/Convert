package salve.core.privatefamily;

import java.net.URI;

/** Literal RFC1918 destinations only: no DNS, IP geolocation or public node endpoints. */
public final class PrivatePolicy {
    private PrivatePolicy() { }
    public static boolean ipv4(String host) {
        if (host == null) return false;
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) return false;
        }
        return true;
    }
    public static boolean privateIp(String host) {
        if (!ipv4(host)) return false;
        String[] parts = host.split("\\."); int a = Integer.parseInt(parts[0]), b = Integer.parseInt(parts[1]);
        return a == 10 || a == 172 && b >= 16 && b <= 31 || a == 192 && b == 168;
    }
    public static String endpointHost(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            if (!"https".equals(uri.getScheme()) || !privateIp(uri.getHost()) || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))
                    || uri.getPort() == 0 || uri.getPort() > 65535) throw new IllegalArgumentException();
            return uri.getHost();
        } catch (Exception invalid) { throw new IllegalArgumentException("Usa HTTPS y la IPv4 privada del túnel, por ejemplo https://10.77.0.2:8766"); }
    }
}
