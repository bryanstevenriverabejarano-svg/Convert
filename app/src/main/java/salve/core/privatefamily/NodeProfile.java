package salve.core.privatefamily;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Base64;
import salve.core.compute.ComputeNode;

/**
 * Perfil de un nodo generado por salve_private_network.py (QR o salve-perfil.json): nombre,
 * dirección local, dirección remota opcional (IP pública o DDNS), token de consulta, la CA del
 * nodo y el certificado + clave de cliente del móvil para el TLS mutuo. Sin Android.
 */
public final class NodeProfile {
    private static final int MAX = 8192, MAX_DER = 4096;

    public final String name, local, remote, token;
    public final byte[] ca, cert, key;

    private NodeProfile(String name, String local, String remote, String token, byte[] ca, byte[] cert, byte[] key) {
        this.name = name; this.local = local; this.remote = remote; this.token = token;
        this.ca = ca; this.cert = cert; this.key = key;
    }

    public static boolean looksLikeProfile(String raw) {
        return raw != null && raw.trim().startsWith("{") && raw.contains("\"salve\"");
    }

    public static NodeProfile parse(String raw) {
        if (raw == null || raw.length() > MAX) throw new IllegalArgumentException("Perfil de nodo no válido");
        JsonObject json;
        try { json = JsonParser.parseString(raw.trim()).getAsJsonObject(); }
        catch (RuntimeException bad) { throw new IllegalArgumentException("Perfil de nodo no válido"); }
        if (!"nodo".equals(text(json, "salve")) || !json.has("v") || json.get("v").getAsInt() != 2)
            throw new IllegalArgumentException("Perfil de otra versión: vuelve a generarlo con salve_private_network.py");
        String name = text(json, "n").trim();
        if (!name.matches("[^\\p{Cntrl}]{1,60}")) throw new IllegalArgumentException("Nombre de nodo no válido");
        String token = text(json, "t");
        if (!token.matches("[A-Za-z0-9_-]{32,256}")) throw new IllegalArgumentException("Token del perfil no válido");
        String local = ComputeNode.endpoint(text(json, "u"));
        String remote = json.has("r") && !json.get("r").isJsonNull() ? ComputeNode.endpoint(text(json, "r")) : "";
        if (remote.equalsIgnoreCase(local)) remote = "";
        return new NodeProfile(name, local, remote, token, der(json, "ca"), der(json, "c"), der(json, "k"));
    }

    /** Lo que se guarda cifrado en el móvil para este nodo. */
    public FamilyVault.Credential credential() {
        FamilyVault.Credential c = new FamilyVault.Credential();
        Base64.Encoder b64 = Base64.getEncoder();
        c.remote = remote; c.ca = b64.encodeToString(ca); c.cert = b64.encodeToString(cert); c.key = b64.encodeToString(key);
        return c;
    }

    private static String text(JsonObject json, String field) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive()) throw new IllegalArgumentException("Falta «" + field + "» en el perfil");
        return value.getAsString();
    }

    private static byte[] der(JsonObject json, String field) {
        try {
            byte[] bytes = Base64.getDecoder().decode(text(json, field));
            if (bytes.length < 16 || bytes.length > MAX_DER || bytes[0] != 0x30) throw new IllegalArgumentException();
            return bytes;
        } catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Certificado del perfil no válido"); }
    }
}
