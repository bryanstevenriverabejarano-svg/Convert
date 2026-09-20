package salve.core.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class MemoryForgetRequestTest {
    @Test
    public void parsesKnownProfileCategories() {
        assertEquals("name", MemoryForgetRequest.parse("Olvida mi nombre").getCategory());
        assertEquals("residence", MemoryForgetRequest.parse("Borra dónde vivo").getCategory());
        assertEquals("preference_respuestas",
                MemoryForgetRequest.parse("Elimina mi preferencia sobre respuestas").getCategory());
    }

    @Test
    public void rejectsBroadOrUnknownDeletion() {
        assertNull(MemoryForgetRequest.parse("Olvida todo"));
        assertNull(MemoryForgetRequest.parse("Borra aquello"));
        assertTrue(MemoryForgetRequest.looksLikeForgetCommand("Olvida todo"));
        assertFalse(MemoryForgetRequest.looksLikeForgetCommand("Elimina una misión terminada"));
    }
}
