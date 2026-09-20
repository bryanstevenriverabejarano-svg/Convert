package salve.core.conversation;

/** Limita respuestas conservando, cuando es posible, frases completas. */
public final class ResponseLimiter {
    private ResponseLimiter() {}

    public static String limit(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) return text;
        if (maxChars < 4) return text.substring(0, Math.max(0, maxChars));

        int searchFrom = Math.max(0, maxChars / 2);
        int boundary = -1;
        for (int i = maxChars - 1; i >= searchFrom; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                boundary = i + 1;
                break;
            }
        }
        if (boundary < 0) {
            boundary = text.lastIndexOf(' ', maxChars - 1);
        }
        if (boundary <= 0) boundary = maxChars - 1;
        return text.substring(0, boundary).trim() + "…";
    }
}
