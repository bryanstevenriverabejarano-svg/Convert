package salve.core.memory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Extrae términos útiles para recuperación léxica sin salir del dispositivo. */
public final class MemoryQueryTerms {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "para", "pero", "porque", "como", "cuando", "donde", "quien", "esto", "esta",
            "este", "estos", "estas", "sobre", "desde", "hasta", "tengo", "quiero", "puedes",
            "podrias", "dime", "sabes", "algo", "salve", "bryan", "tambien", "mucho", "hacer"));

    private MemoryQueryTerms() {}

    public static List<String> extract(String input, int maxTerms) {
        List<String> result = new ArrayList<>();
        if (input == null || maxTerms <= 0) return result;
        String lowercase = input.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String token : lowercase.split("[^\\p{L}0-9]+")) {
            String folded = Normalizer.normalize(token, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            if (token.length() < 4 || STOP_WORDS.contains(folded)) continue;
            unique.add(token);
            if (unique.size() == maxTerms) break;
        }
        result.addAll(unique);
        return result;
    }
}
