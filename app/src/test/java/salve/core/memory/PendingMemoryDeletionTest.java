package salve.core.memory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PendingMemoryDeletionTest {
    @Test
    public void expiresAfterApprovalWindow() {
        MemoryForgetRequest request = MemoryForgetRequest.parse("Olvida mi nombre");
        PendingMemoryDeletion pending = new PendingMemoryDeletion(request, 1_000L);

        assertFalse(pending.isExpired(120_000L, 120_000L));
        assertTrue(pending.isExpired(121_001L, 120_000L));
    }
}
