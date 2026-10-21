package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import salve.core.conversation.ChatMessage;

/**
 * A small shared checkpoint for one existing conversational inference.
 *
 * It coordinates already-collected context; it does not infer private mental
 * states, read new sources, grant permissions, run tools, or produce hidden
 * reasoning. Tool statuses are controller receipts, not claims of goal success.
 */
public final class CognitiveCohesionFrame {
    public enum Phase {
        GROUND_RESPONSE, COMPARE_ALTERNATIVES, VERIFY_TOOL_OUTCOME, REPLAN_AFTER_FAILURE
    }

    private final Phase phase;
    private final boolean runtime, selfModel, memory, context, history;
    private final int observedReceipts;

    private CognitiveCohesionFrame(Phase phase, boolean runtime, boolean selfModel,
                                   boolean memory, boolean context, boolean history,
                                   int observedReceipts) {
        this.phase = phase;
        this.runtime = runtime;
        this.selfModel = selfModel;
        this.memory = memory;
        this.context = context;
        this.history = history;
        this.observedReceipts = observedReceipts;
    }

    public static CognitiveCohesionFrame observe(JsonObject state, String selfModel,
                                                 String memory, String context,
                                                 List<ChatMessage> history,
                                                 JsonArray receipts, boolean decisionPresent) {
        boolean failed = false, completed = false;
        int observed = 0;
        if (receipts != null) for (JsonElement value : receipts) {
            if (!value.isJsonObject()) continue;
            JsonObject receipt = value.getAsJsonObject();
            if (!receipt.has("status") || !receipt.get("status").isJsonPrimitive()
                    || !receipt.get("status").getAsJsonPrimitive().isString()) continue;
            String status = receipt.get("status").getAsString();
            if ("failed".equals(status)) { failed = true; observed++; }
            else if ("completed".equals(status)) { completed = true; observed++; }
        }

        Phase phase = failed ? Phase.REPLAN_AFTER_FAILURE
                : completed ? Phase.VERIFY_TOOL_OUTCOME
                : decisionPresent ? Phase.COMPARE_ALTERNATIVES
                : Phase.GROUND_RESPONSE;
        return new CognitiveCohesionFrame(phase,
                state != null && "Android/controller".equals(primitive(state, "source")),
                present(selfModel), present(memory), present(context),
                history != null && !history.isEmpty(), Math.min(2, observed));
    }

    /** No user content, personal memories or tool payloads are copied here. */
    public JsonObject toJson() {
        JsonObject out = new JsonObject();
        out.addProperty("phase", phase.name());
        JsonArray sources = new JsonArray();
        if (runtime) sources.add("runtime_observed");
        if (selfModel) sources.add("self_model_unverified");
        if (memory) sources.add("memory_unverified");
        if (context) sources.add("context_unverified");
        if (history) sources.add("conversation_unverified");
        out.add("sources_present_not_truth", sources);
        out.addProperty("tool_receipts_observed", observedReceipts);
        // Source presence never proves a claim. The controller still gates tool calls.
        out.addProperty("rule", "separar evidencia e hipótesis; no afirmar éxito sin comprobación");
        return out;
    }

    public Phase getPhase() { return phase; }

    private static boolean present(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private static String primitive(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()
                || !obj.get(key).getAsJsonPrimitive().isString()) return null;
        return obj.get(key).getAsString();
    }
}
