package salve.core.agent;

import com.google.gson.*;
import java.util.ArrayList;
import java.util.List;

/** A strict tool proposal or a proposed answer. Neither can grant itself capabilities. */
public final class AgentDecision {
    public String tool = "", input = "", answer = "";
    public List<String> evidence = new ArrayList<>(), outline = new ArrayList<>();
    public static AgentDecision parse(String text) {
        if (text == null || text.length() > 12_000) throw new IllegalArgumentException("Invalid decision size");
        String raw = text.trim();
        if (raw.startsWith("```json\n") && raw.endsWith("```")) raw = raw.substring(8, raw.length() - 3).trim();
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        for (String key : json.keySet()) if (!java.util.Arrays.asList("tool", "input", "answer", "evidence", "plan").contains(key))
            throw new IllegalArgumentException("Unknown decision field");
        AgentDecision d = new AgentDecision();
        if (json.has("answer")) {
            if (json.has("tool") || json.has("input")) throw new IllegalArgumentException("Ambiguous decision");
            d.answer = string(json, "answer", 2500);
            d.evidence = strings(json, "evidence", 8, 60);
            if (d.answer.isEmpty() || d.evidence.isEmpty()) throw new IllegalArgumentException("Answer without evidence");
        } else {
            d.tool = string(json, "tool", 40); d.input = string(json, "input", 6000);
            if (d.tool.isEmpty() || d.input.isEmpty()) throw new IllegalArgumentException("Incomplete tool call");
        }
        d.outline = strings(json, "plan", 6, 160);
        return d;
    }
    private static String string(JsonObject o, String key, int max) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected text");
        String value = e.getAsString().trim();
        if (value.length() > max) throw new IllegalArgumentException("Text budget exceeded");
        return value;
    }
    private static List<String> strings(JsonObject o, String key, int max, int chars) {
        List<String> values = new ArrayList<>();
        if (!o.has(key)) return values;
        JsonArray list = o.getAsJsonArray(key);
        if (list.size() > max) throw new IllegalArgumentException("List budget exceeded");
        for (JsonElement value : list) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                    || value.getAsString().length() > chars) throw new IllegalArgumentException("Invalid list item");
            values.add(value.getAsString());
        }
        return values;
    }
}
