package salve.core.memory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bounded multi-term evidence ranking, not a truth classifier or entity resolver. */
public final class MemoryEvidenceRanker {
    public static final int MAX_CANDIDATES = 32;
    public static final class Candidate {
        public final int id;
        public final String text;
        public final long timestamp;
        public Candidate(int id, String text, long timestamp) {
            this.id = id; this.text = text == null ? "" : text; this.timestamp = timestamp;
        }
    }
    private MemoryEvidenceRanker() {}

    /** Group coverage first, exact vocabulary second; opposing statements remain separate. */
    public static List<Integer> rankExpanded(List<Candidate> candidates, MemorySearchQuery query, int limit) {
        if (candidates == null || candidates.size() > MemorySearchService.MAX_CANDIDATES
                || query == null || limit < 0 || limit > 4) throw new IllegalArgumentException("Evidence budget exceeded");
        List<Scored> unique = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Candidate candidate : candidates) if (candidate != null && seen.add(candidate.id)) {
            String text = candidate.text.substring(0, Math.min(candidate.text.length(), 64_000));
            Set<String> words = new HashSet<>(java.util.Arrays.asList(normalize(text).split("[^\\p{L}0-9]+")));
            int groups = 0, exact = 0;
            for (List<String> group : query.groups())
                for (String term : group) if (words.contains(term)) { groups++; break; }
            for (String term : query.terms()) if (words.contains(term)) exact++;
            unique.add(new Scored(candidate, groups, exact));
        }
        unique.sort(Comparator.<Scored>comparingInt(c -> c.groups).reversed()
                .thenComparing(Comparator.comparingInt((Scored c) -> c.exact).reversed())
                .thenComparing(Comparator.comparingLong((Scored c) -> c.record.timestamp).reversed())
                .thenComparingInt(c -> c.record.id));
        List<Integer> result = new ArrayList<>();
        for (Scored candidate : unique) {
            if (result.size() == limit) break;
            if (candidate.groups > 0) result.add(candidate.record.id);
        }
        return result;
    }
    private static final class Scored {
        final Candidate record;
        final int groups, exact;
        Scored(Candidate record, int groups, int exact) { this.record = record; this.groups = groups; this.exact = exact; }
    }

    /** Coverage first, recency only as a tie breaker. Keeps contradictory evidence distinct. */
    public static List<Integer> rank(List<Candidate> candidates, List<String> queryTerms, int limit) {
        if (candidates == null || candidates.size() > MAX_CANDIDATES || queryTerms == null
                || queryTerms.size() > 12 || limit < 0 || limit > MAX_CANDIDATES)
            throw new IllegalArgumentException("Evidence budget exceeded");
        Set<String> terms = new HashSet<>();
        for (String term : queryTerms) {
            String normalized = normalize(term);
            if (!normalized.isEmpty()) terms.add(normalized);
        }
        List<Candidate> unique = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Candidate candidate : candidates)
            if (candidate != null && seen.add(candidate.id)) unique.add(candidate);
        unique.sort(Comparator.<Candidate>comparingInt(c -> coverage(c.text, terms)).reversed()
                .thenComparing(Comparator.comparingLong((Candidate c) -> c.timestamp).reversed())
                .thenComparingInt(c -> c.id));
        List<Integer> result = new ArrayList<>();
        for (Candidate candidate : unique) {
            if (result.size() == limit) break;
            if (coverage(candidate.text, terms) > 0) result.add(candidate.id);
        }
        return result;
    }
    private static int coverage(String text, Set<String> terms) {
        String normalized = normalize(text.length() > 64_000 ? text.substring(0, 64_000) : text);
        int score = 0;
        for (String term : terms) {
            int at = -1;
            while ((at = normalized.indexOf(term, at + 1)) >= 0) {
                int end = at + term.length();
                if ((at == 0 || !Character.isLetterOrDigit(normalized.charAt(at - 1)))
                        && (end == normalized.length() || !Character.isLetterOrDigit(normalized.charAt(end)))) {
                    score++; break;
                }
            }
        }
        return score;
    }
    private static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
}
