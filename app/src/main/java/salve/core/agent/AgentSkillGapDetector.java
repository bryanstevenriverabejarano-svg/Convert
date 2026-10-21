package salve.core.agent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import salve.core.autonomy.InductiveSkillProposal;

/**
 * Controller-owned opportunity detector. Only original, user-authored goals
 * plus real failed receipts can suggest a bounded local skill; untrusted page
 * contents, model opinions and status text alone never confer permissions.
 */
public final class AgentSkillGapDetector {
    public static final long WINDOW_MS = 7L * 24 * 60 * 60_000L;
    private AgentSkillGapDetector() { }

    /** Requires two DISTINCT recent failed runs in the same recognized family. */
    public static InductiveSkillProposal.Kind detect(List<AgentRun> runs, long now) {
        if (runs == null || now <= 0) return null;
        for (InductiveSkillProposal.Kind kind : InductiveSkillProposal.Kind.values()) {
            Set<String> ids = new HashSet<>();
            for (AgentRun run : runs) {
                if (run == null || run.id == null || run.goal == null || run.codeAllowed
                        || !run.terminal() || run.updatedAt > now
                        || now - run.updatedAt > WINDOW_MS
                        || !(run.status.equals("BLOCKED") || run.status.equals("PARTIAL"))
                        || "VERIFIED".equals(run.goalVerification)
                        || !kind.equals(InductiveSkillProposal.recognize(run.goal))
                        || !hasFailedToolReceipt(run)) continue;
                ids.add(run.id);
                if (ids.size() >= 2) return kind;
            }
        }
        return null;
    }

    private static boolean hasFailedToolReceipt(AgentRun run) {
        if (run.observations == null) return false;
        for (AgentRun.Observation observed : run.observations) {
            if (observed != null && ("ERROR".equals(observed.status)
                    || "BLOCKED".equals(observed.status))) return true;
        }
        return false;
    }
}
