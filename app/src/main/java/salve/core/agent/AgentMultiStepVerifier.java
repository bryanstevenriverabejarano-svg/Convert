package salve.core.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded multi-step verifier for an explicit two-page reading task.
 * No model can choose the required URLs or change the success criteria.
 *
 * This verifies the two reads, NOT the truth of the pages or quality of a
 * comparison/synthesis. Such open-ended goals remain UNKNOWN.
 */
final class AgentMultiStepVerifier {
    private static final Pattern TWO_READS = Pattern.compile(
            "(?iu)^\\s*(?:lee|abre|consulta)\\s+"
                    + "(https://[^\\s<>\\[\\]{}]+)\\s+y\\s+"
                    + "(https://[^\\s<>\\[\\]{}]+)\\s*$");

    private AgentMultiStepVerifier() { }

    /** Null: objective is outside this limited, independently testable contract. */
    static AgentGoalVerifier.Result evaluate(AgentRun run) {
        if (run == null || run.goal == null || run.codeAllowed) return null;
        Matcher match = TWO_READS.matcher(run.goal);
        if (!match.matches()) return null;
        String first = match.group(1), second = match.group(2);
        if (first.equals(second)
                || AgentPolicy.denial(run, "web.read", first) != null
                || AgentPolicy.denial(run, "web.read", second) != null)
            return null;

        String[] expected = {first, second};
        Map<String, AgentRun.Observation> success = new HashMap<>();
        Set<String> seenIds = new HashSet<>();
        boolean anyUseful = false;
        if (run.observations != null) {
            for (AgentRun.Observation observation : run.observations) {
                if (observation == null || observation.id == null
                        || !seenIds.add(observation.id)
                        || !"web.read".equals(observation.tool)) continue;
                for (String url : expected) {
                    if (!AgentExperience.fingerprint("web.read", url).equals(observation.fingerprint)
                            || observation.sources == null || !observation.sources.contains(url)
                            || observation.text == null || observation.text.trim().isEmpty()
                            || run.evidence == null || !run.evidence.contains(observation.id)) continue;
                    if ("SUCCESS".equals(observation.status)) {
                        success.putIfAbsent(url, observation);
                        anyUseful = true;
                    } else if ("PARTIAL".equals(observation.status)) {
                        anyUseful = true;
                    }
                }
            }
        }
        if (success.size() == 2 && run.answer != null && !run.answer.trim().isEmpty()) {
            List<String> ids = new ArrayList<>(2);
            ids.add(success.get(first).id);
            ids.add(success.get(second).id);
            if (!ids.get(0).equals(ids.get(1)))
                return new AgentGoalVerifier.Result(AgentGoalVerifier.Verdict.VERIFIED,
                        "PUBLIC_READ_SET", ids.get(0), ids);
        }
        return new AgentGoalVerifier.Result(anyUseful ? AgentGoalVerifier.Verdict.PARTIAL
                : AgentGoalVerifier.Verdict.NOT_VERIFIED,
                "PUBLIC_READ_SET", "", Collections.emptyList());
    }
}
