package salve.core.voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parte lo que Salve va a decir en frases para su voz neuronal. Salve pide al motor una frase
 * cada vez y el motor solo devuelve audio: nunca ejecuta código de Salve a mitad de generar
 * (lo que antes podía cerrar la app). Cada frase conserva su posición en el texto original
 * para que el avatar sepa qué está diciendo.
 *
 * Sin Android, para probarlo en JVM.
 */
public final class SpeechSentences {
    /** Una frase y dónde está en el texto original [start, end). */
    public static final class Piece {
        public final String text;
        public final int start, end;
        Piece(String text, int start, int end) { this.text = text; this.start = start; this.end = end; }
        @Override public String toString() { return text; }
    }

    /** Frases más cortas se juntan con la siguiente: el motor suena mejor y no hace pausas raras. */
    static final int MIN_CHARS = 24;
    /** Frases más largas se parten por comas o espacios: la primera suena antes. */
    static final int MAX_CHARS = 220;

    private SpeechSentences() { }

    public static List<Piece> split(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();
        List<int[]> raw = new ArrayList<>();
        int start = 0, n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            boolean hardBreak = c == '\n';
            boolean sentenceEnd = (c == '.' || c == '!' || c == '?' || c == '…' || c == ';')
                    && (i + 1 == n || Character.isWhitespace(text.charAt(i + 1)) || isClosing(text.charAt(i + 1)))
                    && !isNumberDot(text, i);
            if (hardBreak || sentenceEnd) {
                int end = i + 1;
                while (end < n && isClosing(text.charAt(end))) end++;
                raw.add(new int[] {start, end});
                start = end;
                i = end - 1;
            }
        }
        if (start < n) raw.add(new int[] {start, n});

        // Juntar las cortas con la siguiente; partir las largas.
        List<Piece> out = new ArrayList<>();
        int pendingStart = -1;
        for (int k = 0; k < raw.size(); k++) {
            int s = pendingStart >= 0 ? pendingStart : raw.get(k)[0];
            int e = raw.get(k)[1];
            String content = text.substring(s, e).trim();
            boolean last = k + 1 == raw.size();
            if (content.length() < MIN_CHARS && !last && !text.substring(s, e).contains("\n\n")) {
                pendingStart = s;
                continue;
            }
            pendingStart = -1;
            addLong(text, s, e, out);
        }
        return out;
    }

    private static void addLong(String text, int s, int e, List<Piece> out) {
        while (true) {
            int[] trimmed = trim(text, s, e);
            if (trimmed == null) return;
            s = trimmed[0]; e = trimmed[1];
            if (e - s <= MAX_CHARS) { out.add(new Piece(text.substring(s, e), s, e)); return; }
            int cut = lastBreak(text, s, s + MAX_CHARS);
            out.add(new Piece(text.substring(s, cut).trim(), s, cut));
            s = cut;
        }
    }

    /** Último buen sitio para cortar antes de {@code limit}: coma/dos puntos, si no un espacio. */
    private static int lastBreak(String text, int from, int limit) {
        int minimum = from + MIN_CHARS;
        for (int i = limit - 1; i > minimum; i--) {
            char c = text.charAt(i);
            if ((c == ',' || c == ':' || c == '—' || c == ')') && i + 1 < text.length()
                    && Character.isWhitespace(text.charAt(i + 1))) return i + 1;
        }
        for (int i = limit - 1; i > minimum; i--) if (Character.isWhitespace(text.charAt(i))) return i;
        return limit;
    }

    private static int[] trim(String text, int s, int e) {
        while (s < e && Character.isWhitespace(text.charAt(s))) s++;
        while (e > s && Character.isWhitespace(text.charAt(e - 1))) e--;
        return s < e ? new int[] {s, e} : null;
    }

    private static boolean isClosing(char c) {
        return c == '"' || c == '»' || c == ')' || c == '\'' || c == '”' || c == '!' || c == '?' || c == '.';
    }

    /** «3.5», «1.240 recuerdos» o «Sr.» seguido de minúscula no terminan frase. */
    private static boolean isNumberDot(String text, int i) {
        if (text.charAt(i) != '.') return false;
        boolean digitBefore = i > 0 && Character.isDigit(text.charAt(i - 1));
        boolean digitAfter = i + 1 < text.length() && Character.isDigit(text.charAt(i + 1));
        if (digitBefore && digitAfter) return true;
        int next = i + 1;
        while (next < text.length() && text.charAt(next) == ' ') next++;
        return next < text.length() && next > i + 1 && Character.isLowerCase(text.charAt(next));
    }
}
