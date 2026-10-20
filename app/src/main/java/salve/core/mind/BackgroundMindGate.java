package salve.core.mind;

import java.util.EnumMap;
import java.util.Map;

/**
 * In-process admission controller for existing WorkManager background jobs.
 *
 * Jobs keep their own Android battery/network constraints and cooldowns. This
 * gate stops overlapping cognitive work and always yields to the foreground.
 * It does not unlock external actions, spawn jobs, or change model weights.
 */
public final class BackgroundMindGate {
    /**
     * CONTINUITY: copia sellada de su memoria. Pasa por la misma puerta para no fotografiar la
     * memoria a mitad de un sueño (base de datos y mente quedarían descuadradas).
     */
    public enum Task { REFLECTION_AND_GOALS, CURIOSITY, DREAM, PLAY, CONTINUITY }
    public enum Outcome { COMPLETED_CYCLE, INTERRUPTED_OR_FAILED }

    public static final class Lease implements AutoCloseable {
        private final BackgroundMindGate gate;
        private final Task task;
        private final long sequence;
        private boolean closed;
        private Lease(BackgroundMindGate gate, Task task, long sequence) {
            this.gate = gate; this.task = task; this.sequence = sequence;
        }
        public synchronized void complete() {
            if (!closed) {
                closed = true;
                gate.release(task, sequence, Outcome.COMPLETED_CYCLE);
            }
        }
        @Override public synchronized void close() {
            if (!closed) {
                closed = true;
                gate.release(task, sequence, Outcome.INTERRUPTED_OR_FAILED);
            }
        }
    }

    public static final class Snapshot {
        public final Task active;
        public final Map<Task, Integer> completions;
        public final Map<Task, Outcome> lastOutcomes;
        private Snapshot(Task active, Map<Task, Integer> completions, Map<Task, Outcome> lastOutcomes) {
            this.active = active;
            this.completions = completions;
            this.lastOutcomes = lastOutcomes;
        }
    }

    private static final BackgroundMindGate INSTANCE = new BackgroundMindGate();
    public static BackgroundMindGate get() { return INSTANCE; }

    private Task active;
    private long sequence;
    private final EnumMap<Task, Integer> completed = new EnumMap<>(Task.class);
    private final EnumMap<Task, Outcome> outcomes = new EnumMap<>(Task.class);

    /** Returns null whenever the phone is busy, paused, loading or another job owns the gate. */
    public synchronized Lease acquire(Task task, boolean userConversing, boolean mindPaused,
                                      boolean modelLoading) {
        if (task == null || userConversing || mindPaused || modelLoading || active != null)
            return null;
        active = task;
        sequence++;
        return new Lease(this, task, sequence);
    }

    private synchronized void release(Task task, long token, Outcome outcome) {
        if (active != task || sequence != token) return;
        active = null;
        outcomes.put(task, outcome);
        if (outcome == Outcome.COMPLETED_CYCLE)
            completed.put(task, completed.containsKey(task) ? completed.get(task) + 1 : 1);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(active, new EnumMap<>(completed), new EnumMap<>(outcomes));
    }
}
