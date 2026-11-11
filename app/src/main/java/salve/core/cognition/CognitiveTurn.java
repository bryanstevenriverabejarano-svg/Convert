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
        private final JsonArray receipts;
        private Outcome(ModelResult result, boolean handedOff, JsonArray receipts) {
            this.result = result;
            this.handedOff = handedOff;
            this.receipts = receipts.deepCopy();
        }
        /** Controller-generated tool receipts, never untrusted model thoughts. */
        public JsonArray receipts() { return receipts.deepCopy(); }
        /** Controller fallback: a factual, locally retrieved answer after model routing failed. */
        public static Outcome groundedText(String text) {
            if (text == null || text.isEmpty()) throw new IllegalArgumentException("Falta evidencia.");
            return new Outcome(ModelResult.success(text, 0), false, new JsonArray());
        }
        /** A controller already initiated the existing asynchronous action safely. */
        public static Outcome controllerHandoff(String toolName) {
            JsonArray receipts = new JsonArray();
            JsonObject receipt = new JsonObject();
            receipt.addProperty("name", toolName);
            receipt.addProperty("status", "handoff");
            receipt.addProperty("data", "Pendiente de resultado externo.");
            receipts.add(receipt);
            return new Outcome(ModelResult.success("", 0), true, receipts);
        }
    }
    private CognitiveTurn() { }
    public static Outcome run(Model model, Map<String, Tool> available, BooleanSupplier cancelled) {
        return run(model, available, cancelled, null);
    }

    /**
     * Cross-mode action contract. In a turn with one explicit authorized goal,
     * merely replying "I did it" cannot count as execution. One semantic retry
     * is allowed; no tool may be invented or repeated to satisfy this contract.
     */
    public static Outcome run(Model model, Map<String, Tool> available, BooleanSupplier cancelled,
                              String requiredTool) {
        Map<String, Tool> tools = new LinkedHashMap<>(available);
        if (requiredTool != null && !tools.containsKey(requiredTool))
            requiredTool = null; // never expand the controller's allowlist
        final String mustRun = requiredTool;
        boolean requiredSucceeded = false;
        JsonArray receipts = new JsonArray();
        Set<String> used = new HashSet<>(); // side-effecting tools never repeat in a turn
        Set<String> webQueries = new HashSet<>(); // only changed public searches may repeat
        Set<String> memoryQueries = new HashSet<>(); // read-only memory probes; controller rechecks scope
        boolean repaired = false, repair = false, toolsClosed = false;
        int actions = 0;
        long deadline = System.nanoTime() + 180_000_000_000L;
        ModelResult last = null;
        for (int call = 0; call < MAX_INFERENCES; call++) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) return stopped(ModelResult.Status.CANCELLED, "Turno cancelado", receipts);
            if (System.nanoTime() >= deadline) return stopped(ModelResult.Status.TIMEOUT, "Me quedé sin tiempo para pensar esta respuesta, así que paré antes de hacer nada más", receipts);
            try { last = model.generate(receipts.deepCopy(), repair, toolsClosed ? 0 : MAX_TOOLS - actions); }
            catch (RuntimeException unavailable) { return stopped(ModelResult.Status.ERROR, "Mi modelo no pudo terminar de pensar esta respuesta", receipts); }
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) return stopped(ModelResult.Status.CANCELLED, "Turno cancelado", receipts);
            if (!last.isSuccess()) return new Outcome(last, false, receipts);
            CognitiveReply reply;
            try { reply = CognitiveReply.parseForConversation(last.getText()); }
            catch (IllegalArgumentException invalid) {
                if (!CognitiveReply.isToolAttempt(last.getText())) {
                    // Una respuesta vacía o ilegible se reintenta una vez antes de rendirse.
                    if (!repaired) { repaired = true; repair = true; continue; }
                    return stopped(ModelResult.Status.ERROR,
                            "Mi respuesta salió vacía o a medias", receipts);
                }
                if (repaired) return stopped(ModelResult.Status.ERROR,
                        "No entendí bien la acción que iba a hacer, así que no la hice", receipts);
                repaired = true; repair = true; continue;
            }
            repair = false;
            if (reply.name == null) {
                if (mustRun != null && !requiredSucceeded) {
                    // The small local model may choose normal prose for an action.
                    // Retry once with TASK_STATE.required_action, then fail honestly.
                    if (!repaired && call < MAX_INFERENCES - 1) {
                        repaired = true;
                        repair = true;
                        continue;
                    }
                    return stopped(ModelResult.Status.ERROR,
                            "Entendí la petición, pero no he ejecutado la herramienta necesaria. "
                            + "Consulta Registro de ejecución o revisa los permisos", receipts);
                }
                return new Outcome(ModelResult.success(reply.message, last.getLatencyMillis())
                        .withProvider(last.getProvider()), false, receipts);
            }
            if (System.nanoTime() >= deadline) return stopped(ModelResult.Status.TIMEOUT, "Me quedé sin tiempo antes de hacerlo, así que no lo hice", receipts);
            Tool tool = tools.get(reply.name);
            if (tool == null) return stopped(ModelResult.Status.ERROR, "Intenté hacer algo que ahora no tengo disponible, así que no lo hice", receipts);
            // A new read-only web query can follow a first result. Never retry
            // the same query and NEVER repeat device/cloud/write operations.
            String publicQuery = null;
            if ("web.search".equals(reply.name) && reply.arguments.has("query")
                    && reply.arguments.get("query").isJsonPrimitive()
                    && reply.arguments.get("query").getAsJsonPrimitive().isString())
                publicQuery = reply.arguments.get("query").getAsString().trim().toLowerCase(java.util.Locale.ROOT);
            boolean newPublicSearch = "web.search".equals(reply.name)
                    && publicQuery != null && publicQuery.length() >= 3
                    && !webQueries.contains(publicQuery);
            String memoryQuery = null;
            if ("memory.lookup".equals(reply.name) && reply.arguments.has("query")
                    && reply.arguments.get("query").isJsonPrimitive()
                    && reply.arguments.get("query").getAsJsonPrimitive().isString())
                memoryQuery = reply.arguments.get("query").getAsString().trim().toLowerCase(java.util.Locale.ROOT);
            boolean newMemoryProbe = "memory.lookup".equals(reply.name)
                    && memoryQuery != null && memoryQuery.length() >= 3
                    && !memoryQueries.contains(memoryQuery);
            if (toolsClosed || actions >= MAX_TOOLS
                    || used.contains(reply.name) && !(newPublicSearch || newMemoryProbe)
                    || call == MAX_INFERENCES - 1) {
                // Pedir otra vez lo que ya tiene (memory.lookup dos veces) no es un fallo para Bryan:
                // se le cierran las herramientas y responde con los resultados que ya tiene.
                if (!toolsClosed && !receipts.isEmpty() && call < MAX_INFERENCES - 1) { toolsClosed = true; continue; }
                return stopped(ModelResult.Status.ERROR, "Me estaba repitiendo, así que paré", receipts);
            }
            used.add(reply.name);
            if (publicQuery != null) webQueries.add(publicQuery);
            if (memoryQuery != null) memoryQueries.add(memoryQuery);
            actions++;
            JsonObject receipt = new JsonObject();
            receipt.addProperty("name", reply.name);
            receipt.addProperty("observed_at_ms", System.currentTimeMillis());
            try {
                Receipt result = tool.operation.execute(reply.arguments.deepCopy());
                if (result.handoff) {
                    if (reply.name.equals(mustRun)) requiredSucceeded = true;
                    receipt.addProperty("status", "handoff");
                    receipt.addProperty("data", "Pendiente de aprobación o resultado externo.");
                    receipts.add(receipt);
                    return new Outcome(ModelResult.success("", last.getLatencyMillis()), true, receipts);
                }
                receipt.addProperty("status", "completed");
                receipt.addProperty("data", result.data);
                if (reply.name.equals(mustRun)) requiredSucceeded = true;
            } catch (RuntimeException failure) {
                // No exception text or model-generated arguments become higher-priority instructions.
                receipt.addProperty("status", "failed");
                receipt.addProperty("data", "El controlador rechazó los argumentos o la operación falló. No hay éxito confirmado.");
                if (reply.name.equals(mustRun)) {
                    receipts.add(receipt);
                    return stopped(ModelResult.Status.ERROR,
                            "El controlador rechazó la operación solicitada. No la he realizado", receipts);
                }
                if ("whatsapp.draft".equals(reply.name)) {
                    // No second LLM response may reinterpret a rejected message draft
                    // as delivered or prepared. The controller owns this statement.
                    receipts.add(receipt);
                    return new Outcome(ModelResult.success(
                            "No preparé el borrador de WhatsApp: los datos no coincidían con tu orden o Android lo rechazó. No se envió nada.",
                            last.getLatencyMillis()), false, receipts);
                }
            }
            receipts.add(receipt);
        }
        return stopped(ModelResult.Status.ERROR, "Lo intenté varias veces y no me salió una respuesta que valga", receipts);
    }
    private static Outcome stopped(ModelResult.Status status, String message, JsonArray receipts) {
        // Bryan lee esto: lo que llegó a hacer, en palabras, nunca nombres internos de herramientas.
        StringBuilder progress = new StringBuilder(message);
        if (!receipts.isEmpty()) {
            java.util.List<String> done = new java.util.ArrayList<>(), failed = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement item : receipts) {
                JsonObject receipt = item.getAsJsonObject();
                String what = humanName(receipt.get("name").getAsString());
                (receipt.get("status").getAsString().equals("completed") ? done : failed).add(what);
            }
            if (!done.isEmpty()) progress.append(". Llegué a ").append(String.join(" y ", done));
            if (!failed.isEmpty()) progress.append(done.isEmpty() ? ". Intenté " : "; intenté ")
                    .append(String.join(" y ", failed)).append(", sin confirmar que saliera bien");
        }
        progress.append('.');
        return new Outcome(ModelResult.failure(status, progress.toString().replace("..", "."), 0), false, receipts);
    }

    /** Nombre de herramienta → lo que hizo, dicho como persona. */
    static String humanName(String tool) {
        switch (tool == null ? "" : tool) {
            case "memory.lookup": return "buscar en mis recuerdos";
            case "whatsapp.draft": return "preparar el borrador de WhatsApp";
            case "web.search": return "buscar en internet";
            case "sketch.draw": return "dibujar";
            case "cloud.sync": return "sincronizar con pCloud";
            case "runtime.status": return "revisar mi estado";
            case "TAP": case "ESCRIBIR": return "preparar una acción en el móvil";
            case "DEPLOY_WEB": return "preparar una web";
            case "AVATAR_LIST": case "AVATAR_WEAR": case "AVATAR_CREATE": return "cambiar mi ropa";
            case "SOLVE_CHALLENGE": return "resolver el reto";
            default: return "usar una herramienta";
        }
    }
}
