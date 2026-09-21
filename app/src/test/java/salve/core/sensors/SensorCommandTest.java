package salve.core.sensors;

import org.junit.Test;
import static org.junit.Assert.*;

public class SensorCommandTest {
    @Test public void sessionsHaveExplicitShortDurations() {
        assertEquals(30_000, SensorCommand.parse("activa tus sensores").durationMillis);
        assertEquals(60_000, SensorCommand.parse(" Activa   sensores durante 60 segundos ").durationMillis);
        assertEquals(SensorCommand.Action.STATUS, SensorCommand.parse("qué percibes ahora").action);
        assertEquals(SensorCommand.Action.STOP, SensorCommand.parse("detén sensores").action);
    }
    @Test public void questionsNegationsAndUnboundedCaptureDoNotActivate() {
        for (String text : new String[]{"no activa tus sensores", "¿activa tus sensores?",
                "activa sensores durante 600 segundos", "activa sensores siempre", "me siento feliz", ""}) {
            assertNull(text, SensorCommand.parse(text));
        }
        assertNull(SensorCommand.parse(null));
    }

    @Test public void declarativeAsrPunctuationPreservesExplicitCommands() {
        for (String text : new String[]{"Activa tus sensores.", "ACTIVA SENSORES!",
                " Activa sensores. ", "activa sensores...", "activa sensores!!"}) {
            SensorCommand command = SensorCommand.parse(text);
            assertNotNull(text, command);
            assertEquals(text, SensorCommand.Action.START, command.action);
            assertEquals(text, 30_000L, command.durationMillis);
        }
        assertEquals(60_000L,
                SensorCommand.parse("Activa tus sensores durante 60 segundos!").durationMillis);
    }

    @Test public void stopCommandsAcceptAsrPunctuation() {
        for (String text : new String[]{"Desactiva sensores.", "desactiva tus sensores!", "Detén sensores."}) {
            SensorCommand command = SensorCommand.parse(text);
            assertNotNull(text, command);
            assertEquals(text, SensorCommand.Action.STOP, command.action);
            assertEquals(text, 0L, command.durationMillis);
        }
    }

    @Test public void statusQuestionsAreReadOnly() {
        for (String text : new String[]{"¿Qué percibes ahora?", "Qué percibes ahora?",
                "¿ Estado de los sensores ?", "consultar sensores.", "¿Qué percibes ahora?!"}) {
            SensorCommand command = SensorCommand.parse(text);
            assertNotNull(text, command);
            assertEquals(text, SensorCommand.Action.STATUS, command.action);
            assertEquals(text, 0L, command.durationMillis);
        }
    }

    @Test public void punctuationDoesNotConvertQuestionsOrNegationsIntoActions() {
        for (String text : new String[]{"¿Activa tus sensores?", "Activa sensores?",
                "Activa sensores?!", "¿Activa sensores.", "¿Desactiva sensores?",
                "No actives sensores.", "No, activa sensores.", "no desactiva sensores!",
                "Si puedes, activa sensores.", "activa sensores. y mira alrededor",
                "\"activa sensores.\"", "activa sensores durante 600 segundos.",
                "activa sensores siempre!", ".!", "¿Qué percibes ahora? Activa sensores."}) {
            assertNull(text, SensorCommand.parse(text));
        }
    }
}
