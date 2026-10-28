package salve.core.satellite;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * El lenguaje entre un satélite y la Salve del dueño, a través del nodo del dueño (un servidor con
 * dirección fija en internet al que llegan los dos, estén donde estén). Solo construye y lee los
 * mensajes; no abre conexiones (de eso se encarga {@link SatelliteLink}).
 *
 * <p>Rutas en el nodo:
 * <ul>
 *   <li>POST {@code /v1/satellite/register} — el satélite se presenta (id, nombre, capacidades).</li>
 *   <li>POST {@code /v1/satellite/heartbeat} — sigo aquí y disponible; el nodo responde si hay trabajo.</li>
 *   <li>POST {@code /v1/satellite/result} — aquí tienes el resultado del trabajo pedido.</li>
 * </ul></p>
 */
public final class SatelliteProtocol {
    public static final String REGISTER = "/v1/satellite/register";
    public static final String HEARTBEAT = "/v1/satellite/heartbeat";
    public static final String RESULT = "/v1/satellite/result";

    private SatelliteProtocol() { }

    /** «Sigo aquí»: id, si está disponible para trabajar, y qué capacidades ofrece ahora mismo. */
    public static JsonObject heartbeat(String satelliteId, boolean available, SatelliteCapabilities caps) {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("id", satelliteId);
        payload.addProperty("available", available);
        payload.add("capabilities", (caps == null ? new SatelliteCapabilities(false, false, false) : caps).toJson());
        return payload;
    }

    /** Un trabajo que el nodo asigna a este satélite, o null si no hay nada que hacer. */
    public static final class Assignment {
        public final String taskId, kind;
        public final JsonObject params;
        public Assignment(String taskId, String kind, JsonObject params) {
            this.taskId = taskId; this.kind = kind; this.params = params;
        }
    }

    /**
     * Lee la respuesta del nodo a un latido. Formato: {@code {"task":{"id":…,"kind":"compute|screen|sensors",…}}}
     * o un objeto sin «task» cuando no hay trabajo. Devuelve null si no hay nada o el mensaje no es válido.
     */
    public static Assignment parseAssignment(JsonObject response) {
        if (response == null || !response.has("task") || !response.get("task").isJsonObject()) return null;
        JsonObject task = response.getAsJsonObject("task");
        if (!task.has("id") || !task.has("kind")) return null;
        String id = task.get("id").getAsString();
        String kind = task.get("kind").getAsString();
        if (id.isEmpty() || !(kind.equals("compute") || kind.equals("screen") || kind.equals("sensors"))) return null;
        JsonObject params = task.has("params") && task.get("params").isJsonObject() ? task.getAsJsonObject("params") : new JsonObject();
        return new Assignment(id, kind, params);
    }

    /** Un trabajo solo se acepta si el satélite ofrece esa capacidad (el dueño la activó). */
    public static boolean allowed(Assignment task, SatelliteCapabilities caps) {
        if (task == null || caps == null) return false;
        switch (task.kind) {
            case "compute": return caps.compute;
            case "screen": return caps.screen;
            case "sensors": return caps.sensors;
            default: return false;
        }
    }

    /** El resultado de un trabajo, para devolverlo al nodo. */
    public static JsonObject result(String satelliteId, String taskId, JsonObject output) {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("id", satelliteId);
        payload.addProperty("task", taskId);
        payload.add("output", output == null ? new JsonObject() : output);
        return payload;
    }
}
