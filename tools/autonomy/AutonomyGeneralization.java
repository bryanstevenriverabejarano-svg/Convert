import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import salve.core.autonomy.AutonomousToolLab;

/** Real warmup and unseen-input evaluation; this harness receives no oracle answers. */
public final class AutonomyGeneralization {
    private static final class MemoryStore implements AutonomousToolLab.StateStore {
        private String value;
        public String read() { return value; }
        public void write(String value) { this.value = value; }
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        for (byte element : digest) output.append(String.format(java.util.Locale.ROOT, "%02x", element & 255));
        return output.toString();
    }

    /** Exact family-related persisted state plus the hash of the complete journal. */
    private static JsonObject snapshot(MemoryStore store, String family) throws Exception {
        JsonObject result = new JsonObject();
        result.addProperty("family", family);
        result.addProperty("persisted", store.value != null);
        if (store.value == null) {
            result.add("fullJournalSha256", JsonNull.INSTANCE);
            return result;
        }
        result.addProperty("fullJournalSha256", sha256(store.value));
        JsonObject state = JsonParser.parseString(store.value).getAsJsonObject();
        JsonObject relevant = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() || value.isJsonNull()) {
                relevant.add(entry.getKey(), value.deepCopy());
            } else if (value.isJsonObject() && value.getAsJsonObject().has(family)) {
                relevant.add(entry.getKey(), value.getAsJsonObject().get(family).deepCopy());
            } else if (value.isJsonArray()) {
                JsonArray selected = new JsonArray();
                for (JsonElement element : value.getAsJsonArray()) {
                    if (element.isJsonObject() && element.getAsJsonObject().has("family")
                            && family.equals(element.getAsJsonObject().get("family").getAsString())) {
                        selected.add(element.deepCopy());
                    }
                }
                relevant.add(entry.getKey(), selected);
            }
        }
        result.add("familyState", relevant);
        return result;
    }

    private static AutonomousToolLab execute(Path input, Path output, String phase, int expected,
                                              MemoryStore store, AutonomousToolLab lab) throws Exception {
        int count = 0, verified = 0;
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 64000 || count >= expected) throw new IllegalArgumentException("Corpus fuera de límites");
                JsonObject challenge = JsonParser.parseString(line).getAsJsonObject();
                if ("warmup".equals(phase) && count > 0 && count % 104 == 0) lab = new AutonomousToolLab(store);
                String family = challenge.get("family").getAsString();
                JsonObject before = snapshot(store, family);
                String recalled = lab.proceduralContext(family);
                JsonObject report = lab.solve(challenge, () -> false);
                report.addProperty("evaluationPhase", phase);
                report.add("stateBefore", before);
                report.add("stateAfter", snapshot(store, family));
                report.addProperty("proceduralContextBefore", recalled);
                writer.write(report.toString()); writer.newLine(); writer.flush();
                count++;
                if (report.has("verified") && report.get("verified").getAsBoolean()) verified++;
            }
        }
        if (count != expected) throw new IllegalArgumentException("Número de retos incorrecto: " + count);
        // A failed candidate is evidence for Python to assess, not a reason to
        // silently discard the run or change the frozen solver.
        System.out.println(phase + " executed=" + count + " solver_verified=" + verified);
        return lab;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Uso: warmup.jsonl holdout.jsonl warmup-report.jsonl holdout-report.jsonl");
        MemoryStore store = new MemoryStore();
        AutonomousToolLab lab = new AutonomousToolLab(store);
        execute(Path.of(args[0]), Path.of(args[2]), "warmup", 416, store, lab);
        // Reload from the persisted journal before seeing any holdout input.
        lab = new AutonomousToolLab(store);
        execute(Path.of(args[1]), Path.of(args[3]), "holdout", 128, store, lab);
    }
}
