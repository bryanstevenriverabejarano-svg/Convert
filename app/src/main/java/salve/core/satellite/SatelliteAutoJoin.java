package salve.core.satellite;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Un satélite busca sola en la red local una Salve a la que unirse, sin preguntar nada. Escucha los
 * anuncios {@code _salvenode._tcp} (los mismos que publica el script del nodo del dueño) y, en cuanto
 * encuentra una, la guarda como su Salve. Búsqueda acotada en el tiempo; nada de escanear direcciones.
 *
 * <p>Si no encuentra ninguna, el satélite se queda a la espera (no arranca una Salve propia). El dueño
 * puede unirlo también a mano pegando su enlace de emparejamiento.</p>
 */
public final class SatelliteAutoJoin {
    private static final String TAG = "Salve/Satelite";
    private static final String SERVICE_TYPE = "_salvenode._tcp.";
    private static final long SEARCH_MS = 20_000;

    private final Context app;
    private final SatelliteStore store;
    private final NsdManager manager;
    private final Handler main = new Handler(Looper.getMainLooper());
    private NsdManager.DiscoveryListener discovery;
    private boolean joined;

    public SatelliteAutoJoin(Context context) {
        this.app = context.getApplicationContext();
        this.store = new SatelliteStore(app);
        this.manager = (NsdManager) app.getSystemService(Context.NSD_SERVICE);
    }

    /** Arranca la búsqueda solo si esta instalación es un satélite aún sin Salve asignada. */
    public void startIfPending() {
        if (manager == null || !store.satellitePendingParent()) return;
        try {
            discovery = new NsdManager.DiscoveryListener() {
                @Override public void onDiscoveryStarted(String type) { }
                @Override public void onServiceFound(NsdServiceInfo info) { resolve(info); }
                @Override public void onServiceLost(NsdServiceInfo info) { }
                @Override public void onDiscoveryStopped(String type) { }
                @Override public void onStartDiscoveryFailed(String type, int code) { stop(); }
                @Override public void onStopDiscoveryFailed(String type, int code) { }
            };
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery);
            main.postDelayed(this::stop, SEARCH_MS);
        } catch (RuntimeException unavailable) {
            Log.i(TAG, "Sin descubrimiento de red: el satélite espera a que lo unan a mano.");
        }
    }

    private void resolve(NsdServiceInfo info) {
        try {
            manager.resolveService(info, new NsdManager.ResolveListener() {
                @Override public void onResolveFailed(NsdServiceInfo i, int code) { }
                @Override public void onServiceResolved(NsdServiceInfo resolved) { join(resolved); }
            });
        } catch (RuntimeException busy) { /* otro resolve en curso: el siguiente anuncio reintenta */ }
    }

    private synchronized void join(NsdServiceInfo resolved) {
        if (joined) return;
        try {
            Map<String, byte[]> txt = new HashMap<>();
            if (resolved.getAttributes() != null) txt.putAll(resolved.getAttributes());
            salve.core.compute.NodePairing pairing = salve.core.compute.NodePairing.fromAdvert(resolved.getServiceName(), txt);
            store.becomeSatelliteOf(pairing.name, pairing.endpoint);
            joined = true;
            Log.i(TAG, "Satélite unido a «" + pairing.name + "» por la red local.");
            main.post(this::stop);
        } catch (IllegalArgumentException notSalve) {
            Log.i(TAG, "Anuncio ignorado (no es una Salve compatible): " + notSalve.getMessage());
        }
    }

    public void stop() {
        if (discovery != null && manager != null) {
            try { manager.stopServiceDiscovery(discovery); } catch (RuntimeException ignored) { }
            discovery = null;
        }
    }
}
