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
    public static final int MAX_SEARCHES = 6;
    private final RecuerdoDao memories;
    public MemorySearchService(RecuerdoDao memories) { this.memories = memories; }
    public static final class Result {
        public final List<RecuerdoEntity> records;
        public final boolean failed, expanded;
        Result(List<RecuerdoEntity> records, boolean failed, boolean expanded) {
            this.records = records; this.failed = failed; this.expanded = expanded;
        }
    }
    public Result search(MemorySearchQuery query, boolean personalHistory, int limit) {
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
            candidates.add(new MemoryEvidenceRanker.Candidate(record.id, record.frase, record.timestamp));
        List<RecuerdoEntity> selected = new ArrayList<>();
        for (Integer id : MemoryEvidenceRanker.rankExpanded(candidates, query, limit)) selected.add(found.get(id));
        return new Result(selected, failed, query.hasExpansion());
    }
}
