package salve.core.cognition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** One packaged source for each native decoder; no model/user supplied grammar is accepted. */
public final class CognitiveOutputConstraints {
    private CognitiveOutputConstraints() { }
    private static final class Schema { private static final String TEXT = read("cognitive_reply.schema.json"); }
    private static final class Grammar { private static final String TEXT = read("cognitive_reply.gbnf"); }
    public static String jsonSchema() { return Schema.TEXT; }
    public static String gbnf() { return Grammar.TEXT; }
    private static String read(String name) {
        try (InputStream input = CognitiveOutputConstraints.class.getResourceAsStream("/salve/" + name)) {
            if (input == null) throw new IOException("Missing packaged constraint");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int count;
            while ((count = input.read(chunk)) != -1) {
                if (bytes.size() + count > 16384) throw new IOException("Oversized constraint");
                bytes.write(chunk, 0, count);
            }
            String text = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            if (text.trim().isEmpty()) throw new IOException("Empty constraint");
            return text;
        } catch (IOException unavailable) {
            throw new IllegalStateException("No se pudo cargar el contrato de generación local", unavailable);
        }
    }
}
