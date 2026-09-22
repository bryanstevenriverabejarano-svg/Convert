package salve.data.sync;

import java.net.URI;

/** Reject absent/example configuration before touching the network or consuming queue retries.
 * This precheck does not establish server authentication or durable remote storage.
 */
public final class CloudUploadPolicy {
    private CloudUploadPolicy() {}

    public static boolean isConfigured(String endpoint, String token) {
        if (token == null || token.trim().length() < 32
                || !token.equals(token.trim()) || token.contains("pon_aqui")
                || token.contains("\r") || token.contains("\n") || endpoint == null) return false;
        try {
            URI uri = new URI(endpoint);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getFragment() == null;
        } catch (Exception invalid) {
            return false;
        }
    }
}
