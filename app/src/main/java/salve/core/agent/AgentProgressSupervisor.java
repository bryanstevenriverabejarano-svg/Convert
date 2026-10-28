package salve.core.agent;

/**
 * Deterministic, read-only oversight on top of AgentRun checkpoints.
 * It never requeues, rewrites a goal, grants permissions or accepts a model
 * self-review as independent evidence. The store/AgentEngine remain the
 * authority on leases, idempotent actions and final verdicts.
 */
public final class AgentProgressSupervisor {
    private AgentProgressSupervisor() { }

    public enum Phase {
        PENDING, EXECUTING, NEEDS_RESOURCES, STALLED, PARTIAL, VERIFIED,
        UNVERIFIED, STOPPED
    }

    public static final class Finding {
        public final Phase phase;
        public final String advice;
        public final int completedObservations;
        public final int errorObservations;

        private Finding(Phase phase, String advice, int complete, int errors) {
            this.phase = phase; this.advice = advice;
            completedObservations = complete; errorObservations = errors;
        }

        public String summary() {
            return "Supervisor: " + phase + "; recibos útiles " + completedObservations
                    + ", fallos " + errorObservations + ". " + advice;
        }
    }

    public static Finding inspect(AgentRun run) {
        if (run == null || run.status == null)
            return new Finding(Phase.STOPPED, "No hay plan comprobable.", 0, 0);
        int useful = 0, errors = 0;
        if (run.observations != null) for (AgentRun.Observation o : run.observations) {
            if (o == null || o.status == null) continue;
            if (o.useful()) useful++;
            else if ("ERROR".equals(o.status) || "BLOCKED".equals(o.status)) errors++;
        }
        if ("CANCELLED".equals(run.status) || "CANCELED".equals(run.status))
            return new Finding(Phase.STOPPED, "Plan cancelado; ninguna tarea debe retomarse automáticamente.", useful, errors);
        if ("QUEUED".equals(run.status))
            return new Finding(Phase.PENDING, "Trabajo pendiente: Android puede aplazarlo; conservar el objetivo original.", useful, errors);
        if ("RUNNING".equals(run.status))
            return new Finding(run.skippedRepeats >= 2 ? Phase.STALLED : Phase.EXECUTING,
                    run.skippedRepeats >= 2
                            ? "Hay intentos repetidos: revisar recibos y estrategia antes de volver a ejecutar."
                            : "Ejecutándose con puntos de control e idempotencia.", useful, errors);
        if ("BLOCKED".equals(run.status))
            return new Finding(Phase.NEEDS_RESOURCES,
                    "El bloqueo debe resolverse sin inventar éxitos. Consultar recursos o pedir una acción explícita.", useful, errors);
        if ("PARTIAL".equals(run.status))
            return new Finding(Phase.PARTIAL,
                    "Mostrar qué recibos apoyan la respuesta y qué objetivo sigue sin verificar.", useful, errors);
        if ("SUCCEEDED".equals(run.status)) {
            if ("VERIFIED".equals(run.goalVerification))
                return new Finding(Phase.VERIFIED, "Se conserva el resultado con comprobación independiente.", useful, errors);
            return new Finding(Phase.UNVERIFIED,
                    "La revisión del modelo no constituye verificación externa del objetivo.", useful, errors);
        }
        return new Finding(Phase.STOPPED,
                "Estado final sin resolución autónoma confirmada.", useful, errors);
    }
}
