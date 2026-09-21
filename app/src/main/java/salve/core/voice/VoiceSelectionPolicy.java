package salve.core.voice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Portable selection from voices actually reported by the current Android TTS engine. */
public final class VoiceSelectionPolicy {
    public static final class Choice {
        public final String name, language, country;
        public final boolean network, installed;
        public final int quality, latency;
        public Choice(String name, String language, String country, boolean network, boolean installed, int quality, int latency) {
            this.name = name; this.language = language; this.country = country;
            this.network = network; this.installed = installed; this.quality = quality; this.latency = latency;
        }
        public String label() { return name + " · " + language + (country.isEmpty() ? "" : "-" + country)
                + (network ? " · necesita red" : " · sin red"); }
    }
    private VoiceSelectionPolicy() {}
    public static List<Choice> available(List<Choice> voices, boolean allowNetwork) {
        List<Choice> result = new ArrayList<>();
        if (voices != null) for (Choice voice : voices) {
            if (voice != null && voice.name != null && voice.language != null && voice.country != null
                    && voice.installed && "es".equalsIgnoreCase(voice.language) && (allowNetwork || !voice.network)) result.add(voice);
        }
        result.sort(Comparator.comparing((Choice v) -> !"ES".equalsIgnoreCase(v.country))
                .thenComparing(v -> v.network).thenComparingInt(v -> -v.quality)
                .thenComparingInt(v -> v.latency).thenComparing(v -> v.name));
        return result;
    }
    public static Choice select(List<Choice> voices, VoiceProfile profile) {
        List<Choice> choices = available(voices, profile.allowNetwork);
        for (Choice voice : choices) if (voice.name.equals(profile.voiceName)) return voice;
        return choices.isEmpty() ? null : choices.get(0);
    }
}
