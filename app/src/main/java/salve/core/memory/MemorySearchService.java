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
    /** Explicit laboratory variants only. Normal production callers always use BASELINE. */
    public enum Strategy { BASELINE, EXACT_FAST_PATH }
    private final RecuerdoDao memories;
    public MemorySearchService(RecuerdoDao memories) { this.memories = memories; }
    public static final class Result {
        public final List<RecuerdoEntity> records;
        public final boolean failed, expanded;
        /** True only if a second, strictly bounded late-topic query produced hits. */
        public final boolean recovered;
        /** Actual indexed DAO reads, including failed attempts. Instrumentation only. */
        public final int indexReads;
        /** Query actually responsible for the returned lexical excerpts. */
        public final MemorySearchQuery evidenceQuery;
        Result(List<RecuerdoEntity> records, boolean failed, boolean expanded) {
            this(records, failed, expanded, false, null, 0);
        }
        Result(List<RecuerdoEntity> records, boolean failed, boolean expanded,
               boolean recovered, MemorySearchQuery evidenceQuery, int indexReads) {
            this.records = records;
            this.failed = failed;
            this.expanded = expanded;
            this.recovered = recovered;
            this.evidenceQuery = evidenceQuery;
            this.indexReads = indexReads;
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
        return search(query, personalHistory, limit, people, when, Strategy.BASELINE);
    }

    /**
     * Controlled A/B variation for paired, independently labeled memory experiments.
     * EXACT_FAST_PATH short-circuits ONLY after the existing full-topic FTS query
     * already yielded >= limit personally authorized records. It never changes the
     * normal caller's behavior or the original parser/provenance/ranking constraints.
     * A faster result can miss relevant evidence; a quality gate MUST reject it.
     */
    public Result search(MemorySearchQuery query, boolean personalHistory, int limit,
                         java.util.Map<String, Double> people, MemorySalience.When when,
                         Strategy strategy) {
        if (query == null || strategy == null || limit < 0 || limit > 4)
            throw new IllegalArgumentException("Memory result budget exceeded");
        Map<Integer, RecuerdoEntity> found = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> requests = new LinkedHashMap<>();
        if (!query.isEmpty()) {
            // Full-topic hits get a chance before frequent individual words fill the candidate pool.
            requests.put(query.exactExpression(), 12);
            if (query.hasExpansion()) requests.put(query.expandedExpression(), 12);
            for (List<String> group : query.groups()) requests.putIfAbsent(MemorySearchQuery.expression(group), 8);
        }
        boolean failed = false;
        int reads = 0;
        for (Map.Entry<String, Integer> request : requests.entrySet()) {
            reads++;
            try {
                List<RecuerdoEntity> rows = memories.buscarIndice(request.getKey(), personalHistory, request.getValue());
                for (int i = 0; i < Math.min(rows.size(), request.getValue()); i++) {
                    RecuerdoEntity record = rows.get(i);
                    if (record != null && (!personalHistory || MemoryProvenance.isPersonalCandidate(record)))
                        found.putIfAbsent(record.id, record);
                    if (found.size() == MAX_CANDIDATES) break;
                }
            } catch (RuntimeException unavailable) { failed = true; }
            // A deliberate experiment only: skip the subsequent synonym and
            // single-term queries when exact multi-term evidence already fills
            // the requested output budget. Normal production always checks all.
            if (strategy == Strategy.EXACT_FAST_PATH && !failed && limit > 0
                    && query.terms().size() >= 2 && reads == 1 && found.size() >= limit)
                break;
        }
        List<MemoryEvidenceRanker.Candidate> candidates = new ArrayList<>();
        for (RecuerdoEntity record : found.values())
            candidates.add(new MemoryEvidenceRanker.Candidate(record.id, record.frase, record.timestamp,
                    MemorySalience.score(record.frase, record.intensidad, people)));
        List<RecuerdoEntity> selected = new ArrayList<>();
        for (Integer id : MemoryEvidenceRanker.rankExpanded(candidates, query, limit, when == null ? MemorySalience.When.ANY : when))
            selected.add(found.get(id));
        return new Result(selected, failed, query.hasExpansion(), false, query, reads);
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
        return searchWithRecovery(input, query, personalHistory, limit, people, when, Strategy.BASELINE);
    }

    /** Experimental overload; the ordinary conversation path is always BASELINE. */
    public Result searchWithRecovery(String input, MemorySearchQuery query,
            boolean personalHistory, int limit, java.util.Map<String, Double> people,
            MemorySalience.When when, Strategy strategy) {
        Result original = search(query, personalHistory, limit, people, when, strategy);
        if (!original.records.isEmpty() || limit == 0) return original;
        MemorySearchQuery focused = MemorySearchQuery.tailFocus(input, query);
        if (focused == null) return original;
        Map<Integer, RecuerdoEntity> found = new LinkedHashMap<>();
        boolean failed = original.failed;
        int reads = original.indexReads;
        for (List<String> group : focused.groups()) {
            reads++;
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
                original.expanded, false, query, reads);
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
            return new Result(original.records, failed, original.expanded, false, query, reads);
        return new Result(selected, failed, true, true, focused, reads);
    }
}
