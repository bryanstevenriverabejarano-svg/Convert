package salve.core.memory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MemoryWritePolicyTest {
    @Test
    public void storesStableFirstPersonFacts() {
        assertTrue(MemoryWritePolicy.shouldPersist("Mi nombre es Bryan"));
        assertTrue(MemoryWritePolicy.shouldPersist("Prefiero respuestas cortas"));
        assertTrue(MemoryWritePolicy.shouldPersist("Vivo en Vitoria-Gasteiz"));
    }

    @Test
    public void skipsQuestionsAndCasualMessages() {
        assertFalse(MemoryWritePolicy.shouldPersist("¿Cómo estás?"));
        assertFalse(MemoryWritePolicy.shouldPersist("Cuéntame algo interesante"));
        assertFalse(MemoryWritePolicy.shouldPersist("Prefiero café?"));
    }

    @Test
    public void extractsStructuredProfileFact() {
        MemoryProfileFact fact = MemoryWritePolicy.extractProfileFact("Vivo en Vitoria-Gasteiz");

        assertEquals("residence", fact.getCategory());
        assertEquals("Vivo en Vitoria-Gasteiz", fact.getStatement());
    }
}
