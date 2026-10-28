package salve.core.memory;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Bounded second-pass retrieval for an EXPLICIT recall, not unrestricted
 * re-search or memory writing. The source question is already resolved against
 * USER dialogue by MemoryRecall.query in MotorConversacional.
 *
 * Rationale: a lexical index can return EMPTY while the semantic embedding
 * benefits from a clean 2-4 token subject phrase instead of conversational
 * fillers such as "¿te acuerdas de lo que hablamos de...?".
 */
public final class MemoryRetryPlanner {
    private MemoryRetryPlanner() { }

    public static String secondQuery(String originalInput, String firstQuery,
            ConversationMemoryGrounding.Status firstStatus, boolean explicitRecall,
            boolean verifiedOwner) {
        if (!verifiedOwner || !explicitRecall
                || firstStatus != ConversationMemoryGrounding.Status.EMPTY
                || firstQuery == null || firstQuery.length() > 512
                || originalInput == null || originalInput.length() > 512
                || ConversationMemoryGrounding.chronologyDirection(originalInput) != 0
                || ConversationMemoryGrounding.chronologyDirection(firstQuery) != 0)
            return null;
        String intent = normalized(originalInput);
        if (intent.matches(".*\\b(?:mi ubicacion actual|mi posicion actual|donde estoy|gps)\\b.*")
                || intent.matches(".*\\b(?:borra|olvida|elimina|desactiva)\\b.*"))
            return null; // Commands and live sensor states are never memory recovery.
        List<String> terms = MemorySearchQuery.parse(firstQuery).terms();
        if (terms.size() < 2 || terms.size() > MemorySearchQuery.MAX_TERMS) return null;
        String subject = String.join(" ", terms);
        String previous = normalized(firstQuery).replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
        if (subject.equals(previous) || subject.isEmpty()) return null;
        // Never include an assistant-generated hypothesis, another person's profile,
        // a raw URL or an entire transcript in the follow-up query.
        if (subject.length() > 100) return null;
        return subject;
    }

    private static String normalized(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
