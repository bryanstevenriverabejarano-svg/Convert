package salve.core.memory;

/** Solicitud de borrado pendiente de confirmación humana. */
public final class PendingMemoryDeletion {
    private final MemoryForgetRequest request;
    private final long requestedAtMillis;

    public PendingMemoryDeletion(MemoryForgetRequest request, long requestedAtMillis) {
        if (request == null) throw new IllegalArgumentException("request no puede ser null");
        this.request = request;
        this.requestedAtMillis = requestedAtMillis;
    }

    public MemoryForgetRequest getRequest() {
        return request;
    }

    public boolean isExpired(long nowMillis, long ttlMillis) {
        return ttlMillis < 0 || nowMillis - requestedAtMillis > ttlMillis;
    }
}
