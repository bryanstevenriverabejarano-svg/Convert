package salve.core;

/** Resultado tipado de una inferencia; evita tratar mensajes de error como respuestas. */
public final class ModelResult {
    public enum Status { SUCCESS, UNAVAILABLE, TIMEOUT, CANCELLED, ERROR }

    private final Status status;
    private final String text;
    private final String error;
    private final long latencyMillis;
    private final String provider;

    private ModelResult(Status status, String text, String error, long latencyMillis) {
        this(status, text, error, latencyMillis, null);
    }

    private ModelResult(Status status, String text, String error, long latencyMillis, String provider) {
        this.provider = provider;
        this.status = status;
        this.text = text;
        this.error = error;
        this.latencyMillis = Math.max(0L, latencyMillis);
    }

    public static ModelResult success(String text, long latencyMillis) {
        return new ModelResult(Status.SUCCESS, text == null ? "" : text, null, latencyMillis);
    }

    public static ModelResult failure(Status status, String error, long latencyMillis) {
        if (status == Status.SUCCESS) throw new IllegalArgumentException("SUCCESS requiere texto");
        return new ModelResult(status, null, error, latencyMillis);
    }

    public ModelResult withProvider(String name) { return new ModelResult(status, text, error, latencyMillis, name); }
    public String getProvider() { return provider; }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public Status getStatus() { return status; }
    public String getText() { return text; }
    public String getError() { return error; }
    public long getLatencyMillis() { return latencyMillis; }
}
