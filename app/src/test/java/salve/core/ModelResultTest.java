package salve.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModelResultTest {
    @Test
    public void representsSuccessWithoutErrorText() {
        ModelResult result = ModelResult.success("hola", 12L);
        assertTrue(result.isSuccess());
        assertEquals("hola", result.getText());
        assertEquals(12L, result.getLatencyMillis());
    }

    @Test
    public void representsFailureSeparately() {
        ModelResult result = ModelResult.failure(ModelResult.Status.TIMEOUT, "tardó demasiado", 45L);
        assertFalse(result.isSuccess());
        assertEquals(ModelResult.Status.TIMEOUT, result.getStatus());
        assertEquals("tardó demasiado", result.getError());
    }
}
