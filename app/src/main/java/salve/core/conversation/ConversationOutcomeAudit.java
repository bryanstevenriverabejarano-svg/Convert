package salve.core.conversation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Shared bounded delivery audit for conversational and direct routes. An
 * accepted output is never promoted to truth simply for being generated.
 * No prompt, personal content, model chain-of-thought or tool payload is logged.
 */
public final class ConversationOutcomeAudit {
    public enum Status {
        FAILED, PENDING_APPROVAL, NEEDS_REVISION,
        EVIDENCE_UNAVAILABLE, EVIDENCE_OBSERVED_NOT_VERIFIED, RESPONSE_UNVERIFIED
    }
    public final Status status;
    public final String demandIssue;
    public final int toolCompleted, toolFailed;
    public final String route;
    public final ConversationOutcomeEvidence evidence;

    private ConversationOutcomeAudit(Status status, String demandIssue,
                                     int toolCompleted, int toolFailed, String route,
                                     ConversationOutcomeEvidence evidence) {
        this.status = status; this.demandIssue = demandIssue;
        this.toolCompleted = toolCompleted; this.toolFailed = toolFailed;
        this.route = route; this.evidence = evidence;
    }

    /** "delivered" is technical delivery, not factual verification or user approval. */
    public static ConversationOutcomeAudit evaluate(String request, String answer,
                                                     JsonArray receipts, boolean delivered,
                                                     String route) {
        return evaluate(request, answer, receipts, delivered, route, null);
    }

    /** A trusted adapter supplies observations; the LLM cannot claim verification. */
    public static ConversationOutcomeAudit evaluate(String request, String answer,
                                                     JsonArray receipts, boolean delivered,
                                                     String route, ConversationOutcomeEvidence evidence) {
        int completed = 0, failed = 0;
        boolean handoff = false;
        if (receipts != null) for (JsonElement item : receipts) {
            if (!item.isJsonObject()) continue;
            JsonObject receipt = item.getAsJsonObject();
            if (!receipt.has("status") || !receipt.get("status").isJsonPrimitive()) continue;
            switch (receipt.get("status").getAsString()) {
                case "completed": completed++; break;
                case "failed": failed++; break;
                case "handoff": handoff = true; break;
                default: break;
            }
        }
        String issue = delivered ? TurnChecks.problem(request, answer, false) : null;
        Status status = !delivered || answer == null || answer.trim().isEmpty()
                ? Status.FAILED
                : handoff ? Status.PENDING_APPROVAL
                : issue != null ? Status.NEEDS_REVISION
                : evidence != null && (evidence.state == ConversationOutcomeEvidence.State.ABSENT
                        || evidence.state == ConversationOutcomeEvidence.State.ERROR) ? Status.EVIDENCE_UNAVAILABLE
                : failed > 0 && completed == 0 ? Status.EVIDENCE_UNAVAILABLE
                : evidence != null || completed > 0 ? Status.EVIDENCE_OBSERVED_NOT_VERIFIED
                : Status.RESPONSE_UNVERIFIED;
        String safeRoute = route == null || !route.matches("[a-z_]{1,24}") ? "unknown" : route;
        return new ConversationOutcomeAudit(status, issue, completed, failed, safeRoute, evidence);
    }

    /** Only generic status flags are suitable for the execution journal. */
    public JsonObject receipt() {
        JsonObject obj = new JsonObject();
        obj.addProperty("route", route);
        obj.addProperty("status", status.name());
        obj.addProperty("tool_completed", toolCompleted);
        obj.addProperty("tool_failed", toolFailed);
        obj.addProperty("demand_issue_detected", demandIssue != null);
        obj.addProperty("human_verified", false);
        obj.addProperty("answer_factually_verified", false);
        if (evidence != null) obj.add("controller_evidence", evidence.toJson());
        return obj;
    }
}
