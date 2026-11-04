package salve.core;

import java.util.regex.Pattern;

/**
 * Quita el «pensamiento en voz alta» de los modelos que lo hacen: bloques {@code <think>…</think>}
 * (Qwen 3 / 3.5) y canales {@code <|channel>…<channel|>} (Gemma 4). Salve dice y guarda solo la
 * respuesta. El motor ya les pide contestar sin pensar; esto es la red por si alguno piensa igualmente.
 * Lógica pura para pruebas JVM.
 */
public final class ThinkingFilter {
    private static final Pattern[] CLOSED = {
            Pattern.compile("(?s)<think>.*?</think>"), Pattern.compile("(?s)<\\|channel>.*?<channel\\|>")};
    private static final Pattern[] UNCLOSED = {
            Pattern.compile("(?s)<think>.*$"), Pattern.compile("(?s)<\\|channel>.*$")};
    private static final String[][] ORPHAN = {{"</think>", "(?s)^.*?</think>"}, {"<channel|>", "(?s)^.*?<channel\\|>"}};
    /** Marcas de turno de Gemma 4 que no deben leerse en voz alta si se cuelan. */
    private static final Pattern TURN = Pattern.compile("<\\|turn>(?:model|user|system)?\\n?|<turn\\|>");

    private ThinkingFilter() { }

    public static String strip(String text) {
        if (text == null) return null;
        boolean marked = text.contains("think>") || text.contains("channel") || text.contains("turn");
        if (!marked) return text;
        String out = text;
        for (Pattern p : CLOSED) out = p.matcher(out).replaceAll("");
        // Pensamiento cortado por el límite de tokens: no hay respuesta que salvar detrás.
        for (Pattern p : UNCLOSED) out = p.matcher(out).replaceAll("");
        // Plantillas que abren el pensamiento en el propio prompt: el texto empieza ya pensando.
        for (String[] o : ORPHAN) if (out.contains(o[0])) out = out.replaceFirst(o[1], "");
        out = TURN.matcher(out).replaceAll("");
        return out.equals(text) ? text : out.trim();
    }
}
