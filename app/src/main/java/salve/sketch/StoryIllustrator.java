package salve.sketch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ilustra un cuento mientras se cuenta: después de cada párrafo que nombra algo que Salve sabe trazar
 * (un castillo, la luna, un dragón…) coloca una marca de dibujo. Así el relato se reparte en partes
 * y cada dibujo se va trazando mientras ella sigue escribiendo la siguiente.
 */
public final class StoryIllustrator {
    public static final int MAX_DRAWINGS = 4;
    private static final Pattern PARAGRAPH = Pattern.compile("\\n\\s*\\n");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?…](?=\\s|$)");

    private StoryIllustrator() { }

    /**
     * @param completedOnly true mientras se escribe: no ilustra la última parte, que aún puede cambiar.
     */
    public static String annotate(String text, SketchLibrary library, boolean completedOnly) {
        if (text == null || library == null || text.trim().isEmpty() || SketchProtocol.contains(text)) return text;
        List<int[]> units = units(text);
        if (completedOnly && !units.isEmpty()) {
            int[] last = units.get(units.size() - 1);
            // La última unidad sólo cuenta si después ya empezó otra (párrafo cerrado).
            if (!text.substring(last[1]).matches("(?s)\\s*\\n\\s*\\n.*")) units.remove(units.size() - 1);
        }
        Set<String> used = new HashSet<>();
        StringBuilder out = new StringBuilder();
        int cursor = 0, drawings = 0;
        for (int[] unit : units) {
            if (drawings == MAX_DRAWINGS) break;
            String part = text.substring(unit[0], unit[1]);
            List<SketchLibrary.Motif> chosen = new ArrayList<>();
            for (SketchLibrary.Mention m : library.mentions(part, true)) {
                if (used.contains(m.motif.id)) continue;
                chosen.add(m.motif);
                if (chosen.size() == 2) break;
            }
            if (chosen.isEmpty()) continue;
            for (SketchLibrary.Motif m : chosen) used.add(m.id);
            SketchScene scene = SketchScene.compose(chosen);
            out.append(text, cursor, unit[1]).append("\n").append(SketchProtocol.tag(scene)).append("\n");
            cursor = unit[1];
            drawings++;
        }
        out.append(text.substring(cursor));
        return out.toString();
    }

    /** Párrafos; si sólo hay uno largo, grupos de dos frases. Devuelve [inicio, fin) de cada parte. */
    static List<int[]> units(String text) {
        List<int[]> units = new ArrayList<>();
        Matcher m = PARAGRAPH.matcher(text);
        int cursor = 0;
        while (m.find()) {
            if (!text.substring(cursor, m.start()).trim().isEmpty()) units.add(new int[]{cursor, m.start()});
            cursor = m.end();
        }
        if (!text.substring(cursor).trim().isEmpty()) units.add(new int[]{cursor, trimEnd(text)});
        if (units.size() > 1 || text.length() < 280) return units;
        List<int[]> sentences = new ArrayList<>();
        Matcher end = SENTENCE_END.matcher(text);
        int start = 0, count = 0;
        while (end.find()) {
            if (++count % 2 == 0) { sentences.add(new int[]{start, end.end()}); start = end.end(); }
        }
        if (start < trimEnd(text)) sentences.add(new int[]{start, trimEnd(text)});
        return sentences.size() > 1 ? sentences : units;
    }

    private static int trimEnd(String text) {
        int e = text.length();
        while (e > 0 && Character.isWhitespace(text.charAt(e - 1))) e--;
        return e;
    }
}
