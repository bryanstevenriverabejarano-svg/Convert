package salve.core.satellite;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Lo que un satélite le ofrece a la Salve del dueño. Son los tres usos que Bryan eligió:
 * cómputo (prestar CPU/GPU), pantalla (mostrar a Salve y su avatar) y sensores (cámara, micrófono,
 * ubicación). El dueño decide cuáles activa en cada satélite; por defecto, los tres.
 *
 * <p>Esto describe la oferta; no ejecuta nada por sí mismo. La ejecución real de cada trabajo exige,
 * como en el resto de la app, la confirmación y los permisos correspondientes en el satélite.</p>
 */
public final class SatelliteCapabilities {
    public final boolean compute, screen, sensors;

    public SatelliteCapabilities(boolean compute, boolean screen, boolean sensors) {
        this.compute = compute; this.screen = screen; this.sensors = sensors;
    }

    /** Lo que Bryan eligió para un satélite: los tres usos. */
    public static SatelliteCapabilities all() { return new SatelliteCapabilities(true, true, true); }

    public boolean none() { return !compute && !screen && !sensors; }

    public JsonArray toJson() {
        JsonArray array = new JsonArray();
        if (compute) array.add("compute");
        if (screen) array.add("screen");
        if (sensors) array.add("sensors");
        return array;
    }

    public static SatelliteCapabilities fromJson(JsonArray array) {
        boolean c = false, s = false, se = false;
        if (array != null) for (int i = 0; i < array.size(); i++) {
            switch (array.get(i).getAsString()) {
                case "compute": c = true; break;
                case "screen": s = true; break;
                case "sensors": se = true; break;
                default: // una capacidad que esta versión no conoce: se ignora
            }
        }
        return new SatelliteCapabilities(c, s, se);
    }

    /**
     * Cómo se presenta el satélite a su Salve al unirse: un identificador estable, un nombre legible,
     * sus capacidades y la versión del protocolo. No lleva datos personales ni memoria.
     */
    public JsonObject registration(String satelliteId, String displayName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("id", satelliteId);
        payload.addProperty("name", displayName == null ? "" : displayName);
        payload.add("capabilities", toJson());
        payload.addProperty("role", "satellite");
        return payload;
    }
}
