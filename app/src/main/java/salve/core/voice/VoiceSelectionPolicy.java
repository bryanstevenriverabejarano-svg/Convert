package salve.core.voice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Portable selection from voices actually reported by the current Android TTS engine.
 * Region is an accent preference; Android does not expose a reliable gender field.
 */
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
        // Automatic selection still prefers installed offline speech when network is allowed.
        // A user's explicit, eligible voice choice takes precedence in select().
        result.sort(Comparator.comparing((Choice v) -> v.network)
                .thenComparingInt(v -> regionPriority(v.country))
                .thenComparing(Comparator.comparingInt((Choice v) -> v.quality).reversed())
                .thenComparingInt(v -> v.latency).thenComparing(v -> v.name));
        return result;
    }
    public static Choice select(List<Choice> voices, VoiceProfile profile) {
        List<Choice> choices = available(voices, profile.allowNetwork);
        for (Choice voice : choices) if (voice.name.equals(profile.voiceName)) return voice;
        return choices.isEmpty() ? null : choices.get(0);
    }

    /** Match recognition to the selected Spanish voice; default to Mexican Spanish. */
    public static String recognitionLanguageTag(Choice selected) {
        if (selected == null || !"es".equalsIgnoreCase(selected.language) || selected.country == null) {
            return "es-MX";
        }
        String country = selected.country.toUpperCase(Locale.ROOT);
        if (country.matches("[A-Z]{2}") || "419".equals(country)) return "es-" + country;
        return "es-MX";
    }

    private static int regionPriority(String country) {
        switch (country.toUpperCase(Locale.ROOT)) {
            case "MX": return 0;
            case "US": return 1;
            case "419":
            case "AR": case "BO": case "CL": case "CO": case "CR": case "CU":
            case "DO": case "EC": case "GT": case "HN": case "NI": case "PA":
            case "PE": case "PR": case "PY": case "SV": case "UY": case "VE":
                return 2;
            case "": return 3;
            case "ES": return 4;
            default: return 5;
        }
    }
}
