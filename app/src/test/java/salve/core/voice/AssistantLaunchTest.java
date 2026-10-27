package salve.core.voice;

import static org.junit.Assert.*;

import org.junit.Test;

public class AssistantLaunchTest {
    @Test public void assistAndHeadsetVoiceCommandOpenVoice() {
        assertTrue(AssistantLaunch.isVoiceLaunch("android.intent.action.ASSIST"));
        assertTrue(AssistantLaunch.isVoiceLaunch("android.intent.action.VOICE_COMMAND"));
        assertFalse(AssistantLaunch.isVoiceLaunch("android.intent.action.MAIN"));
        assertFalse(AssistantLaunch.isVoiceLaunch(null));
    }

    @Test public void recognisesAskingHerToBeTheAssistant() {
        for (String yes : new String[]{"Hazte mi asistente", "Salve, quiero que seas mi asistente digital",
                "¿Cómo te pongo de asistente del móvil?", "conviértete en el asistente predeterminado"})
            assertTrue(yes, AssistantLaunch.asksToBecomeAssistant(yes));
        for (String no : new String[]{"mi asistente de Google no funciona", "hola", "eres buena asistente"})
            assertFalse(no, AssistantLaunch.asksToBecomeAssistant(no));
    }
}
