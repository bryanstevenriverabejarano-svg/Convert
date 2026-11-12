package salve.core.voice;

import static org.junit.Assert.*;
import org.junit.Test;

/** En Android Auto la voz y el foco van con el mismo uso (guía); fuera del coche, todo como siempre. */
public class SpeechAudioPolicyTest {
    @Test public void inTheCarVoiceAndFocusShareTheGuidanceUsage() {
        assertEquals(SpeechAudioPolicy.USAGE_GUIDANCE, SpeechAudioPolicy.speechUsage(true));
        assertEquals(SpeechAudioPolicy.USAGE_GUIDANCE, SpeechAudioPolicy.focusUsage(true));
    }

    @Test public void outsideTheCarNothingChanges() {
        assertEquals("la voz sigue siendo multimedia", SpeechAudioPolicy.USAGE_MEDIA, SpeechAudioPolicy.speechUsage(false));
        assertEquals("el foco sigue siendo de asistente", SpeechAudioPolicy.USAGE_ASSISTANT, SpeechAudioPolicy.focusUsage(false));
    }

    @Test public void valuesMatchAndroidAudioAttributes() {
        assertEquals(1, SpeechAudioPolicy.USAGE_MEDIA);              // USAGE_MEDIA
        assertEquals(12, SpeechAudioPolicy.USAGE_GUIDANCE);          // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        assertEquals(16, SpeechAudioPolicy.USAGE_ASSISTANT);         // USAGE_ASSISTANT
    }

    @Test public void ownFocusOnlyInTheCarAndWithoutAnOpenVoiceConversation() {
        assertTrue("respuesta escrita en el coche: pide foco breve", SpeechAudioPolicy.needsOwnFocus(true, false));
        assertFalse("con la conversación de voz abierta ya tiene foco (pedir otro la cortaría)",
                SpeechAudioPolicy.needsOwnFocus(true, true));
        assertFalse("fuera del coche, nada nuevo", SpeechAudioPolicy.needsOwnFocus(false, false));
        assertFalse(SpeechAudioPolicy.needsOwnFocus(false, true));
    }

    @Test public void inHandsFreeHerVoiceGoesThroughTheCallChannelSoEchoIsCancelled() {
        assertEquals(SpeechAudioPolicy.USAGE_VOICE_COMMUNICATION, SpeechAudioPolicy.speechUsage(false, true));
        assertEquals(SpeechAudioPolicy.USAGE_MEDIA, SpeechAudioPolicy.speechUsage(false, false));
        assertEquals("en el coche manda la guía", SpeechAudioPolicy.USAGE_GUIDANCE, SpeechAudioPolicy.speechUsage(true, true));
        assertEquals(SpeechAudioPolicy.speechUsage(false), SpeechAudioPolicy.speechUsage(false, false));
    }
}
