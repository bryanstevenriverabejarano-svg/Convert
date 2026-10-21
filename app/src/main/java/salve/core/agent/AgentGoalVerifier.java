package salve.core.agent;

/** A controller-owned check, independent from generated explanations. */
public final class AgentGoalVerifier {
    public enum Verdict { VERIFIED, PARTIAL, NOT_VERIFIED, UNKNOWN }
    private AgentGoalVerifier() { }
    public static Verdict objectiveStatus(AgentRun run) {
        if (run == null || run.goal == null) return Verdict.UNKNOWN;
        return Verdict.UNKNOWN;
    }
}
