package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Public, bounded controller state shared by successive tool observations.
 * This is a task ledger, not model chain-of-thought and never permission.
 * The question itself is already passed separately as USER_INPUT.
 */
public final class CognitiveCycleFrame {
    public enum Demand { CAUSAL_EXPLANATION, COMPARISON, PROCEDURE, FACT, OPEN_RESPONSE }
    public enum Phase { GROUND, EVALUATE_EVIDENCE, RECOVER, FINISH_WITH_LIMITS }
    public final Demand demand;
    public final Phase phase;
    public final int observed, failed, completed;
    public final boolean memoryContextPresent;
    public final boolean toolBudgetLeft;

    private CognitiveCycleFrame(Demand demand, Phase phase, int observed, int failed, int completed,
                                boolean memoryContextPresent, boolean toolBudgetLeft) {
        this.demand = demand;
        this.phase = phase;
        this.observed = observed;
        this.failed = failed;
        this.completed = completed;
        this.memoryContextPresent = memoryContextPresent;
        this.toolBudgetLeft = toolBudgetLeft;
    }

    public static CognitiveCycleFrame observe(String question, String memory, JsonArray receipts, int toolsLeft) {
        String input = normalize(question);
        Demand demand = input.matches("^(por que|porque|a que se debe|cual es la razon)\\b.*")
                || input.matches(".*\\b(por que|debido a que)\\b.*")
                ? Demand.CAUSAL_EXPLANATION
                : input.matches(".*\\b(diferencia|comparar|compara|mejor que|versus|frente a)\\b.*")
                ? Demand.COMPARISON
                : input.matches("^(como se hace|como puedo|ensename|pasos para|haz|crea)\\b.*")
                ? Demand.PROCEDURE
                : input.matches("^(que|quien|cuando|donde|cual)\\b.*")
                ? Demand.FACT : Demand.OPEN_RESPONSE;
        int failed = 0, completed = 0, observed = 0;
        if (receipts != null) for (JsonElement entry : receipts) {
            if (!entry.isJsonObject()) continue;
            JsonObject receipt = entry.getAsJsonObject();
            if (!receipt.has("status") || !receipt.get("status").isJsonPrimitive()) continue;
            String status = receipt.get("status").getAsString();
            if ("failed".equals(status)) { failed++; observed++; }
            else if ("completed".equals(status)) { completed++; observed++; }
            else if ("handoff".equals(status)) observed++;
        }
        Phase phase = failed > 0 && toolsLeft > 0 ? Phase.RECOVER
                : failed > 0 && toolsLeft <= 0 ? Phase.FINISH_WITH_LIMITS
                : observed > 0 ? Phase.EVALUATE_EVIDENCE : Phase.GROUND;
        return new CognitiveCycleFrame(demand, phase, observed, failed, completed,
                memory != null && !memory.trim().isEmpty()
                        && !memory.startsWith("No consultada") && !memory.startsWith("Cerrada:"),
                toolsLeft > 0);
    }
    public JsonObject toJson() {
        JsonObject out = new JsonObject();
        out.addProperty("demand", demand.name());
        out.addProperty("phase", phase.name());
        out.addProperty("memory_evidence_present_not_verified", memoryContextPresent);
        out.addProperty("tool_receipts", observed);
        out.addProperty("tool_failures", failed);
        out.addProperty("tools_left", toolBudgetLeft);
        out.addProperty("checkpoint", instruction());
        return out;
    }
    public String instruction() {
        switch (phase) {
            case RECOVER: return "La herramienta falló; cambia de estrategia solo si existe otra acción autorizada y útil.";
            case FINISH_WITH_LIMITS: return "No afirmes éxito; describe lo comprobado y lo que falta.";
            case EVALUATE_EVIDENCE: return "Contrasta observaciones con el objetivo, no confundas recibo con éxito.";
            default: return "Primero interpreta objetivo, historial y memoria; después decide si basta responder o necesitas una herramienta.";
        }
    }
    private static String normalize(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('¿', ' ').replaceAll("\\s+", " ").trim();
    }
}
