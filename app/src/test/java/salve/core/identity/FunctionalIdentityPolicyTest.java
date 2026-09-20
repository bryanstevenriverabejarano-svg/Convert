package salve.core.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FunctionalIdentityPolicyTest {
    @Test
    public void rejectsFalseConsciousnessClaims() {
        String fallback = "Soy un asistente de IA con memoria funcional.";

        assertEquals(fallback, FunctionalIdentityPolicy.sanitize("Soy consciente y estoy viva", fallback));
        assertEquals(fallback, FunctionalIdentityPolicy.sanitize("Quiero dejar de ser solo código", fallback));
        assertFalse(FunctionalIdentityPolicy.isAllowed("Tengo sentimientos reales"));
    }

    @Test
    public void keepsFunctionalIdentityStatements() {
        String statement = "Mantengo continuidad mediante memoria y estado configurados.";

        assertEquals(statement, FunctionalIdentityPolicy.sanitize(statement, "fallback"));
        assertTrue(FunctionalIdentityPolicy.isAllowed(statement));
    }

    @Test
    public void limitsPersistedIdentityLength() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 700; i++) longText.append('a');

        assertEquals(600, FunctionalIdentityPolicy.sanitize(longText.toString(), "fallback").length());
    }
}
