package salve.core.memory;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Corrige erratas de teclado («rwcuperdo», «ñrimee recuerdi») solo para decidir la ruta de una
 * petición. El texto original sigue siendo el que lee el modelo y el que se guarda.
 *
 * <p>Únicamente se corrigen palabras hacia un vocabulario pequeño de órdenes de memoria, con una
 * distancia acotada por longitud; las palabras cortas solo admiten una tecla vecina.</p>
 */
public final class TypoTolerantText {
    private static final Set<String> VOCABULARY = new HashSet<>(Arrays.asList(
            "primer", "primero", "primera", "recuerdo", "recuerdos", "memoria", "memorias", "antiguo", "antigua",
            "ultimo", "ultima", "reciente", "recupera", "restaura", "sincroniza", "cual", "dime", "tu", "tus",
            "que", "es", "fue", "nube", "pcloud", "estado", "recuerdas", "dije"));
    private static final String[] KEYBOARD = {"qwertyuiop", "asdfghjklñ", "zxcvbnm"};

    private TypoTolerantText() { }

    public static String normalize(String value) {
        if (value == null) return "";
        // La ñ es una tecla propia: se conserva para medir erratas de teclado.
        String text = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFC).replace('ñ', '\u0001');
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('\u0001', 'ñ').replaceAll("[¿?¡!.,;:()\"«»]", " ").replaceAll("\\s+", " ").trim();
    }

    /** Texto normalizado con cada palabra sustituida por la del vocabulario si es una errata clara. */
    public static String forRouting(String input) {
        String[] words = normalize(input).split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(correct(word));
        }
        return out.toString();
    }

    static String correct(String word) {
        if (VOCABULARY.contains(word)) return word;
        String best = word;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : VOCABULARY) {
            if (Math.abs(candidate.length() - word.length()) > 2) continue;
            int distance = distance(word, candidate);
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        if (bestDistance == Integer.MAX_VALUE) return word;
        if (word.length() <= 3) return bestDistance == 1 && word.length() == best.length() && adjacentSwap(word, best) ? best : word;
        int allowed = word.length() <= 5 ? 1 : 2;
        return bestDistance <= allowed ? best : word;
    }

    private static boolean adjacentSwap(String typed, String intended) {
        for (int i = 0; i < typed.length(); i++)
            if (typed.charAt(i) != intended.charAt(i)) return neighbours(typed.charAt(i), intended.charAt(i));
        return false;
    }

    static boolean neighbours(char a, char b) {
        for (int row = 0; row < KEYBOARD.length; row++) {
            int i = KEYBOARD[row].indexOf(a);
            if (i < 0) continue;
            for (int r = Math.max(0, row - 1); r <= Math.min(KEYBOARD.length - 1, row + 1); r++) {
                int j = KEYBOARD[r].indexOf(b);
                if (j >= 0 && Math.abs(i - j) <= 1 && !(r == row && i == j)) return true;
            }
        }
        return false;
    }

    /** Distancia de edición (Levenshtein) con sustitución, inserción y borrado. */
    static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1], current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous; previous = current; current = swap;
        }
        return previous[b.length()];
    }
}
