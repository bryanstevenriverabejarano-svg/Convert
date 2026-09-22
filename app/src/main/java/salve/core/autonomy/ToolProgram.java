package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Small executable declarative program, synthesized from audited capabilities, never Java/shell. */
public final class ToolProgram {
    private ToolProgram() { }
    public static JsonObject generate(String family, String strategy) {
        if (!ToolKernels.strategies(family).contains(strategy)) throw new IllegalArgumentException("Estrategia no permitida");
        JsonObject program = new JsonObject();
        program.addProperty("schema", 1);
        program.addProperty("language", "salve-tools/1");
        program.addProperty("family", family);
        JsonArray steps = new JsonArray();
        JsonObject validate = new JsonObject(); validate.addProperty("op", "validate_input");
        JsonObject solve = new JsonObject(); solve.addProperty("op", "solve"); solve.addProperty("strategy", strategy);
        JsonObject verify = new JsonObject(); verify.addProperty("op", "verify_exact");
        steps.add(validate); steps.add(solve); steps.add(verify); program.add("steps", steps);
        return program;
    }
    public static String strategy(JsonObject program) {
        try {
            String family = program.get("family").getAsString();
            String strategy = program.getAsJsonArray("steps").get(1).getAsJsonObject().get("strategy").getAsString();
            if (!generate(family, strategy).equals(program)) throw new IllegalArgumentException("Programa alterado");
            return strategy;
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Programa fuera del lenguaje permitido", invalid);
        }
    }
    public static String sha256(JsonObject program) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(program.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
