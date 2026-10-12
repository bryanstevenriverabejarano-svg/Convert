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
}
