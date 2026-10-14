package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import salve.core.ModelResult;

/** Bounded tool loop. The controller owns permissions, receipts and budgets, never the model. */
public final class CognitiveTurn {
    public static final int MAX_TOOLS = 2, MAX_INFERENCES = 4;
    public interface Model { ModelResult generate(JsonArray results, boolean repair, int callsLeft); }
    public interface Operation { Receipt execute(JsonObject arguments); }
    public static final class Tool {
        public final String description;
        private final Operation operation;
        public Tool(String description, Operation operation) { this.description = description; this.operation = operation; }
    }
    public static final class Receipt {
        public final String data;
        public final boolean handoff;
        public Receipt(String data) { this(data, false); }
        private Receipt(String data, boolean handoff) { this.data = data; this.handoff = handoff; }
        /** Existing asynchronous approval/result UI owns completion; do not ask the model to claim success. */
        public static Receipt handoff() { return new Receipt("Pendiente del controlador", true); }
    }
    public static final class Outcome {
        public final ModelResult result;
        public final boolean handedOff;
        private Outcome(ModelResult result, boolean handedOff) { this.result = result; this.handedOff = handedOff; }
    }
    private CognitiveTurn() { }
    public static Outcome run(Model model, Map<String, Tool> available, BooleanSupplier cancelled) {
        Map<String, Tool> tools = new LinkedHashMap<>(available);
        JsonArray receipts = new JsonArray();
        Set<String> used = new HashSet<>(); // one invocation per tool per turn; no repeated failed side effects
        boolean repaired = false, repair = false;
        int actions = 0;
        long deadline = System.nanoTime() + 180_000_000_000L;
        ModelResult last = null;
        for (int call = 0; call < MAX_INFERENCES; call++) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) return stopped(ModelResult.Status.CANCELLED, "Turno cancelado", receipts);
            if (System.nanoTime() >= deadline) return stopped(ModelResult.Status.TIMEOUT, "Se agotó el presupuesto del turno; no se iniciaron más acciones", receipts);
            try { last = model.generate(receipts.deepCopy(), repair, MAX_TOOLS - actions); }
            catch (RuntimeException unavailable) { return stopped(ModelResult.Status.ERROR, "No pude preparar o completar la inferencia", receipts); }
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) return stopped(ModelResult.Status.CANCELLED, "Turno cancelado", receipts);
            if (!last.isSuccess()) return new Outcome(last, false);
            CognitiveReply reply;
            try { reply = CognitiveReply.parse(last.getText()); }
            catch (IllegalArgumentException invalid) {
                if (repaired) return stopped(ModelResult.Status.ERROR, "El modelo no respetó el contrato JSON tras una corrección. No ejecuté esa salida", receipts);
                repaired = true; repair = true; continue;
            }
            repair = false;
            if (reply.name == null) return new Outcome(ModelResult.success(reply.message, last.getLatencyMillis())
                    .withProvider(last.getProvider()), false);
            if (System.nanoTime() >= deadline) return stopped(ModelResult.Status.TIMEOUT, "Se agotó el presupuesto antes de ejecutar la acción", receipts);
            Tool tool = tools.get(reply.name);
            if (tool == null) return stopped(ModelResult.Status.ERROR, "La herramienta solicitada no está disponible en este turno; no la ejecuté", receipts);
            if (actions >= MAX_TOOLS || !used.add(reply.name) || call == MAX_INFERENCES - 1)
                return stopped(ModelResult.Status.ERROR, "Detuve las solicitudes repetidas o sin presupuesto", receipts);
            actions++;
            JsonObject receipt = new JsonObject();
            receipt.addProperty("name", reply.name);
            receipt.addProperty("observed_at_ms", System.currentTimeMillis());
            try {
                Receipt result = tool.operation.execute(reply.arguments.deepCopy());
                if (result.handoff) return new Outcome(ModelResult.success("", last.getLatencyMillis()), true);
                receipt.addProperty("status", "completed");
                receipt.addProperty("data", result.data);
            } catch (RuntimeException failure) {
                // No exception text or model-generated arguments become higher-priority instructions.
                receipt.addProperty("status", "failed");
                receipt.addProperty("data", "El controlador rechazó los argumentos o la operación falló. No hay éxito confirmado.");
            }
            receipts.add(receipt);
        }
        return stopped(ModelResult.Status.ERROR, "Se agotaron las inferencias del turno sin una respuesta válida", receipts);
    }
    private static Outcome stopped(ModelResult.Status status, String message, JsonArray receipts) {
        StringBuilder progress = new StringBuilder(message);
        if (!receipts.isEmpty()) {
            progress.append(". Pasos comprobados:");
            for (com.google.gson.JsonElement item : receipts) {
                JsonObject receipt = item.getAsJsonObject();
                progress.append(' ').append(receipt.get("name").getAsString()).append(": ")
                        .append(receipt.get("status").getAsString().equals("completed") ? "completado" : "sin éxito confirmado").append(';');
            }
        }
        return new Outcome(ModelResult.failure(status, progress.toString(), 0), false);
    }
}
