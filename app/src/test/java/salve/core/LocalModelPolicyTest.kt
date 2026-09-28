package salve.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class LocalModelPolicyTest {
    @Test fun successfulDolphinNeverRequestsGemma() = runBlocking {
        val calls = mutableListOf<String>()
        val answer = LocalModelPolicy.prepare(false, null, { calls += it; "ready" }, { fail("No failure expected") })
        assertEquals("ready", answer)
        assertEquals(listOf(LocalModelPolicy.PRIMARY), calls)
    }
    @Test fun preparationFailureTriesGemmaExactlyOnce() = runBlocking {
        val calls = mutableListOf<String>()
        var recorded: Exception? = null
        val answer = LocalModelPolicy.prepare(false, null, {
            calls += it
            if (it == LocalModelPolicy.PRIMARY) throw IOException("broken download or inference")
            "cached Gemma"
        }, { recorded = it })
        assertEquals(listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.FALLBACK), calls)
        assertNotNull(recorded)
        assertEquals("cached Gemma", answer)
    }
    @Test fun fallbackFailureDoesNotLoopBackToDolphin() = runBlocking {
        val calls = mutableListOf<String>()
        try {
            LocalModelPolicy.prepare(false, null, { calls += it; throw IOException(it) }, {})
            fail("Expected failure")
        } catch (expected: IOException) { assertEquals(LocalModelPolicy.FALLBACK, expected.message) }
        assertEquals(listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.FALLBACK), calls)
    }
    @Test fun cancellationNeverTriggersFallback() = runBlocking {
        val calls = mutableListOf<String>()
        try {
            LocalModelPolicy.prepare(false, null, { calls += it; throw CancellationException("paused") }, { fail("Cancellation is not a model error") })
            fail("Expected cancellation")
        } catch (expected: CancellationException) { }
        assertEquals(listOf(LocalModelPolicy.PRIMARY), calls)
    }
    @Test fun inferenceFallbackRequiresRecordedFailure() = runBlocking {
        val calls = mutableListOf<String>()
        try {
            LocalModelPolicy.prepare(true, null, { calls += it }, {})
            fail("Expected missing failure evidence")
        } catch (expected: IllegalArgumentException) { }
        assertTrue(calls.isEmpty())
        LocalModelPolicy.prepare(true, "native inference failed", { calls += it }, { fail("Already failed") })
        assertEquals(listOf(LocalModelPolicy.FALLBACK), calls)
    }
    @Test fun importedGgufIsNotMistakenForManagedDolphin() {
        assertFalse(LocalModelPolicy.isDolphin("/models/imported-123.gguf"))
        assertFalse(LocalModelPolicy.isDolphin("/models/Gemma.litertlm"))
        assertTrue(LocalModelPolicy.isDolphin("/models/Dolphin3.0-Llama3.2-3B-Q4_K_M-ac6b1ee.gguf"))
    }
}
