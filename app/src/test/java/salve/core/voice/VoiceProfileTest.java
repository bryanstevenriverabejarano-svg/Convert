package salve.core.voice;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class VoiceProfileTest {
    private static VoiceSelectionPolicy.Choice voice(String name, String language, String country, boolean network, boolean installed) {
        return new VoiceSelectionPolicy.Choice(name, language, country, network, installed, 300, 200);
    }
    @Test public void defaultsAreOfflineAndUseRestrainedProsody() {
        VoiceProfile value = VoiceProfile.defaults();
        assertFalse(value.allowNetwork); assertEquals(.95f, value.rate, .001f); assertEquals(1.04f, value.pitch, .001f);
    }
    @Test public void offlinePolicyCannotSelectANetworkVoiceEvenIfSaved() {
        VoiceProfile value = new VoiceProfile("cloud", 1, 1, false);
        VoiceSelectionPolicy.Choice chosen = VoiceSelectionPolicy.select(Arrays.asList(
                voice("cloud", "es", "ES", true, true), voice("offline", "es", "MX", false, true)), value);
        assertEquals("offline", chosen.name);
    }
    @Test public void missingOrUninstalledSpanishVoicesDoNotFallBackToAnotherLanguage() {
        assertNull(VoiceSelectionPolicy.select(Arrays.asList(voice("en", "en", "US", false, true),
                voice("missing", "es", "ES", false, false)), VoiceProfile.defaults()));
    }
    @Test public void aSavedInstalledVoiceWinsOverTheAutomaticChoice() {
        VoiceSelectionPolicy.Choice chosen = VoiceSelectionPolicy.select(Arrays.asList(
                voice("Spain", "es", "ES", false, true), voice("Mexico", "es", "MX", false, true)),
                new VoiceProfile("Mexico", 1, 1, false));
        assertEquals("Mexico", chosen.name);
    }
    @Test public void networkVoicesRequireAnExplicitProfileChange() {
        VoiceSelectionPolicy.Choice cloud = voice("cloud", "es", "ES", true, true);
        assertNull(VoiceSelectionPolicy.select(Arrays.asList(cloud), VoiceProfile.defaults()));
        assertEquals("cloud", VoiceSelectionPolicy.select(Arrays.asList(cloud), new VoiceProfile("cloud", 1, 1, true)).name);
    }
    @Test public void invalidRatesAndPitchAreRejected() {
        for (float rate : new float[]{Float.NaN, Float.POSITIVE_INFINITY, .1f, 4f}) {
            try { new VoiceProfile("", rate, 1, false); fail(); } catch (IllegalArgumentException expected) { }
        }
        try { new VoiceProfile("", 1, Float.NaN, false); fail(); } catch (IllegalArgumentException expected) { }
    }
}
