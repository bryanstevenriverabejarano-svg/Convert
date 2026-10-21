package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Bounded, privacy-safe observable execution summary.
 *
 * "completed" means the registered tool returned a receipt; it never asserts
 * that the user's objective was met. Tool data and model output are omitted.
 * Callbacks that were handed to an approval UI are not counted as successes.
 */
public final class CognitiveToolAudit {
    private final JsonArray actions = new JsonArray();
    private int completed, failed, handoffs;

    private CognitiveToolAudit() { }

    public static CognitiveToolAudit from(CognitiveTurn.Outcome outcome) {
        CognitiveToolAudit audit = new CognitiveToolAudit();
        if (outcome == null) return audit;
        for (JsonElement value : outcome.receipts()) {
            if (!value.isJsonObject()) continue;
            JsonObject item = value.getAsJsonObject();
            if (!item.has("name") || !item.has("status")) continue;
            String status = item.get("status").getAsString();
            if ("completed".equals(status)) audit.completed++;
            else if ("failed".equals(status)) audit.failed++;
            else if ("handoff".equals(status)) audit.handoffs++;
            else continue;
            JsonObject safe = new JsonObject();
            safe.addProperty("tool", item.get("name").getAsString());
            safe.addProperty("status", status);
            audit.actions.add(safe);
        }
        return audit;
    }

    public boolean hasActions() { return actions.size() > 0; }
    public JsonObject toJson() {
        JsonObject report = new JsonObject();
        report.addProperty("tool_receipts_completed_not_goal_proof", completed);
        report.addProperty("tool_receipts_failed", failed);
        report.addProperty("pending_external_approval_or_result", handoffs);
        report.addProperty("goal_verified", false);
        report.add("actions_without_payloads", actions.deepCopy());
        return report;
    }
}
