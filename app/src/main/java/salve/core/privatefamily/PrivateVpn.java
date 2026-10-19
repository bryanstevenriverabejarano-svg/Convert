package salve.core.privatefamily;

import android.content.Context;
import android.net.*;
import com.wireguard.android.backend.*;
import com.wireguard.config.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.*;
import okhttp3.*;

/** Only this app's tunnel and exact configured /32 peers can carry private requests. */
public final class PrivateVpn implements Tunnel {
    public static final ExecutorService CONTROL = Executors.newSingleThreadExecutor();
    private static PrivateVpn instance;
    private final Context context;
    private GoBackend backend;
    private final AtomicLong epoch = new AtomicLong();
    private volatile boolean up;
    private volatile Config active;
    private volatile String status = "VPN desconectada";
    private PrivateVpn(Context context) { this.context = context.getApplicationContext(); }
    public static synchronized PrivateVpn get(Context context) {
        if (instance == null) instance = new PrivateVpn(context); return instance;
    }
    @Override public String getName() { return "SalveFamilia"; }
    @Override public void onStateChange(State state) { up = state == State.UP; epoch.incrementAndGet(); status = up ? "VPN conectada" : "VPN desconectada"; }
    public void invalidate() { up = false; epoch.incrementAndGet(); }
    public String status() { return status; }
    public boolean isUp() { return up; }
    public void connect() throws Exception {
        FamilyVault.Data data = new FamilyVault(context).read();
        Config config = validate(data.config, context.getPackageName()); trust(data.ca);
        if (backend == null) backend = new GoBackend(context);
        active = config; backend.setState(this, State.UP, config);
    }
    public void disconnect() throws Exception { invalidate(); if (backend != null) backend.setState(this, State.DOWN, null); active = null; status = "VPN desconectada"; }
    public void failed() { invalidate(); status = "No se pudo conectar la VPN; revisa el perfil y el permiso de Android"; }
    public static Config validate(String text, String packageName) throws Exception {
        if (text == null || text.length() > 32768) throw new IllegalArgumentException("Perfil demasiado grande");
        Config config = Config.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        com.wireguard.config.Interface original = config.getInterface();
        if (original.getAddresses().size() != 1 || !original.getDnsServers().isEmpty() || !original.getDnsSearchDomains().isEmpty()
                || !original.getExcludedApplications().isEmpty() || !original.getIncludedApplications().isEmpty()
                || config.getPeers().isEmpty() || config.getPeers().size() > 4) throw new IllegalArgumentException("Perfil privado: una dirección, 1–4 peers y sin DNS ni listas de apps");
        InetNetwork own = original.getAddresses().iterator().next();
        if (!PrivatePolicy.privateIp(own.getAddress().getHostAddress()) || own.getMask() != 32) throw new IllegalArgumentException("La dirección propia debe ser privada /32");
        Set<String> routes = new HashSet<>();
        for (Peer peer : config.getPeers()) {
            if (peer.getAllowedIps().isEmpty()) throw new IllegalArgumentException("Cada peer necesita AllowedIPs privadas /32");
            for (InetNetwork route : peer.getAllowedIps()) {
                String host = route.getAddress().getHostAddress();
                if (!PrivatePolicy.privateIp(host) || route.getMask() != 32 || host.equals(own.getAddress().getHostAddress()) || !routes.add(host))
                    throw new IllegalArgumentException("Solo rutas privadas /32 distintas de la dirección propia");
            }
            // Numeric underlay endpoint: never ask a DNS resolver to locate a family node.
            if (peer.getEndpoint().isPresent() && (!PrivatePolicy.ipv4(peer.getEndpoint().get().getHost()) || peer.getEndpoint().get().getPort() == 0))
                throw new IllegalArgumentException("El endpoint UDP de WireGuard debe ser una IPv4 numérica");
        }
        if (routes.size() > 4) throw new IllegalArgumentException("Máximo cuatro direcciones de nodos");
        com.wireguard.config.Interface.Builder ownApp = new com.wireguard.config.Interface.Builder()
                .setKeyPair(original.getKeyPair()).addAddresses(original.getAddresses()).includeApplication(packageName);
        if (original.getListenPort().isPresent()) ownApp.setListenPort(original.getListenPort().get());
        if (original.getMtu().isPresent()) ownApp.setMtu(original.getMtu().get());
        return new Config.Builder().setInterface(ownApp.build()).addPeers(config.getPeers()).build();
    }
    private Network network() throws IOException {
        Config config = active;
        if (!up || config == null) throw new IOException("VPN desconectada");
        InetAddress own = config.getInterface().getAddresses().iterator().next().getAddress();
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        Network[] networks = manager.getAllNetworks();
        boolean physical = false;
        for (Network candidate : networks) {
            NetworkCapabilities caps = manager.getNetworkCapabilities(candidate);
            if (caps != null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) physical = true;
        }
        if (!physical) throw new IOException("Sin conexión física para el túnel");
        for (Network network : networks) {
            NetworkCapabilities caps = manager.getNetworkCapabilities(network); LinkProperties links = manager.getLinkProperties(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && links != null)
                for (LinkAddress address : links.getLinkAddresses()) if (address.getAddress().equals(own)) return network;
        }
        throw new IOException("El túnel privado no está disponible");
    }
    public boolean available() { try { network(); return true; } catch (IOException error) { return false; } }
    public final class Lease {
        public final OkHttpClient client;
        private final long generation;
        private final Network selected;
        private Lease(OkHttpClient client, Network selected, long generation) { this.client = client; this.selected = selected; this.generation = generation; }
        public boolean valid() { try { return up && generation == epoch.get() && selected.equals(network()); } catch (IOException error) { return false; } }
        public void close() { client.dispatcher().cancelAll(); client.connectionPool().evictAll(); client.dispatcher().executorService().shutdown(); }
    }
    public Lease lease(String endpoint) throws IOException {
        try {
            String host = PrivatePolicy.endpointHost(endpoint); long generation = epoch.get(); Network selected = network();
            boolean allowed = false;
            Config config = active; if (config == null) throw new IOException("VPN desconectada");
            for (Peer peer : config.getPeers()) for (InetNetwork route : peer.getAllowedIps()) if (host.equals(route.getAddress().getHostAddress())) allowed = true;
            if (!allowed) throw new IOException("La dirección no está autorizada en WireGuard");
            X509TrustManager manager = trust(new FamilyVault(context).read().ca);
            SSLContext tls = SSLContext.getInstance("TLS"); tls.init(null, new TrustManager[]{manager}, null);
            OkHttpClient client = new OkHttpClient.Builder().socketFactory(selected.getSocketFactory())
                    .sslSocketFactory(tls.getSocketFactory(), manager).proxy(Proxy.NO_PROXY)
                    .dns(name -> { if (!host.equals(name)) throw new java.net.UnknownHostException(); return Collections.singletonList(InetAddress.getByName(host)); })
                    .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                    .connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
                    .connectTimeout(3, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS).build();
            Lease lease = new Lease(client, selected, generation);
            if (!lease.valid()) { lease.close(); throw new IOException("VPN desconectada"); } return lease;
        } catch (IOException error) { throw error; }
        catch (Exception invalid) { throw new IOException("Revisa VPN, dirección privada y CA del nodo"); }
    }
    public static X509TrustManager trust(String pem) throws Exception {
        if (pem == null || pem.length() > 65536) throw new IllegalArgumentException("CA inválida");
        Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
        if (certs.isEmpty() || certs.size() > 4) throw new IllegalArgumentException("Importa 1–4 certificados CA de tus nodos");
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType()); store.load(null); int i = 0;
        for (Certificate cert : certs) {
            X509Certificate ca = (X509Certificate) cert; ca.checkValidity();
            if (ca.getBasicConstraints() < 0) throw new IllegalArgumentException("Se necesita la CA, no el certificado del servidor");
            store.setCertificateEntry("node" + i++, ca);
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); factory.init(store);
        for (TrustManager manager : factory.getTrustManagers()) if (manager instanceof X509TrustManager) return (X509TrustManager) manager;
        throw new IllegalArgumentException("CA inválida");
    }
}
