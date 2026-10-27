package salve.core.reasoning;

import static org.junit.Assert.*;

import org.junit.Test;
import salve.core.cognitive.ReasoningEngine;

public class AnalogyReasonerTest {
    @Test public void parsesSpanishAnalogyQuestions() {
        AnalogyReasoner.Question q = AnalogyReasoner.parse("¿El pájaro es al nido como el perro es a qué?");
        assertEquals("pajaro", q.a);
        assertEquals("nido", q.b);
        assertEquals("perro", q.c);
        q = AnalogyReasoner.parse("Salve, el estrés es al insomnio lo que el café es a…");
        assertEquals("estres", q.a);
        assertEquals("cafe", q.c);
        assertNull(AnalogyReasoner.parse("¿Qué tal estás?"));
        assertNull(AnalogyReasoner.parse("Mi casa es a las afueras"));
    }

    @Test public void usesWhatBryanToldHerFirst() {
        CausalMemory memory = new CausalMemory(new ReasoningEngine(), null);
        memory.learn("El estrés provoca insomnio. El café provoca nerviosismo.");
        String block = AnalogyReasoner.promptBlock("El estrés es al insomnio como el café es a qué", memory);
        assertTrue(block, block.contains("con lo que Bryan te contó") && block.contains("nerviosismo"));
    }

    @Test public void otherwiseGuidesTheModelToNameTheRelationFirst() {
        String block = AnalogyReasoner.promptBlock("El pájaro es al nido como el perro es a qué", new CausalMemory(new ReasoningEngine(), null));
        assertTrue(block, block.contains("nombra en una frase la relación") && block.contains("«perro»"));
        assertEquals("", AnalogyReasoner.promptBlock("hola", null));
    }
}
