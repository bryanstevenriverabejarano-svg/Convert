package salve.core.cognition;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import salve.core.autonomy.AutonomousToolCommand;
import salve.core.conversation.ChatMessage;

/**
 * El motor local reutiliza lo que coincide con el principio del prompt anterior. Entre las
 * inferencias de un mismo turno (elegir herramienta → explicar el recibo → reparar formato),
 * casi todo debe coincidir: lo que cambia (estado, tarea, recibos) va al final.
 */
public class CognitivePromptCacheOrderTest {
    private static String rep(String text, int length) {
        StringBuilder out = new StringBuilder();
        while (out.length() < length) out.append(text);
        return out.substring(0, length);
    }

    private static JsonObject state(long at, long ram) {
        JsonObject state = new JsonObject();
        state.addProperty("at_ms", at);
        state.addProperty("ram_available", ram);
        state.addProperty("phase", "before_inference");
        return state;
    }

    private static List<ChatMessage> history() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 6; i++)
            history.add(new ChatMessage(i % 2 == 0 ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT,
                    rep("Mensaje " + i + " de la conversación. ", 320), 1000 + i));
        return history;
    }

    /** Lo que recibe el motor: {sistema, usuario}, como en GgufLlm. */
    private static String engineView(String prompt) {
        String[] arranged = CognitivePrompt.cacheFriendly(CognitivePrompt.splitStableRules(prompt));
        return arranged[0] + "\u0000" + arranged[1];
    }

    private static int common(String a, String b) {
        int i = 0, n = Math.min(a.length(), b.length());
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    @Test public void receiptInferenceReusesMostOfThePrompt() {
        String input = "¿Cómo está tu memoria?";
        Map<String, CognitiveTurn.Tool> tools = CognitivePrompt.limitTools(CognitivePrompt.prioritize(
                CognitiveToolCatalog.build(input, false, true, false, (n, a) -> CognitiveTurn.Receipt.handoff()), input), 7000);
        String memory = rep("Recuerdo verificado de Bryan. ", 3200), self = rep("Soy Salve. ", 1500);
        JsonArray receipts = new JsonArray();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("name", SelfCommandIntent.TOOL);
        receipt.addProperty("status", "completed");
        receipt.addProperty("data", "MEMORY_HEALTH_REAL: " + rep("1.240 recuerdos, índice al día. ", 900));
        receipts.add(receipt);

        String first = CognitivePrompt.build("", input, state(1000, 3_000_000_000L), self, memory, "Contexto.",
                history(), tools, new JsonArray(), false, 2, 7000);
        String second = CognitivePrompt.build("", input, state(9000, 2_900_000_000L), self, memory, "Contexto.",
                history(), tools, receipts, false, 1, 7000);
        String last = CognitivePrompt.build("", input, state(9500, 2_900_000_000L), self, memory, "Contexto.",
                history(), tools, receipts, true, 0, 7000);
        for (String prompt : new String[] {first, second, last}) assertTrue(prompt.length() <= 7000);

        String a = engineView(first), b = engineView(second), c = engineView(last);
        assertTrue("recibo: reutiliza " + common(a, b) + " de " + b.length(), common(a, b) * 10 >= b.length() * 7);
        // La última inferencia oculta el catálogo, pero las reglas fijas y la memoria siguen igual.
        assertTrue("final: reutiliza " + common(b, c) + " de " + c.length(), common(b, c) * 10 >= c.length() * 6);

        // Nada se pierde: el recibo y la petición llegan enteros al modelo.
        JsonObject data = AutonomousToolCommand.parseObject(second.substring(second.lastIndexOf('\n') + 1));
        assertEquals(input, data.get("USER_INPUT").getAsString());
        assertTrue(data.get("TOOL_RESULTS").getAsString().contains("MEMORY_HEALTH_REAL"));
        assertFalse(data.get("MEMORY_CONTEXT").getAsString().isEmpty());
        // Memoria e historial se cortan en el mismo sitio aunque llegue el recibo.
        JsonObject before = AutonomousToolCommand.parseObject(first.substring(first.lastIndexOf('\n') + 1));
        assertFalse(data.getAsJsonArray("HISTORY").isEmpty());
        assertEquals(before.get("HISTORY"), data.get("HISTORY"));
        assertEquals(before.get("MEMORY_CONTEXT"), data.get("MEMORY_CONTEXT"));
        assertEquals(before.get("SELF_MODEL"), data.get("SELF_MODEL"));
    }

    @Test public void systemStateMovesToTheEndUnchanged() {
        String prompt = CognitivePrompt.build("", "hola", state(1234, 5L), "", "", "", history(),
                java.util.Collections.emptyMap(), new JsonArray(), false, 2, 7000);
        String[] split = CognitivePrompt.splitStableRules(prompt);
        String[] arranged = CognitivePrompt.cacheFriendly(split);
        assertEquals(split[0], arranged[0]);
        String stateLine = prompt.substring(prompt.indexOf("SYSTEM_STATE: "), prompt.indexOf('\n', prompt.indexOf("SYSTEM_STATE: ")));
        assertTrue(arranged[1].startsWith("SALVE_COGNITIVE_V1\n{"));
        assertTrue(arranged[1].endsWith("\n" + stateLine));
        // El mismo contenido, sólo en otro orden.
        assertEquals(split[1].length(), arranged[1].length());
        assertNull(CognitivePrompt.cacheFriendly(null));
    }

    @Test public void timingTextSaysWhichInferenceAndWhatTheEngineReused() {
        assertEquals("Inferencia 1 (primera): 2.5 s",
                TurnTiming.inference(1, 0, false, 2_500_000_000L, null));
        assertEquals("Inferencia 2 (tras 1 recibo(s)): 1.0 s · gráfica: leyó 400 tokens (+1500 ya en caché)",
                TurnTiming.inference(2, 1, false, 1_000_000_000L, "gráfica: leyó 400 tokens (+1500 ya en caché)"));
        assertTrue(TurnTiming.inference(3, 1, true, 0, "").contains("reparación de formato"));
        assertEquals("2 inferencia(s): modelo 3.5 s, herramientas y resto 0.5 s, total 4.0 s",
                TurnTiming.turn(2, 3_500_000_000L, 4_000_000_000L));
    }
}
