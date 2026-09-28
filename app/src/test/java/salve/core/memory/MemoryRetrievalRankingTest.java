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

public class MemoryRetrievalRankingTest {
    private static RecuerdoEntity record(int id, String text) {
        RecuerdoEntity r = new RecuerdoEntity(); r.id = id; r.frase = text; r.timestamp = 100 - id; return r;
    }
    @Test public void laterQueryTermCanDisplaceFourFirstKeywordHits() {
        List<String> lookedUp = new ArrayList<>();
        RecuerdoDao dao = (RecuerdoDao) Proxy.newProxyInstance(RecuerdoDao.class.getClassLoader(),
                new Class<?>[]{RecuerdoDao.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("buscarRecientes")) throw new AssertionError(method.getName());
                    String term = (String) args[0]; lookedUp.add(term);
                    assertEquals(8, args[1]);
                    if (term.equalsIgnoreCase("pcloud")) return Collections.singletonList(record(5, "Salve pCloud evidencia específica"));
                    return Arrays.asList(record(1, "Salve saludo"), record(2, "Salve avatar"),
                            record(3, "Salve voz"), record(4, "Salve conversación"));
                });
        ConversationMemoryGrounding.Result result = new ConversationMemoryGrounding(dao, null, null).retrieve("Salve pCloud");
        assertTrue(lookedUp.size() >= 2);
        assertTrue(result.getContext().contains("recuerdos:5"));
        assertTrue(result.getContext().indexOf("recuerdos:5") < result.getContext().indexOf("recuerdos:1"));
        assertFalse(result.getContext().contains("recuerdos:4"));
        assertTrue(result.getContext().length() <= 3200);
    }
    @Test public void retrievalFailureStillMarksPartialEvidenceNotEmptyMemory() {
        RecuerdoDao dao = (RecuerdoDao) Proxy.newProxyInstance(RecuerdoDao.class.getClassLoader(),
                new Class<?>[]{RecuerdoDao.class}, (proxy, method, args) -> {
                    if (((String) args[0]).equalsIgnoreCase("pcloud")) throw new IllegalStateException("offline");
                    return Collections.singletonList(record(1, "Salve recuerdo"));
                });
        ConversationMemoryGrounding.Result result = new ConversationMemoryGrounding(dao, null, null).retrieve("Salve pCloud");
        assertEquals(ConversationMemoryGrounding.Status.PARTIAL, result.getStatus());
        assertTrue(result.getContext().contains("ERROR_DE_LECTURA"));
    }
}
