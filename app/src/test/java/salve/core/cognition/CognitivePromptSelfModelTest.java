package salve.core.cognition;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import salve.core.conversation.ChatMessage;

public class CognitivePromptSelfModelTest {
    private static final String SELF = "YO_PERSISTENTE v3\nNarrativa: Soy Salve.\nCreencias:\n- [b1 bryan 0.60 fuentes:recuerdos:4] Bryan revisa facturas los lunes";

    @Test public void selfModelKeepsItsPlaceUnderHeavyMemoryAndHistory() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 16; i++) history.add(new ChatMessage(i % 2 == 0 ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT,
                "Mensaje largo de historial ".repeat(40), i));
        String prompt = CognitivePrompt.build("", "¿Qué sueles hacer los lunes?", new JsonObject(), SELF,
                "recuerdo ".repeat(2000), "contexto ".repeat(2000), history, Collections.emptyMap(), new JsonArray(), false, 2, 6000);
        assertTrue(prompt.length() <= 6000);
        JsonObject data = salve.core.autonomy.AutonomousToolCommand.parseObject(prompt.substring(prompt.lastIndexOf('\n') + 1));
        assertEquals(SELF, data.get("SELF_MODEL").getAsString());
    }

    @Test public void selfModelIsBoundedSoItCannotStarveTheTurn() {
        String huge = "creencia ".repeat(5000);
        String prompt = CognitivePrompt.build("", "hola", new JsonObject(), huge, "", "", Collections.emptyList(),
                Collections.emptyMap(), new JsonArray(), false, 2, 8000);
        JsonObject data = salve.core.autonomy.AutonomousToolCommand.parseObject(prompt.substring(prompt.lastIndexOf('\n') + 1));
        assertTrue(data.get("SELF_MODEL").getAsString().length() <= 1400);
        assertEquals("hola", data.get("USER_INPUT").getAsString());
    }

    @Test public void legacyCallersStillProduceTheSameShape() {
        String prompt = CognitivePrompt.build("", "hola", new JsonObject(), "", "", Collections.emptyList(),
                Collections.emptyMap(), new JsonArray(), false, 2, 8000);
        JsonObject data = salve.core.autonomy.AutonomousToolCommand.parseObject(prompt.substring(prompt.lastIndexOf('\n') + 1));
        assertFalse(data.has("SELF_MODEL"));
    }
}
