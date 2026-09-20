package salve.core.tools;

/** Acción de herramienta preparada por el modelo y pendiente de aprobación humana. */
public final class PendingToolAction {
    public enum Type { TAP, TAP_NODE, WRITE_TEXT, DEPLOY_WEB }

    private static final int MAX_TEXT_CHARS = 2_000;
    private static final int MAX_HTML_CHARS = 100_000;

    private final Type type;
    private final int firstNumber;
    private final int secondNumber;
    private final String payload;
    private final long createdAtMillis;

    private PendingToolAction(Type type, int firstNumber, int secondNumber,
                              String payload, long createdAtMillis) {
        this.type = type;
        this.firstNumber = firstNumber;
        this.secondNumber = secondNumber;
        this.payload = payload == null ? "" : payload;
        this.createdAtMillis = createdAtMillis;
    }

    public static PendingToolAction tap(int x, int y, long now) {
        return new PendingToolAction(Type.TAP, clamp(x), clamp(y), "", now);
    }

    public static PendingToolAction tapNode(int nodeId, long now) {
        return new PendingToolAction(Type.TAP_NODE, Math.max(0, nodeId), 0, "", now);
    }

    public static PendingToolAction writeText(String text, long now) {
        return new PendingToolAction(Type.WRITE_TEXT, 0, 0, limit(text, MAX_TEXT_CHARS), now);
    }

    public static PendingToolAction deployWeb(String html, long now) {
        return new PendingToolAction(Type.DEPLOY_WEB, 0, 0, limit(html, MAX_HTML_CHARS), now);
    }

    private static int clamp(int value) { return Math.max(0, Math.min(10_000, value)); }

    private static String limit(String value, int maxChars) {
        if (value == null) return "";
        return value.length() <= maxChars ? value : value.substring(0, maxChars);
    }

    public boolean isExpired(long now, long ttlMillis) {
        return ttlMillis < 0 || now - createdAtMillis > ttlMillis;
    }

    public String describe() {
        switch (type) {
            case TAP: return "tocar la pantalla en X:" + firstNumber + " Y:" + secondNumber;
            case TAP_NODE: return "tocar el elemento de pantalla " + firstNumber;
            case WRITE_TEXT: return "escribir " + payload.length() + " caracteres en la aplicación actual";
            case DEPLOY_WEB: return "publicar una página web de " + payload.length() + " caracteres";
            default: return "usar una herramienta";
        }
    }

    public Type getType() { return type; }
    public int getFirstNumber() { return firstNumber; }
    public int getSecondNumber() { return secondNumber; }
    public String getPayload() { return payload; }
    public long getCreatedAtMillis() { return createdAtMillis; }
}
