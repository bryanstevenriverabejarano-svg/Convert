package salve.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeminiProtocolTest {
    private String candidate(String parts, String finish) {
        return "{\"candidates\":[{\"finishReason\":\"" + finish
                + "\",\"content\":{\"parts\":" + parts + "}}]}";
    }

    @Test public void requestContainsQuestionAndActualImagePart() {
        JsonObject request = JsonParser.parseString(GeminiProtocol.request(
                "¿Qué ves?\n\"foto\"", Collections.singletonList("anBlZw=="))).getAsJsonObject();
        JsonArray parts = request.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts");
        assertEquals("¿Qué ves?\n\"foto\"", parts.get(0).getAsJsonObject().get("text").getAsString());
        JsonObject image = parts.get(1).getAsJsonObject().getAsJsonObject("inlineData");
        assertEquals("image/jpeg", image.get("mimeType").getAsString());
        assertEquals("anBlZw==", image.get("data").getAsString());
    }

    @Test public void textRequestHasNoAmbientImages() {
        JsonObject request = JsonParser.parseString(GeminiProtocol.request("Hola", null)).getAsJsonObject();
        assertEquals(1, request.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts").size());
    }

    @Test public void privateThoughtPartsAreNeverUserText() {
        ModelResult result = GeminiProtocol.response(200, candidate(
                "[{\"thought\":true,\"text\":\"privado\"},{\"text\":\"Hola\"},{\"text\":\" mundo\"}]", "STOP"), 17L);
        assertEquals("Hola mundo", result.getText());
        assertEquals(17L, result.getLatencyMillis());
    }

    @Test public void thoughtOnlyEmptyOrMalformedResultsAreNotInferenceSuccess() {
        String[] bodies = {"{}", "{\"candidates\":[]}", "not JSON", "null",
                candidate("[{\"thought\":true,\"text\":\"privado\"}]", "STOP"),
                candidate("[{\"text\":\" \"}]", "STOP")};
        for (String body : bodies) assertFalse(GeminiProtocol.response(200, body, 0L).isSuccess());
    }

    @Test public void blockedTruncatedAndToolOnlyContentAreNotCompletedAnswers() {
        assertFalse(GeminiProtocol.response(200,
                "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", 0L).isSuccess());
        assertFalse(GeminiProtocol.response(200, candidate("[{\"text\":\"partial\"}]", "MAX_TOKENS"), 0L).isSuccess());
        assertFalse(GeminiProtocol.response(200, candidate("[{\"text\":\"blocked\"}]", "SAFETY"), 0L).isSuccess());
        assertFalse(GeminiProtocol.response(200, candidate("[{\"functionCall\":{}}]", "STOP"), 0L).isSuccess());
    }

    @Test public void httpErrorsRemainErrorsAndDoNotExposeRawBody() {
        for (int code : new int[]{400, 401, 403, 404, 429, 500, 302}) {
            ModelResult result = GeminiProtocol.response(code, "secret-key-and-user-data", 3L);
            assertFalse(result.isSuccess());
            assertNull(result.getText());
            assertFalse(result.getError().contains("secret-key"));
            assertTrue(result.getError().contains(Integer.toString(code)));
        }
    }

    @Test public void modelIsConfigurableButCannotChangeEndpoint() {
        assertEquals(GeminiProtocol.DEFAULT_MODEL, GeminiProtocol.modelName(" "));
        assertEquals("gemini-custom", GeminiProtocol.modelName("gemini-custom"));
        for (String bad : new String[]{"https://example.com", "../other", "model?key=secret", "models/model"}) {
            assertThrows(IllegalArgumentException.class, () -> GeminiProtocol.modelName(bad));
        }
    }

    @Test public void invalidImagesCannotBeSilentlyOmitted() {
        assertThrows(IllegalArgumentException.class, () -> GeminiProtocol.request("foto", Collections.singletonList("")));
    }
}
