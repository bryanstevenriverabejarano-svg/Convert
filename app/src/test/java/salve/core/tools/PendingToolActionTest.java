package salve.core.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PendingToolActionTest {
    @Test
    public void expiresAfterTtl() {
        PendingToolAction action = PendingToolAction.tap(10, 20, 1_000L);
        assertFalse(action.isExpired(1_500L, 1_000L));
        assertTrue(action.isExpired(2_001L, 1_000L));
    }

    @Test
    public void clampsCoordinates() {
        PendingToolAction action = PendingToolAction.tap(-5, 50_000, 0L);
        assertEquals(0, action.getFirstNumber());
        assertEquals(10_000, action.getSecondNumber());
    }

    @Test
    public void limitsSensitivePayloadSize() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 3_000; i++) text.append('a');
        PendingToolAction action = PendingToolAction.writeText(text.toString(), 0L);
        assertEquals(2_000, action.getPayload().length());
    }
}
