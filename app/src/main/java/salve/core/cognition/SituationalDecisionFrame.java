package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Observable, bounded evidence frame for a difficult conversational decision.
 *
 * This is NOT a model of anyone's mind and does not autonomously authorize actions.
 * It quotes possible priorities from the user's words and summarizes controller
 * receipts. The LLM must still evaluate alternatives. The tool controller is the
 * only executor and keeps the existing authorization boundary.
 */
public final class SituationalDecisionFrame {
    private static final Pattern CONTRAST = Pattern.compile(
            "(?iu)\\s+(?:pero|mientras(?:\\s+que)?|aunque|sin\\s+embargo|a\\s+la\\s+vez|al\\s+mismo\\s+tiempo|por\\s+otra\\s+parte)\\s+");
    private static final Pattern DECISION = Pattern.compile(
            "(?u).*\\b(?:decidir|decido|decision|elegir|elijo|priorizar|conviene|deberia|que hago|cual es mejor|vale la pena)\\b.*");
    private static final Pattern PRESSURE = Pattern.compile(
            "(?u).*\\b(?:urgente|ultima oportunidad|ultima vez|despedirme|antes de|se acaba|vence|caduca|solo tengo|poco tiempo|se termina)\\b.*");

    public enum Situation { POTENTIAL_CONFLICT, DECISION_REQUEST, TIME_CONSTRAINT }
    private final Situation situation;
    private final List<String> statedPriorities;
    private final boolean possibleTimePressure;
    private final List<ReceiptStatus> receipts;
    private final boolean toolCatalogAvailable;

    private static final class ReceiptStatus {
        final String name;
        final String status;
        ReceiptStatus(String name, String status) { this.name = name; this.status = status; }
    }

    private SituationalDecisionFrame(Situation situation, List<String> statedPriorities,
                                     boolean possibleTimePressure, List<ReceiptStatus> receipts,
                                     boolean toolCatalogAvailable) {
        this.situation = situation;
        this.statedPriorities = Collections.unmodifiableList(new ArrayList<>(statedPriorities));
        this.possibleTimePressure = possibleTimePressure;
        this.receipts = Collections.unmodifiableList(new ArrayList<>(receipts));
        this.toolCatalogAvailable = toolCatalogAvailable;
    }

    /** Returns null for ordinary turns; avoids wasting small-model context. */
    public static SituationalDecisionFrame observe(String userInput, JsonArray toolReceipts,
                                                    Map<String, CognitiveTurn.Tool> tools) {
        if (userInput == null || userInput.trim().isEmpty()) return null;
        String input = userInput.trim();
        String normalized = normalize(input);
        String[] clauses = CONTRAST.split(input, 3);
        boolean contrast = clauses.length >= 2
                && !clauses[0].trim().isEmpty() && !clauses[1].trim().isEmpty();
        boolean decision = DECISION.matcher(normalized).matches();
        boolean time = PRESSURE.matcher(normalized).matches();
        if (!contrast && !decision && !time) return null;

        List<String> priorities = new ArrayList<>();
        if (contrast) {
            priorities.add(excerpt(clauses[0].trim(), 94));
            priorities.add(excerpt(clauses[1].trim(), 94));
        } else {
            priorities.add(excerpt(input, 115));
        }
        List<ReceiptStatus> statuses = new ArrayList<>();
        if (toolReceipts != null) for (JsonElement item : toolReceipts) {
            if (statuses.size() >= 2) break;
            if (!item.isJsonObject()) continue;
            JsonObject o = item.getAsJsonObject();
            String name = string(o, "name");
            String status = string(o, "status");
            if (name == null || status == null) continue;
            // A completed tool receipt is not proof that the outside-world goal succeeded.
            if (!"completed".equals(status) && !"failed".equals(status)) status = "unknown";
            statuses.add(new ReceiptStatus(excerpt(name, 45), status));
        }
        Situation kind = contrast ? Situation.POTENTIAL_CONFLICT
                : decision ? Situation.DECISION_REQUEST : Situation.TIME_CONSTRAINT;
        return new SituationalDecisionFrame(kind, priorities, time, statuses,
                tools != null && !tools.isEmpty());
    }

    /** Compact controller-owned evidence metadata, not a command or personal judgment. */
    public JsonObject toJson() {
        JsonObject frame = new JsonObject();
        frame.addProperty("type", situation.name());
        JsonArray priorities = new JsonArray();
        for (String priority : statedPriorities) priorities.add(priority);
        frame.add("user_fragments_unverified", priorities);
        frame.addProperty("possible_time_pressure", possibleTimePressure);
        frame.addProperty("decision_rule", "compare alternatives; preserve options; no assumed feelings or permissions");
        frame.addProperty("tool_available", toolCatalogAvailable);
        JsonArray observations = new JsonArray();
        for (ReceiptStatus receipt : receipts) {
            JsonObject item = new JsonObject();
            item.addProperty("tool", receipt.name);
            item.addProperty("status", receipt.status);
            observations.add(item);
        }
        frame.add("receipts_observed_not_goal_proof", observations);
        return frame;
    }

    public Situation getSituation() { return situation; }
    public List<String> getStatedPriorities() { return statedPriorities; }
    public boolean hasPossibleTimePressure() { return possibleTimePressure; }

    private static String string(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()
                || !object.get(field).getAsJsonPrimitive().isString()) return null;
        return object.get(field).getAsString();
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
    private static String excerpt(String text, int limit) {
        if (text.length() <= limit) return text;
        int end = limit - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }
}
