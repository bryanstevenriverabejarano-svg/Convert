package salve.core.cognition;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import salve.core.conversation.ChatMessage;

/** Las reglas fijas se separan para que el motor local las conserve leídas entre respuestas. */
public class CognitivePromptCacheSplitTest {
    private static String prompt(long at, String input, String core) {
        JsonObject state = new JsonObject();
        state.addProperty("at_ms", at);
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage(ChatMessage.Role.USER, "hola", 1));
        history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, "¡Hola, Bryan!", 2));
        return CognitivePrompt.build(core, input, state, "", "", "", history, Collections.emptyMap(),
                new JsonArray(), false, 2, 7000);
    }

    @Test public void rulesAreIdenticalAcrossTurnsAndNothingIsLost() {
        String a = prompt(1000L, "¿Qué tal?", ""), b = prompt(2000L, "Cuéntame un cuento", "");
        String[] sa = CognitivePrompt.splitStableRules(a), sb = CognitivePrompt.splitStableRules(b);
        assertNotNull(sa);
        assertNotNull(sb);
        // Mismas reglas aunque cambien la hora y el mensaje: eso es lo que el motor reutiliza.
        assertEquals(sa[0], sb[0]);
        assertTrue(sa[0].contains("Eres Salve"));
        // El resto sigue siendo un prompt cognitivo válido, con el estado y los datos completos.
        assertTrue(CognitivePrompt.isCognitive(sa[1]));
        assertFalse(sa[1].contains("Eres Salve"));
        assertEquals(a.length(), sa[0].length() + sa[1].length());
        assertTrue(sa[1].contains("\"at_ms\":1000"));
        assertTrue(sa[1].endsWith(a.substring(a.lastIndexOf('\n') + 1)));
    }

    @Test public void fullMultilineCoreIsKeptWhole() {
        String core = "NÚCLEO\nlínea dos\nlínea tres";
        String[] split = CognitivePrompt.splitStableRules(prompt(1L, "hola", core));
        assertNotNull(split);
        assertTrue(split[0].startsWith(core));
    }

    @Test public void formatRepairFlagSurvivesTheSplit() {
        JsonObject state = new JsonObject();
        String repair = CognitivePrompt.build("", "hola", state, "", "", "", Collections.emptyList(),
                Collections.emptyMap(), new JsonArray(), true, 0, 7000);
        assertTrue(CognitivePrompt.isFormatRepair(CognitivePrompt.splitStableRules(repair)[1]));
    }

    @Test public void otherPromptsAreLeftAlone() {
        assertNull(CognitivePrompt.splitStableRules("Cuéntame un cuento"));
        assertNull(CognitivePrompt.splitStableRules(null));
    }
}
