package salve.core.memory;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dato estable del usuario con una clave que permite reemplazar contradicciones. */
public final class MemoryProfileFact {
    private static final Pattern FIXED_FACT = Pattern.compile(
            "^(mi nombre es|me llamo|vivo en|soy de|trabajo en|mi objetivo es|mi cumpleanos es)\\s+(.+)$");
    private static final Pattern PREFERENCE = Pattern.compile(
            "^(prefiero|no me gusta)\\s+(.+)$");

    private final String category;
    private final String statement;

    private MemoryProfileFact(String category, String statement) {
        this.category = category;
        this.statement = statement;
    }

    public String getCategory() {
        return category;
    }

    public String getStatement() {
        return statement;
    }

    public String getStorageTag() {
        return "profile:" + category;
    }

    public static MemoryProfileFact parse(String input) {
        if (input == null || input.trim().isEmpty() || input.trim().endsWith("?")) return null;
        String normalized = normalize(input).replaceFirst(
                "^(?:(?:correccion|en realidad)[,:]?\\s+|no,\\s+)?(?:ahora\\s+)?", "");

        Matcher fixed = FIXED_FACT.matcher(normalized);
        if (fixed.matches()) {
            String category;
            switch (fixed.group(1)) {
                case "mi nombre es":
                case "me llamo": category = "name"; break;
                case "vivo en": category = "residence"; break;
                case "soy de": category = "origin"; break;
                case "trabajo en": category = "work"; break;
                case "mi objetivo es": category = "goal"; break;
                default: category = "birthday";
            }
            return new MemoryProfileFact(category, input.trim());
        }

        Matcher preference = PREFERENCE.matcher(normalized);
        if (preference.matches()) {
            String topic = firstMeaningfulWord(preference.group(2));
            if (!topic.isEmpty()) {
                return new MemoryProfileFact("preference_" + topic, input.trim());
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
        return Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[.!]+$", "")
                .replaceAll("\\s+", " ");
    }
}
