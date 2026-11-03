package salve.core.voice;

/** Ajusta longitud y estilo sin mezclar la política de voz con el proveedor de IA. */
public final class VoiceResponsePolicy {
    private static final int VOICE_MAX_CHARS = 420;
    private static final int DRIVING_VOICE_MAX_CHARS = 220;
    private static final int TEXT_MAX_CHARS = 900;

    private VoiceResponsePolicy() {}

    public static int maxResponseChars(boolean voiceInput) {
        return maxResponseChars(voiceInput, false);
    }

    /** Mientras Android Auto está conectado, prioriza respuestas más breves sin recortar el chat escrito. */
    public static int maxResponseChars(boolean voiceInput, boolean driving) {
        return !voiceInput ? TEXT_MAX_CHARS : (driving ? DRIVING_VOICE_MAX_CHARS : VOICE_MAX_CHARS);
    }

    public static String promptInstruction(boolean voiceInput) {
        return promptInstruction(voiceInput, false);
    }

    public static String promptInstruction(boolean voiceInput, boolean driving) {
        if (!voiceInput) return "";
        if (driving) return "MODO CONDUCCIÓN: responde oralmente en una o dos frases breves. "
                + "No sugieras mirar, tocar o leer el teléfono durante la marcha. "
                + "Si una operación requiere pantalla, indícalo para cuando esté estacionado.\n\n";
        return "MODO VOZ: responde de forma oral, directa y breve; evita listas largas y formato Markdown.\n\n";
    }
}
