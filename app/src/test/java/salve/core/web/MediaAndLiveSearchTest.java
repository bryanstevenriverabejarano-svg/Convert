package salve.core.web;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaAndLiveSearchTest {
    @Test public void playsMusicOnlyWithAClearSignal() {
        MediaRequest queen = MediaRequest.parse("Pon música de Queen");
        assertNotNull(queen); assertTrue(queen.autoplay); assertEquals("música queen", queen.query);
        MediaRequest song = MediaRequest.parse("Salve, ponme bohemian rhapsody en YouTube");
        assertNotNull(song); assertEquals("bohemian rhapsody", song.query);
        assertEquals("despacito", MediaRequest.parse("reproduce despacito").query);
        assertEquals("despacito", MediaRequest.parse("pon la canción despacito").query);
        assertNull(MediaRequest.parse("pon una alarma a las 8"));
        assertNull(MediaRequest.parse("pon mi huella"));
        assertNull(MediaRequest.parse("quiero escuchar tu voz"));
        assertNull(MediaRequest.parse("quiero ver tu mente"));
    }

    @Test public void youtubeSearchShowsResultsWithoutPlaying() {
        MediaRequest search = MediaRequest.parse("busca tutoriales de java en youtube");
        assertNotNull(search); assertFalse(search.autoplay); assertEquals("tutoriales de java", search.query);
        assertEquals("https://m.youtube.com/results?search_query=tutoriales+de+java", search.searchUrl());
        assertEquals("dQw4w9WgXcQ", MediaRequest.videoId("/watch?v=dQw4w9WgXcQ&pp=abc"));
        assertNull(MediaRequest.videoId("/shorts/abc"));
    }

    @Test public void miniWindowCommands() {
        assertEquals(LiveSearchCommand.Action.SHOW, LiveSearchCommand.parse("Muestra tus búsquedas en tiempo real"));
        assertEquals(LiveSearchCommand.Action.HIDE, LiveSearchCommand.parse("no me muestres tus búsquedas"));
        assertEquals(LiveSearchCommand.Action.CLOSE, LiveSearchCommand.parse("para la música"));
        assertNull(LiveSearchCommand.parse("busca el tiempo en Vitoria"));
    }
}
