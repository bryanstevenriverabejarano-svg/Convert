package salve.devices;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

/** Validates a resolved, explicitly selected local HTTP service without DNS or network I/O. */
public final class LocalServiceEndpoint {
    private LocalServiceEndpoint() { }

    public static String url(String serviceType, InetAddress address, int port) {
        String type = serviceType == null ? "" : serviceType;
        if (type.endsWith(".")) type = type.substring(0, type.length() - 1);
        final String scheme;
        if ("_http._tcp".equals(type)) scheme = "http";
        else if ("_https._tcp".equals(type)) scheme = "https";
        else throw new IllegalArgumentException("El servicio no es HTTP ni HTTPS.");
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isMulticastAddress() || !isLocal(address)) {
            throw new IllegalArgumentException("El servicio no resolvió una dirección de la red local.");
        }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Puerto del servicio inválido.");
        try {
            return new URI(scheme, null, address.getHostAddress(), port, "/", null, null).toASCIIString();
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Dirección local inválida.", error);
        }
    }

    private static boolean isLocal(InetAddress address) {
        if (address.isSiteLocalAddress() || address.isLinkLocalAddress()) return true;
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc; // IPv6 unique local addresses.
    }
}
