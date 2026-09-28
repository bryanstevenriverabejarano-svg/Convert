package salve.core.tasks;

/** Durable work state. A model can supply observations, never this control record. */
public final class ResearchTask {
    public String id, question, status, owner, receipt, error;
    public int attempts;
    public long createdAt, updatedAt, leaseUntil, nextAttemptAt;

    public boolean terminal() {
        return "SUCCEEDED".equals(status) || "PARTIAL".equals(status)
                || "FAILED".equals(status) || "CANCELLED".equals(status);
    }
}
