package salve.core.conversation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Idioma de la respuesta. Un modelo pequeño multilingüe puede «cambiar de carril» al inglés
 * (15/10/2026: «Estoy con mucho sueño. ¿Y tú?» → «Oh, I'm feeling quite relaxed…»). Se detecta
 * por palabras funcionales, que no dependen del tema, y la respuesta se rehace en español.
 */
public final class LanguageGuard {
    private static final Set<String> EN = new HashSet<>(Arrays.asList(
            "the", "and", "is", "are", "you", "your", "i", "i'm", "im", "it", "it's", "to", "of", "that", "this",
            "with", "for", "just", "be", "feeling", "doesn't", "can", "what", "how", "do", "does", "my", "me", "so",
            "or", "about", "have", "here", "there", "would", "like", "sounds", "really", "quite", "a", "an"));
    private static final Set<String> ES = new HashSet<>(Arrays.asList(
            "el", "la", "los", "las", "y", "es", "son", "tu", "tú", "yo", "que", "de", "en", "con", "para", "por",
            "un", "una", "me", "mi", "te", "lo", "se", "no", "si", "sí", "como", "pero", "muy", "más", "estoy",
            "eres", "estás", "hay", "algo", "qué", "cómo", "del", "al", "ya", "hoy"));

    private LanguageGuard() { }

    /** true si el texto es claramente inglés (y no un español con alguna palabra inglesa). */
    public static boolean isMostlyEnglish(String text) {
        if (text == null) return false;
        int en = 0, es = 0;
        for (String word : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}' ]+", " ").split("\\s+")) {
            if (word.isEmpty()) continue;
            if (EN.contains(word)) en++;
            if (ES.contains(word)) es++;
        }
        return en >= 4 && en > es * 2;
    }

    /** «Dímelo en español», «repítelo en español», «tradúcelo», «lo que dijiste en inglés…». */
    public static boolean asksForSpanish(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
        return t.matches(".*\\b(en espanol|en castellano|traducelo|traduce lo|traduceme|no hables en ingles|"
                + "por que (me )?(hablas|escribes|respondes) en ingles|dijiste (antes )?en ingles)\\b.*");
    }
}
