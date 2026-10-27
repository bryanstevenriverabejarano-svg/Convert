package salve.core.voice;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

public class VoiceSpeakerAndGenderTest {
    @Test public void recognitionFollowsThePhoneSpanishNotSalvesAccent() {
        assertEquals("es-ES", VoiceSelectionPolicy.speakerLanguageTag(new Locale("es", "ES"), "es-MX"));
        assertEquals("es-VE", VoiceSelectionPolicy.speakerLanguageTag(new Locale("es", "VE"), "es-MX"));
        assertEquals("es-ES", VoiceSelectionPolicy.speakerLanguageTag(new Locale("es"), "es-MX"));
        assertEquals("es-MX", VoiceSelectionPolicy.speakerLanguageTag(Locale.ENGLISH, "es-MX"));
    }

    @Test public void femaleVoicesComeFirstWhenTheNameSaysSo() {
        VoiceSelectionPolicy.Choice male = new VoiceSelectionPolicy.Choice("SMTm00", "es", "MX", false, true, 400, 200);
        VoiceSelectionPolicy.Choice unknown = new VoiceSelectionPolicy.Choice("es-mx-x-esa-local", "es", "MX", false, true, 400, 200);
        VoiceSelectionPolicy.Choice female = new VoiceSelectionPolicy.Choice("SMTf00", "es", "ES", false, true, 300, 200);
        List<VoiceSelectionPolicy.Choice> sorted = VoiceSelectionPolicy.available(Arrays.asList(male, unknown, female), false);
        assertEquals("SMTf00", sorted.get(0).name);
        assertEquals("SMTm00", sorted.get(2).name);
        assertEquals(0, VoiceSelectionPolicy.femininePriority("es-us-x-sfb#female_1-local"));
        assertEquals(2, VoiceSelectionPolicy.femininePriority("es-us-x-sfb#male_2-local"));
    }

    @Test public void bryansOwnChoiceAlwaysWins() {
        VoiceSelectionPolicy.Choice male = new VoiceSelectionPolicy.Choice("SMTm00", "es", "ES", false, true, 400, 200);
        VoiceSelectionPolicy.Choice female = new VoiceSelectionPolicy.Choice("SMTf00", "es", "ES", false, true, 400, 200);
        VoiceProfile chosen = new VoiceProfile("SMTm00", 1f, 1f, false);
        assertEquals("SMTm00", VoiceSelectionPolicy.select(Arrays.asList(male, female), chosen).name);
    }
}
