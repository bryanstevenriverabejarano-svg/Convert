package salve.core.memory;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import salve.data.db.RecuerdoEntity;

public class SemanticSalienceOrderTest {
    private static RecuerdoEntity record(int id, String text, int intensity, long at) {
        RecuerdoEntity r = new RecuerdoEntity();
        r.id = id; r.frase = text; r.intensidad = intensity; r.timestamp = at;
        return r;
    }

    @Test public void importanceOrdersSemanticMatchesWhenNoTimeIsAsked() {
        RecuerdoEntity calm = record(1, "Fuimos al parque", 2, 100);
        RecuerdoEntity moving = record(2, "El día que nació mi sobrino", 9, 50);
        List<RecuerdoEntity> semantic = Arrays.asList(calm, moving);
        List<RecuerdoEntity> out = ConversationMemoryGrounding.reorderBySalience(
                semantic, Collections.<RecuerdoEntity>emptyList(), new HashMap<>(), MemorySalience.When.ANY);
        assertEquals(2, out.get(0).id);
    }

    @Test public void requestedTimeStillComesFirstThenImportance() {
        RecuerdoEntity oldMoving = record(1, "Mi graduación", 10, 100);
        RecuerdoEntity newCalm = record(2, "Compré pan", 1, 900);
        RecuerdoEntity newMoving = record(3, "Mi madre me llamó emocionada", 8, 900);
        List<RecuerdoEntity> out = ConversationMemoryGrounding.reorderBySalience(
                Arrays.asList(oldMoving, newCalm, newMoving), Collections.<RecuerdoEntity>emptyList(),
                new HashMap<>(), MemorySalience.When.RECENT);
        assertEquals(3, out.get(0).id);
        assertEquals(2, out.get(1).id);
        assertEquals(1, out.get(2).id);
    }

    @Test public void closePeopleRaiseImportanceAndLexicalOrderIsUntouched() {
        Map<String, Double> people = new HashMap<>();
        people.put("vivina", 0.9);
        RecuerdoEntity other = record(1, "Hablé con un vecino", 3, 10);
        RecuerdoEntity family = record(2, "Hablé con Vivina", 3, 10);
        RecuerdoEntity lexical = record(3, "Coincidencia exacta", 0, 10);
        List<RecuerdoEntity> out = ConversationMemoryGrounding.reorderBySalience(
                Arrays.asList(lexical, other, family), Collections.singletonList(lexical), people, MemorySalience.When.ANY);
        assertEquals(3, out.get(0).id);
        assertEquals(2, out.get(1).id);
    }
}
