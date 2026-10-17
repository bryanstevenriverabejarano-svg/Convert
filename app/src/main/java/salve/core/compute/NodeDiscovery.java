package salve.core.compute;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Busca en la red local los nodos que el dueño ya ha anunciado con
 * `salve_node_setup.py anunciar`. Solo propone datos para el formulario: no guarda,
 * no activa y no envía el token a nadie. Cualquier equipo de la red puede anunciarse,
 * así que la UI pide confirmar que el nodo es propio.
 */
public final class NodeDiscovery {
    public interface Listener {
        void onFound(NodePairing node);
        void onStopped(String problem); // null si se detuvo sin error
    }

    private final NsdManager nsd;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayDeque<NsdServiceInfo> pending = new ArrayDeque<>();
    private final Set<String> seen = new HashSet<>();
    private NsdManager.DiscoveryListener discovery;
    private Listener listener;
    private boolean resolving;

    public NodeDiscovery(Context context) {
        nsd = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
    }

    public void start(Listener listener) {
        if (discovery != null) return;
        this.listener = listener;
        discovery = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String type) { }
            @Override public void onDiscoveryStopped(String type) { }
            @Override public void onServiceFound(NsdServiceInfo info) { main.post(() -> enqueue(info)); }
            @Override public void onServiceLost(NsdServiceInfo info) { }
            @Override public void onStartDiscoveryFailed(String type, int code) {
                main.post(() -> finish("No se pudo buscar en la red (código " + code + ")"));
            }
            @Override public void onStopDiscoveryFailed(String type, int code) { }
        };
        try {
            nsd.discoverServices(NodePairing.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery);
        } catch (RuntimeException error) {
            finish("No se pudo buscar en la red");
        }
    }

    public void stop() { finish(null); }

    private void finish(String problem) {
        NsdManager.DiscoveryListener current = discovery;
        Listener notify = listener;
        discovery = null; listener = null; pending.clear(); resolving = false;
        if (current != null) {
            try { nsd.stopServiceDiscovery(current); } catch (RuntimeException ignored) { }
        }
        if (notify != null) notify.onStopped(problem);
    }

    private void enqueue(NsdServiceInfo info) {
        if (discovery == null || !seen.add(info.getServiceName())) return;
        pending.add(info);
        resolveNext();
    }

    /** Antes de Android 14 solo se admite una resolución a la vez: se hace en cola. */
    @SuppressWarnings("deprecation")
    private void resolveNext() {
        if (resolving || discovery == null || pending.isEmpty()) return;
        resolving = true;
        NsdServiceInfo next = pending.poll();
        try {
            nsd.resolveService(next, new NsdManager.ResolveListener() {
                @Override public void onResolveFailed(NsdServiceInfo info, int code) { main.post(() -> done(null)); }
                @Override public void onServiceResolved(NsdServiceInfo info) { main.post(() -> done(info)); }
            });
        } catch (RuntimeException error) {
            done(null);
        }
    }

    private void done(NsdServiceInfo info) {
        resolving = false;
        if (info != null && listener != null) {
            try { listener.onFound(NodePairing.fromAdvert(info.getServiceName(), info.getAttributes())); }
            catch (IllegalArgumentException ignored) { /* anuncio ajeno o inválido: se ignora */ }
        }
        resolveNext();
    }
}
