package salve.core;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class LLMCoderTest {
    @Test public void localCodeAndFixNeverCallCloud() {
        AtomicInteger localCalls = new AtomicInteger();
        LLMCoder coder = new LLMCoder(() -> true,
                prompt -> { fail("No cloud calls in local mode"); return null; },
                prompt -> { localCalls.incrementAndGet(); return ModelResult.success("```java\nclass X {}\n```", 1); });
        assertEquals("class X {}", coder.generateCode("Crea X", "Java"));
        assertEquals("class X {}", coder.generateFix("Revisa X", "X"));
        assertEquals(2, localCalls.get());
    }

    @Test public void failedLocalInferenceDoesNotBecomeCodeOrUseCloud() {
        LLMCoder coder = new LLMCoder(() -> true,
                prompt -> { fail("No cloud fallback"); return null; },
                prompt -> ModelResult.failure(ModelResult.Status.ERROR, "Native error", 1));
        String result = coder.generateCode("Crea X", "Java");
        assertTrue(result.startsWith("// No se pudo"));
        assertFalse(result.contains("Native error"));
    }

    @Test public void providerChoiceIsReadForEachRequest() {
        AtomicBoolean localMode = new AtomicBoolean(false);
        LLMCoder coder = new LLMCoder(localMode::get,
                prompt -> ModelResult.success("cloud", 1),
                prompt -> ModelResult.success("local", 1));
        assertEquals("cloud", coder.generateCode("Crea X", "Java"));
        localMode.set(true);
        assertEquals("local", coder.generateFix("Revisa X", "X"));
    }

    @Test public void unreadablePreferenceDoesNotTransmitPrompt() {
        LLMCoder coder = new LLMCoder(() -> { throw new IllegalStateException("Preference unavailable"); },
                prompt -> { fail("Unknown policy cannot authorize cloud"); return null; },
                prompt -> { fail("No routing without policy"); return null; });
        assertTrue(coder.generateFix("Revisa X", "X").startsWith("// No se pudo"));
    }

    @Test public void blankOutputAndCloudCancellationAreNotActionableCode() {
        LLMCoder blank = new LLMCoder(() -> true, null,
                prompt -> ModelResult.success("```java\n\n```", 1));
        assertTrue(blank.generateCode("Crea X", "Java").startsWith("// No se pudo"));
        LLMCoder cancelled = new LLMCoder(() -> false,
                prompt -> ModelResult.failure(ModelResult.Status.CANCELLED, "Stopped", 1),
                prompt -> { fail("Cancellation stops generation"); return null; });
        assertTrue(cancelled.generateFix("Revisa X", "X").startsWith("// No se pudo"));
    }
}
