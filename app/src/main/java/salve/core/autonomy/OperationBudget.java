package salve.core.autonomy;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Deterministic work limit plus cancellation; no generated program can extend its budget. */
public final class OperationBudget {
    public static final class Exhausted extends RuntimeException {
        public Exhausted() { super("Presupuesto de operaciones agotado"); }
    }
    private final int maximum;
    private final BooleanSupplier stopped;
    private int used;
    public OperationBudget(int maximum) { this(maximum, () -> false); }
    public OperationBudget(int maximum, BooleanSupplier stopped) {
        if (maximum < 1 || maximum > 20_000_000 || stopped == null) throw new IllegalArgumentException();
        this.maximum = maximum;
        this.stopped = stopped;
    }
    public void tick() {
        if (Thread.currentThread().isInterrupted() || stopped.getAsBoolean())
            throw new CancellationException("Trabajo cancelado");
        if (used >= maximum) throw new Exhausted();
        used++;
    }
    public int getUsed() { return used; }
}
