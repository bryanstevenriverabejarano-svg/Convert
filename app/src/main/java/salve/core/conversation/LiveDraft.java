package salve.core.conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Texto que Salve está escribiendo ahora mismo (por flujo, palabra a palabra).
 *
 * <p>Sirve a dos consumidores a la vez: la pantalla, que muestra el borrador limpio, y la voz,
 * que recibe frases completas para leerlas mientras el modelo sigue escribiendo las siguientes.
 * Así una parte de Salve habla mientras la otra piensa la frase siguiente.</p>
 *
 * <p>La limpieza (entradilla, eco, fuga de diálogo) solo toca el principio de cada parte, por
 * eso la voz retiene el arranque hasta tener texto suficiente para limpiarlo antes de leerlo.</p>
 */
public final class LiveDraft {
    /** La voz no empieza hasta tener esto escrito: lo bastante para quitar «Claro que sí. Aquí tienes…». */
    public static final int HOLD_CHARS = 240;
    /** Fragmento máximo para el sintetizador (Android limita la longitud por enunciado). */
    static final int MAX_SPEECH_CHARS = 1500;
    private static final Pattern BOUNDARY = Pattern.compile("[.!?…]+[»\"”')]*(?=\\s)|\\n");

    private final List<StringBuilder> parts = new ArrayList<>();
    private final UnaryOperator<String> cleaner;
    private final int maxChars;
    private String spoken = "";
    private int received;

    public LiveDraft(UnaryOperator<String> cleaner, int maxChars) {
        this.cleaner = cleaner == null ? UnaryOperator.identity() : cleaner;
        this.maxChars = maxChars;
        parts.add(new StringBuilder());
    }

    public synchronized void append(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        parts.get(parts.size() - 1).append(chunk);
        received += chunk.length();
    }

    /** Empieza otra parte (capítulo): se limpia por separado. */
    public synchronized void nextPart() {
        if (parts.get(parts.size() - 1).length() > 0) parts.add(new StringBuilder());
    }

    /** Caracteres recibidos en total (para saber si un motor no emitió flujo). */
    public synchronized int received() { return received; }

    /** Borrador limpio para la pantalla. */
    public synchronized String visible() {
        StringBuilder out = new StringBuilder();
        for (StringBuilder part : parts) {
            String clean = cleaner.apply(part.toString());
            if (clean == null || clean.trim().isEmpty()) continue;
            if (out.length() > 0) out.append("\n\n");
            out.append(clean.trim());
        }
        String text = out.toString();
        return text.length() > maxChars ? ResponseLimiter.limit(text, maxChars) : text;
    }

    /**
     * Siguiente fragmento para la voz: frases ya completas que aún no se leyeron, o null. Con
     * {@code finished} entrega también la cola sin puntuación final.
     */
    public synchronized String nextSpeech(boolean finished) {
        String clean = visible();
        int totalRaw = 0;
        for (StringBuilder part : parts) totalRaw += part.length();
        if (!finished && totalRaw < HOLD_CHARS) return null;
        // Si la limpieza cambió algo ya leído (raro), se continúa desde la misma posición.
        int from = clean.startsWith(spoken) ? spoken.length() : Math.min(spoken.length(), clean.length());
        int end = finished ? clean.length() : lastBoundary(clean, from);
        if (end <= from) return null;
        if (end - from > MAX_SPEECH_CHARS) {
            int cut = lastBoundary(clean.substring(0, from + MAX_SPEECH_CHARS), from);
            end = cut > from ? cut : from + MAX_SPEECH_CHARS;
        }
        String chunk = clean.substring(from, end).trim();
        spoken = clean.substring(0, end);
        return chunk.isEmpty() ? nextSpeechOrNull(finished) : chunk;
    }

    private String nextSpeechOrNull(boolean finished) {
        return finished && spoken.length() < visible().length() ? nextSpeech(true) : null;
    }

    /** Todo lo que falta por leer, en fragmentos aptos para el sintetizador. */
    public synchronized List<String> drainSpeech() {
        List<String> out = new ArrayList<>();
        for (int guard = 0; guard < 32; guard++) {
            String next = nextSpeech(true);
            if (next == null) break;
            out.add(next);
        }
        return out;
    }

    private static int lastBoundary(String text, int from) {
        Matcher m = BOUNDARY.matcher(text);
        int end = -1;
        while (m.find()) if (m.end() > from) end = m.end();
        return end;
    }
}
