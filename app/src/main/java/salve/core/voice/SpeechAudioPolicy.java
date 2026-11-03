package salve.core.voice;

/**
 * Por dónde suena la voz de Salve. Android Auto solo manda al coche el audio que llega con el mismo
 * uso con el que se pidió el foco: si la sesión de voz pide foco como asistente y el TTS habla como
 * multimedia, el coche no lo reproduce. En modo conducción, foco y voz van juntos como «guía» (lo
 * que usan los navegadores): el coche baja la radio o la música y la pone por sus altavoces.
 * Fuera del coche no cambia nada: el foco sigue siendo de asistente y la voz, multimedia, como antes.
 * Valores = constantes de android.media.AudioAttributes; lógica pura para pruebas JVM.
 */
public final class SpeechAudioPolicy {
    public static final int USAGE_MEDIA = 1;
    public static final int USAGE_GUIDANCE = 12;   // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
    public static final int USAGE_ASSISTANT = 16;

    private SpeechAudioPolicy() { }

    /** Uso con el que habla el TTS. */
    public static int speechUsage(boolean driving) { return driving ? USAGE_GUIDANCE : USAGE_MEDIA; }

    /** Uso con el que la conversación por voz pide el foco de audio. */
    public static int focusUsage(boolean driving) { return driving ? USAGE_GUIDANCE : USAGE_ASSISTANT; }

    /**
     * Sin conversación por voz abierta (respuesta a un mensaje escrito), en el coche hay que pedir un
     * foco breve para esa respuesta; con la conversación abierta ya lo tiene ella y pedir otro la cortaría.
     */
    public static boolean needsOwnFocus(boolean driving, boolean liveVoiceActive) {
        return driving && !liveVoiceActive;
    }
}
