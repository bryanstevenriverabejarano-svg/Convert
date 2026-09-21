package salve.core.conversation;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import salve.core.ModelResult;
import static org.junit.Assert.*;

public class ConversationModelRouterTest {
    private ModelResult unavailable() {
        return ModelResult.failure(ModelResult.Status.UNAVAILABLE, "no runtime", 0L);
    }

    @Test public void configuredCloudCanAnswerWithoutCallingLocal() {
        ModelResult result = ConversationModelRouter.generate(false,
                () -> ModelResult.success("Respuesta del proveedor", 9L),
                () -> { throw new AssertionError("Local must not run"); });
        assertEquals("Respuesta del proveedor", result.getText());
    }

    @Test public void noCloudUsesLocalInference() {
        AtomicInteger calls = new AtomicInteger();
        ModelResult result = ConversationModelRouter.generate(false, null, () -> {
            calls.incrementAndGet();
            return ModelResult.success("Respuesta local", 2L);
        });
        assertTrue(result.isSuccess());
        assertEquals(1, calls.get());
    }

    @Test public void cloudErrorFallsBackToRealLocalAdapter() {
        ModelResult result = ConversationModelRouter.generate(false,
                () -> ModelResult.failure(ModelResult.Status.TIMEOUT, "timeout", 45L),
                () -> ModelResult.success("local", 5L));
        assertEquals("local", result.getText());
    }

    @Test public void missingModelsNeverInventConversation() {
        ModelResult result = ConversationModelRouter.generate(false, null, this::unavailable);
        assertEquals(ModelResult.Status.UNAVAILABLE, result.getStatus());
        assertNull(result.getText());
    }

    @Test public void imageFailureCannotFallBackToTextOnlyModel() {
        ModelResult result = ConversationModelRouter.generate(true, this::unavailable,
                () -> { throw new AssertionError("Text-only model cannot see the image"); });
        assertFalse(result.isSuccess());
        assertNull(result.getText());
    }

    @Test public void emptySuccessAndBrokenProviderAreFailures() {
        assertFalse(ConversationModelRouter.generate(false, null,
                () -> ModelResult.success("  ", 1L)).isSuccess());
        assertFalse(ConversationModelRouter.generate(false, null,
                () -> { throw new IllegalStateException("native init failed"); }).isSuccess());
    }

    @Test public void cancelledTurnDoesNotInvokeAnotherModel() {
        ModelResult result = ConversationModelRouter.generate(false,
                () -> ModelResult.failure(ModelResult.Status.CANCELLED, "cancel", 0L),
                () -> { throw new AssertionError("Cancelled turn"); });
        assertEquals(ModelResult.Status.CANCELLED, result.getStatus());
    }

    @Test public void interruptedWorkerDoesNotCallProviders() {
        Thread.currentThread().interrupt();
        try {
            assertEquals(ModelResult.Status.CANCELLED,
                    ConversationModelRouter.generate(false,
                            () -> { throw new AssertionError("Interrupted worker"); }, null).getStatus());
        } finally { Thread.interrupted(); }
    }
}
