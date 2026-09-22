package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Bounded symbolic synthesis, exact feedback, regression checks and versioned tool promotion. */
public final class AutonomousToolLab {
    public interface StateStore {
        String read() throws Exception;
        void write(String json) throws Exception;
    }
    private static final int ATTEMPT_OPERATIONS = 5_000_000;
    private static final int MAX_ATTEMPTS = 4;
    private static final int REGRESSION_CASES = 3;
    private final StateStore store;
    private final AtomicBoolean paused = new AtomicBoolean();
    private JsonObject state;

    public AutonomousToolLab() { this(new StateStore() {
        private String value;
        public String read() { return value; }
        public void write(String json) { value = json; }
    }); }

    public AutonomousToolLab(StateStore store) {
        if (store == null) throw new IllegalArgumentException("Falta almacenamiento");
        this.store = store;
        try {
            String raw = store.read();
            state = raw == null ? initialState() : restore(raw);
        } catch (Exception invalid) {
            throw new IllegalStateException("No pude leer el laboratorio; no he borrado su estado", invalid);
        }
    }

    private static JsonObject initialState() {
        JsonObject value = new JsonObject();
        value.addProperty("schema", 1); value.addProperty("revision", 0);
        value.add("tools", new JsonObject()); value.add("regressions", new JsonObject());
        value.add("receipts", new JsonArray());
        return value;
    }

    private static JsonObject restore(String raw) {
        if (raw.length() > 512_000) throw new IllegalArgumentException("Estado demasiado grande");
        JsonObject value = JsonParser.parseString(raw).getAsJsonObject();
        if (!value.get("schema").toString().equals("1"))
            throw new IllegalArgumentException("Estado no compatible");
        counter(value.get("revision"), 0);
        JsonObject tools = value.getAsJsonObject("tools");
        if (tools.size() > 4 || value.getAsJsonArray("receipts").size() > 24) throw new IllegalArgumentException();
        for (String family : tools.keySet()) {
            if (ToolKernels.strategies(family).isEmpty()) throw new IllegalArgumentException();
            JsonObject entry = tools.getAsJsonObject(family);
            checkProgramFamily(entry.getAsJsonObject("program"), family);
            if (entry.has("previous")) checkProgramFamily(entry.getAsJsonObject("previous"), family);
            counter(entry.get("version"), 1);
        }
        JsonObject regressions = value.getAsJsonObject("regressions");
        if (regressions.size() > 4) throw new IllegalArgumentException();
        OperationBudget restoreBudget = new OperationBudget(ATTEMPT_OPERATIONS);
        for (String family : regressions.keySet()) {
            if (ToolKernels.strategies(family).isEmpty() || regressions.getAsJsonArray(family).size() > REGRESSION_CASES)
                throw new IllegalArgumentException();
            String validationStrategy;
            switch (family) {
                case "route": validationStrategy = "BELLMAN_FORD"; break;
                case "knapsack": validationStrategy = "DYNAMIC_PROGRAMMING"; break;
                case "schedule": validationStrategy = "WEIGHTED_DP"; break;
                case "dependencies": validationStrategy = "KAHN_LAYERS"; break;
                default: throw new IllegalArgumentException("Familia no compatible");
            }
            for (JsonElement input : regressions.getAsJsonArray(family)) {
                if (!input.isJsonObject()) throw new IllegalArgumentException("Regresión sin datos válidos");
                // The general kernel checks the complete input contract with bounded work.
                // Do not run the saved program or require compatibility with a rolled-back version.
                ToolKernels.execute(family, validationStrategy, input.getAsJsonObject(), restoreBudget);
            }
        }
        return value;
    }

