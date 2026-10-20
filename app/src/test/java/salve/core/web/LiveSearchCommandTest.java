package salve.core.web;

import static org.junit.Assert.*;

import org.junit.Test;

public class LiveSearchCommandTest {
    @Test public void showOpensTheLiveWindow() {
        assertEquals(LiveSearchCommand.Action.SHOW, LiveSearchCommand.parse("Muéstrame lo que haces"));
        assertEquals(LiveSearchCommand.Action.SHOW, LiveSearchCommand.parse("Salve, enséñame cómo escribes"));
        assertEquals(LiveSearchCommand.Action.SHOW, LiveSearchCommand.parse("abre salve en directo"));
        assertEquals(LiveSearchCommand.Action.SHOW, LiveSearchCommand.parse("muéstrame tus búsquedas"));
    }

    @Test public void hideAndCloseStillWork() {
        assertEquals(LiveSearchCommand.Action.HIDE, LiveSearchCommand.parse("no me muestres tus búsquedas"));
        assertEquals(LiveSearchCommand.Action.CLOSE, LiveSearchCommand.parse("cierra la mini ventana"));
        assertNull(LiveSearchCommand.parse("¿qué haces hoy?"));
    }
}
