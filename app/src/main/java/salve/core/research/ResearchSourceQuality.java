package salve.core.research;

import java.net.URI;
import salve.core.NetworkResourcePolicy;

/** A URL is a candidate, never proof. Reject navigation pages and access walls before synthesis. */
public final class ResearchSourceQuality {
    private ResearchSourceQuality() { }
    public static boolean candidate(String url) {
        if (url == null || url.length() > 500 || !NetworkResourcePolicy.validateKnowledgeUrl(url).allowed) return false;
        try {
            URI uri = URI.create(url); String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(java.util.Locale.ROOT);
            String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
            if (host.startsWith("consent.") || host.startsWith("accounts.")) return false;
            if (path.matches("/(?:accounts/)?(?:login|signin|sign-in|signup|register|privacy|terms|help|search|explore)(?:/.*)?")) return false;
            boolean portal = host.matches("(?:.*\\.)?(?:instagram|facebook|meta|youtube|tiktok|x|twitter)\\.com");
            return !portal || !(path.isEmpty() || path.equals("/"));
        } catch (RuntimeException malformed) { return false; }
    }
    public static boolean usable(String url, String title, String text) {
        if (!candidate(url) || text == null || text.trim().isEmpty()) return false;
        String heading = ResearchIntent.normalized(title);
        String body = ResearchIntent.normalized(text);
        if (heading.matches(".*(?:tu navegador esta obsoleto|browser is no longer supported|access denied|just a moment|captcha).*")) return false;
        if (body.contains("our systems have detected unusual traffic") || body.contains("enable javascript and cookies to continue")) return false;
        return !(body.length() < 1000 && (body.contains("inicia sesion para continuar") || body.contains("log in to continue")
                || body.contains("sign in to continue") || body.contains("tu navegador esta obsoleto")
                || body.contains("your browser is no longer supported")));
    }
}
