package salve.core.voice;

import static org.junit.Assert.*;
import static salve.core.voice.MusicControlCommand.Action.*;
import org.junit.Test;

/** Controlar la música que suena (Spotify) por voz, sin confundirlo con otras cosas. */
public class MusicControlCommandTest {
    @Test public void playPauseNextPrevious() {
        assertEquals(PLAY, MusicControlCommand.parse("Dale play"));
        assertEquals(PLAY, MusicControlCommand.parse("salve, dale al play"));
        assertEquals(PLAY, MusicControlCommand.parse("sigue con la música"));
        assertEquals(PAUSE, MusicControlCommand.parse("pausa"));
        assertEquals(PAUSE, MusicControlCommand.parse("para la música"));
        assertEquals(PAUSE, MusicControlCommand.parse("pon en pausa"));
        assertEquals(NEXT, MusicControlCommand.parse("siguiente canción"));
        assertEquals(NEXT, MusicControlCommand.parse("pasa de canción"));
        assertEquals(NEXT, MusicControlCommand.parse("otra canción"));
        assertEquals(PREVIOUS, MusicControlCommand.parse("la anterior"));
        assertEquals(PREVIOUS, MusicControlCommand.parse("pon la canción anterior"));
    }

    @Test public void nowPlayingAndPlaylists() {
        assertEquals(NOW_PLAYING, MusicControlCommand.parse("¿Qué canción suena?"));
        assertEquals(NOW_PLAYING, MusicControlCommand.parse("cómo se llama esta canción"));
        assertEquals(NOW_PLAYING, MusicControlCommand.parse("léeme el título de la canción"));
        assertEquals(NOW_PLAYING, MusicControlCommand.parse("quién canta esto"));
        assertEquals(LIST_PLAYLISTS, MusicControlCommand.parse("revisa mis listas de música"));
        assertEquals(LIST_PLAYLISTS, MusicControlCommand.parse("léeme mis listas de Spotify"));
        assertEquals(LIST_PLAYLISTS, MusicControlCommand.parse("¿qué listas tengo?"));
        assertEquals(LIST_PLAYLISTS, MusicControlCommand.parse("cuáles son mis playlists"));
        assertEquals(GRANT_ACCESS, MusicControlCommand.parse("Salve, dale acceso a la música"));
        assertEquals(GRANT_ACCESS, MusicControlCommand.parse("activa el control de spotify"));
    }

    @Test public void notMusicOrders() {
        assertNull("«para» a secas es callarse, no la música", MusicControlCommand.parse("para"));
        assertNull(MusicControlCommand.parse("pon mi lista Gym")); // eso lo pone AppActionCommand
        assertNull(MusicControlCommand.parse("¿qué tiempo hace?"));
        assertNull(MusicControlCommand.parse("siguiente paso de la receta"));
        assertNull(MusicControlCommand.parse(null));
    }
}
