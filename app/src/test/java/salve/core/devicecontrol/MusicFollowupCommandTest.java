package salve.core.devicecontrol;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import salve.core.conversation.ChatMessage;

import static org.junit.Assert.*;

public class MusicFollowupCommandTest {
    private static ChatMessage user(String s) {
        return new ChatMessage(ChatMessage.Role.USER, s, 0);
    }
    private static ChatMessage salve(String s) {
        return new ChatMessage(ChatMessage.Role.ASSISTANT, s, 0);
    }

    @Test public void songTitleAfterSpotifyQuestionBecomesPlayRequest() {
        List<ChatMessage> history = Arrays.asList(
                user("salve pon música en Spotify"),
                salve("Claro. ¿Qué canción te gustaría que ponga?"),
                user("lluvia sepeda"));
        MusicFollowupCommand command = MusicFollowupCommand.parse("lluvia sepeda", history);
        assertNotNull(command);
        assertEquals(MusicFollowupCommand.Kind.TRACK, command.kind);
        assertEquals("lluvia sepeda", command.target);
    }

    @Test public void yesPlayThatSongReusesUserSpecifiedTitleNotModelGuess() {
        List<ChatMessage> history = Arrays.asList(
                user("pon música en Spotify"),
                salve("¿Qué canción te gustaría que ponga?"),
                user("Lluvia Cepeda"),
                salve("Lluvia Cepeda"),
                user("sí pon esa canción"));
        MusicFollowupCommand command = MusicFollowupCommand.parse("sí pon esa canción", history);
        assertNotNull(command);
        assertEquals(MusicFollowupCommand.Kind.TRACK, command.kind);
        assertEquals("Lluvia Cepeda", command.target);
    }

    @Test public void userDelegatingSongSelectsExistingFavoritesNotInventedTrack() {
        List<ChatMessage> history = Arrays.asList(
                user("salve pon música en Spotify"),
                salve("¿Qué canción te gustaría que ponga?"),
                user("pon la que tú quieras"));
        MusicFollowupCommand command = MusicFollowupCommand.parse("pon la que tú quieras", history);
        assertNotNull(command);
        assertEquals(MusicFollowupCommand.Kind.FAVORITES, command.kind);
    }

    @Test public void noOldOrUnrelatedContextCanLaunchMusic() {
        assertNull(MusicFollowupCommand.parse("Lluvia Cepeda", Arrays.asList(
                user("¿cómo estás?"), salve("Hola, ¿qué canción te gustaría que ponga?"),
                user("Lluvia Cepeda"))));
        assertNull(MusicFollowupCommand.parse("no pongas esa canción", Arrays.asList(
                user("pon música en Spotify"), salve("¿Qué canción?"),
                user("no pongas esa canción"))));
        assertNull(MusicFollowupCommand.parse("lluvia sepeda", Arrays.asList(
                user("pon música en Spotify"), salve("¿Qué canción?"),
                user("lluvia sepeda"), salve("De acuerdo"),
                user("cuéntame un chiste"), salve("Un chiste"), user("lluvia sepeda"))));
    }

    @Test public void neverTreatAssistantTitleAsAuthorizedUserChoice() {
        assertNull(MusicFollowupCommand.parse("sí pon esa canción", Arrays.asList(
                user("pon música en Spotify"),
                salve("¿Qué canción quieres?"),
                salve("Me gustaría Lluvia"),
                user("sí pon esa canción"))));
    }
}
