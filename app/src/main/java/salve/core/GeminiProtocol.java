package salve.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;

/** JSON contract for generateContent, independent of Android and network transport. */
public final class GeminiProtocol {
    public static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private GeminiProtocol() {}

    public static String modelName(String value) {
        String model = value == null ? "" : value.trim();
        if (model.isEmpty()) return DEFAULT_MODEL;
        if (!model.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("Usa el identificador del modelo, sin URL ni prefijo models/.");
        }
        return model;
    }

    public static String request(String prompt, List<String> jpegBase64) {
        if (prompt == null || prompt.trim().isEmpty()) throw new IllegalArgumentException("Prompt vacío");
        JsonArray parts = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("text", prompt);
        parts.add(text);
        if (jpegBase64 != null) {
            for (String jpeg : jpegBase64) {
                if (jpeg == null || jpeg.isEmpty()) throw new IllegalArgumentException("Imagen vacía");
                JsonObject data = new JsonObject();
                data.addProperty("mimeType", "image/jpeg");
                data.addProperty("data", jpeg);
                JsonObject part = new JsonObject();
                part.add("inlineData", data);
                parts.add(part);
            }
        }
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject body = new JsonObject();
        body.add("contents", contents);
        JsonObject config = new JsonObject();
        config.addProperty("maxOutputTokens", 4096);
        body.add("generationConfig", config);
        return body.toString();
    }

    public static ModelResult response(int code, String body, long latency) {
        // Do not expose raw server responses: they may echo user content or credentials.
        if (code < 200 || code >= 300) {
            String detail;
            if (code == 401 || code == 403) detail = "Revisa la clave API y sus permisos";
            else if (code == 404) detail = "El modelo no existe o no está disponible para esta clave";
            else if (code == 429) detail = "Cuota o límite de solicitudes alcanzado";
            else detail = "La petición a Gemini falló";
            return ModelResult.failure(ModelResult.Status.ERROR, detail + " (HTTP " + code + ")", latency);
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root.has("promptFeedback") && root.getAsJsonObject("promptFeedback").has("blockReason")) {
                return failure("Gemini bloqueó la solicitud", latency);
            }
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return failure("Gemini no devolvió candidatos", latency);
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            String finish = candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : "";
            if (!finish.equals("STOP")) {
                return failure(finish.equals("MAX_TOKENS")
                        ? "Gemini alcanzó el límite antes de completar la respuesta"
                        : "Gemini no completó la respuesta", latency);
            }
            StringBuilder text = new StringBuilder();
            for (JsonElement item : candidate.getAsJsonObject("content").getAsJsonArray("parts")) {
                JsonObject part = item.getAsJsonObject();
                if (part.has("thought") && part.get("thought").getAsBoolean()) continue;
                if (part.has("text") && !part.get("text").isJsonNull()) text.append(part.get("text").getAsString());
            }
            return text.toString().trim().isEmpty() ? failure("Gemini no devolvió texto público", latency)
                    : ModelResult.success(text.toString().trim(), latency);
        } catch (RuntimeException e) {
            return failure("Respuesta de Gemini no válida", latency);
        }
    }

    private static ModelResult failure(String error, long latency) {
        return ModelResult.failure(ModelResult.Status.ERROR, error, latency);
    }
}
