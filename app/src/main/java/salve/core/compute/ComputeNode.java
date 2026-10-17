package salve.core.compute;

import java.net.URI;

/** Only an owner-entered endpoint may become a node. No discovery from model output. */
public final class ComputeNode {
    public final String id, name, endpoint, token;
    public ComputeNode(String id, String name, String endpoint, String token) {
        if (id == null || !id.matches("[a-zA-Z0-9-]{1,64}")) throw new IllegalArgumentException("Identificador inválido");
        if (name == null || !name.matches("[^\\p{Cntrl}]{1,60}")) throw new IllegalArgumentException("Nombre de 1 a 60 caracteres");
        if (token == null || !token.matches("[A-Za-z0-9_-]{32,256}")) throw new IllegalArgumentException("Token de 32 a 256 caracteres seguros");
        this.id = id; this.name = name; this.endpoint = endpoint(endpoint); this.token = token;
    }
    public static String endpoint(String raw) {
        try {
            String value = raw == null ? "" : raw.trim().replaceAll("/+$", "");
            URI uri = new URI(value);
            if (value.length() > 500 || !"https".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getFragment() != null
                    || (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535))
                    || !(uri.getRawPath().isEmpty()) || value.contains("\\")) throw new IllegalArgumentException();
            // LAN/VPN nodes are intentional here; ordinary web browsing keeps its own public-IP policy.
            return value;
        } catch (Exception invalid) { throw new IllegalArgumentException("Usa https://servidor:puerto, sin ruta, credenciales ni parámetros"); }
    }
}
