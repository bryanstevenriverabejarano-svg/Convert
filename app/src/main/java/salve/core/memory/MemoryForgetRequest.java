package salve.core.memory;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interpreta únicamente solicitudes de olvido de categorías de perfil conocidas. */
public final class MemoryForgetRequest {
    private static final Pattern PREFERENCE = Pattern.compile(
            "^(?:olvida|borra|elimina)\\s+(?:mi |la )?preferencia(?: sobre| de)?\\s+(.+)$");

    private final String category;
    private final String description;

    private MemoryForgetRequest(String category, String description) {
        this.category = category;
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public static boolean looksLikeForgetCommand(String input) {
        String normalized = normalize(input);
        if (normalized.matches("^olvida\\b.*")) return true;
        return normalized.matches("^(borra|elimina)\\b.*\\b(mi nombre|nombre|donde vivo|residencia|direccion|"
                + "mi origen|origen|mi trabajo|trabajo|mi objetivo|objetivo|cumpleanos|preferencia|"
                + "memoria|recuerdo|todo|aquello)\\b.*");
    }

    public static MemoryForgetRequest parse(String input) {
        String normalized = normalize(input);
        if (!looksLikeForgetCommand(normalized) || normalized.matches(".*\\b(todo|todos|toda la memoria)\\b.*")) {
            return null;
        }

        if (normalized.matches(".*\\b(mi nombre|como me llamo|nombre)\\b.*")) {
            return new MemoryForgetRequest("name", "tu nombre");
        }
        if (normalized.matches(".*\\b(donde vivo|mi residencia|mi direccion|residencia)\\b.*")) {
            return new MemoryForgetRequest("residence", "tu residencia");
        }
        if (normalized.matches(".*\\b(de donde soy|mi origen|origen)\\b.*")) {
            return new MemoryForgetRequest("origin", "tu lugar de origen");
        }
        if (normalized.matches(".*\\b(donde trabajo|mi trabajo|trabajo)\\b.*")) {
            return new MemoryForgetRequest("work", "tu trabajo");
        }
        if (normalized.matches(".*\\b(mi objetivo|objetivo)\\b.*")) {
            return new MemoryForgetRequest("goal", "tu objetivo");
        }
        if (normalized.matches(".*\\b(mi cumpleanos|cumpleanos)\\b.*")) {
            return new MemoryForgetRequest("birthday", "tu cumpleaños");
        }

        Matcher preference = PREFERENCE.matcher(normalized);
        if (preference.matches()) {
            String topic = firstMeaningfulWord(preference.group(1));
            if (!topic.isEmpty()) {
                return new MemoryForgetRequest("preference_" + topic, "tu preferencia sobre " + topic);
            }
        }
        return null;
    }

    private static String firstMeaningfulWord(String value) {
        for (String word : value.split("\\s+")) {
            if (!word.matches("el|la|los|las|un|una|unos|unas")) {
                return word.replaceAll("[^a-z0-9_]", "");
            }
        }
        return "";
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[.!?]+$", "")
                .replaceAll("\\s+", " ");
    }
}
