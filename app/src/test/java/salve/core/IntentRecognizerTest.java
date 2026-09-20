package salve.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IntentRecognizerTest {
    private final IntentRecognizer recognizer = new IntentRecognizer();

    @Test
    public void casualAndAmbiguousInputsRemainConversation() {
        assertEquals(IntentType.NINGUNO, recognizer.recognize("Hoy ha sido un día largo").type);
        assertEquals(IntentType.NINGUNO, recognizer.recognize("Podríamos hablar de música").type);
        assertEquals(IntentType.NINGUNO, recognizer.recognize("Haz algo interesante").type);
    }

    @Test
    public void explicitStateChangingIntentKeepsItsSlot() {
        IntentRecognizer.Intent intent = recognizer.recognize("Añade misión preparar una demo");

        assertEquals(IntentType.AGREGAR_MISION, intent.type);
        assertEquals("preparar una demo", intent.slots.get("mision"));
    }

    @Test
    public void explicitSearchRemainsAvailable() {
        IntentRecognizer.Intent intent = recognizer.recognize("Busca arquitectura hexagonal");

        assertEquals(IntentType.BUSCAR_WEB, intent.type);
        assertEquals("arquitectura hexagonal", intent.slots.get("termino"));
    }

    @Test
    public void askingAboutAMemoryDoesNotStoreANewOne() {
        IntentRecognizer.Intent intent = recognizer.recognize("¿Qué recuerdo tienes de vacaciones?");

        assertEquals(IntentType.BUSCAR_RECUERDO_TEXT, intent.type);
    }
}
