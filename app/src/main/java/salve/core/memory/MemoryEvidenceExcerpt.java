package salve.core.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verbatim window near matching words, with explicit omission marks. Never synthesizes memory. */
public final class MemoryEvidenceExcerpt {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}0-9]+");
    private MemoryEvidenceExcerpt() { }
    public static String extract(String text, MemorySearchQuery query, int limit) {
        if (limit < 8) throw new IllegalArgumentException("Excerpt budget too small");
        if (text == null) return "";
        if (text.length() <= limit) return text;
        List<int[]> hits = new ArrayList<>();
        Matcher words = WORD.matcher(text.substring(0, Math.min(text.length(), 64_000)));
        int[] counts = new int[query.groups().size()];
        while (words.find()) {
            String word = MemorySearchQuery.normalize(words.group());
            for (int group = 0; group < counts.length; group++) {
                if (counts[group] < 32 && query.groups().get(group).contains(word)) {
                    hits.add(new int[]{words.start(), group}); counts[group]++;
                }
            }
        }
        int width = limit - 2, start = 0, best = -1;
        for (int[] hit : hits) {
            int candidate = Math.max(0, hit[0] - width / 4);
            boolean[] covered = new boolean[counts.length];
            int score = 0;
            for (int[] other : hits) if (other[0] >= candidate && other[0] < candidate + width
                    && !covered[other[1]]) { covered[other[1]] = true; score++; }
            if (score > best) { best = score; start = candidate; }
        }
        if (start > 0 && Character.isLowSurrogate(text.charAt(start))) start++;
        int end = Math.min(text.length(), start + width);
        if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return (start > 0 ? "…" : "") + text.substring(start, end) + (end < text.length() ? "…" : "");
    }
}
