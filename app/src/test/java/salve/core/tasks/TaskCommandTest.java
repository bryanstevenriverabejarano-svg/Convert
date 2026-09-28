package salve.core.tasks;

import org.junit.Test;
import static org.junit.Assert.*;

public class TaskCommandTest {
    @Test public void preservesCaseSensitiveUrlAndAccentedTopic() {
        assertEquals("https://example.org/CaseSensitive?Key=Valor", TaskCommand.parse(
                "Salve, investiga y recuerda: https://example.org/CaseSensitive?Key=Valor").argument);
        assertEquals("Memoria episódica", TaskCommand.parse("crea una tarea de investigación: Memoria episódica").argument);
    }
    @Test public void decomposedAccentsHaveCorrectOffsets() {
        assertEquals("Tema", TaskCommand.parse("crea una tarea de investigacio\u0301n: Tema").argument);
    }
    @Test public void negationDiscussionAndQuotedCommandsDoNotAuthorizeWork() {
        assertNull(TaskCommand.parse("no investiga y recuerda: fotos"));
        assertNull(TaskCommand.parse("ella dice investiga y recuerda: fotos"));
        assertNull(TaskCommand.parse("\"investiga y recuerda: fotos\""));
        assertNull(TaskCommand.parse("¿qué significa investiga y recuerda?"));
    }
    @Test public void routesControlWithoutRequiringModel() {
        assertEquals(TaskCommand.Action.LIST, TaskCommand.parse("mis tareas").action);
        assertEquals(TaskCommand.Action.RESULT, TaskCommand.parse("resultado tarea 12345678").action);
        assertEquals("ultima", TaskCommand.parse("resultado de mi última tarea").argument);
        assertEquals(TaskCommand.Action.CANCEL, TaskCommand.parse("cancelar tarea 12345678").action);
        assertEquals(TaskCommand.Action.RESUME, TaskCommand.parse("reanuda tarea 12345678").action);
        assertEquals(TaskCommand.Action.PAUSE_ALL, TaskCommand.parse("pausa tus tareas").action);
    }
    @Test public void globalPauseIncludesTasksAndHasExactScope() {
        assertEquals(Boolean.TRUE, TaskCommand.globalAutonomyPause("pausa tu autonomía"));
        assertEquals(Boolean.TRUE, TaskCommand.globalAutonomyPause("  PAUSA   tu   autonomía  "));
        assertEquals(TaskCommand.Action.PAUSE_ALL, TaskCommand.parse("pausa  tus  tareas").action);
        assertEquals(Boolean.FALSE, TaskCommand.globalAutonomyPause("reanuda tu autonomía"));
        assertNull(TaskCommand.globalAutonomyPause("¿qué hace pausa tu autonomía?"));
    }
    @Test public void emptyTopicIsReservedForHelpfulValidation() {
        assertEquals("", TaskCommand.parse("investiga y recuerda:").argument);
        assertNull(TaskCommand.parse("investiga y recuerda: " + "x".repeat(2400)));
    }
}
