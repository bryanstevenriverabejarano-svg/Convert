package salve.core.agent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks the product delivered to the user, not merely whether a tool ran.
 * Only literal bounded arithmetic goals qualify for this narrow contract.
 */
final class AgentArithmeticVerifier {
    private AgentArithmeticVerifier() { }

    /** null means unsupported goal, not a verifier failure. */
    static AgentGoalVerifier.Result evaluate(AgentRun run) {
        if (run == null || run.codeAllowed) return null;
        AgentArithmeticTask task = AgentArithmeticTask.parse(run.goal);
        if (task == null) return null;
        if (run.observations == null || run.evidence == null)
            return verdict(AgentGoalVerifier.Verdict.NOT_VERIFIED, "");

        String expectedCall = AgentExperience.fingerprint("math.compute", run.goal);
        String expectedReceipt = "RESULTADO_ARITMETICO=" + task.referenceResult();
        Set<String> seen = new HashSet<>();
        boolean matched = false;
        String evidenceId = "";
        for (AgentRun.Observation observation : run.observations) {
            if (observation == null || observation.id == null || !seen.add(observation.id)
                    || !"math.compute".equals(observation.tool)
                    || !expectedCall.equals(observation.fingerprint)
                    || !run.evidence.contains(observation.id)) continue;
            if (!"SUCCESS".equals(observation.status)
                    || !expectedReceipt.equals(observation.text)) continue;
            matched = true;
            evidenceId = observation.id;
            break;
        }
        if (!matched) return verdict(AgentGoalVerifier.Verdict.NOT_VERIFIED, "");
        if (run.answer == null || run.answer.trim().isEmpty())
            return verdict(AgentGoalVerifier.Verdict.PARTIAL, "");
        if (!task.matchesAnswer(run.answer))
            return verdict(AgentGoalVerifier.Verdict.NOT_VERIFIED, "");
        return verdict(AgentGoalVerifier.Verdict.VERIFIED, evidenceId);
    }

    private static AgentGoalVerifier.Result verdict(AgentGoalVerifier.Verdict state, String receiptId) {
        return new AgentGoalVerifier.Result(state, "ARITHMETIC_EXACT", receiptId,
                receiptId.isEmpty() ? Collections.emptyList()
                        : Collections.singletonList(receiptId));
    }
}
