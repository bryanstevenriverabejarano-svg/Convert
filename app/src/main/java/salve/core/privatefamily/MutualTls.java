package salve.core.privatefamily;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.OkHttpClient;

/**
 * Cliente HTTPS con TLS mutuo para un nodo: presenta el certificado del móvil y solo confía en la
 * CA de ese nodo (no en las CA públicas). Sin proxy, redirecciones ni reintentos automáticos.
 */
public final class MutualTls {
    private MutualTls() { }

    public static OkHttpClient client(FamilyVault.Credential credential) throws Exception {
        Base64.Decoder b64 = Base64.getDecoder();
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate ca = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(b64.decode(credential.ca)));
        X509Certificate mine = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(b64.decode(credential.cert)));
        ca.checkValidity(); mine.checkValidity();
        if (ca.getBasicConstraints() < 0) throw new IllegalArgumentException("El perfil no trae una CA");
        PrivateKey key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(b64.decode(credential.key)));

        char[] none = new char[0];
        KeyStore identity = KeyStore.getInstance("PKCS12"); identity.load(null, none);
        identity.setKeyEntry("movil", key, none, new Certificate[]{mine, ca});
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(identity, none);

        KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType()); trusted.load(null, null);
        trusted.setCertificateEntry("nodo-ca", ca);
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trusted);
        X509TrustManager manager = null;
        for (TrustManager candidate : trust.getTrustManagers()) if (candidate instanceof X509TrustManager) manager = (X509TrustManager) candidate;
        if (manager == null) throw new IllegalStateException("Sin gestor de confianza");

        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(keys.getKeyManagers(), new TrustManager[]{manager}, null);
        return new OkHttpClient.Builder()
                .sslSocketFactory(tls.getSocketFactory(), manager)
                .proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                .connectionPool(new ConnectionPool(1, 30, TimeUnit.SECONDS))
                .connectTimeout(3, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS)
                .dns(MutualTls::checkedLookup)
                .build();
    }

    /** Nunca hacia el propio móvil ni direcciones especiales; IP privadas, públicas y DDNS sí. */
    static List<InetAddress> checkedLookup(String host) throws UnknownHostException {
        List<InetAddress> addresses = Dns.SYSTEM.lookup(host);
        for (InetAddress ip : addresses)
            if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isMulticastAddress())
                throw new UnknownHostException("Destino de nodo no admitido");
        return addresses;
    }
}
