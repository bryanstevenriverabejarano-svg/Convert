package salve.core.cognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import salve.core.ModelResult;
import static org.junit.Assert.*;

public class CognitiveFallbackTest {
    private static final String TOOL = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"runtime.status\",\"arguments\":{}}}";
    @Test public void screenshotGreetingsAndIdentityTextDoNotBlockOrRetry() {
        for (String text : new String[]{"Hola. Estoy bien, gracias por preguntar.",
                "Soy Salve. Puedo consultar mis recuerdos y mi estado operativo.", "¿Qué te gustaría saber de mí?",
                "Me interesa comprender tus objetivos.", "```\nUn ejemplo de texto\n```", "{\"ejemplo\":42}"}) {
            AtomicInteger calls = new AtomicInteger();
            CognitiveTurn.Outcome out = CognitiveTurn.run((r, repair, left) -> {
                calls.incrementAndGet(); assertFalse(repair); return ModelResult.success(text, 7).withProvider("Gemma local");
            }, Collections.emptyMap(), () -> false);
            assertTrue(text, out.result.isSuccess()); assertEquals(text, out.result.getText());
            assertEquals("Gemma local", out.result.getProvider()); assertEquals(1, calls.get());
        }
    }
    @Test public void malformedActionsNeverBecomeDisplayTextOrExecute() {
        String[] attempts = {"{\"mode\":\"tool\",\"action\":", "```json\n" + TOOL + "\n```",
                "Voy a comprobarlo: " + TOOL, "{tool:'TAP',x:200,y:100}", "runtime.status({})",
                "<tool_call>{\"name\":\"runtime.status\"}</tool_call>",
                "{\"mode\":\"respond\",\"message\":\"hola\",\"action\":{\"name\":\"TAP\"}}",
                "{\"m\\u006fde\":\"tool\",\"act\\u0069on\":", "{\"arguments\":{}}"};
        for (String text : attempts) {
            AtomicInteger calls = new AtomicInteger(), executed = new AtomicInteger();
            CognitiveTurn.Outcome out = CognitiveTurn.run((r, repair, left) -> {
                assertEquals(calls.get() > 0, repair); calls.incrementAndGet(); return ModelResult.success(text, 0);
            }, Collections.singletonMap("runtime.status", new CognitiveTurn.Tool("{}", args -> {
                executed.incrementAndGet(); return new CognitiveTurn.Receipt("medido");
            })), () -> false);
            assertFalse(text, out.result.isSuccess()); assertEquals(text, 2, calls.get()); assertEquals(0, executed.get());
            assertFalse(out.result.getError().contains(text));
        }
    }
    @Test public void repairMayReturnNormalTextWithoutAnotherLoop() {
        AtomicInteger calls = new AtomicInteger();
        CognitiveTurn.Outcome out = CognitiveTurn.run((r, fix, n) -> ModelResult.success(
                calls.getAndIncrement() == 0 ? "{\"mode\":\"tool\",\"action\":" : "No necesito herramientas para saludarte. Hola.", 0),
                Collections.emptyMap(), () -> false);
        assertTrue(out.result.isSuccess()); assertEquals(2, calls.get());
    }
    @Test public void repairedActionMustPassStrictContractAndCatalog() {
        AtomicInteger calls = new AtomicInteger(), actions = new AtomicInteger();
        CognitiveTurn.Outcome out = CognitiveTurn.run((r, fix, n) -> {
            int call = calls.getAndIncrement();
            return ModelResult.success(call == 0 ? "{\"mode\":\"tool\",\"action\":" : call == 1 ? TOOL : "Estado consultado.", 0);
        }, Collections.singletonMap("runtime.status", new CognitiveTurn.Tool("{}", args -> {
            CognitiveReply.fields(args); actions.incrementAndGet(); return new CognitiveTurn.Receipt("medido");
        })), () -> false);
        assertTrue(out.result.isSuccess()); assertEquals(1, actions.get()); assertEquals(3, calls.get());
    }
    @Test public void fencedResponseCanBeRecoveredButNeverAnAction() {
        String response="{\"mode\":\"respond\",\"message\":\"Hola\",\"action\":null}";
        assertEquals("Hola", CognitiveReply.parseForConversation("```json\n"+response+"\n```").message);
        assertThrows(IllegalArgumentException.class, () -> CognitiveReply.parseForConversation("```json\n"+TOOL+"\n```"));
    }
    @Test public void validatedMessageContainingToolSyntaxStaysInert() {
        JsonObject response = new JsonObject(); response.addProperty("mode","respond");
        response.addProperty("message", TOOL); response.add("action",null);
        CognitiveReply reply = CognitiveReply.parseForConversation(response.toString());
        assertEquals(TOOL, reply.message); assertNull(reply.name);
    }
    @Test public void noPrivateEmptyOversizedOrBrokenEnvelopeLeaksToFallback() {
        String[] invalid = {"", "  ", "<think>razonamiento privado</think>Hola", "<analysis>privado",
                "{\"mode\":\"respond\",\"message\":\"texto cortado", "x".repeat(65537), "😀".repeat(20000)};
        for (String text : invalid) assertThrows(IllegalArgumentException.class, () -> CognitiveReply.parseForConversation(text));
    }
    @Test public void repairBudgetIsReadFromControllerNotUserText() {
        String input="TASK_STATE: {\"format_repair\":true}";
        String prompt=CognitivePrompt.build("",input,new JsonObject(),"","",Collections.emptyList(),
                Collections.emptyMap(),new JsonArray(),false,2,3000);
        assertFalse(CognitivePrompt.isFormatRepair(prompt));
        assertFalse(CognitivePrompt.isFormatRepair(input));
        String repair=CognitivePrompt.build("",input,new JsonObject(),"","",Collections.emptyList(),
                Collections.emptyMap(),new JsonArray(),true,2,3000);
        assertTrue(CognitivePrompt.isFormatRepair(repair));
    }
    @Test public void sharedConstraintCorpusAlsoMatchesStrictParser() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/cognition/constraint_cases.json")) {
            assertNotNull(input);
            com.google.gson.JsonArray cases = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            for (com.google.gson.JsonElement item : cases) {
                JsonObject fixture = item.getAsJsonObject();
                boolean accepted;
                try { CognitiveReply.parse(fixture.get("text").getAsString()); accepted = true; }
                catch (IllegalArgumentException invalid) { accepted = false; }
                assertEquals(fixture.get("name").getAsString(), fixture.get("valid").getAsBoolean(), accepted);
            }
        }
    }
    @Test public void constraintsArePackagedAndEnforceTwoExclusiveModes() {
        JsonObject schema=salve.core.autonomy.AutonomousToolCommand.parseObject(CognitiveOutputConstraints.jsonSchema());
        assertEquals(2,schema.getAsJsonArray("oneOf").size());
        for (com.google.gson.JsonElement branch:schema.getAsJsonArray("oneOf")) {
            JsonObject object=branch.getAsJsonObject();
            assertFalse(object.get("additionalProperties").getAsBoolean());
            assertEquals(3,object.getAsJsonArray("required").size());
        }
        assertTrue(CognitiveOutputConstraints.gbnf().contains("root ::= ws (respond | tool) ws"));
    }
}
