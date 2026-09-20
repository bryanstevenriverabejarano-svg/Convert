package salve.core.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MemoryProfileFactTest {
    @Test
    public void aliasesShareTheSameProfileCategory() {
        assertEquals("name", MemoryProfileFact.parse("Mi nombre es Bryan").getCategory());
        assertEquals("name", MemoryProfileFact.parse("Me llamo Carlos").getCategory());
    }

    @Test
    public void preferencesAreSeparatedByTopic() {
        assertEquals("preference_respuestas",
                MemoryProfileFact.parse("Prefiero respuestas cortas").getCategory());
        assertEquals("preference_cafe",
                MemoryProfileFact.parse("Prefiero café solo").getCategory());
    }

    @Test
    public void changedPreferenceUsesTheSameCategory() {
        MemoryProfileFact oldFact = MemoryProfileFact.parse("Prefiero respuestas cortas");
        MemoryProfileFact newFact = MemoryProfileFact.parse("Prefiero respuestas detalladas");

        assertEquals(oldFact.getStorageTag(), newFact.getStorageTag());
    }

    @Test
    public void questionsAndCasualMessagesAreNotProfileFacts() {
        assertNull(MemoryProfileFact.parse("¿Prefiero café?"));
        assertNull(MemoryProfileFact.parse("Hola, ¿cómo estás?"));
    }
}
