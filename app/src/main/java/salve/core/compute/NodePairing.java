package salve.core.compute;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Datos para rellenar el formulario de "Añadir nodo autorizado". Nunca guarda nada:
 * el dueño revisa los campos y pulsa Guardar. Mismas reglas que ComputeNode.
 *
 * QR:   salve://nodo?v=1&n=<nombre>&u=<https-url>&t=<token>   (generado por salve_node_setup.py)
 * mDNS: _salvenode._tcp con TXT v=1, n=<nombre>, u=<https-url>; sin token.
 */
public final class NodePairing {
    public static final String SERVICE_TYPE = "_salvenode._tcp";
    private static final int MAX_QR = 1024;

    public final String name, endpoint, token; // token es "" si viene de un anuncio de red
    public final boolean fromNetwork;

    private NodePairing(String name, String endpoint, String token, boolean fromNetwork) {
        this.name = name; this.endpoint = endpoint; this.token = token; this.fromNetwork = fromNetwork;
    }

    /** Lee un QR de emparejamiento. Lanza IllegalArgumentException con un mensaje para el usuario. */
    public static NodePairing fromQr(String raw) {
        if (raw == null || raw.length() > MAX_QR) throw new IllegalArgumentException("Este QR no es de un nodo de Salve");
        URI uri;
        try { uri = new URI(raw.trim()); } catch (Exception bad) { throw new IllegalArgumentException("Este QR no es de un nodo de Salve"); }
        if (!"salve".equals(uri.getScheme()) || !"nodo".equals(uri.getHost()) || uri.getRawQuery() == null)
            throw new IllegalArgumentException("Este QR no es de un nodo de Salve");
        Map<String, String> q = query(uri.getRawQuery());
        if (!"1".equals(q.get("v"))) throw new IllegalArgumentException("QR de una versión no compatible; actualiza el script o la app");
        String token = q.getOrDefault("t", "");
        if (!token.matches("[A-Za-z0-9_-]{32,256}")) throw new IllegalArgumentException("El token del QR no es válido");
        return new NodePairing(name(q.get("n")), ComputeNode.endpoint(q.get("u")), token, false);
    }

    /** Lee los TXT de un anuncio mDNS. El contenido viene de la red: se valida igual que lo escrito a mano. */
    public static NodePairing fromAdvert(String serviceName, Map<String, byte[]> txt) {
        if (txt == null || !"1".equals(text(txt.get("v")))) throw new IllegalArgumentException("Anuncio no compatible");
        String n = text(txt.get("n"));
        return new NodePairing(name(n == null || n.isEmpty() ? serviceName : n), ComputeNode.endpoint(text(txt.get("u"))), "", true);
    }

    private static String name(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.matches("[^\\p{Cntrl}]{1,60}")) throw new IllegalArgumentException("Nombre de nodo no válido");
        return value;
    }

    private static String text(byte[] bytes) { return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8); }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = decode(pair.substring(0, eq));
            if (out.containsKey(key)) throw new IllegalArgumentException("QR con campos repetidos");
            out.put(key, decode(pair.substring(eq + 1)));
        }
        return out;
    }

    private static String decode(String value) {
        try { return URLDecoder.decode(value, "UTF-8"); }
        catch (Exception bad) { throw new IllegalArgumentException("QR mal formado"); }
    }
}
