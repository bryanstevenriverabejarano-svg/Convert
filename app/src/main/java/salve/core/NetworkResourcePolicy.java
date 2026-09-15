package salve.core;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Politica comun para impedir descargas arbitrarias, SSRF y fuentes no autorizadas. */
public final class NetworkResourcePolicy {
    public static final long MAX_KNOWLEDGE_BYTES = 1_000_000L;

    private static final Set<String> KNOWLEDGE_HOSTS = immutableSet(
            "es.wikipedia.org", "en.wikipedia.org", "www.wikidata.org");
    private static final Set<String> MODEL_HOSTS = immutableSet(
            "huggingface.co", "cdn-lfs.huggingface.co", "arzenit.com");

    private NetworkResourcePolicy() { }

    public static Validation validateKnowledgeUrl(String rawUrl) {
        return validate(rawUrl, KNOWLEDGE_HOSTS);
    }

    public static Validation validateModelUrl(String rawUrl) {
        return validate(rawUrl, MODEL_HOSTS);
    }

    private static Validation validate(String rawUrl, Set<String> allowedHosts) {
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
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (!allowedHosts.contains(normalizedHost)) {
                return Validation.denied("Dominio no autorizado: " + normalizedHost);
            }
            return Validation.allowed(uri.toString());
        } catch (IllegalArgumentException error) {
            return Validation.denied("URL invalida");
        }
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
