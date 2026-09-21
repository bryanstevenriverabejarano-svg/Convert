package salve.core.voice;

/** A configurable delivery style, not a claim to train or clone a new voice. */
public final class VoiceProfile {
    public final String voiceName;
    public final float rate, pitch;
    public final boolean allowNetwork;
    public VoiceProfile(String voiceName, float rate, float pitch, boolean allowNetwork) {
        if (voiceName == null || voiceName.length() > 256 || !Float.isFinite(rate) || !Float.isFinite(pitch)
                || rate < .75f || rate > 1.25f || pitch < .80f || pitch > 1.25f) {
            throw new IllegalArgumentException("Voz o parámetros fuera del intervalo permitido.");
        }
        this.voiceName = voiceName; this.rate = rate; this.pitch = pitch; this.allowNetwork = allowNetwork;
    }
    public static VoiceProfile defaults() { return new VoiceProfile("", .95f, 1.04f, false); }
    public String styleInstruction() {
        return "ESTILO DE SALVE: cálida y discretamente tímida, curiosa y clara. "
                + "Muestra curiosidad con preguntas pertinentes, sin añadirlas a cada respuesta. "
                + "Explica con precisión y cercanía, sin muletillas, tartamudeos fingidos ni infantilizarte. "
                + "Este estilo no cambia los hechos, la incertidumbre ni las reglas de herramientas.\n";
    }
}