    private static long counter(JsonElement value, long minimum) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Contador no entero");
        String raw = value.getAsString();
        if (raw.length() > 19 || !raw.matches("0|[1-9][0-9]*"))
            throw new IllegalArgumentException("Contador no entero");
        long parsed = Long.parseLong(raw);
        if (parsed < minimum) throw new IllegalArgumentException("Contador fuera de rango");
        return parsed;
    }

    private static void checkProgramFamily(JsonObject program, String family) {
        ToolProgram.strategy(program);
        if (!family.equals(program.get("family").getAsString())) throw new IllegalArgumentException("Familia cambiada");
    }

    public void pause() { paused.set(true); }
    public void resume() { paused.set(false); }
    public boolean isPaused() { return paused.get(); }

    /** No oracle answers or reference programs enter the planner; only pass/fail summaries do. */
    public synchronized JsonObject solve(JsonObject challenge, BooleanSupplier stop) {
        long started = System.nanoTime();
        JsonObject report = new JsonObject();
        report.add("challenge", challenge == null ? new JsonObject() : challenge.deepCopy());
        report.addProperty("synthesis", "symbolic");
        report.addProperty("verified", false); report.addProperty("promoted", false);
        report.addProperty("adapted", false); report.addProperty("reused", false);
        report.addProperty("rolledBack", false); report.addProperty("status", "unverified");
        JsonArray attempts = new JsonArray(); report.add("attempts", attempts);
        int totalOperations = 0;
        try {
            if (stop == null || challenge == null || challenge.toString().length() > 64_000
                    || !challenge.has("schema") || !challenge.get("schema").toString().equals("1")
                    || !challenge.has("id") || !challenge.get("id").isJsonPrimitive()
                    || !challenge.get("id").getAsJsonPrimitive().isString()
                    || !challenge.get("id").getAsString().matches("[A-Za-z0-9_-]{1,96}"))
                throw new IllegalArgumentException("Reto inválido");
            String family = challenge.get("family").getAsString();
            List<String> available = ToolKernels.strategies(family);
            if (available.isEmpty()) throw new IllegalArgumentException("Familia no compatible");
            JsonObject input = challenge.getAsJsonObject("input");
            if (input == null) throw new IllegalArgumentException("Faltan datos");
            BooleanSupplier stopped = () -> paused.get() || stop.getAsBoolean();
            new OperationBudget(1, stopped).tick();
            JsonObject existing = state.getAsJsonObject("tools").getAsJsonObject(family);
            String existingStrategy = existing == null ? null : ToolProgram.strategy(existing.getAsJsonObject("program"));
            Set<String> candidates = new LinkedHashSet<>();
            if (existingStrategy != null) candidates.add(existingStrategy);
            candidates.addAll(available);
            JsonArray regressions = state.getAsJsonObject("regressions").getAsJsonArray(family);
            boolean invalidatedExisting = false;
            for (String strategy : candidates) {
                if (attempts.size() >= MAX_ATTEMPTS) break;
                OperationBudget budget = new OperationBudget(ATTEMPT_OPERATIONS, stopped);
                JsonObject program = ToolProgram.generate(family, strategy);
                JsonObject attempt = new JsonObject();
                attempt.addProperty("strategy", strategy); attempt.add("program", program.deepCopy());
                attempt.addProperty("programSha256", ToolProgram.sha256(program));
                attempt.addProperty("passed", false); attempt.addProperty("regressionChecks", 0);
                long attemptStart = System.nanoTime();
                JsonObject result = null;
                boolean passed = false;
                try {
                    ToolInterpreter.Execution execution = ToolInterpreter.run(program, input, budget);
                    result = execution.result;
                    passed = execution.verification.passed;
                    attempt.addProperty("feedback", execution.verification.summary);
                    int checked = 0;
                    if (passed && regressions != null) {
                        for (JsonElement old : regressions) {
                            if (old.equals(input)) continue;
                            ToolInterpreter.Execution regression = ToolInterpreter.run(program, old.getAsJsonObject(), budget);
                            checked++;
                            if (!regression.verification.passed) {
                                passed = false;
                                attempt.addProperty("feedback", "El candidato falla una regresión previamente verificada");
                                break;
                            }
                        }
                    }
                    attempt.addProperty("regressionChecks", checked);
                } catch (OperationBudget.Exhausted exhausted) {
                    passed = false;
                    attempt.addProperty("feedback", "Presupuesto del candidato agotado; no se acepta");
                } catch (CancellationException cancelled) {
                    passed = false;
                    attempt.addProperty("feedback", "Cancelado; no se acepta");
                    throw cancelled;
                } catch (RuntimeException invalid) {
                    passed = false;
                    attempt.addProperty("feedback", "Datos o precondiciones incompatibles con el candidato");
                } finally {
                    attempt.addProperty("passed", passed);
                    if (result != null) attempt.add("result", result);
                    attempt.addProperty("operations", budget.getUsed());
                    attempt.addProperty("elapsedNanos", Math.max(0L, System.nanoTime() - attemptStart));
                    totalOperations += budget.getUsed();
                    attempts.add(attempt);
                }
                if (!passed) {
                    if (strategy.equals(existingStrategy)) invalidatedExisting = true;
                    continue;
                }
                // Re-check cancellation immediately before committing a candidate.
                new OperationBudget(1, stopped).tick();
                JsonObject next = state.deepCopy();
                JsonObject entry = new JsonObject();
                boolean changed = existing == null || !program.equals(existing.getAsJsonObject("program"));
                long version = existing == null ? 1
                        : Math.addExact(existing.get("version").getAsLong(), changed ? 1L : 0L);
                entry.addProperty("version", version); entry.add("program", program.deepCopy());
                if (changed && existing != null) entry.add("previous", existing.get("program").deepCopy());
                else if (existing != null && existing.has("previous")) entry.add("previous", existing.get("previous").deepCopy());
                next.getAsJsonObject("tools").add(family, entry);
                JsonArray archive = regressions == null ? new JsonArray() : regressions.deepCopy();
                boolean known = false;
                for (JsonElement old : archive) if (old.equals(input)) known = true;
                if (!known) archive.add(input.deepCopy());
                while (archive.size() > REGRESSION_CASES) archive.remove(0);
                next.getAsJsonObject("regressions").add(family, archive);
                next.addProperty("revision", Math.addExact(state.get("revision").getAsLong(), 1L));
                JsonObject receipt = new JsonObject();
                receipt.addProperty("id", challenge.get("id").getAsString()); receipt.addProperty("family", family);
                receipt.addProperty("strategy", strategy); receipt.addProperty("version", version);
                receipt.addProperty("verified", true); receipt.addProperty("attempts", attempts.size());
                next.getAsJsonArray("receipts").add(receipt);
                trimReceipts(next);
                report.add("program", program); report.add("result", result);
                report.addProperty("verified", true); report.addProperty("status", "verified");
                report.addProperty("adapted", attempts.size() > 1);
                report.addProperty("reused", !changed);
                report.addProperty("previousRejected", invalidatedExisting);
                report.addProperty("version", version);
                report.addProperty("decisionSummary", attempts.size() > 1
                        ? "Descarté candidatos que fallaron el contrato y verifiqué una alternativa con regresiones."
                        : "Verifiqué el programa con los datos actuales y las regresiones conservadas.");
                try {
                    store.write(next.toString());
                    state = next;
                    report.addProperty("promoted", true);
                } catch (Exception unavailable) {
                    report.addProperty("status", "storage_error");
                    report.addProperty("decisionSummary", "Resultado verificado, pero no guardé la herramienta; la versión anterior permanece.");
                }
                return finish(report, started, totalOperations);
            }
            report.addProperty("adapted", attempts.size() > 1);
            report.addProperty("previousRejected", invalidatedExisting);
            report.addProperty("decisionSummary", "Ningún candidato superó todas las comprobaciones; no cambié la herramienta guardada.");
            if (totalOperations >= ATTEMPT_OPERATIONS * attempts.size() && attempts.size() > 0)
                report.addProperty("status", "budget_exhausted");
        } catch (CancellationException cancelled) {
            report.addProperty("status", "cancelled");
            report.addProperty("decisionSummary", "Trabajo detenido; no se promocionó ningún candidato.");
        } catch (ArithmeticException exhaustedCounter) {
            report.addProperty("status", "storage_error");
            report.addProperty("decisionSummary", "El contador del registro alcanzó su límite; conservé la versión guardada.");
        } catch (RuntimeException invalid) {
            report.addProperty("decisionSummary", "El reto no cumple un contrato compatible; no he ejecutado código libre ni cambiado herramientas.");
        }
        return finish(report, started, totalOperations);
    }

    /**
     * Keep finite evidence for every currently saved tool, even when one family dominates traffic.
     * Prefer evicting stale-version receipts; otherwise remove the oldest duplicate family receipt,
     * preserving that family's most recent receipt matching its current program and version.
     */
    private static void trimReceipts(JsonObject next) {
        JsonArray receipts = next.getAsJsonArray("receipts");
        JsonObject tools = next.getAsJsonObject("tools");
        while (receipts.size() > 24) {
            int removable = -1;
            for (int i = 0; i < receipts.size(); i++) {
                if (!matchesCurrentTool(receipts.get(i), tools)) { removable = i; break; }
            }
            if (removable < 0) {
                for (int i = 0; i < receipts.size() && removable < 0; i++) {
                    String family = receipts.get(i).getAsJsonObject().get("family").getAsString();
                    for (int j = i + 1; j < receipts.size(); j++) {
                        if (family.equals(receipts.get(j).getAsJsonObject().get("family").getAsString())) {
                            removable = i;
                            break;
                        }
                    }
                }
            }
            // At most four current families exist, so a full 25-receipt window has a duplicate.
            if (removable < 0) throw new IllegalStateException("No se pudo acotar la memoria de herramientas");
            receipts.remove(removable);
        }
    }

    private static boolean matchesCurrentTool(JsonElement element, JsonObject tools) {
        try {
            JsonObject receipt = element.getAsJsonObject();
            String family = receipt.get("family").getAsString();
            JsonObject entry = tools.getAsJsonObject(family);
            if (entry == null || !receipt.getAsJsonPrimitive("verified").isBoolean()
                    || !receipt.get("verified").getAsBoolean()) return false;
            long attempts = counter(receipt.get("attempts"), 1);
            return attempts <= MAX_ATTEMPTS
                    && counter(receipt.get("version"), 1) == counter(entry.get("version"), 1)
                    && ToolProgram.strategy(entry.getAsJsonObject("program"))
                            .equals(receipt.get("strategy").getAsString());
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private static JsonObject finish(JsonObject report, long start, int operations) {
        report.addProperty("operations", operations);
        report.addProperty("elapsedNanos", Math.max(0L, System.nanoTime() - start));
        return report;
    }

    public synchronized String describe() {
        JsonObject tools = state.getAsJsonObject("tools");
        return "Laboratorio " + (paused.get() ? "pausado" : "disponible") + ". Herramientas declarativas verificadas: "
                + tools.size() + ". Familias: " + String.join(", ", tools.keySet())
                + ". Revisiones guardadas: " + state.get("revision").getAsLong()
                + ". Síntesis simbólica acotada; no demuestra superinteligencia ni ejecuta código libre.";
    }

    public synchronized String proceduralContext(String family) {
        return ToolExperienceMemory.contextFor(state, family);
    }

    public synchronized String rollback(String family) {
        if (paused.get()) return "El laboratorio está pausado; reanúdalo para cambiar una versión.";
        JsonObject old = state.getAsJsonObject("tools").getAsJsonObject(family);
        if (old == null || !old.has("previous")) return "No hay una versión anterior para esa familia.";
        final long version;
        final long revision;
        try {
            version = Math.addExact(old.get("version").getAsLong(), 1L);
            revision = Math.addExact(state.get("revision").getAsLong(), 1L);
        } catch (ArithmeticException exhaustedCounter) {
            return "El contador del registro alcanzó su límite; conservé la versión actual.";
        }
        JsonObject next = state.deepCopy();
        JsonObject entry = next.getAsJsonObject("tools").getAsJsonObject(family);
        JsonObject previous = entry.getAsJsonObject("previous").deepCopy();
        checkProgramFamily(previous, family);
        entry.add("previous", entry.get("program").deepCopy());
        entry.add("program", previous); entry.addProperty("version", version);
        next.addProperty("revision", revision);
        try { store.write(next.toString()); state = next; }
        catch (Exception unavailable) { return "No pude guardar la reversión; conservé la versión actual."; }
        return "Versión anterior restaurada. Se comprobará de nuevo antes de reutilizarla; no se presupone válida para retos nuevos.";
    }
}
