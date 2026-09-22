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
    @Test public void residenceCorrectionsReplaceTheSamePersistentCategory() {
        for (String correction : new String[]{"Ahora vivo en Lima", "No, vivo en Lima",
                "En realidad vivo en Lima", "Corrección: ahora vivo en Lima"}) {
            MemoryProfileFact fact = MemoryWritePolicy.extractProfileFact(correction);
            assertEquals("profile:residence", fact.getStorageTag());
            assertEquals(correction, fact.getStatement());
        }
    }

    @Test public void negationsAreNotTurnedIntoPositiveProfileFacts() {
        assertNull(MemoryProfileFact.parse("No vivo en Lima"));
        assertNull(MemoryProfileFact.parse("No me llamo Bryan"));
        assertNull(MemoryProfileFact.parse("Si ahora vivo en Lima"));
    }

}
