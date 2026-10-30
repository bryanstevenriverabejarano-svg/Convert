package salve.core.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Evidence-driven autonomous next-step choice for an already authorized plan.
 *
 * This is a controller-owned description of the situation, not a grant of new
 * capabilities. The agent still chooses an action and AgentPolicy independently
 * checks every proposal. Neither model assertions nor raw web/tool text become
 * success evidence here.
 */
public final class AgentChoiceFrame {
    public enum Next {
        GATHER_EVIDENCE,
        VERIFY_AND_SYNTHESIZE,
        CHANGE_APPROACH,
        REPORT_LIMITS
    }

    private final Next next;
    private final int useful, failed, remainingTools, remainingInferences;
    private final JsonArray historicalWeakTools;
    private final int replanAttempts;
    private final boolean needsReview;

    private AgentChoiceFrame(Next next, int useful, int failed,
                             int remainingTools, int remainingInferences,
                             JsonArray historicalWeakTools, int replans, boolean needsReview) {
        this.next = next;
        this.useful = useful;
        this.failed = failed;
        this.remainingTools = remainingTools;
        this.remainingInferences = remainingInferences;
        this.historicalWeakTools = historicalWeakTools;
        this.replanAttempts = replans;
        this.needsReview = needsReview;
    }

    public static AgentChoiceFrame observe(AgentRun run) {
        if (run == null || run.observations == null) throw new IllegalArgumentException("Plan ausente");
        int useful = 0, failures = 0;
        for (AgentRun.Observation o : run.observations) {
            if (o == null) continue;
            if ("SUCCESS".equals(o.status) || "PARTIAL".equals(o.status)) useful++;
            else if ("ERROR".equals(o.status) || "BLOCKED".equals(o.status)) failures++;
        }
        int tools = Math.max(0, AgentEngine.MAX_STEPS - run.observations.size());
        int calls = Math.max(0, AgentEngine.MAX_MODEL_CALLS - run.modelCalls);
        Next next;
        if (tools == 0 || calls <= 1 || "NEEDS_REVIEW".equals(run.replanState))
            next = Next.REPORT_LIMITS;
        else if (run.observations.isEmpty()) next = Next.GATHER_EVIDENCE;
        else if (useful > 0) next = Next.VERIFY_AND_SYNTHESIZE;
        else next = failures > 0 ? Next.CHANGE_APPROACH : Next.GATHER_EVIDENCE;
        return new AgentChoiceFrame(next, useful, failures, tools, calls,
                historicalFailures(run.experience), run.replanAttempts,
                "NEEDS_REVIEW".equals(run.replanState));
    }

    public Next next() { return next; }

    /** Only tool identifiers and counters; never raw goals, memories, or model thoughts. */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("next", next.name());
        o.addProperty("useful_tool_receipts_not_goal_proof", Math.min(8, useful));
        o.addProperty("failed_tool_receipts", Math.min(8, failed));
        o.addProperty("steps_remaining", remainingTools);
        o.addProperty("inferences_remaining", remainingInferences);
        o.addProperty("corrective_replans_in_original_journal", Math.min(AgentEngine.MAX_REPLANS,
                Math.max(0, replanAttempts)));
        o.addProperty("needs_owner_review_not_automatic_retry", needsReview);
        o.add("weak_tools_from_related_runs_not_prohibition", historicalWeakTools.deepCopy());
        return o;
    }

    private static JsonArray historicalFailures(String experience) {
        JsonArray warnings = new JsonArray();
        if (experience == null || !AgentExperience.valid(experience) || experience.isEmpty())
            return warnings;
        try {
            JsonObject previous = JsonParser.parseString(experience).getAsJsonObject();
            for (JsonElement value : previous.getAsJsonArray("herramientas")) {
                JsonObject item = value.getAsJsonObject();
                String tool = item.get("tool").getAsString();
                int errors = item.get("error").getAsInt() + item.get("bloqueo").getAsInt();
                int successes = item.get("ok").getAsInt();
                if (AgentPolicy.TOOLS.contains(tool) && errors >= 2 && errors > successes) {
                    warnings.add(tool);
                    if (warnings.size() >= 2) break;
                }
            }
        } catch (RuntimeException ignored) {
            return new JsonArray();
        }
        return warnings;
    }
}
