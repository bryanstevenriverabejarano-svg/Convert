package salve.core;

import java.util.regex.Pattern;

/**
 * Quita el «pensamiento en voz alta» de modelos como Qwen 3 / 3.5 (bloques {@code <think>…</think>}):
 * Salve dice y guarda solo la respuesta. El motor ya les pide contestar sin pensar; esto es la red
 * por si alguno piensa igualmente. Lógica pura para pruebas JVM.
 */
public final class ThinkingFilter {
    private static final Pattern CLOSED = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern UNCLOSED = Pattern.compile("(?s)<think>.*$");
    private static final Pattern ORPHAN_CLOSE = Pattern.compile("(?s)^.*?</think>");

    private ThinkingFilter() { }

    public static String strip(String text) {
        if (text == null || (!text.contains("<think>") && !text.contains("</think>"))) return text;
        String out = CLOSED.matcher(text).replaceAll("");
        // Pensamiento cortado por el límite de tokens: no hay respuesta que salvar detrás.
        out = UNCLOSED.matcher(out).replaceAll("");
        // Plantillas que abren <think> en el propio prompt: el texto empieza ya pensando.
        if (out.contains("</think>")) out = ORPHAN_CLOSE.matcher(out).replaceFirst("");
        return out.trim();
    }
}
