package salve.core.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Las páginas que Salve leyó para responder, al final de su mensaje en el chat: así Bryan puede
 * abrirlas, contrastarlas y discutirlas con ella. Va al chat y al historial, nunca a la voz.
 */
public final class SourceList {
    public static final int MAX = 5;

    private SourceList() { }

    /** Nota «Fuentes que he leído» (vacía si no hay ninguna). Cada fuente: {título, url}. */
    public static String note(List<String[]> sources) {
        if (sources == null || sources.isEmpty()) return "";
        StringBuilder out = new StringBuilder("\n\nFuentes que he leído (ábrelas y contrastamos):");
        Set<String> seen = new LinkedHashSet<>();
        for (String[] source : sources) {
            if (source == null || source.length < 2 || source[1] == null) continue;
            String url = source[1].trim();
            if (!url.startsWith("https://") && !url.startsWith("http://")) continue;
            if (!seen.add(url) || seen.size() > MAX) continue;
            String title = source[0] == null ? "" : source[0].replaceAll("\\s+", " ").trim();
            if (title.length() > 80) title = title.substring(0, 79) + "…";
            out.append("\n• ").append(title.isEmpty() ? host(url) : title + " (" + host(url) + ")").append("\n  ").append(url);
        }
        return seen.isEmpty() ? "" : out.toString();
    }

    static String host(String url) {
        String rest = url.replaceFirst("^https?://", "");
        int slash = rest.indexOf('/');
        String host = slash < 0 ? rest : rest.substring(0, slash);
        return host.startsWith("www.") ? host.substring(4) : host;
    }
}
