package salve.core.memory;

import java.util.Locale;
import java.util.regex.Pattern;

/** Decide qué mensajes declarativos merecen memoria a largo plazo. */
public final class MemoryWritePolicy {
    private static final Pattern STABLE_USER_FACT = Pattern.compile(
            "\\b(mi nombre es|me llamo|vivo en|soy de|trabajo en|prefiero|no me gusta|"
                    + "mi objetivo es|mi cumpleaños es|soy alérgic[oa] a)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private MemoryWritePolicy() {}

    public static boolean shouldPersist(String input) {
        if (input == null) return false;
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 5 || normalized.endsWith("?")) return false;
        return STABLE_USER_FACT.matcher(normalized).find();
    }
}
