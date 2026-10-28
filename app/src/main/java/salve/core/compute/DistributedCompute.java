package salve.core.compute;

import android.content.Context;
import android.os.Looper;
import salve.core.ModelResult;
import java.util.List;
import java.util.function.Supplier;

/** App-owned orchestration; a node receives only the current prompt, never the memory database. */
public final class DistributedCompute {
    private final ComputeNodeStore store;
    private final ComputeRouter router;
    private final ComputeHttpTransport transport;
    private final SatelliteDispatch satellites;
    public DistributedCompute(Context context) {
        store = new ComputeNodeStore(context);
        transport = new ComputeHttpTransport(context);
        router = new ComputeRouter(transport, () -> System.nanoTime() / 1_000_000L);
        satellites = new SatelliteDispatch(transport);
    }
    public boolean enabled() { return store.enabled(); }
    public void setEnabled(boolean enabled) { store.setEnabled(enabled); }
    public String status() { return enabled() ? router.status() : "Nodos desactivados"; }
    public ModelResult generate(String prompt, String role, Supplier<ModelResult> local) {
        if (!enabled() || Looper.myLooper() == Looper.getMainLooper()) return local.get();
        long revision = store.revision();
        List<ComputeNode> nodes = store.loadActive(); // los nodos pausados por el dueño no reciben tareas
        java.util.function.BooleanSupplier stopped =
                () -> Thread.currentThread().isInterrupted() || !store.enabled() || store.revision() != revision;
        // Reparte el cómputo a un satélite (a través del nodo) solo si tú lo usas como respaldo del local.
        return router.generate(nodes, true, role, prompt, stopped,
                () -> {
                    for (ComputeNode node : nodes) {
                        if (stopped.getAsBoolean()) break;
                        ModelResult viaSatellite = satellites.compute(node, prompt, stopped);
                        if (viaSatellite != null) return viaSatellite;
                    }
                    return local.get();
                });
    }
}
