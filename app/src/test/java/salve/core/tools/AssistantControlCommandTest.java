package salve.core.tools;

import org.junit.Test;
import static org.junit.Assert.*;

public class AssistantControlCommandTest {
    @Test public void bedCanBeShownAndHiddenWithExplicitShortCommands() {
        assertEquals(AssistantControlCommand.Type.BED, AssistantControlCommand.parse("Salve, cama.").type);
        assertEquals(AssistantControlCommand.Type.BED, AssistantControlCommand.parse("muestra tu cama").type);
        assertEquals(AssistantControlCommand.Type.HIDE_BED, AssistantControlCommand.parse("guarda la cama").type);
        assertEquals(AssistantControlCommand.Type.HIDE_BED, AssistantControlCommand.parse("oculta la cama").type);
        assertNull(AssistantControlCommand.parse("No muestres la cama"));
        assertNull(AssistantControlCommand.parse("Mi cama es blanca"));
        assertNull(AssistantControlCommand.parse("¿Dónde está la cama?"));
    }
    @Test public void financeOnlyOpensForAnExplicitCommand() {
        assertEquals(AssistantControlCommand.Type.FINANCES, AssistantControlCommand.parse("Salve, abre finanzas").type);
        assertNull(AssistantControlCommand.parse("Quiero aprender de finanzas"));
        assertNull(AssistantControlCommand.parse("No abras finanzas"));
    }
    @Test public void exactAvatarCommandHandlesSpanishAccents() {
        assertEquals(AssistantControlCommand.Type.SLEEP, AssistantControlCommand.parse("Salve, ACUÉSTATE!").type);
    }
    @Test public void aQuestionAboutAnActionDoesNotExecuteIt() {
        assertNull(AssistantControlCommand.parse("¿Puedes explicar cómo se crea una cama?"));
        assertNull(AssistantControlCommand.parse("No quiero que camines"));
        assertNull(AssistantControlCommand.parse("Qué hace WhatsApp"));
    }
    @Test public void preservesTheWholeRecipeNameAndObjective() {
        AssistantControlCommand result = AssistantControlCommand.parse("aprende la rutina saludo familiar: Escribir ¡Hola, mamá!");
        assertEquals(AssistantControlCommand.Type.LEARN_RECIPE, result.type);
        assertEquals("saludo familiar: Escribir ¡Hola, mamá!", result.argument);
    }
    @Test public void keepsFullNameForRunAndDelete() {
        assertEquals("saludo familiar", AssistantControlCommand.parse("ejecuta tu rutina saludo familiar").argument);
        assertEquals(AssistantControlCommand.Type.DELETE_RECIPE, AssistantControlCommand.parse("elimina la rutina saludo familiar").type);
    }
    @Test public void noEmptyOrImplicitCommand() {
        assertNull(AssistantControlCommand.parse(null));
        assertNull(AssistantControlCommand.parse(""));
        assertNull(AssistantControlCommand.parse("aprende la rutina"));
    }
    @Test public void deviceAndWardrobeCommandsAreDistinct() {
        assertEquals(AssistantControlCommand.Type.DEVICES, AssistantControlCommand.parse("Mi móvil").type);
        assertEquals(AssistantControlCommand.Type.PAJAMAS, AssistantControlCommand.parse("ponte el pijama").type);
    }
}
