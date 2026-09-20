package salve.core.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class MemoryQueryTermsTest {
    @Test
    public void removesNoiseAndPreservesUsefulTerms() {
        List<String> terms = MemoryQueryTerms.extract("¿Qué sabes sobre mi trabajo en Mercedes?", 4);
        assertTrue(terms.contains("trabajo"));
        assertTrue(terms.contains("mercedes"));
        assertFalse(terms.contains("sobre"));
    }

    @Test
    public void respectsTermLimit() {
        assertEquals(2, MemoryQueryTerms.extract("programacion matematicas ingles gimnasio", 2).size());
    }
}
