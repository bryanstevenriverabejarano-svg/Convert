package salve.core.memory;
import org.junit.Test;
import static org.junit.Assert.*;
public class CloudMemoryCommandTest {
    @Test public void explicitRecoveryAndStatusAreSeparateFromRememberingAndNegation() {
        assertEquals(CloudMemoryCommand.Action.RESTORE,CloudMemoryCommand.parse("Salve, recupera tus recuerdos de pCloud, por favor"));
        assertEquals(CloudMemoryCommand.Action.STATUS,CloudMemoryCommand.parse("¿Cómo va la recuperación?"));
        assertNull(CloudMemoryCommand.parse("no recuperes tus recuerdos"));
        assertNull(CloudMemoryCommand.parse("¿Recuerdas mi viaje?"));
    }

    @org.junit.Test public void cloudMemoryQuestionsAreRecognised() {
        org.junit.Assert.assertEquals(CloudMemoryCommand.Action.ASK, CloudMemoryCommand.parse("Y el primer recuerdo que tienes en p cloud"));
        org.junit.Assert.assertEquals(CloudMemoryCommand.Action.ASK, CloudMemoryCommand.parse("¿Qué recuerdos tienes en la nube?"));
        org.junit.Assert.assertNull(CloudMemoryCommand.parse("¿Cuál es tu primer recuerdo?"));
    }
}
