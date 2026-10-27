package salve.core;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Velocidad real del modelo local, medida en el móvil: cuánto tarda la primera palabra (lectura del
 * prompt) y cuántas palabras por segundo escribe después (decodificación, lo que acelera MTP).
 * Así se comprueba con números si MTP y E4B ayudan de verdad, en vez de fiarse de benchmarks ajenos.
 */
public final class GenerationSpeed {
    /** Una respuesta medida. */
    public static final class Sample {
        public final long firstWordMs, writingMs;
        public final int words;
        public final String engine;
        /** Tokens que ocupó la conversación (lo leído + lo escrito); -1 si el motor no lo dice. */
        public final int tokens;
        Sample(long firstWordMs, long writingMs, int words, String engine) { this(firstWordMs, writingMs, words, engine, -1); }
        Sample(long firstWordMs, long writingMs, int words, String engine, int tokens) {
            this.firstWordMs = firstWordMs; this.writingMs = writingMs; this.words = words; this.engine = engine; this.tokens = tokens;
        }
        /** La misma medida con el tamaño de la conversación en tokens. */
        public Sample withTokens(int count) { return new Sample(firstWordMs, writingMs, words, engine, count); }
        /** Palabras por segundo al escribir; 0 si fue demasiado corta para medirla bien. */
        public double wordsPerSecond() { return words < 3 || writingMs < 200 ? 0 : words * 1000.0 / writingMs; }
        public String describe() {
            String speed = wordsPerSecond() > 0 ? String.format(Locale.ROOT, " · %.1f palabras/s", wordsPerSecond()).replace('.', ',') : "";
            return String.format(Locale.ROOT, "primera palabra en %.1f s", firstWordMs / 1000.0).replace('.', ',') + speed
                    + " · " + words + " palabras" + (tokens > 0 ? " · " + tokens + " tokens en total" : "") + " · " + engine;
        }
    }

    private static final int WINDOW = 10;
    private static final ArrayDeque<Sample> recent = new ArrayDeque<>();

    private GenerationSpeed() { }

    /** Mide una respuesta. {@code firstChunkAt} ≤ 0 si no llegó ningún fragmento. */
    public static Sample measure(long startedAt, long firstChunkAt, long endedAt, String text, String engine) {
        long first = firstChunkAt > 0 ? Math.max(0, firstChunkAt - startedAt) : Math.max(0, endedAt - startedAt);
        long writing = firstChunkAt > 0 ? Math.max(0, endedAt - firstChunkAt) : 0;
        return new Sample(first, writing, countWords(text), engine == null ? "?" : engine);
    }

    public static synchronized void record(Sample sample) {
        if (sample == null) return;
        recent.addLast(sample);
        while (recent.size() > WINDOW) recent.removeFirst();
    }

    public static synchronized Sample last() { return recent.peekLast(); }

    /** «Velocidad (media de 5): primera palabra 1,4 s · 21,3 palabras/s»; vacío si aún no hay medidas. */
    public static synchronized String summary() {
        if (recent.isEmpty()) return "";
        long first = 0; double speed = 0; int speedSamples = 0;
        for (Sample s : recent) {
            first += s.firstWordMs;
            if (s.wordsPerSecond() > 0) { speed += s.wordsPerSecond(); speedSamples++; }
        }
        long tokens = 0; int tokenSamples = 0;
        for (Sample s : recent) if (s.tokens > 0) { tokens += s.tokens; tokenSamples++; }
        String out = String.format(Locale.ROOT, "Velocidad (media de %d): primera palabra %.1f s", recent.size(),
                first / 1000.0 / recent.size());
        if (speedSamples > 0) out += String.format(Locale.ROOT, " · %.1f palabras/s", speed / speedSamples);
        if (tokenSamples > 0) out += " · " + (tokens / tokenSamples) + " tokens por mensaje";
        return out.replace('.', ',');
    }

    static synchronized void clear() { recent.clear(); }

    /** Palabras del texto público (el sobre JSON no cuenta: «"message":» no es una palabra dicha). */
    static int countWords(String text) {
        if (text == null) return 0;
        java.util.regex.Matcher message = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)").matcher(text);
        String plain = (message.find() ? message.group(1) : text).replaceAll("[{}\\[\\]\":,]", " ");
        int count = 0;
        for (String w : plain.trim().split("\\s+")) if (w.matches(".*\\p{L}.*|.*\\d.*")) count++;
        return count;
    }
}
