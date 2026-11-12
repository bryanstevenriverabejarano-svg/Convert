package salve.core.cognition;

import salve.core.voice.MusicControlCommand;

/** Reuse the current music recognizer as an authorization gate, never as executor. */
public final class MediaActionIntent {
    private MediaActionIntent() { }

    static final String TOOL = "device.media_control";

    /** La acción de la frase exacta o, si su modelo de significado la entendió, la de su canónica. */
    static MusicControlCommand.Action expected(String input) {
        MusicControlCommand.Action exact = MusicControlCommand.parse(input);
        if (exact != null || input == null) return exact;
        String canonical = ActionMeaning.canonicalFor(input, TOOL);
        return canonical == null ? null : MusicControlCommand.parse(canonical);
    }

    /** Solo la frase exacta (lo que el controlador puede exigir sin que lo elija el modelo). */
    static boolean offeredExact(String input) { return controllable(MusicControlCommand.parse(input)); }

    public static boolean offered(String input) { return controllable(expected(input)); }

    private static boolean controllable(MusicControlCommand.Action expected) {
        return expected == MusicControlCommand.Action.PLAY
                || expected == MusicControlCommand.Action.PAUSE
                || expected == MusicControlCommand.Action.NEXT
                || expected == MusicControlCommand.Action.PREVIOUS
                || expected == MusicControlCommand.Action.NOW_PLAYING;
    }

    /** The LLM must never turn "pause" into "play" or "next" into "previous". */
    public static MusicControlCommand.Action verifiedAction(String userInput, String proposed) {
        MusicControlCommand.Action expected = expected(userInput);
        if (!offered(userInput) || proposed == null
                || !expected.name().equalsIgnoreCase(proposed.trim())) {
            throw new IllegalArgumentException("Acción de música distinta de la petición original.");
        }
        return expected;
    }
}
