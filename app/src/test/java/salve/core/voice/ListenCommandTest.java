package salve.core.voice;

import static org.junit.Assert.*;
import org.junit.Test;

public class ListenCommandTest {
    @Test public void listensWhenAsked() {
        ListenCommand song = ListenCommand.parse("Salve, escucha esta canción y dime cuál es");
        assertNotNull(song);
        assertTrue(song.music);
        assertEquals("cual es", song.question);
        assertEquals(ListenCommand.DEFAULT_SECONDS, song.seconds);

        ListenCommand noise = ListenCommand.parse("oye este ruido");
        assertFalse(noise.music);
        assertTrue(noise.question.startsWith("Escucha el audio"));

        assertEquals(15, ListenCommand.parse("escucha esto durante 15 segundos").seconds);
        assertEquals(ListenCommand.MAX_SECONDS, ListenCommand.parse("escucha esto durante 90 segundos").seconds);
        assertNotNull(ListenCommand.parse("¿Qué es ese ruido?"));
        assertTrue(ListenCommand.parse("reconoce esta canción").music);
        assertEquals("con que tono te hablo", ListenCommand.parse("escucha mi voz y dime con qué tono te hablo").question);
    }

    @Test public void ordinaryTalkIsNotListening() {
        assertNull(ListenCommand.parse("oye"));
        assertNull(ListenCommand.parse("oye, qué tal estás"));
        assertNull(ListenCommand.parse("escucha, mañana tengo médico"));
        assertNull("eso lo lee Spotify", ListenCommand.parse("qué canción suena"));
        assertNull(ListenCommand.parse(null));
    }
}
