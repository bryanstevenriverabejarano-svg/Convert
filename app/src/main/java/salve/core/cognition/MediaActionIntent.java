package salve.core.cognition;

import salve.core.voice.MusicControlCommand;

/** Reuse the current music recognizer as an authorization gate, never as executor. */
public final class MediaActionIntent {
    private MediaActionIntent() { }

    public static boolean offered(String input) {
        MusicControlCommand.Action expected = MusicControlCommand.parse(input);
        return expected == MusicControlCommand.Action.PLAY
                || expected == MusicControlCommand.Action.PAUSE
                || expected == MusicControlCommand.Action.NEXT
                || expected == MusicControlCommand.Action.PREVIOUS
                || expected == MusicControlCommand.Action.NOW_PLAYING;
    }

    /** The LLM must never turn "pause" into "play" or "next" into "previous". */
    public static MusicControlCommand.Action verifiedAction(String userInput, String proposed) {
        MusicControlCommand.Action expected = MusicControlCommand.parse(userInput);
        if (!offered(userInput) || proposed == null
                || !expected.name().equalsIgnoreCase(proposed.trim())) {
            throw new IllegalArgumentException("Acción de música distinta de la petición original.");
        }
        return expected;
    }
}
