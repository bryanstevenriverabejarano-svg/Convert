package salve.core.memory;

import static org.junit.Assert.*;

import org.junit.Test;

public class TypoTolerantChronologyTest {
    @Test public void correctsKeyboardTyposOnlyForMemoryWords() {
        assertEquals("cual es tu primer recuerdo", TypoTolerantText.forRouting("Cual es ru ñrimee recuerdi"));
        assertEquals("dime cual es tu primer recuerdo", TypoTolerantText.forRouting("dime cual es tu primer rwcuperdo?"));
        assertEquals("de tu primer recuerdo", TypoTolerantText.forRouting("De tu primer rexuerdo"));
        assertEquals("hola mundo", TypoTolerantText.forRouting("Hola mundo"));
        assertEquals("su casa", TypoTolerantText.forRouting("su casa"));
    }

    @Test public void recognisesOldestMemoryQuestionsFromTheScreenshots() {
        assertEquals(-1, ConversationMemoryGrounding.chronologyDirection("Entonces dime cual es tu primer rwcuperdo?"));
        assertEquals(-1, ConversationMemoryGrounding.chronologyDirection("Cual es ru ñrimee recuerdi  o que te dije en ese momento?"));
        assertEquals(-1, ConversationMemoryGrounding.chronologyDirection("De tu primer rexuerdo que te dije en el"));
        assertEquals(-1, ConversationMemoryGrounding.chronologyDirection("¿Cuál es tu primer recuerdo?"));
        assertEquals(1, ConversationMemoryGrounding.chronologyDirection("dime tu último recuerdo"));
    }

    @Test public void leavesTopicalFirstMemoriesToNormalSearch() {
        assertEquals(0, ConversationMemoryGrounding.chronologyDirection("cual es tu primer recuerdo del viaje a Roma"));
        assertEquals(0, ConversationMemoryGrounding.chronologyDirection("el primer recuerdo de la infancia es importante"));
        assertEquals(0, ConversationMemoryGrounding.chronologyDirection("hablemos de música"));
    }
}
