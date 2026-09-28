package salve.core.tasks;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Recoverable read -> evidence checkpoint -> atomic memory/outbox commit. */
public final class ResearchTaskEngine {
    public static final int MAX_ATTEMPTS = 3;
    public static final long LEASE_MS = 15 * 60_000L;
    public interface Store {
        ResearchTask get(String id);
        ResearchTask claim(String id, String owner, long now);
        boolean owns(String id, String owner);
        boolean stage(String id, String owner, String receipt, long now);
        boolean finish(String id, String owner, long now);
        void release(String id, String owner, String error, long now, boolean retryable);
    }
    public interface Researcher {
        ResearchReceipt research(String question, BooleanSupplier stopped) throws Exception;
    }
    public static final class Unavailable extends Exception {
        public final boolean retryable;
        public Unavailable(boolean retryable) { this.retryable = retryable; }
    }
    private final Store store;
    private final Researcher researcher;
    private final LongSupplier clock;
    public ResearchTaskEngine(Store store, Researcher researcher, LongSupplier clock) {
        this.store = store; this.researcher = researcher; this.clock = clock;
    }
    public ResearchTask run(String id, BooleanSupplier interrupted) {
        String owner = UUID.randomUUID().toString();
        ResearchTask task = store.claim(id, owner, clock.getAsLong());
        if (task == null) return store.get(id);
        BooleanSupplier stopped = () -> interrupted.getAsBoolean() || !store.owns(id, owner);
        try {
            if (stopped.getAsBoolean()) throw new CancellationException();
            if (task.receipt == null) {
                ResearchReceipt receipt = researcher.research(task.question, stopped);
                if (stopped.getAsBoolean()) throw new CancellationException();
                if (!store.stage(id, owner, receipt.encode(), clock.getAsLong())) return store.get(id);
            } else {
                // Validate a recovered checkpoint before the store can mutate personal memory.
                ResearchReceipt.decode(task.receipt);
            }
            if (stopped.getAsBoolean()) throw new CancellationException();
            store.finish(id, owner, clock.getAsLong());
        } catch (CancellationException stoppedRun) {
            store.release(id, owner, "Trabajo interrumpido; conserva su punto de recuperación.", clock.getAsLong(), true);
        } catch (Unavailable unavailable) {
            store.release(id, owner, unavailable.retryable
                    ? "La lectura pública falló; se reintentará dentro del presupuesto."
                    : "No se recuperaron fuentes utilizables para completar la tarea.", clock.getAsLong(), unavailable.retryable);
        } catch (IllegalArgumentException invalid) {
            store.release(id, owner, "El resultado no cumple el contrato de evidencia.", clock.getAsLong(), false);
        } catch (Exception failure) {
            // Never persist exception text: it may contain query data, URLs or credentials.
            store.release(id, owner, "No se pudo completar el paso; el estado anterior se conserva.", clock.getAsLong(), true);
        }
        return store.get(id);
    }
}
