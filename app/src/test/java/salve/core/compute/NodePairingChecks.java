package salve.core.compute;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Comprobaciones sin Android: java NodePairingChecks (ver scripts/test-compute-router.sh). */
public final class NodePairingChecks {
    private static final String TOKEN = "tbEKWczN_fKsN-6_2Rk3hnwsgcM-b67_SvJ6TzKgDnQ";

    public static void main(String[] args) {
        NodePairing ok = NodePairing.fromQr("salve://nodo?v=1&n=Mi%20PC%20%26%20co&u=https%3A%2F%2Fpc.tail1.ts.net&t=" + TOKEN);
        check("Mi PC & co".equals(ok.name), "nombre decodificado");
        check("https://pc.tail1.ts.net".equals(ok.endpoint), "url decodificada");
        check(TOKEN.equals(ok.token) && !ok.fromNetwork, "token del QR");

        rejects("https://pc.tail1.ts.net");
        rejects("salve://otro?v=1&n=PC&u=https%3A%2F%2Fpc&t=" + TOKEN);
        rejects("salve://nodo?v=2&n=PC&u=https%3A%2F%2Fpc&t=" + TOKEN);
        rejects("salve://nodo?v=1&n=PC&u=http%3A%2F%2Fpc&t=" + TOKEN);            // sin TLS
        rejects("salve://nodo?v=1&n=PC&u=https%3A%2F%2Fpc%2Fv1&t=" + TOKEN);       // con ruta
        rejects("salve://nodo?v=1&n=PC&u=https%3A%2F%2Fpc&t=corto");
        rejects("salve://nodo?v=1&n=PC&u=https%3A%2F%2Fpc&t=" + TOKEN + "&t=" + TOKEN.replace('t', 'x'));
        rejects("salve://nodo?v=1&n=&u=https%3A%2F%2Fpc&t=" + TOKEN);
        rejects(null);

        Map<String, byte[]> txt = new HashMap<>();
        txt.put("v", b("1")); txt.put("u", b("https://pc.tail1.ts.net")); txt.put("n", b("Torre"));
        NodePairing advert = NodePairing.fromAdvert("servicio", txt);
        check("Torre".equals(advert.name) && advert.token.isEmpty() && advert.fromNetwork, "anuncio sin token");
        txt.remove("n");
        check("servicio".equals(NodePairing.fromAdvert("servicio", txt).name), "nombre del servicio si falta n");
        txt.put("u", b("http://192.168.1.20:8766"));
        try { NodePairing.fromAdvert("x", txt); throw new AssertionError("anuncio http aceptado"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("NodePairing: OK");
    }

    private static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    private static void rejects(String raw) {
        try { NodePairing.fromQr(raw); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Aceptado: " + raw);
    }

    private static void check(boolean condition, String what) { if (!condition) throw new AssertionError(what); }
}
