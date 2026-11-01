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
    @Test
    public void talkingAboutSavingIsNotPermissionToWriteMemory() {
        for (String discussion : new String[]{
                "¿Por qué debería guardar recuerdos?",
                "¿Debería guardar una idea o dejarla pasar?",
                "No quiero guardar nada de esto",
                "Creo que guardar mis notas sería útil",
                "Analiza las ventajas de guardar la información",
                "En un cuento alguien dijo: guarda este secreto",
                "Podríamos guardar algunos datos, pero no estoy seguro",
                "Me preguntaba si es bueno guardar recuerdos",
                "¿Qué significa x = y?",
                "x = y",
                "2 + 2 = 4",
                "guarda"
        }) {
            assertEquals("Persistencia implícita: " + discussion, IntentType.NINGUNO,
                    recognizer.recognize(discussion).type);
        }
    }

    @Test
    public void deliberateMemoryCommandsStillWorkAndKeepOnlyPayload() {
        for (String[] example : new String[][]{
                {"Guarda que mi novela se llama Aurora", "que mi novela se llama aurora"},
                {"Salve, por favor, guarda mi proyecto favorito", "mi proyecto favorito"},
                {"Anota x = y", "x = y"},
                {"Guardar: x = y", "x = y"},
                {"Aprendí que la historia empieza en la Luna", "la historia empieza en la luna"},
                {"Registra: una decisión pendiente", "una decisión pendiente"}
        }) {
            IntentRecognizer.Intent actual = recognizer.recognize(example[0]);
            assertEquals(example[0], IntentType.GUARDAR_RECUERDO, actual.type);
            assertEquals(example[0], example[1], actual.slots.get("frase"));
        }
    }

    @Test
    public void describingSleepOrMissionsNeverStartsBackgroundWork() {
        for (String discussion : new String[]{
                "¿Cómo funciona el modo sueño?",
                "A veces quiero dormir mejor",
                "Explícame el ciclo de sueño de Salve",
                "En la historia alguien dice duerme tranquilamente",
                "¿Qué significa nueva misión?",
                "Creo que una nueva misión puede ser útil",
                "¿Puedes explicarme cómo se agrega una misión?",
                "No quiero activar el modo sueño",
                "Mi duda es si debo iniciar un ciclo de sueño"
        }) {
            assertEquals("Una conversación no autoriza efectos: " + discussion,
                    IntentType.NINGUNO, recognizer.recognize(discussion).type);
        }
    }

    @Test
    public void explicitSleepCommandsArePreserved() {
        for (String request : new String[]{
                "Duerme", "Duerme y reflexiona", "Salve, duerme ahora",
                "Inicia el ciclo de sueño", "Activa el modo sueño",
                "Salve, ponte a dormir", "Modo sueño"
        }) {
            assertEquals(request, IntentType.CICLO_SUENO, recognizer.recognize(request).type);
        }
    }

    @Test
    public void onlyExplicitMissionCommandStoresAnActualMission() {
        for (String[] request : new String[][]{
                {"Nueva misión: desarrollar el cuento", "desarrollar el cuento"},
                {"Salve, agrega misión analizar la memoria", "analizar la memoria"},
                {"Añade misión preparar una demo", "preparar una demo"}
        }) {
            IntentRecognizer.Intent result = recognizer.recognize(request[0]);
            assertEquals(request[0], IntentType.AGREGAR_MISION, result.type);
            assertEquals(request[0], request[1], result.slots.get("mision"));
        }
    }

}
