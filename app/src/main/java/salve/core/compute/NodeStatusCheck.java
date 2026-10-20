package salve.core.compute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * Comprobación de todos los nodos solo cuando el dueño la pide (botón Estado). No hay sondeo
 * periódico: fuera de esto, un nodo solo se consulta justo antes de delegarle una tarea.
 * Usa el mismo transporte que las tareas (HTTPS verificado + token), en paralelo y acotado.
 */
public final class NodeStatusCheck {
    private static final long BUDGET_MS = 6_000;

    public static final class Result {
        public final String name, model, problem;
        public final boolean online;
        public final int freeSlots;
        public final long millis;
        /** GPU/VRAM y roles informados por el nodo; vacíos si no los da. */
        public String telemetry = "";
        public java.util.List<String> roles = java.util.Collections.emptyList();

        Result(String name, boolean online, String model, int freeSlots, long millis, String problem) {
            this.name = name; this.online = online; this.model = model;
            this.freeSlots = freeSlots; this.millis = millis; this.problem = problem;
        }

        public String line() {
            if (!online) return "✗ " + name + " · " + problem;
            String slots = freeSlots == 0 ? "ocupado" : freeSlots == 1 ? "1 plaza libre" : freeSlots + " plazas libres";
            return "✓ " + name + " · " + model + " · " + slots + " · " + millis + " ms";
        }
    }

    private NodeStatusCheck() { }

    /** Resultados en el mismo orden que {@code nodes}. Nunca lanza: un nodo caído es un resultado. */
    public static List<Result> run(List<ComputeNode> nodes, ComputeRouter.Transport transport,
                                   BooleanSupplier stopped, LongSupplier clockMs) {
        if (nodes == null || nodes.isEmpty()) return Collections.emptyList();
        List<ComputeNode> bounded = nodes.subList(0, Math.min(nodes.size(), ComputeRouter.MAX_NODES));
        ExecutorService pool = Executors.newFixedThreadPool(bounded.size(), r -> {
            Thread thread = new Thread(r, "salve-node-status"); thread.setDaemon(true); return thread;
        });
        try {
            List<Future<Result>> pending = new ArrayList<>();
            for (ComputeNode node : bounded) pending.add(pool.submit(() -> check(node, transport, stopped, clockMs)));
            List<Result> results = new ArrayList<>();
            long deadline = clockMs.getAsLong() + BUDGET_MS;
            for (int i = 0; i < pending.size(); i++) {
                ComputeNode node = bounded.get(i);
                try {
                    long wait = Math.max(1, deadline - clockMs.getAsLong());
                    results.add(pending.get(i).get(wait, TimeUnit.MILLISECONDS));
                } catch (java.util.concurrent.TimeoutException slow) {
                    pending.get(i).cancel(true);
                    results.add(new Result(node.name, false, "", 0, 0, "no respondió a tiempo"));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    results.add(new Result(node.name, false, "", 0, 0, "comprobación cancelada"));
                } catch (ExecutionException | CancellationException failed) {
                    results.add(new Result(node.name, false, "", 0, 0, "no se pudo comprobar"));
                }
            }
            return results;
        } finally { pool.shutdownNow(); }
    }

    static Result check(ComputeNode node, ComputeRouter.Transport transport, BooleanSupplier stopped, LongSupplier clockMs) {
        long start = clockMs.getAsLong();
        try {
            ComputeRouter.Health health = transport.health(node, stopped);
            Result ok = new Result(node.name, true, health.model, health.freeSlots, Math.max(0, clockMs.getAsLong() - start), "");
            ok.telemetry = health.telemetry(); ok.roles = health.roles;
            return ok;
        } catch (IOException | RuntimeException failure) {
            return new Result(node.name, false, "", 0, 0, problem(failure));
        }
    }

    /** Motivo comprensible a partir de los mensajes fijos del transporte (sin datos sensibles). */
    static String problem(Exception failure) {
        String message = failure.getMessage() == null ? "" : failure.getMessage();
        if (failure instanceof CancellationException) return "comprobación cancelada";
        if (message.contains("No hay conexión")) return "no hay conexión (¿PC encendido, Salve Nodo abierto y puerto permitido?)";
        if (message.contains("perfil")) return "el certificado del perfil no es válido: vuelve a escanear el QR del nodo";
        if (message.contains("(401)") || message.contains("(403)")) return "token rechazado: vuelve a escanear el QR del nodo";
        if (message.contains("Certificado")) return "certificado no aceptado en el TLS mutuo: vuelve a escanear el QR del nodo";
        if (message.contains("No se encuentra")) return "no se encuentra el servidor (¿PC encendido o dominio DDNS correcto?)";
        if (message.contains("no preparado")) return "servicio activo, pero el modelo no está listo (¿llama-server en marcha?)";
        if (message.matches(".*\\((?:5[0-9]{2})\\).*")) return "el servicio del nodo falló";
        if (message.contains("Tiempo")) return "no respondió a tiempo";
        return "sin conexión con el servicio del nodo";
    }

    public static String summary(List<Result> results) {
        if (results.isEmpty()) return "No hay nodos registrados.";
        int online = 0;
        StringBuilder out = new StringBuilder();
        for (Result r : results) { if (r.online) online++; out.append(r.line()).append('\n'); }
        return online + " de " + results.size() + " nodos responden ahora.\n\n" + out.toString().trim();
    }
}
