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
