package salve.core.conversation;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A model may refine a read-only memory query, but it cannot silently search
 * an unrelated personal topic injected by a web page or other tool output.
 * The caller must separately enforce authenticated private-memory access.
 */
public final class MemoryProbePolicy {
    private MemoryProbePolicy() { }

    /** Throws before reading memory when a proposed query is out of scope. */
    public static String scoped(String input, List<ChatMessage> history, String proposed) {
        String original = MemoryRecall.query(input, history);
        if (proposed == null || proposed.trim().isEmpty()) return original;
        String query = proposed.trim().replaceAll("\\s+", " ");
        if (query.length() < 3 || query.length() > 160
                || proposed.indexOf('\n') >= 0 || proposed.indexOf('\r') >= 0
                || query.contains("://") || query.contains("@")
                || query.contains("{") || query.contains("}")) {
            throw new IllegalArgumentException("Consulta de memoria fuera del formato permitido");
        }
        Set<String> allowed = terms(original);
        allowed.addAll(terms(input));
        // Recent USER turns belong to the same context; tool/page text does not.
        if (history != null) {
            int count = 0;
            for (int i = history.size() - 1; i >= 0 && count < 3; i--) {
                ChatMessage message = history.get(i);
                if (message.getRole() != ChatMessage.Role.USER) continue;
                allowed.addAll(terms(message.getContent()));
                count++;
            }
        }
        String normalizedInput = norm(input);
        if (normalizedInput.matches(".*\\b(eres|unica|unico|identidad|tu mente|tu historia)\\b.*")) {
            allowed.add("salve"); allowed.add("ident"); allowed.add("recue");
        }
        boolean overlap = false;
        for (String word : terms(query)) {
            if (allowed.contains(word)) { overlap = true; break; }
        }
        if (!overlap) throw new IllegalArgumentException("Consulta de memoria ajena al objetivo de este turno");
        return query;
    }
    private static Set<String> terms(String input) {
        Set<String> out = new HashSet<>();
        for (String word : norm(input).split(" ")) {
            if (word.length() < 4 || STOP.contains(word)) continue;
            out.add(word.substring(0, Math.min(5, word.length())));
        }
        return out;
    }
    private static String norm(String input) {
        return Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
    private static final Set<String> STOP = new HashSet<>(java.util.Arrays.asList(
            "quiero", "dime", "donde", "cuando", "porque", "puedes",
            "tengo", "tienes", "sobre", "esto", "esta", "este", "hacer",
            "sabes", "saber", "como", "para", "cual", "entre", "desde",
            "estas", "eres", "pero", "tambien", "otras", "cosas", "antes"));
}
