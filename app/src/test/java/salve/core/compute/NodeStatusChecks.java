package salve.core.compute;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/** Comprobaciones sin Android ni red: java NodeStatusChecks (ver scripts/test-compute-router.sh). */
public final class NodeStatusChecks {
    private static final ComputeNode PC = new ComputeNode("a", "Mi PC", "https://pc.example", "x".repeat(40));
    private static final ComputeNode TORRE = new ComputeNode("b", "Torre", "https://torre.example", "y".repeat(40));
    private static final ComputeNode LENTO = new ComputeNode("c", "Lento", "https://lento.example", "z".repeat(40));

    public static void main(String[] args) {
        AtomicLong clock = new AtomicLong(1000);
        ComputeRouter.Transport transport = new ComputeRouter.Transport() {
            public ComputeRouter.Health health(ComputeNode n, BooleanSupplier stopped) throws IOException {
                if (n == TORRE) throw new IOException("Nodo no disponible (401)");
                return new ComputeRouter.Health("qwen-7b", 1, 12000, 1024);
            }
            public String generate(ComputeNode n, ComputeRouter.Health h, String id, String role, String p, BooleanSupplier s) {
                throw new AssertionError("Estado nunca genera texto");
            }
        };
        List<NodeStatusCheck.Result> results = NodeStatusCheck.run(Arrays.asList(PC, TORRE), transport, () -> false, clock::get);
        check(results.size() == 2 && results.get(0).name.equals("Mi PC") && results.get(1).name.equals("Torre"), "orden");
        check(results.get(0).online && results.get(0).line().contains("qwen-7b") && results.get(0).line().contains("1 plaza libre"), "online");
        check(!results.get(1).online && results.get(1).problem.contains("token"), "401 → token");
        String summary = NodeStatusCheck.summary(results);
        check(summary.startsWith("1 de 2 nodos responden"), summary);
        check(!summary.contains("x".repeat(40)) && !summary.contains("y".repeat(40)), "sin tokens");

        check(NodeStatusCheck.problem(new IOException("Certificado HTTPS no válido")).contains("certificado"), "tls");
        check(NodeStatusCheck.problem(new IOException("No se encuentra el servidor")).contains("no se encuentra"), "dns");
        check(NodeStatusCheck.problem(new IOException("Nodo no preparado")).contains("modelo no está listo"), "ready");
        check(NodeStatusCheck.problem(new IOException("Nodo no disponible (503)")).contains("falló"), "5xx");
        check(NodeStatusCheck.problem(new IOException("La petición al nodo falló")).contains("sin conexión"), "otro");

        // Un nodo colgado no bloquea el resto: se marca como lento y se libera.
        ComputeRouter.Transport hanging = new ComputeRouter.Transport() {
            public ComputeRouter.Health health(ComputeNode n, BooleanSupplier stopped) throws IOException {
                if (n == LENTO) { try { Thread.sleep(60_000); } catch (InterruptedException stop) { throw new IOException("interrumpido"); } }
                return new ComputeRouter.Health("m", 2, 12000, 1024);
            }
            public String generate(ComputeNode n, ComputeRouter.Health h, String id, String role, String p, BooleanSupplier s) { return ""; }
        };
        long start = System.nanoTime();
        List<NodeStatusCheck.Result> slow = NodeStatusCheck.run(Arrays.asList(PC, LENTO), hanging, () -> false,
                () -> System.nanoTime() / 1_000_000L);
        long ms = (System.nanoTime() - start) / 1_000_000L;
        check(slow.get(0).online && !slow.get(1).online && slow.get(1).problem.contains("a tiempo"), "lento");
        check(ms < 10_000, "presupuesto acotado: " + ms + " ms");
        check(NodeStatusCheck.run(java.util.Collections.emptyList(), hanging, () -> false, clock::get).isEmpty(), "vacío");
        System.out.println("NodeStatusCheck: OK");
    }

    private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
}
