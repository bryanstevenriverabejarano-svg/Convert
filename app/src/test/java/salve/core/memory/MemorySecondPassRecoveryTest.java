package salve.core.memory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import salve.data.db.RecuerdoDao;
import salve.data.db.RecuerdoEntity;
import static org.junit.Assert.*;

public class MemorySecondPassRecoveryTest {
    private static final String LONG_QUESTION =
            "¿Me puedes buscar las explicaciones detalladas sobre nuestro sistema antiguo del proyecto Orion?";

    private static RecuerdoEntity row(int id, String content, String tags) {
        RecuerdoEntity r = new RecuerdoEntity();
        r.id = id; r.frase = content; r.timestamp = 1000L + id;
        r.etiquetas = tags;
        return r;
    }

    private static final class IndexedMemory {
        final List<RecuerdoEntity> records = new ArrayList<>();
        final List<String> requests = new ArrayList<>();
        boolean fail;
        final RecuerdoDao dao = (RecuerdoDao) Proxy.newProxyInstance(
                RecuerdoDao.class.getClassLoader(), new Class<?>[] { RecuerdoDao.class },
                (proxy, method, args) -> {
                    if (!"buscarIndice".equals(method.getName()))
                        throw new UnsupportedOperationException(method.getName());
                    requests.add((String) args[0]);
                    if (fail) throw new IllegalStateException("sensitive database detail");
                    String expression = ((String) args[0]).replace("\"", "");
                    List<RecuerdoEntity> out = new ArrayList<>();
                    for (RecuerdoEntity candidate : records)
                        if (expression.contains("orion") && candidate.frase.toLowerCase().contains("orion"))
                            out.add(candidate);
                    return out;
                });
    }

    @Test public void lateTopicIsRecoveredOnlyWhenFirstPassIsEmpty() {
        IndexedMemory store = new IndexedMemory();
        store.records.add(row(7, "Proyecto Orion: sensores y diagnóstico de fallos",
                "[\"hecho_usuario\"]"));
        MemorySearchQuery first = MemorySearchQuery.parse(LONG_QUESTION);
        assertEquals(4, first.terms().size());
        assertFalse(first.terms().contains("orion"));
        assertEquals(Arrays.asList("proyecto", "orion"),
                MemorySearchQuery.tailFocus(LONG_QUESTION, first).terms());
        MemorySearchService.Result found = new MemorySearchService(store.dao)
                .searchWithRecovery(LONG_QUESTION, first, true, 4, null, MemorySalience.When.ANY);
        assertTrue(found.recovered);
        assertFalse(found.failed);
        assertEquals(1, found.records.size());
        assertEquals(7, found.records.get(0).id);
        assertEquals("orion", found.evidenceQuery.terms().get(1));
        assertTrue(store.requests.size() <= MemorySearchService.MAX_SEARCHES);
    }

    @Test public void recoveredPersonalMemoryDoesNotPromoteExternalResearchToPersonalHistory() {
        IndexedMemory store = new IndexedMemory();
        store.records.add(row(2, "Orion, datos de un sitio web",
                "[\"fuentes_externas\"]"));
        store.records.add(row(3, "Orion, nuestra conversación",
                "[\"hecho_usuario\"]"));
        MemorySearchService.Result result = new MemorySearchService(store.dao)
                .searchWithRecovery(LONG_QUESTION, MemorySearchQuery.parse(LONG_QUESTION),
                        true, 4, null, MemorySalience.When.ANY);
        assertTrue(result.recovered);
        assertEquals(1, result.records.size());
        assertEquals(3, result.records.get(0).id);
    }

    @Test public void noFallbackForShortQuestionsOrExistingEvidence() {
        IndexedMemory store = new IndexedMemory();
        store.records.add(row(3, "Orion, nuestra conversación",
                "[\"hecho_usuario\"]"));
        MemorySearchQuery shortQuery = MemorySearchQuery.parse("Proyecto Orion");
        assertNull(MemorySearchQuery.tailFocus("Proyecto Orion", shortQuery));
        MemorySearchService.Result shortResult = new MemorySearchService(store.dao)
                .searchWithRecovery("Proyecto Orion", shortQuery, true, 4,
                        null, MemorySalience.When.ANY);
        assertFalse(shortResult.recovered);
        assertEquals(1, shortResult.records.size());
        assertTrue(store.requests.size() <= MemorySearchService.MAX_SEARCHES);
    }

    @Test public void failedReadsNeverClaimKnowledgeOrLeakExceptionMessages() {
        IndexedMemory store = new IndexedMemory();
        store.fail = true;
        MemorySearchService.Result result = new MemorySearchService(store.dao)
                .searchWithRecovery(LONG_QUESTION, MemorySearchQuery.parse(LONG_QUESTION),
                        true, 4, null, MemorySalience.When.ANY);
        assertTrue(result.failed);
        assertFalse(result.recovered);
        assertTrue(result.records.isEmpty());
        assertTrue(store.requests.size() <= MemorySearchService.MAX_SEARCHES);
    }

    @Test public void noTopicMeansNoUnboundedDatabaseSearch() {
        IndexedMemory store = new IndexedMemory();
        MemorySearchQuery empty = MemorySearchQuery.parse("hola gracias");
        assertNull(MemorySearchQuery.tailFocus("hola gracias", empty));
        MemorySearchService.Result result = new MemorySearchService(store.dao)
                .searchWithRecovery("hola gracias", empty, false, 4, null, MemorySalience.When.ANY);
        assertTrue(result.records.isEmpty());
        assertEquals(Collections.emptyList(), store.requests);
    }
}
