package salve.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ModelRuntimeInfoTest {
    @Test public void answersModelQuestionsInTextAndVoiceWithoutGeneratingAGuess() {
        for (String question : new String[]{"¿Qué modelo estás usando?", "qué modelo usas", "Salve, qué modelo de lenguaje utilizas",
                "¿Estás usando Dolphin o Gemma?", "modelo activo", "¿Cuál es tu modelo activo?"}) {
            assertTrue(question, ModelRuntimeInfo.isStatusQuestion(question));
        }
        assertFalse(ModelRuntimeInfo.isStatusQuestion("Descarga Dolphin y explícamelo"));
        assertFalse(ModelRuntimeInfo.isStatusQuestion("Qué modelo de teléfono me recomiendas"));
        assertFalse(ModelRuntimeInfo.isStatusQuestion("Dime qué modelo usas para hacer un ejemplo de programación"));
    }
    @Test public void eachTurnReceivesTheCurrentModelNotTheHistoricalName() {
        String big = ModelRuntimeInfo.context("/models/Dolphin3.0-Llama3.1-8B-Q4_K_M-fd2736a.gguf");
        String small = ModelRuntimeInfo.context("/models/Dolphin3.0-Llama3.2-3B-Q4_K_M-ac6b1ee.gguf");
        String gemma = ModelRuntimeInfo.context("/models/gemma-4-E2B-it-6e5c4f1.litertlm");
        assertTrue(big.contains(LocalModelPolicy.PRIMARY));
        assertTrue(small.contains(LocalModelPolicy.LIGHT));
        assertFalse(small.contains(LocalModelPolicy.PRIMARY));
        assertTrue(gemma.contains(LocalModelPolicy.FALLBACK));
        assertTrue(ModelRuntimeInfo.name("/models/imported-1.gguf").startsWith("Modelo importado"));
        assertEquals("ningún modelo local", ModelRuntimeInfo.name(null));
    }
    @Test public void providerIsCapturedWithTheResponseNotReadLaterFromPreferences() {
        ModelResult response = ModelResult.success("Hola", 7).withProvider(LocalModelPolicy.LIGHT);
        assertEquals(LocalModelPolicy.LIGHT, response.getProvider());
        assertEquals("Hola", response.getText());
        assertEquals(7, response.getLatencyMillis());
        assertTrue(response.isSuccess());
    }
}
