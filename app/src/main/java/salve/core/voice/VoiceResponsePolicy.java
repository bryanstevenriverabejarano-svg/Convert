package salve.core.voice;

/** Ajusta longitud y estilo sin mezclar la política de voz con el proveedor de IA. */
public final class VoiceResponsePolicy {
    private static final int VOICE_MAX_CHARS = 420;
    private static final int TEXT_MAX_CHARS = 900;

    private VoiceResponsePolicy() {}

    public static int maxResponseChars(boolean voiceInput) {
        return voiceInput ? VOICE_MAX_CHARS : TEXT_MAX_CHARS;
    }

    public static String promptInstruction(boolean voiceInput) {
        return voiceInput
                ? "MODO VOZ: responde de forma oral, directa y breve; evita listas largas y formato Markdown.\n\n"
                : "";
    }
}
