package salve.core.voice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VoiceResponsePolicyTest {
    @Test
    public void voiceResponsesAreShorterThanTextResponses() {
        int voice = VoiceResponsePolicy.maxResponseChars(true);
        int text = VoiceResponsePolicy.maxResponseChars(false);
        assertTrue(voice < text);
        assertEquals(420, voice);
        assertEquals(900, text);
    }

    @Test
    public void onlyVoiceAddsSpokenStyleInstruction() {
        assertFalse(VoiceResponsePolicy.promptInstruction(true).isEmpty());
        assertTrue(VoiceResponsePolicy.promptInstruction(false).isEmpty());
    }

    @Test
    public void drivingAffectsOnlySpokenLengthAndAddsSafetyInstruction() {
        assertEquals(220, VoiceResponsePolicy.maxResponseChars(true, true));
        assertEquals(420, VoiceResponsePolicy.maxResponseChars(true, false));
        assertEquals(900, VoiceResponsePolicy.maxResponseChars(false, true));
        assertTrue(VoiceResponsePolicy.promptInstruction(true, true).contains("CONDUCCIÓN"));
        assertTrue(VoiceResponsePolicy.promptInstruction(true, true).contains("estacionado"));
        assertTrue(VoiceResponsePolicy.promptInstruction(false, true).isEmpty());
    }
}
