package salve.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Collections;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Opt-in tests call actual runtimes. No credentials, canned outputs or network mocks. */
public class RealInferenceTest {
    @Test public void downloadedLocalModelReceivesActualImagePixels() {
        optedIn("runRealLocalVision");
        SalveLLM engine = SalveLLM.getInstance(context());
        assertTrue("Download and activate Gemma 4 in the app first", engine.supportsVision());
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        try {
            ModelResult result = engine.generateImageResult(
                    "¿Cuál es el color principal de esta imagen? Responde con una palabra en español.", image);
            assertTrue(result.getError(), result.isSuccess());
            assertTrue(result.getText().toLowerCase(Locale.ROOT).contains("rojo"));
        } finally { image.recycle(); }
    }
    /** Opt in on a device with its downloaded model; verifies RAW output, before text fallback. */
    @Test public void configuredLocalRuntimeHonorsCognitiveSchema() {
        optedIn("runRealStructuredLocal");
        SalveLLM engine = SalveLLM.getInstance(context());
        engine.forceReloadModel();
        for (String question : new String[]{"Hola Salve, ¿cómo estás?", "Me gustaría saber de ti."}) {
            String prompt = salve.core.cognition.CognitivePrompt.build("", question,
                    new com.google.gson.JsonObject(), "", "", Collections.emptyList(),
                    Collections.emptyMap(), new com.google.gson.JsonArray(), false, 2,
                    engine.getConversationPromptBudgetChars());
            ModelResult result = engine.generateResult(prompt, SalveLLM.Role.CONVERSACIONAL);
            assertTrue(result.getError(), result.isSuccess());
            salve.core.cognition.CognitiveReply reply = salve.core.cognition.CognitiveReply.parse(result.getText());
            assertNull("No tools were offered", reply.name);
            assertFalse(reply.message.trim().isEmpty());
        }
    }

    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void optedIn(String flag) {
        assumeTrue("Real inference not requested", "true".equals(
                InstrumentationRegistry.getArguments().getString(flag)));
    }

    @Test public void geminiGeneratesText() {
        optedIn("runRealGemini");
        GeminiService service = GeminiService.getInstance(context());
        assertTrue("Configure Gemini in the app first", service.isAvailable());
        ModelResult result = service.generateResultSync("¿Cuánto es 17 + 25? Responde solo con el número.", null);
        assertTrue(result.getError(), result.isSuccess());
        assertEquals("42", result.getText().trim());
    }

    @Test public void geminiReceivesAndDescribesImagePixels() {
        optedIn("runRealGemini");
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        try {
            ModelResult result = GeminiService.getInstance(context()).generateResultSync(
                    "¿Cuál es el color principal de esta imagen? Responde con una palabra en español.",
                    Collections.singletonList(image));
            assertTrue(result.getError(), result.isSuccess());
            assertTrue(result.getText().toLowerCase(Locale.ROOT).contains("rojo"));
        } finally { image.recycle(); }
    }

    @Test public void configuredLocalRuntimeGeneratesText() {
        optedIn("runRealLocal");
        SalveLLM engine = SalveLLM.getInstance(context());
        engine.forceReloadModel();
        ModelResult result = engine.generateResult("¿Cuánto es 17 + 25? Responde solo con el número.",
                SalveLLM.Role.CONVERSACIONAL);
        assertTrue(result.getError(), result.isSuccess());
        assertEquals("42", result.getText().trim());
    }
}
