package salve.core.cognition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import salve.core.autonomy.AutonomousToolCommand;

/** The model/controller boundary. Never extracts JSON from prose or executes message text. */
public final class CognitiveReply {
    public final String message, name;
    public final JsonObject arguments;
    private CognitiveReply(String message, String name, JsonObject arguments) {
        this.message = message; this.name = name; this.arguments = arguments;
    }
    public static CognitiveReply parse(String raw) {
        JsonObject value = AutonomousToolCommand.parseObject(raw);
        fields(value, "mode", "message", "action");
        String mode = string(value, "mode");
        if (mode.equals("respond") && value.get("action").isJsonNull()) {
            String message = string(value, "message");
            if (message.trim().isEmpty()) throw new IllegalArgumentException("Respuesta vacía");
            return new CognitiveReply(message, null, null);
        }
        if (mode.equals("tool") && value.get("message").isJsonNull() && value.get("action").isJsonObject()) {
            JsonObject action = value.getAsJsonObject("action");
            fields(action, "name", "arguments");
            String name = string(action, "name");
            if (!name.matches("[A-Za-z][A-Za-z0-9_.]{0,63}") || !action.get("arguments").isJsonObject())
                throw new IllegalArgumentException("Acción inválida");
            return new CognitiveReply(null, name, action.getAsJsonObject("arguments"));
        }
        throw new IllegalArgumentException("Contrato de salida inválido");
    }
    /** Conversation-only fallback. Strict parse remains available to verify constrained generation. */
    public static CognitiveReply parseForConversation(String raw) {
        try { return parse(raw); }
        catch (IllegalArgumentException invalid) {
            String text = CognitiveTextFallback.checkedText(raw);
            if (text == null) throw invalid;
            String unfenced = CognitiveTextFallback.unwrapFence(text);
            if (unfenced != null && CognitiveTextFallback.brokenEnvelope(unfenced)) {
                // Recover a complete response envelope only. Never recover an action from fences/prose.
                CognitiveReply reply = parse(unfenced);
                if (reply.name == null) return reply;
                throw invalid;
            }
            if (isToolAttempt(text)) throw invalid;
            if (CognitiveTextFallback.brokenEnvelope(text)) {
                // Sobre cortado a medias: se rescata el mensaje ya escrito en vez de perder la respuesta.
                String partial = CognitiveTextFallback.partialMessage(text);
                if (partial == null) throw invalid;
                return new CognitiveReply(partial, null, null);
            }
            return new CognitiveReply(text, null, null);
        }
    }
    public static boolean isToolAttempt(String raw) { return CognitiveTextFallback.toolAttempt(raw); }

    public static void fields(JsonObject value, String... names) {
        if (value.size() != names.length) throw new IllegalArgumentException("Campos incorrectos");
        for (String name : names) if (!value.has(name)) throw new IllegalArgumentException("Falta " + name);
    }
    public static String string(JsonObject value, String name) {
        JsonElement element = value.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Se requiere texto: " + name);
        return element.getAsString();
    }
}
