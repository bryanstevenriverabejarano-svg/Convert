package salve.core.voice;

import static org.junit.Assert.*;
import static salve.core.voice.VoiceEngineCommand.Target.*;
import org.junit.Test;

public class VoiceEngineCommandTest {
    @Test public void choosesTheNeuralVoice() {
        assertEquals(NEURAL, VoiceEngineCommand.parse("Usa tu voz nueva"));
        assertEquals(NEURAL, VoiceEngineCommand.parse("Activar voz neuronal"));
        assertEquals(NEURAL, VoiceEngineCommand.parse("pon una voz más femenina"));
        assertEquals(NEURAL, VoiceEngineCommand.parse("descarga tu voz neuronal"));
        assertEquals(NEURAL, VoiceEngineCommand.parse("Salve, habla con tu voz natural"));
    }

    @Test public void goesBackToAndroid() {
        assertEquals(ANDROID, VoiceEngineCommand.parse("usa la voz de Android"));
        assertEquals(ANDROID, VoiceEngineCommand.parse("vuelve a tu voz de antes"));
        assertEquals(ANDROID, VoiceEngineCommand.parse("quita la voz nueva"));
        assertEquals(ANDROID, VoiceEngineCommand.parse("Desactivar voz neuronal"));
        assertEquals(ANDROID, VoiceEngineCommand.parse("Salve, desactiva la voz neuronal"));
        assertEquals(ANDROID, VoiceEngineCommand.parse("apaga la voz neuronal"));
        assertEquals(ANDROID, VoiceEngineCommand.parse("deshabilita la voz neuronal"));
    }

    @Test public void conversationIsNotACommand() {
        assertNull(VoiceEngineCommand.parse("me gusta tu voz nueva"));
        assertNull(VoiceEngineCommand.parse("qué voz tienes"));
        assertNull(VoiceEngineCommand.parse("usa la cuenta de gmail"));
        assertNull(VoiceEngineCommand.parse(null));
    }
}
