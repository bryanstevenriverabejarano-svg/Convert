package salve.core.conversation;

import java.util.Map;
import salve.core.cognition.CapabilityPlanner;
import salve.core.cognition.CognitiveTurn;

/**
 * Immutable interpretation of ONE user turn. This is the entry point for
 * "understand the objective", not another competing intent detector.
 *
 * Reuses ConversationRequestAnalyzer and ReasoningPlanner. CapabilityPlanner
 * only RANKS tools that the existing controller has already authorized;
 * neither this frame nor model output grants permission or executes effects.
 *
 * The same frame is carried from conversation planning to catalog ranking.
 * An exam/simulated turn with an overriding catalog keeps its own controls.
 */
public final class TurnObjective {
    private final String input;
    private final ConversationAnalysis analysis;
    private final ReasoningPlan plan;

    private TurnObjective(String input, ConversationAnalysis analysis, ReasoningPlan plan) {
        this.input = input;
        this.analysis = analysis;
        this.plan = plan;
    }

    /** No model calls, I/O, memory reads or changes to the authorization state. */
    public static TurnObjective understand(String input, boolean hasPriorContext,
                                            boolean explicitExternalSearch) {
        String text = input == null ? "" : input;
        ConversationAnalysis analysis = ConversationRequestAnalyzer.analyze(text, hasPriorContext);
        ReasoningPlan plan = ReasoningPlanner.plan(text, analysis, explicitExternalSearch);
        return new TurnObjective(text, analysis, plan);
    }

    public ConversationAct act() { return analysis.getAct(); }
    public boolean needsClarification() { return analysis.needsClarification(); }
    public ReasoningPlan plan() { return plan; }

    /** Preserves the existing contextual contract; no duplicate prose heuristics. */
    public String toPromptContext() {
        return "ACTO_CONVERSACIONAL: " + act().name() + " | " + plan.toPromptContext();
    }

    /**
     * Ordered subset of the already authorized capability map, never a new set
     * of permissions. The executor still rechecks all effects and private data.
     */
    public Map<String, CognitiveTurn.Tool> rankAuthorized(
            Map<String, CognitiveTurn.Tool> authorized) {
        return CapabilityPlanner.rank(authorized, input);
    }
}
