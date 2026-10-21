package salve.core.memory;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import salve.data.db.RecuerdoDao;
import salve.data.db.RecuerdoEntity;

/** La amígdala y el tiempo: desempatan entre recuerdos que se parecen igual a la pregunta. */
public class MemorySalienceTest {
    private static final long DAY = 86_400_000L;
    private static final long NOW = 1_800_000_000_000L;

    private static RecuerdoEntity rec(int id, String text, int intensity, long daysAgo) {
        RecuerdoEntity r = new RecuerdoEntity();
        r.id = id; r.frase = text; r.intensidad = intensity; r.timestamp = NOW - daysAgo * DAY; r.etiquetas = "[]";
        return r;
    }

    private static ConversationMemoryGrounding grounding(RecuerdoEntity... rows) {
        List<RecuerdoEntity> all = Arrays.asList(rows);
        RecuerdoDao dao = (RecuerdoDao) Proxy.newProxyInstance(RecuerdoDao.class.getClassLoader(),
                new Class<?>[]{RecuerdoDao.class}, (proxy, method, args) -> {
                    if (method.getName().equals("buscarIndice")) return all;
                    throw new AssertionError(method.getName());
                });
        ConversationMemoryGrounding g = new ConversationMemoryGrounding(dao, null, null);
        g.setClock(() -> NOW);
        return g;
    }

    private static int first(ConversationMemoryGrounding.Result r, int... ids) {
        int best = -1, at = Integer.MAX_VALUE;
        for (int id : ids) {
            int i = r.getContext().indexOf("recuerdos:" + id + "\"");
            if (i >= 0 && i < at) { at = i; best = id; }
        }
        return best;
    }

    @Test public void readsWhetherTheQuestionAsksForRecentOrOld() {
        assertEquals(MemorySalience.When.RECENT, MemorySalience.when("¿Te acuerdas de lo que hablamos el otro día de navegar?"));
        assertEquals(MemorySalience.When.RECENT, MemorySalience.when("ayer te conté lo del coche"));
        assertEquals(MemorySalience.When.OLD, MemorySalience.when("hace mucho tiempo te hablé de navegar"));
        assertEquals(MemorySalience.When.OLD, MemorySalience.when("¿Qué te dije la primera vez sobre el mar?"));
        assertEquals(MemorySalience.When.ANY, MemorySalience.when("¿Qué sabes de navegar?"));
    }

    @Test public void agoInPlainWords() {
        assertEquals("hoy", MemorySalience.ago(NOW - 3_600_000L, NOW));
        assertEquals("ayer", MemorySalience.ago(NOW - DAY, NOW));
        assertEquals("hace 3 días", MemorySalience.ago(NOW - 3 * DAY, NOW));
        assertEquals("hace 3 semanas", MemorySalience.ago(NOW - 21 * DAY, NOW));
        assertEquals("hace 5 meses", MemorySalience.ago(NOW - 150 * DAY, NOW));
        assertEquals("hace 1 año", MemorySalience.ago(NOW - 400 * DAY, NOW));
        assertEquals("", MemorySalience.ago(0, NOW));
    }

    @Test public void emotionAndClosePeopleRaiseTheScore() {
        Map<String, Double> people = new HashMap<>();
        people.put("bryan", 0.95);
        people.put("juan", 0.1);
        assertTrue(MemorySalience.score("Navegamos juntos", 9, people) > MemorySalience.score("Navegamos juntos", 2, people));
        assertTrue(MemorySalience.score("Bryan y yo navegamos", 5, people) > MemorySalience.score("Juan y yo navegamos", 5, people));
        // Por palabra entera: «Bryanna» no es Bryan.
        assertEquals(MemorySalience.score("Bryanna navegó", 5, people), MemorySalience.score("Alguien navegó", 5, people), 1e-9);
    }

    @Test public void atEqualMatchTheMemoryThatMatteredMoreComesFirst() {
        ConversationMemoryGrounding g = grounding(
                rec(1, "Hablamos de navegar en el puerto", 2, 10),
                rec(2, "Hablamos de navegar y me emocioné muchísimo", 10, 10));
        assertEquals(2, first(g.retrieve("navegar"), 1, 2));
    }

    @Test public void atEqualMatchTheMemoryWithSomeoneCloseComesFirst() {
        ConversationMemoryGrounding g = grounding(
                rec(1, "Juan me habló de navegar", 5, 10),
                rec(2, "Bryan me habló de navegar", 5, 10));
        Map<String, Double> people = new HashMap<>();
        people.put("bryan", 0.9);
        people.put("juan", 0.1);
        g.setPeopleWeights(() -> people);
        assertEquals(2, first(g.retrieve("navegar"), 1, 2));
    }

    @Test public void theSameMemoryTwiceIsTellApartByWhen() {
        ConversationMemoryGrounding g = grounding(
                rec(1, "Me explicaste qué es navegar", 5, 200),
                rec(2, "Me explicaste qué es navegar", 5, 2));
        assertEquals(2, first(g.retrieve("el otro día me explicaste qué es navegar"), 1, 2));
        assertEquals(1, first(g.retrieve("hace mucho tiempo me explicaste qué es navegar"), 1, 2));
        String ctx = g.retrieve("navegar").getContext();
        assertTrue(ctx, ctx.contains("\"cuando\":\"hace 2 días\""));
        assertTrue(ctx, ctx.contains("\"cuando\":\"hace 6 meses\""));
    }

    @Test public void matchingTheQuestionStillComesBeforeEmotion() {
        ConversationMemoryGrounding g = grounding(
                rec(1, "Navegar por el río con la familia", 1, 10),
                rec(2, "Navegar emocionante", 10, 10));
        // «río» y «familia» también están en la pregunta: el que más se parece va primero aunque importe menos.
        assertEquals(1, first(g.retrieve("navegar río familia"), 1, 2));
    }

    @Test public void strongMemoriesAreMarkedForTheModel() {
        String ctx = grounding(rec(1, "Navegar contigo fue increíble", 9, 3)).retrieve("navegar").getContext();
        assertTrue(ctx, ctx.contains("\"importancia\":\"alta\""));
    }
}
