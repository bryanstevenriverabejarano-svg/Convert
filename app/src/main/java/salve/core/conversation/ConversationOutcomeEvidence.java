package salve.core.conversation;

import com.google.gson.JsonObject;
import salve.core.memory.ConversationMemoryGrounding;
import salve.core.research.PublicResearchCoordinator;

/**
 * Evidence metadata from trusted memory/research controllers, not assertions
 * emitted by the LLM or an untrusted page. No content, names or URLs retained.
 */
public final class ConversationOutcomeEvidence {
    public enum Kind { MEMORY, RESEARCH }
    public enum State { OBSERVED, PARTIAL, ABSENT, ERROR }
    public final Kind kind;
    public final State state;
    public final int sources;
    public final int operations;

    private ConversationOutcomeEvidence(Kind kind, State state, int sources, int operations) {
        this.kind = kind; this.state = state;
        this.sources = Math.max(0, Math.min(99, sources));
        this.operations = Math.max(0, Math.min(99, operations));
    }
    public static ConversationOutcomeEvidence memory(ConversationMemoryGrounding.Result memory) {
        if (memory == null) return new ConversationOutcomeEvidence(Kind.MEMORY, State.ABSENT, 0, 0);
        State state;
        switch (memory.getStatus()) {
            case ERROR: state = State.ERROR; break;
            case PARTIAL: state = State.PARTIAL; break;
            case FOUND: state = memory.hasEvidence() ? State.OBSERVED : State.ABSENT; break;
            default: state = State.ABSENT; break;
        }
        return new ConversationOutcomeEvidence(Kind.MEMORY, state, memory.hasEvidence() ? 1 : 0, 1);
    }
    public static ConversationOutcomeEvidence research(
            PublicResearchCoordinator.Status status, int sources, int searches) {
        State state;
        switch (status == null ? PublicResearchCoordinator.Status.NO_SOURCES : status) {
            case ANSWERED: state = sources > 0 ? State.OBSERVED : State.ABSENT; break;
            case PARTIAL:
            case SOURCES_ONLY:
            case BUDGET_EXHAUSTED: state = sources > 0 ? State.PARTIAL : State.ABSENT; break;
            case NO_SOURCES: state = State.ABSENT; break;
            default: state = State.ERROR; break;
        }
        return new ConversationOutcomeEvidence(Kind.RESEARCH, state, sources, searches);
    }
    public JsonObject toJson() {
        JsonObject out = new JsonObject();
        out.addProperty("kind", kind.name());
        out.addProperty("state", state.name());
        out.addProperty("source_count", sources);
        out.addProperty("operations", operations);
        out.addProperty("source_presence_is_not_fact_check", true);
        return out;
    }
}
