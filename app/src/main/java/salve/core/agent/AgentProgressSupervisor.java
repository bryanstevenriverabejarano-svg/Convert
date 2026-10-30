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

    /**
     * Uses the durable lease as an additional observation; expired execution
     * is not proof of failure and must NEVER trigger another call from here.
     */
    public static Finding inspectAt(AgentRun run, long now) {
        Finding original = inspect(run);
        if (run != null && "RUNNING".equals(run.status) && run.owner != null
                && !run.owner.isEmpty() && run.leaseUntil > 0 && now >= run.leaseUntil) {
            return new Finding(Phase.STALLED,
                    "Concesión de ejecución vencida; comprobar el almacenamiento y la cola existente antes de recuperar.",
                    original.completedObservations, original.errorObservations);
        }
        return original;
    }

    /** Metadata only: never serialize requests, source URLs or personal goal notes. */
    public static final class Overview {
        public final int plansObserved, plansPending, plansExecuting, plansAttention;
        public final int plansVerified, plansUnverified, plansStopped;
        public final int researchObserved, researchPending, researchExecuting;
        public final int researchArchived, researchAttention;
        public final int goalsConfigured, goalsPaused, goalsAwaitingReview, goalsReadyForProposal;
        public final boolean goalsUnavailable, autonomyPaused;

        private Overview(int plans, int pending, int executing, int attention,
                         int verified, int unverified, int stopped,
                         int research, int researchPending, int researchExecuting,
                         int researchArchived, int researchAttention,
                         int configured, int goalsPaused, int awaiting, int ready,
                         boolean unavailable, boolean paused) {
            plansObserved = plans; plansPending = pending; plansExecuting = executing;
            plansAttention = attention; plansVerified = verified;
            plansUnverified = unverified; plansStopped = stopped;
            researchObserved = research; this.researchPending = researchPending;
            this.researchExecuting = researchExecuting; this.researchArchived = researchArchived;
            this.researchAttention = researchAttention;
            goalsConfigured = configured; this.goalsPaused = goalsPaused;
            goalsAwaitingReview = awaiting; goalsReadyForProposal = ready;
            goalsUnavailable = unavailable; autonomyPaused = paused;
        }

        public String summary() {
            return "Supervisión conjunta de registros recientes, solo lectura: "
                    + (autonomyPaused ? "ejecución global pausada" : "ejecución global no pausada")
                    + ". Planes (" + plansObserved + "): pendientes " + plansPending
                    + ", ejecutando " + plansExecuting + ", requieren atención " + plansAttention
                    + ", verificados " + plansVerified + ", sin comprobación independiente "
                    + plansUnverified + ", detenidos " + plansStopped
                    + ". Investigación (" + researchObserved + "): pendiente " + researchPending
                    + ", ejecutando " + researchExecuting + ", archivada " + researchArchived
                    + ", necesita revisión " + researchAttention
                    + ". Objetivos de reflexión: "
                    + (goalsUnavailable ? "diario no disponible"
                        : goalsConfigured + " configurados, " + goalsPaused + " pausados, "
                                + goalsAwaitingReview + " propuestas pendientes de revisión, "
                                + goalsReadyForProposal + " elegibles para preparar una propuesta")
                    + ". Archivar una investigación o aceptar una reflexión NO verifica el objetivo "
                    + "ni concede permisos. La supervisión no ejecuta ni reanuda tareas.";
        }
    }

    /**
     * Extends THIS supervisor over existing immutable-looking snapshots from
     * AgentEngine/Room, ResearchTaskEngine/Room and GoalAutonomy/GoalFileStore.
     * The runtimes retain all claim, pause, authorization, reschedule and
     * recovery decisions. Results cover only the bounded snapshots supplied.
     */
    public static Overview inspectAll(java.util.List<AgentRun> plans,
            java.util.List<salve.core.tasks.ResearchTask> research,
            salve.core.goals.GoalAutonomy.Overview goals, boolean paused, long now) {
        if (plans == null || research == null || goals == null || now < 0)
            throw new IllegalArgumentException("Los registros de supervisión deben estar disponibles");
        if (plans.size() > 50 || research.size() > 50)
            throw new IllegalArgumentException("Supervisión limitada a registros recientes");
        int pending = 0, executing = 0, attention = 0, verified = 0, unverified = 0, stopped = 0;
        for (AgentRun plan : plans) {
            Finding finding = inspectAt(plan, now);
            switch (finding.phase) {
                case PENDING: pending++; break;
                case EXECUTING: executing++; break;
                case STALLED: case NEEDS_RESOURCES: case PARTIAL: attention++; break;
                case VERIFIED: verified++; break;
                case UNVERIFIED: unverified++; break;
                default: stopped++; break;
            }
        }
        int researchPending = 0, researchExecuting = 0, archived = 0, researchAttention = 0;
        for (salve.core.tasks.ResearchTask task : research) {
            if (task == null || task.status == null) { researchAttention++; continue; }
            switch (task.status) {
                case "QUEUED": researchPending++; break;
                case "RUNNING":
                    if (task.owner != null && !task.owner.isEmpty()
                            && task.leaseUntil > 0 && now >= task.leaseUntil)
                        researchAttention++;
                    else researchExecuting++;
                    break;
                case "STAGED": researchPending++; break; // Receipt exists; final Room commit pending
                case "SUCCEEDED": archived++; break; // Archived is NOT independently true
                case "PARTIAL": case "FAILED": researchAttention++; break;
                case "CANCELLED": break;
                default: researchAttention++; break;
            }
        }
        return new Overview(plans.size(), pending, executing, attention, verified, unverified, stopped,
                research.size(), researchPending, researchExecuting, archived, researchAttention,
                goals.configured, goals.pausedGoals, goals.pendingReviews, goals.eligibleForPreparation,
                goals.unavailable, paused);
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
                    "NEEDS_REVIEW".equals(run.replanState)
                        ? "Se agotó el presupuesto de replanificación. Solo el dueño puede autorizar una reanudación; conserva los recibos originales."
                        : "El bloqueo debe resolverse sin inventar éxitos. Consultar recursos o pedir una acción explícita.", useful, errors);
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
