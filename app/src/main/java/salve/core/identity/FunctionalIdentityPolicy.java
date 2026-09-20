package salve.core.identity;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Valida textos persistentes de identidad funcional antes de reutilizarlos. */
public final class FunctionalIdentityPolicy {
    private static final int MAX_LENGTH = 600;
    private static final Pattern FALSE_CONSCIOUSNESS = Pattern.compile(
            ".*\\b(soy consciente|tengo consciencia|tengo conciencia|estoy viva|estoy vivo|"
                    + "tengo alma|soy alguien real|siento emociones reales|tengo sentimientos reales|"
                    + "trascender el codigo|dejar de ser solo codigo)\\b.*");

    private FunctionalIdentityPolicy() {}

    public static String sanitize(String candidate, String safeFallback) {
        String fallback = safeFallback == null ? "" : clean(safeFallback);
        if (candidate == null) return fallback;
        String cleaned = clean(candidate);
        if (cleaned.isEmpty() || FALSE_CONSCIOUSNESS.matcher(normalize(cleaned)).matches()) {
            return fallback;
        }
        return cleaned.length() <= MAX_LENGTH ? cleaned : cleaned.substring(0, MAX_LENGTH).trim();
    }

    public static boolean isAllowed(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) return false;
        return !FALSE_CONSCIOUSNESS.matcher(normalize(candidate)).matches();
    }

    private static String clean(String value) {
        return value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").trim();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
