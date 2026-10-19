package salve.core.compute;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import salve.core.ModelResult;

/** Bounded whole-task offload. No executable tasks, provisioning, weights or memory replication. */
public final class ComputeRouter {
    public static final int MAX_NODES = 4, MAX_PROMPT_CHARS = 48000, MAX_OUTPUT_TOKENS = 2048;
    public static final Set<String> ROLES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "CONVERSACIONAL", "REFLEXION", "SISTEMA", "PLANIFICADOR", "OBSERVADOR", "SINTETIZADOR",
            "EVALUADOR", "CREADOR", "CURIOSO", "NARRADOR", "SALVENTE")));
    public static final class Health {
        public final String model;
        public final int freeSlots, maxPromptChars, maxOutputTokens;
        public Health(String model, int freeSlots, int maxPromptChars, int maxOutputTokens) {
            if (model == null || !model.matches("[^\\p{Cntrl}]{1,120}") || freeSlots < 0 || freeSlots > 32
                    || maxPromptChars < 1 || maxPromptChars > MAX_PROMPT_CHARS
                    || maxOutputTokens < 1 || maxOutputTokens > MAX_OUTPUT_TOKENS)
                throw new IllegalArgumentException("Capacidad inválida");
            this.model = model; this.freeSlots = freeSlots;
            this.maxPromptChars = maxPromptChars; this.maxOutputTokens = maxOutputTokens;
        }
    }
    public interface Transport {
        Health health(ComputeNode node, BooleanSupplier stopped) throws IOException;
        String generate(ComputeNode node, Health health, String id, String role, String prompt,
                        BooleanSupplier stopped) throws IOException;
    }
    private final Transport transport;
    private final LongSupplier clock;
    private final Map<String, Long> retryAfter = new HashMap<>();
    private volatile String status = "Nodos: sin tareas delegadas";
    public ComputeRouter(Transport transport, LongSupplier clock) { this.transport = transport; this.clock = clock; }
    public String status() { return status; }
    public static boolean heavy(String role, int promptChars) {
        return promptChars >= 6000 || Arrays.asList("NARRADOR", "REFLEXION", "SINTETIZADOR", "EVALUADOR", "CREADOR", "SALVENTE").contains(role);
    }
    public synchronized ModelResult generate(List<ComputeNode> nodes, boolean enabled, String role, String prompt,
            BooleanSupplier stopped, Supplier<ModelResult> local) {
        long start = clock.getAsLong();
        if (stopped.getAsBoolean() || Thread.currentThread().isInterrupted()) return cancelled();
        if (!enabled || !ROLES.contains(role) || prompt == null || prompt.trim().isEmpty()
                || !heavy(role, prompt.length()) || prompt.length() > MAX_PROMPT_CHARS) return local.get();
        retryAfter.keySet().retainAll(nodeIds(nodes));
        ComputeNode selected = null; Health selectedHealth = null;
        try {
            int visited = 0;
            for (ComputeNode node : nodes) {
                if (stopped.getAsBoolean()) throw new CancellationException();
                if (++visited > MAX_NODES) break;
                if (retryAfter.getOrDefault(node.id, 0L) > clock.getAsLong()) continue;
                try {
                    Health health = transport.health(node, stopped);
                    if (health.freeSlots > 0 && prompt.length() <= health.maxPromptChars
                            && (selectedHealth == null || health.freeSlots > selectedHealth.freeSlots)) {
                        selected = node; selectedHealth = health;
                    }
                } catch (IOException | IllegalArgumentException unavailable) {
                    retryAfter.put(node.id, clock.getAsLong() + 60_000);
                }
            }
            if (selected != null) {
                if (stopped.getAsBoolean()) throw new CancellationException();
                status = "Nodo: " + selected.name + " · procesando";
                String text = transport.generate(selected, selectedHealth, UUID.randomUUID().toString(), role, prompt, stopped);
                if (stopped.getAsBoolean()) throw new CancellationException();
                if (text == null || text.trim().isEmpty() || text.length() > 64000) throw new IOException("Respuesta inválida");
                status = "Nodo: " + selected.name + " · respuesta recibida";
                return ModelResult.success(text, clock.getAsLong() - start)
                        .withProvider("Nodo " + selected.name + " / " + selectedHealth.model);
            }
        } catch (CancellationException cancelled) { status = "Nodos: tarea cancelada"; return cancelled(); }
        catch (IOException | IllegalArgumentException failure) {
            if (selected != null) retryAfter.put(selected.id, clock.getAsLong() + 60_000);
            // No second inference on another node: a timed-out request might still be finishing.
        }
        if (stopped.getAsBoolean() || Thread.currentThread().isInterrupted()) return cancelled();
        status = "Nodos: sin resultado disponible · respaldo en el móvil";
        return local.get();
    }
    private static Set<String> nodeIds(List<ComputeNode> nodes) {
        Set<String> ids = new HashSet<>(); for (ComputeNode node : nodes) ids.add(node.id); return ids;
    }
    private static ModelResult cancelled() { return ModelResult.failure(ModelResult.Status.CANCELLED, "Tarea cancelada", 0); }
}
