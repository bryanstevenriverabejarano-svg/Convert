package salve.core.privatefamily;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

/**
 * Aplica la respuesta de /v1/family/status a la copia local de ubicaciones. El móvil manda la
 * versión que ya tiene; si el nodo responde «sin cambios», no viajan coordenadas ni se escribe nada.
 * Sin Android, para poder probarlo.
 */
public final class LocationSync {
    private LocationSync() { }

    /** Versión que el móvil conoce de ese nodo (o null si nunca ha recibido nada). */
    public static String known(Map<String, String> versions, String nodeId) { return versions.get(nodeId); }

    /**
     * @return true si la copia local cambió (nueva ubicación o retirada).
     * @throws IllegalArgumentException si la respuesta no es válida; la copia local no se toca.
     */
    public static boolean apply(Map<String, String> locations, Map<String, String> versions, Map<String, Long> receivedAt,
                                String nodeId, JsonObject response, long nowSeconds) {
        if (response == null || !response.has("protocol") || response.get("protocol").getAsInt() != 1
                || !response.has("available") || !response.get("available").getAsBoolean())
            throw new IllegalArgumentException("Estado de nodo inválido");
        String version = response.has("location_version") ? response.get("location_version").getAsString() : null;
        if (version != null && !version.matches("[a-f0-9]{16}|none")) throw new IllegalArgumentException("Versión inválida");
        if (response.has("unchanged") && response.get("unchanged").getAsBoolean()) {
            if (version == null || !version.equals(versions.get(nodeId))) throw new IllegalArgumentException("«Sin cambios» sin versión conocida");
            return false;
        }
        JsonElement raw = response.get("location");
        if (raw == null || raw.isJsonNull()) {
            boolean had = locations.remove(nodeId) != null;
            receivedAt.remove(nodeId);
            if (version != null) versions.put(nodeId, version); else versions.remove(nodeId);
            return had;
        }
        PrivateLocation location = PrivateLocation.parse(raw.getAsJsonObject(), nowSeconds);
        if (!location.fresh(nowSeconds)) {
            boolean had = locations.remove(nodeId) != null;
            receivedAt.remove(nodeId);
            versions.remove(nodeId); // volver a pedirla entera la próxima vez
            return had;
        }
        String json = location.json().toString();
        boolean changed = !json.equals(locations.get(nodeId));
        locations.put(nodeId, json);
        receivedAt.put(nodeId, nowSeconds);
        if (version != null) versions.put(nodeId, version); else versions.remove(nodeId);
        return changed;
    }

    /** Ubicación guardada si sigue vigente (un GPS caduca a los 15 min); null si no hay. */
    public static PrivateLocation current(Map<String, String> locations, String nodeId, long nowSeconds) {
        String json = locations.get(nodeId);
        if (json == null) return null;
        try {
            PrivateLocation location = PrivateLocation.parse(JsonParser.parseString(json).getAsJsonObject(), nowSeconds);
            return location.fresh(nowSeconds) ? location : null;
        } catch (RuntimeException unreadable) { return null; }
    }

    /** «Vitoria-Gasteiz · Casa» para la constelación; vacío si no hay datos. */
    public static String place(PrivateLocation location) {
        if (location == null) return "";
        String city = location.city.trim(), zone = location.zone.trim();
        if (city.isEmpty() && zone.isEmpty()) return String.format(java.util.Locale.ROOT, "%.3f, %.3f", location.latitude, location.longitude);
        return city.isEmpty() ? zone : zone.isEmpty() ? city : city + " · " + zone;
    }
}
