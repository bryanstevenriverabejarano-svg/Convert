package salve.core.agent;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/** Versioned execution journal: decisions and observations, never private reasoning traces. */
public final class AgentRun {
    public int version = 1;
    public String id, goal, owner = "", status = "QUEUED", bridge = "", error = "", answer = "", feedback = "";
    public boolean codeAllowed;
    public long createdAt, updatedAt, leaseUntil;
    public int modelCalls, revisions, attempts;
    public List<String> outline = new ArrayList<>(), evidence = new ArrayList<>(), providers = new ArrayList<>();
    public List<Observation> observations = new ArrayList<>();
    public Pending pending;
    public static final class Pending {
        public String id, tool, input;
        public Pending(String id, String tool, String input) { this.id = id; this.tool = tool; this.input = input; }
    }
    public static final class Observation {
        public String id, tool, status, text;
        public long elapsedMs;
        public List<String> sources = new ArrayList<>(), links = new ArrayList<>();
        public Observation(String id, String tool, String status, String text) {
            this.id = id; this.tool = tool; this.status = status; this.text = text;
        }
        public boolean useful() { return "SUCCESS".equals(status) || "PARTIAL".equals(status); }
    }
    public boolean terminal() { return !(status.equals("QUEUED") || status.equals("RUNNING")); }
    public String encode() {
        String json = new Gson().toJson(this);
        if (json.length() > 250_000) throw new IllegalArgumentException("Agent journal budget exceeded");
        return json;
    }
    public static AgentRun decode(String json) {
        if (json == null || json.length() > 250_000) throw new IllegalArgumentException("Invalid agent journal");
        AgentRun run = new Gson().fromJson(json, AgentRun.class);
        if (run == null || run.version != 1 || run.goal == null || run.id == null || run.status == null
                || run.observations == null || run.observations.size() > AgentEngine.MAX_STEPS
                || run.outline == null || run.evidence == null || run.providers == null
                || run.owner == null || run.bridge == null || run.error == null || run.answer == null || run.feedback == null
                || run.goal.length() > 2048 || run.id.length() > 60 || run.answer.length() > 2500
                || run.outline.size() > 6 || run.evidence.size() > 8 || run.providers.size() > AgentEngine.MAX_MODEL_CALLS
                || run.attempts < 0 || run.attempts > 8
                || run.modelCalls < 0 || run.modelCalls > AgentEngine.MAX_MODEL_CALLS || run.revisions < 0 || run.revisions > 3)
            throw new IllegalArgumentException("Unsupported agent journal");
        for (String item : run.outline) if (item == null || item.length() > 160) throw new IllegalArgumentException("Invalid outline");
        for (String item : run.evidence) if (item == null || item.length() > 60) throw new IllegalArgumentException("Invalid evidence");
        for (String item : run.providers) if (item == null || item.length() > 150) throw new IllegalArgumentException("Invalid provider");
        if (run.pending != null && (run.pending.id == null || run.pending.tool == null || run.pending.input == null
                || run.pending.id.length() > 60 || run.pending.tool.length() > 40 || run.pending.input.length() > 6000))
            throw new IllegalArgumentException("Invalid pending tool");
        for (Observation o : run.observations) {
            if (o == null || o.id == null || o.tool == null || o.status == null || o.text == null
                    || o.id.length() > 60 || o.tool.length() > 40 || o.text.length() > 6000
                    || o.sources == null || o.sources.size() > 6 || o.links == null || o.links.size() > 12)
                throw new IllegalArgumentException("Invalid observation");
            for (String url : o.sources) if (url == null || url.length() > 500) throw new IllegalArgumentException("Invalid source");
            for (String url : o.links) if (url == null || url.length() > 500) throw new IllegalArgumentException("Invalid link");
        }
        return run;
    }
}
