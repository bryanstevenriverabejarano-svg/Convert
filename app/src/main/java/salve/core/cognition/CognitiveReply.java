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
