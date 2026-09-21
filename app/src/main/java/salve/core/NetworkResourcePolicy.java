package salve.core;

import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Politica comun para permitir lectura publica sin exponer la red local. */
public final class NetworkResourcePolicy {
    public static final long MAX_KNOWLEDGE_BYTES = 1_000_000L;

    private static final Set<String> MODEL_HOSTS = immutableSet(
            "huggingface.co", "cdn-lfs.huggingface.co", "cdn-lfs.hf.co",
            "cas-bridge.xethub.hf.co", "us.aws.cdn.hf.co", "eu.aws.cdn.hf.co", "arzenit.com");

    private NetworkResourcePolicy() { }

    public static Validation validateKnowledgeUrl(String rawUrl) {
        Validation validation = validateSyntax(rawUrl);
        if (!validation.allowed) return validation;
        String host = URI.create(validation.normalizedUrl).getHost();
        if (isReservedHostname(host) || isIpLiteral(host) && !isPublicLiteral(host)) {
            return Validation.denied("Destino local o reservado no permitido");
        }
        return validation;
    }

    public static Validation validateModelUrl(String rawUrl) {
        Validation validation = validateSyntax(rawUrl);
        if (!validation.allowed) return validation;
        String host = URI.create(validation.normalizedUrl).getHost().toLowerCase(Locale.ROOT);
        return MODEL_HOSTS.contains(host) ? validation
                : Validation.denied("Dominio de modelos no autorizado: " + host);
    }

    /** Resuelve el nombre justo antes de conectar y rechaza si cualquier respuesta no es publica. */
    public static Validation validateKnowledgeDestination(String rawUrl) {
        Validation validation = validateKnowledgeUrl(rawUrl);
        if (!validation.allowed) return validation;
        String host = URI.create(validation.normalizedUrl).getHost();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) return Validation.denied("El hostname no resolvio");
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address.getAddress())) {
                    return Validation.denied("El hostname resuelve a una red no publica");
                }
            }
            return validation;
        } catch (UnknownHostException error) {
            return Validation.denied("No se pudo resolver el hostname");
        }
    }

    private static Validation validateSyntax(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return Validation.denied("URL vacia");
        }
        try {
            URI uri = URI.create(rawUrl.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return Validation.denied("Solo se permite HTTPS");
            }
            if (uri.getUserInfo() != null || host == null || uri.getPort() != -1) {
                return Validation.denied("Autoridad de URL no permitida");
            }
            return Validation.allowed(uri.normalize().toString());
        } catch (IllegalArgumentException error) {
            return Validation.denied("URL invalida");
        }
    }

    static boolean isPublicAddress(byte[] bytes) {
        if (bytes.length == 16) {
            boolean mapped = true;
            for (int i = 0; i < 10; i++) mapped &= bytes[i] == 0;
            mapped &= bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
            if (mapped) return isPublicAddress(Arrays.copyOfRange(bytes, 12, 16));
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            // unspecified, loopback, unique-local, link-local, multicast y rangos documentales
            if (allZero(bytes) || isIpv6Loopback(bytes) || (first & 0xfe) == 0xfc
                    || first == 0xfe && (second & 0xc0) == 0x80 || first == 0xff
                    || first == 0x20 && second == 0x01 && (bytes[2] & 0xff) == 0x0d
                    && (bytes[3] & 0xff) == 0xb8) return false;
            return true;
        }
        if (bytes.length != 4) return false;
        int a = bytes[0] & 0xff, b = bytes[1] & 0xff, c = bytes[2] & 0xff;
        return !(a == 0 || a == 10 || a == 127 || a >= 224
                || a == 169 && b == 254 || a == 172 && b >= 16 && b <= 31
                || a == 192 && b == 168 || a == 100 && b >= 64 && b <= 127
                || a == 192 && b == 0 && (c == 0 || c == 2)
                || a == 198 && (b == 18 || b == 19 || b == 51 && c == 100)
                || a == 203 && b == 0 && c == 113);
    }

    private static boolean allZero(byte[] value) {
        for (byte item : value) if (item != 0) return false;
        return true;
    }

    private static boolean isIpv6Loopback(byte[] value) {
        for (int i = 0; i < 15; i++) if (value[i] != 0) return false;
        return value[15] == 1;
    }

    private static boolean isReservedHostname(String host) {
        String value = host.toLowerCase(Locale.ROOT);
        return value.equals("localhost") || value.endsWith(".localhost")
                || value.endsWith(".local") || value.equals("local")
                || value.endsWith(".internal") || value.equals("internal")
                || value.endsWith(".home.arpa") || value.equals("home.arpa")
                || value.endsWith(".invalid") || value.endsWith(".test")
                || value.endsWith(".example") || value.equals("example");
    }

    private static boolean isIpLiteral(String host) {
        return host.indexOf(':') >= 0 || host.matches("[0-9.]+");
    }

    private static boolean isPublicLiteral(String host) {
        try { return isPublicAddress(InetAddress.getByName(host).getAddress()); }
        catch (UnknownHostException error) { return false; }
    }

    private static Set<String> immutableSet(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    public static final class Validation {
        public final boolean allowed;
        public final String normalizedUrl;
        public final String reason;

        private Validation(boolean allowed, String normalizedUrl, String reason) {
            this.allowed = allowed;
            this.normalizedUrl = normalizedUrl;
            this.reason = reason;
        }

        private static Validation allowed(String url) {
            return new Validation(true, url, "Fuente autorizada");
        }

        private static Validation denied(String reason) {
            return new Validation(false, null, reason);
        }
    }
}
