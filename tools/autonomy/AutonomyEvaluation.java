import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import salve.core.autonomy.AutonomousToolLab;

/** Evaluates the actual Android-compatible Java laboratory, not a second Python solver. */
public final class AutonomyEvaluation {
    private static final class MemoryStore implements AutonomousToolLab.StateStore {
        private String value;
        public String read() { return value; }
        public void write(String value) { this.value = value; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3
                || (args.length == 3 && !"--fresh-each-case".equals(args[2])))
            throw new IllegalArgumentException("Uso: corpus.jsonl report.jsonl [--fresh-each-case]");
        boolean freshEachCase = args.length == 3;
        MemoryStore store = new MemoryStore();
        AutonomousToolLab lab = new AutonomousToolLab(store);
        int count = 0, passed = 0;
        try (BufferedReader reader = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(Path.of(args[1]), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 64_000 || count >= 1024) throw new IllegalArgumentException("Límite de evaluación");
                JsonObject challenge = JsonParser.parseString(line).getAsJsonObject();
                if (freshEachCase) {
                    // Controlled ablation: discard the tool and regression journal
                    // before every challenge, while executing the identical solver.
                    store = new MemoryStore();
                    lab = new AutonomousToolLab(store);
                } else if (count > 0 && count % 104 == 0) {
                    // Reconstruct from the persisted journal between full rounds.
                    lab = new AutonomousToolLab(store);
                }
                JsonObject report = lab.solve(challenge, () -> false);
                report.addProperty("evaluationMode", freshEachCase ? "fresh_each_case" : "retained_journal");
                writer.write(report.toString()); writer.newLine(); writer.flush();
                count++;
                if (report.get("verified").getAsBoolean()) passed++;
            }
        }
        System.out.println("executed=" + count + " verified=" + passed);
        if (count != 416 || passed != count) System.exit(1);
    }
}
