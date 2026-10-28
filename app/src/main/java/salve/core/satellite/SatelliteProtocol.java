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
    public static final String ENQUEUE = "/v1/satellite/enqueue";
    public static final String RESULT_OF = "/v1/satellite/result/"; // + id

    private SatelliteProtocol() { }

    /** Lo que la Salve del dueño envía al nodo para repartir un trabajo de cómputo a un satélite. */
    public static JsonObject enqueueCompute(String prompt) {
        JsonObject params = new JsonObject();
        params.addProperty("prompt", prompt == null ? "" : prompt);
        JsonObject body = new JsonObject();
        body.addProperty("kind", "compute");
        body.add("params", params);
        return body;
    }

    /** El texto de un resultado ya recogido del nodo ({ready:true, output:{status,text,…}}), o null. */
    public static String resultText(JsonObject resultReply) {
        if (resultReply == null || !resultReply.has("output") || !resultReply.get("output").isJsonObject()) return null;
        JsonObject output = resultReply.getAsJsonObject("output");
        if (!output.has("status") || !"ok".equals(output.get("status").getAsString())) return null;
        return output.has("text") ? output.get("text").getAsString() : null;
    }

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

    private static final int MAX_PROMPT = 8192;

    /** El texto a inferir que trae un trabajo de cómputo, recortado a un tamaño razonable, o "". */
    public static String computePrompt(JsonObject params) {
        if (params == null || !params.has("prompt")) return "";
        String prompt = params.get("prompt").getAsString();
        if (prompt == null) return "";
        prompt = prompt.trim();
        return prompt.length() > MAX_PROMPT ? prompt.substring(0, MAX_PROMPT) : prompt;
    }

    /** Da forma al resultado de un trabajo de cómputo: estado, texto y latencia. */
    public static JsonObject computeOutput(String status, String text, long latencyMs) {
        JsonObject out = new JsonObject();
        out.addProperty("status", status);
        out.addProperty("text", text == null ? "" : text);
        out.addProperty("latency_ms", latencyMs);
        return out;
    }

    private static final int MAX_SCREEN = 2000;

    /** El texto que un trabajo de pantalla quiere mostrar, recortado, o "". */
    public static String screenText(JsonObject params) {
        if (params == null || !params.has("text")) return "";
        String text = params.get("text").getAsString();
        if (text == null) return "";
        text = text.trim();
        return text.length() > MAX_SCREEN ? text.substring(0, MAX_SCREEN) : text;
    }

    /** Qué sensor pide un trabajo de sensores: "location", "camera", "mic", o "" si no es uno conocido. */
    public static String sensorKind(JsonObject params) {
        if (params == null || !params.has("sensor")) return "";
        String s = params.get("sensor").getAsString();
        if (s == null) return "";
        switch (s) {
            case "location": case "camera": case "mic": return s;
            default: return "";
        }
    }

    /** Un resultado sencillo (estado y un mensaje), para pantalla y para sensores sin datos. */
    public static JsonObject simpleOutput(String status, String message) {
        JsonObject out = new JsonObject();
        out.addProperty("status", status);
        out.addProperty("message", message == null ? "" : message);
        return out;
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
