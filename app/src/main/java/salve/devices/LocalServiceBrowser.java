package salve.devices;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ext.SdkExtensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded discovery of advertised HTTP(S) services. No address or port scanning. */
public final class LocalServiceBrowser {
    public interface Listener {
        void onStatus(String message);
        void onServices(List<Service> services);
    }
    public interface Resolution { void onResolved(String url); void onError(String message); }

    public static final class Service {
        private final NsdServiceInfo info;
        private Service(NsdServiceInfo info) { this.info = info; }
        public String getName() { return info.getServiceName(); }
        public String getType() { return info.getServiceType(); }
        private String key() { return getName() + "|" + getType(); }
    }

    private final NsdManager manager;
    private final WifiManager wifi;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Service> services = new LinkedHashMap<>();
    private final List<NsdManager.DiscoveryListener> discoveries = new ArrayList<>();
    private int generation;
    private int resolution;
    private boolean resolving;
    private Runnable searchTimeout;
    private Runnable resolveTimeout;
    private NsdManager.ResolveListener activeResolver;
    private WifiManager.MulticastLock multicastLock;

    public LocalServiceBrowser(Context context, Listener listener) {
        manager = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
        wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.listener = listener;
    }

    public void start() {
        close();
        services.clear();
        listener.onServices(new ArrayList<>());
        if (manager == null) { listener.onStatus("Este dispositivo no ofrece descubrimiento de servicios."); return; }
        if (!acquireMulticast()) { listener.onStatus("Android no permitió recibir anuncios de la red local."); return; }
        final int search = generation;
        listener.onStatus("Buscando servicios HTTP y HTTPS anunciados en la red durante 12 segundos…");
        discover("_http._tcp.", search);
        discover("_https._tcp.", search);
        releaseMulticastIfIdle();
        searchTimeout = () -> {
            if (generation != search) return;
            stopDiscovery();
            listener.onStatus(services.isEmpty()
                    ? "Búsqueda terminada. No se encontraron servicios web anunciados; esto no significa que no haya dispositivos."
                    : "Búsqueda terminada: " + services.size() + " servicios. Elige uno para revisar su dirección.");
        };
        main.postDelayed(searchTimeout, 12_000);
    }

    private void discover(String type, int search) {
        NsdManager.DiscoveryListener discovery = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String serviceType) { }
            @Override public void onDiscoveryStopped(String serviceType) { }
            @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                main.post(() -> {
                    discoveries.remove(this);
                    releaseMulticastIfIdle();
                    if (generation == search) listener.onStatus("No se pudo buscar " + serviceType + " (" + errorCode + ").");
                });
            }
            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                main.post(() -> {
                    if (generation == search) listener.onStatus("Android no pudo detener una búsqueda (" + errorCode + ").");
                });
            }
            @Override public void onServiceFound(NsdServiceInfo serviceInfo) {
                main.post(() -> {
                    if (generation != search || !discoveries.contains(this) || services.size() >= 30) return;
                    Service service = new Service(serviceInfo);
                    services.put(service.key(), service);
                    listener.onServices(new ArrayList<>(services.values()));
                });
            }
            @Override public void onServiceLost(NsdServiceInfo serviceInfo) {
                main.post(() -> {
                    if (generation != search || !discoveries.contains(this)) return;
                    services.remove(new Service(serviceInfo).key());
                    listener.onServices(new ArrayList<>(services.values()));
                });
            }
        };
        discoveries.add(discovery);
        try { manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, discovery); }
        catch (RuntimeException error) {
            discoveries.remove(discovery);
            listener.onStatus("No se pudo iniciar la búsqueda local. Comprueba la conexión y los permisos de Android.");
        }
    }

    public void resolve(Service service, Resolution callback) {
        if (manager == null || service == null || !services.containsKey(service.key())) {
            callback.onError("El servicio ya no está disponible en esta búsqueda."); return;
        }
        if (resolving) { callback.onError("Espera a que termine la consulta anterior."); return; }
        if (!acquireMulticast()) { callback.onError("Android no permitió consultar el servicio local."); return; }
        resolving = true;
        final int request = ++resolution;
        final int search = generation;
        resolveTimeout = () -> {
            if (generation == search && resolution == request && resolving) {
                resolving = false;
                resolution++;
                cancelResolution();
                releaseMulticastIfIdle();
                callback.onError("El servicio no respondió. Puedes volver a buscarlo.");
            }
        };
        main.postDelayed(resolveTimeout, 10_000);
        try {
            activeResolver = new NsdManager.ResolveListener() {
                @Override public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                    main.post(() -> {
                        if (!finishResolution(search, request)) return;
                        callback.onError("No se pudo consultar el servicio (" + errorCode + ").");
                    });
                }
                @Override public void onServiceResolved(NsdServiceInfo info) {
                    main.post(() -> {
                        if (!finishResolution(search, request)) return;
                        try { callback.onResolved(LocalServiceEndpoint.url(info.getServiceType(), info.getHost(), info.getPort())); }
                        catch (IllegalArgumentException error) { callback.onError(error.getMessage()); }
                    });
                }
            };
            manager.resolveService(service.info, activeResolver);
        } catch (RuntimeException error) {
            finishResolution(search, request);
            callback.onError("Android no pudo consultar este servicio.");
        }
    }

    private boolean finishResolution(int search, int request) {
        if (generation != search || resolution != request || !resolving) return false;
        resolving = false;
        activeResolver = null;
        if (resolveTimeout != null) main.removeCallbacks(resolveTimeout);
        resolveTimeout = null;
        releaseMulticastIfIdle();
        return true;
    }

    private void stopDiscovery() {
        if (searchTimeout != null) main.removeCallbacks(searchTimeout);
        searchTimeout = null;
        List<NsdManager.DiscoveryListener> previous = new ArrayList<>(discoveries);
        discoveries.clear();
        if (manager != null) for (NsdManager.DiscoveryListener discovery : previous) {
            try { manager.stopServiceDiscovery(discovery); } catch (RuntimeException ignored) { }
        }
        releaseMulticastIfIdle();
    }

    public void close() {
        generation++;
        resolution++;
        resolving = false;
        cancelResolution();
        if (resolveTimeout != null) main.removeCallbacks(resolveTimeout);
        resolveTimeout = null;
        stopDiscovery();
    }

    private void cancelResolution() {
        NsdManager.ResolveListener previous = activeResolver;
        activeResolver = null;
        if (manager != null && previous != null && Build.VERSION.SDK_INT >= 34) {
            try { manager.stopServiceResolution(previous); } catch (RuntimeException ignored) { }
        }
        // Older Android releases have no cancellation API; generation checks discard late callbacks.
    }

    private boolean acquireMulticast() {
        if (Build.VERSION.SDK_INT >= 33 && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7) return true;
        if (multicastLock != null && multicastLock.isHeld()) return true;
        if (wifi == null) return false;
        try {
            multicastLock = wifi.createMulticastLock("Salve:LocalServiceBrowser");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
            return true;
        } catch (RuntimeException error) { multicastLock = null; return false; }
    }

    private void releaseMulticastIfIdle() {
        if (resolving || !discoveries.isEmpty()) return;
        WifiManager.MulticastLock previous = multicastLock;
        multicastLock = null;
        if (previous != null) {
            try { if (previous.isHeld()) previous.release(); } catch (RuntimeException ignored) { }
        }
    }
}
