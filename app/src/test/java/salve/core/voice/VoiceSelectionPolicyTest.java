package salve.core.voice;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class VoiceSelectionPolicyTest {
    private static VoiceSelectionPolicy.Choice voice(String name, String country) {
        return voice(name, "es", country, false, true, 300, 200);
    }

    private static VoiceSelectionPolicy.Choice voice(String name, String language, String country,
            boolean network, boolean installed, int quality, int latency) {
        return new VoiceSelectionPolicy.Choice(name, language, country, network, installed, quality, latency);
    }

    private static List<String> names(List<VoiceSelectionPolicy.Choice> choices) {
        List<String> result = new ArrayList<>();
        for (VoiceSelectionPolicy.Choice choice : choices) result.add(choice.name);
        return result;
    }

    @Test public void automaticAccentPreferenceUsesAvailableLatinAmericanVoices() {
        List<VoiceSelectionPolicy.Choice> voices = Arrays.asList(voice("Spain", "ES"),
                voice("Colombia", "CO"), voice("United States", "US"), voice("Mexico", "MX"));
        assertEquals(Arrays.asList("Mexico", "United States", "Colombia", "Spain"),
                names(VoiceSelectionPolicy.available(voices, false)));
        assertEquals("Mexico", VoiceSelectionPolicy.select(voices, VoiceProfile.defaults()).name);
    }

    @Test public void spanishFromSpainRemainsAUsableInstalledFallback() {
        assertEquals("Spain", VoiceSelectionPolicy.select(
                Collections.singletonList(voice("Spain", "ES")), VoiceProfile.defaults()).name);
    }

    @Test public void region419AndGenericSpanishAreUsableFallbacks() {
        assertEquals(Arrays.asList("Latin America", "Generic", "Spain"), names(
                VoiceSelectionPolicy.available(Arrays.asList(voice("Spain", "ES"),
                        voice("Generic", ""), voice("Latin America", "419")), false)));
    }

    @Test public void automaticSelectionPrefersOfflineEvenWhenNetworkIsAllowed() {
        VoiceSelectionPolicy.Choice cloud = voice("Mexico cloud", "es", "MX", true, true, 500, 50);
        VoiceSelectionPolicy.Choice offline = voice("Spain offline", "ES");
        assertEquals("Spain offline", VoiceSelectionPolicy.select(Arrays.asList(cloud, offline),
                new VoiceProfile("", 1, 1, true)).name);
    }

    @Test public void manualInstalledChoiceWinsEvenIfItsRegionIsNotPreferred() {
        assertEquals("Spain", VoiceSelectionPolicy.select(Arrays.asList(voice("Mexico", "MX"),
                voice("Spain", "ES")), new VoiceProfile("Spain", 1, 1, false)).name);
    }

    @Test public void manualNetworkChoiceIsRespectedOnlyWhenExplicitlyAllowed() {
        List<VoiceSelectionPolicy.Choice> voices = Arrays.asList(voice("Mexico", "MX"),
                voice("chosen cloud", "es", "ES", true, true, 300, 200));
        assertEquals("chosen cloud", VoiceSelectionPolicy.select(voices,
                new VoiceProfile("chosen cloud", 1, 1, true)).name);
        assertEquals("Mexico", VoiceSelectionPolicy.select(voices,
                new VoiceProfile("chosen cloud", 1, 1, false)).name);
    }

    @Test public void noDownloadOrOtherLanguageIsSilentlySelected() {
        List<VoiceSelectionPolicy.Choice> voices = Arrays.asList(null,
                voice("missing", "es", "MX", false, false, 300, 200),
                voice("English", "en", "US", false, true, 300, 200),
                voice("network only", "es", "MX", true, true, 300, 200));
        assertNull(VoiceSelectionPolicy.select(voices, VoiceProfile.defaults()));
        assertTrue(VoiceSelectionPolicy.available(null, false).isEmpty());
    }

    @Test public void missingManualVoiceFallsBackToInstalledEligibleVoice() {
        assertEquals("available", VoiceSelectionPolicy.select(Arrays.asList(
                voice("saved", "es", "MX", false, false, 500, 50), voice("available", "CO")),
                new VoiceProfile("saved", 1, 1, false)).name);
    }

    @Test public void tiesUseQualityThenLatencyThenNameIndependentlyOfEnumerationOrder() {
        List<VoiceSelectionPolicy.Choice> voices = new ArrayList<>(Arrays.asList(
                voice("low", "es", "MX", false, true, 100, 50),
                voice("slow", "es", "MX", false, true, 400, 300),
                voice("zeta", "es", "MX", false, true, 400, 100),
                voice("alpha", "es", "MX", false, true, 400, 100)));
        List<String> expected = Arrays.asList("alpha", "zeta", "slow", "low");
        assertEquals(expected, names(VoiceSelectionPolicy.available(voices, false)));
        Collections.reverse(voices);
        assertEquals(expected, names(VoiceSelectionPolicy.available(voices, false)));
    }

    @Test public void recognitionMatchesSelectedSpanishLocaleWithoutChangingSavedChoice() {
        assertEquals("es-US", VoiceSelectionPolicy.recognitionLanguageTag(voice("US", "us")));
        assertEquals("es-ES", VoiceSelectionPolicy.recognitionLanguageTag(voice("Spain", "ES")));
        assertEquals("es-CO", VoiceSelectionPolicy.recognitionLanguageTag(voice("Colombia", "CO")));
        assertEquals("es-419", VoiceSelectionPolicy.recognitionLanguageTag(voice("Latin America", "419")));
    }

    @Test public void recognitionHasASpanishDefaultWhenNoSpecificUsableLocaleExists() {
        assertEquals("es-MX", VoiceSelectionPolicy.recognitionLanguageTag(null));
        assertEquals("es-MX", VoiceSelectionPolicy.recognitionLanguageTag(voice("Generic", "")));
        assertEquals("es-MX", VoiceSelectionPolicy.recognitionLanguageTag(voice("Malformed", "es-MX")));
        assertEquals("es-MX", VoiceSelectionPolicy.recognitionLanguageTag(
                voice("English", "en", "US", false, true, 300, 200)));
    }

    @Test public void malformedEntriesAreIgnoredAndLocaleMatchingIsCaseInsensitive() {
        List<VoiceSelectionPolicy.Choice> voices = Arrays.asList(
                voice(null, "es", "MX", false, true, 300, 200),
                voice("null language", null, "MX", false, true, 300, 200),
                voice("null country", "es", null, false, true, 300, 200),
                voice("Spanish", "ES", "mx", false, true, 300, 200));
        assertEquals(Collections.singletonList("Spanish"), names(VoiceSelectionPolicy.available(voices, false)));
    }
}
