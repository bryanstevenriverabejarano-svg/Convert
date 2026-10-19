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
    public DistributedCompute(Context context) {
        store = new ComputeNodeStore(context);
        router = new ComputeRouter(new ComputeHttpTransport(context), () -> System.nanoTime() / 1_000_000L);
    }
    public boolean enabled() { return store.enabled(); }
    public void setEnabled(boolean enabled) { store.setEnabled(enabled); }
    public String status() { return enabled() ? router.status() : "Nodos desactivados"; }
    public ModelResult generate(String prompt, String role, Supplier<ModelResult> local) {
        if (!enabled() || Looper.myLooper() == Looper.getMainLooper()) return local.get();
        long revision = store.revision();
        List<ComputeNode> nodes = store.loadActive(); // los nodos pausados por el dueño no reciben tareas
        return router.generate(nodes, true, role, prompt,
                () -> Thread.currentThread().isInterrupted() || !store.enabled() || store.revision() != revision, local);
    }
}
