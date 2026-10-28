package salve.core.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import salve.data.db.RecuerdoDao;
import salve.data.db.RecuerdoEntity;

/** Shared indexed retrieval for every provider. No model/network calls or persistent cache. */
public final class MemorySearchService {
    public static final int MAX_CANDIDATES = MemoryEvidenceRanker.MAX_EXPANDED_CANDIDATES;
    public static final int MAX_SEARCHES = 8; // six original lookups + at most two focused retries
    private final RecuerdoDao memories;
    public MemorySearchService(RecuerdoDao memories) { this.memories = memories; }
    public static final class Result {
        public final List<RecuerdoEntity> records;
        public final boolean failed, expanded;
        /** True only if a second, strictly bounded late-topic query produced hits. */
        public final boolean recovered;
        /** Query actually responsible for the returned lexical excerpts. */
        public final MemorySearchQuery evidenceQuery;
        Result(List<RecuerdoEntity> records, boolean failed, boolean expanded) {
            this(records, failed, expanded, false, null);
        }
        Result(List<RecuerdoEntity> records, boolean failed, boolean expanded,
               boolean recovered, MemorySearchQuery evidenceQuery) {
            this.records = records;
            this.failed = failed;
            this.expanded = expanded;
            this.recovered = recovered;
            this.evidenceQuery = evidenceQuery;
        }
    }
    public Result search(MemorySearchQuery query, boolean personalHistory, int limit) {
        return search(query, personalHistory, limit, null, MemorySalience.When.ANY);
    }

    /**
     * @param people nombre → peso de cercanía (puede ser null)
     * @param when si la pregunta pide algo reciente o antiguo
     */
    public Result search(MemorySearchQuery query, boolean personalHistory, int limit,
                         java.util.Map<String, Double> people, MemorySalience.When when) {
        if (limit < 0 || limit > 4) throw new IllegalArgumentException("Memory result budget exceeded");
        Map<Integer, RecuerdoEntity> found = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> requests = new LinkedHashMap<>();
        if (!query.isEmpty()) {
            // Full-topic hits get a chance before frequent individual words fill the candidate pool.
            requests.put(query.exactExpression(), 12);
            if (query.hasExpansion()) requests.put(query.expandedExpression(), 12);
            for (List<String> group : query.groups()) requests.putIfAbsent(MemorySearchQuery.expression(group), 8);
        }
        boolean failed = false;
        for (Map.Entry<String, Integer> request : requests.entrySet()) {
            try {
                List<RecuerdoEntity> rows = memories.buscarIndice(request.getKey(), personalHistory, request.getValue());
                for (int i = 0; i < Math.min(rows.size(), request.getValue()); i++) {
                    RecuerdoEntity record = rows.get(i);
                    if (record != null && (!personalHistory || MemoryProvenance.isPersonalCandidate(record)))
                        found.putIfAbsent(record.id, record);
                    if (found.size() == MAX_CANDIDATES) break;
                }
            } catch (RuntimeException unavailable) { failed = true; }
        }
        List<MemoryEvidenceRanker.Candidate> candidates = new ArrayList<>();
        for (RecuerdoEntity record : found.values())
            candidates.add(new MemoryEvidenceRanker.Candidate(record.id, record.frase, record.timestamp,
                    MemorySalience.score(record.frase, record.intensidad, people)));
        List<RecuerdoEntity> selected = new ArrayList<>();
        for (Integer id : MemoryEvidenceRanker.rankExpanded(candidates, query, limit, when == null ? MemorySalience.When.ANY : when))
            selected.add(found.get(id));
        return new Result(selected, failed, query.hasExpansion(), false, query);
    }

    /**
     * A second, read-only lookup is permitted ONLY when the indexed first pass
     * found no matching records and a long question has unused, concrete terms.
     * Never widen personal-history provenance or the four-record output budget.
     * Also never interpret fallback hits as proof that a factual claim is true.
     */
    public Result searchWithRecovery(String input, MemorySearchQuery query,
            boolean personalHistory, int limit, java.util.Map<String, Double> people,
            MemorySalience.When when) {
        Result original = search(query, personalHistory, limit, people, when);
        if (!original.records.isEmpty() || limit == 0) return original;
        MemorySearchQuery focused = MemorySearchQuery.tailFocus(input, query);
        if (focused == null) return original;
        Map<Integer, RecuerdoEntity> found = new LinkedHashMap<>();
        boolean failed = original.failed;
        for (List<String> group : focused.groups()) {
            try {
                // Exactly one safely quoted OR expression per term group. No raw FTS.
                List<RecuerdoEntity> rows = memories.buscarIndice(
                        MemorySearchQuery.expression(group), personalHistory, 8);
                if (rows == null) continue;
                for (int i = 0; i < Math.min(8, rows.size()); i++) {
                    RecuerdoEntity record = rows.get(i);
                    if (record != null && (!personalHistory || MemoryProvenance.isPersonalCandidate(record)))
                        found.putIfAbsent(record.id, record);
                    if (found.size() >= MAX_CANDIDATES) break;
                }
            } catch (RuntimeException unavailable) { failed = true; }
        }
        if (found.isEmpty()) return new Result(original.records, failed,
                original.expanded, false, query);
        List<MemoryEvidenceRanker.Candidate> candidates = new ArrayList<>();
        for (RecuerdoEntity record : found.values())
            candidates.add(new MemoryEvidenceRanker.Candidate(record.id, record.frase,
                    record.timestamp, MemorySalience.score(record.frase, record.intensidad, people)));
        List<RecuerdoEntity> selected = new ArrayList<>();
        for (Integer id : MemoryEvidenceRanker.rankExpanded(candidates, focused,
                limit, when == null ? MemorySalience.When.ANY : when)) {
            RecuerdoEntity record = found.get(id);
            if (record != null) selected.add(record);
        }
        if (selected.isEmpty())
            return new Result(original.records, failed, original.expanded, false, query);
        return new Result(selected, failed, true, true, focused);
    }
}
