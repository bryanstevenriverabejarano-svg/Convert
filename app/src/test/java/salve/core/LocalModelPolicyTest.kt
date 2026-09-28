package salve.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class LocalModelPolicyTest {
    @Test fun successful8BNeverActivatesABackup() = runBlocking {
        val calls = mutableListOf<String>()
        val answer = LocalModelPolicy.prepare(attempt = { calls += it; "ready" }, onFailure = { _, _ -> fail() })
        assertEquals("ready", answer)
        assertEquals(listOf(LocalModelPolicy.PRIMARY), calls)
    }
    @Test fun lowRamChooses3BBeforeGemma() = runBlocking {
        val calls = mutableListOf<String>()
        var failed: String? = null
        val ram = ModelMemoryPolicy.Snapshot(4L * 1024 * ModelMemoryPolicy.MIB, 12L * 1024 * ModelMemoryPolicy.MIB, 0, false)
        val answer = LocalModelPolicy.prepare(attempt = { id ->
            calls += id
            if (!ModelMemoryPolicy.canLoad(id, ram, false)) throw ModelMemoryPolicy.PressureException("Low RAM")
            id
        }, onFailure = { id, _ -> failed = id })
        assertEquals(LocalModelPolicy.LIGHT, answer)
        assertEquals(LocalModelPolicy.PRIMARY, failed)
        assertEquals(listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.LIGHT), calls)
    }
    @Test fun gemmaIsLastAndThereAreNoLoops() = runBlocking {
        val calls = mutableListOf<String>()
        try {
            LocalModelPolicy.prepare(attempt = { calls += it; throw IOException(it) }, onFailure = { _, _ -> })
            fail("Expected failure")
        } catch (expected: IOException) { assertEquals(LocalModelPolicy.FALLBACK, expected.message) }
        assertEquals(listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.LIGHT, LocalModelPolicy.FALLBACK), calls)
    }
    @Test fun bothDolphinsMustFailBeforeGemmaIsActivated() = runBlocking {
        val calls = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val result = LocalModelPolicy.prepare(attempt = {
            calls += it
            if (it != LocalModelPolicy.FALLBACK) throw IOException("native failure")
            "Gemma ready"
        }, onFailure = { id, _ -> failures += id })
        assertEquals("Gemma ready", result)
        assertEquals(listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.LIGHT), failures)
        assertEquals(failures + LocalModelPolicy.FALLBACK, calls)
    }
    @Test fun cancellationAtAnyStageNeverTriggersTheNextModel() = runBlocking {
        for (start in listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.LIGHT, LocalModelPolicy.FALLBACK)) {
            val calls = mutableListOf<String>()
            try {
                LocalModelPolicy.prepare(start, { calls += it; throw CancellationException("paused") }, { _, _ -> fail() })
                fail("Expected cancellation")
            } catch (expected: CancellationException) { }
            assertEquals(listOf(start), calls)
        }
    }
    @Test fun staleDownloadCannotReplaceRecoveredDolphinOrTryGemma() = runBlocking {
        val calls = mutableListOf<String>()
        try {
            LocalModelPolicy.prepare(LocalModelPolicy.LIGHT, {
                calls += it
                assertFalse(LocalModelPolicy.fallbackAllowed(it, null))
                throw LocalModelPolicy.SupersededFallbackException()
            }, { _, _ -> fail() })
            fail()
        } catch (expected: LocalModelPolicy.SupersededFallbackException) { }
        assertEquals(listOf(LocalModelPolicy.LIGHT), calls)
    }
    @Test fun fallbackMustMatchTheCurrentFailedTier() {
        assertTrue(LocalModelPolicy.fallbackAllowed(LocalModelPolicy.LIGHT, LocalModelPolicy.PRIMARY))
        assertFalse(LocalModelPolicy.fallbackAllowed(LocalModelPolicy.FALLBACK, LocalModelPolicy.PRIMARY))
        assertTrue(LocalModelPolicy.fallbackAllowed(LocalModelPolicy.FALLBACK, LocalModelPolicy.LIGHT))
        assertFalse(LocalModelPolicy.fallbackAllowed(LocalModelPolicy.LIGHT, LocalModelPolicy.LIGHT))
        assertFalse(LocalModelPolicy.fallbackAllowed(LocalModelPolicy.FALLBACK, null))
    }
    @Test fun importsCannotImpersonateAManagedModel() {
        assertFalse(LocalModelPolicy.isDolphin("/models/imported-123.gguf"))
        assertFalse(LocalModelPolicy.isDolphin("/models/Dolphin3.0-Llama3.1-8B-other.gguf"))
        assertTrue(LocalModelPolicy.isDolphin("/models/Dolphin3.0-Llama3.2-3B-Q4_K_M-ac6b1ee.gguf"))
        assertTrue(LocalModelPolicy.isLarge("/models/Dolphin3.0-Llama3.1-8B-Q4_K_M-fd2736a.gguf"))
    }
    @Test fun successfulRetryClearsSavedFailureOnlyAfterRealInference() {
        var pending: String? = LocalModelPolicy.PRIMARY
        val events = mutableListOf<String>()
        val result = LocalModelPolicy.confirmRecovery({
            assertNotNull(pending); events += "probe"; ModelResult.success("Hola Bryan", 10)
        }, { events += "clear"; pending = null })
        assertTrue(result.isSuccess)
        assertEquals(listOf("probe", "clear"), events)
        assertFalse(LocalModelPolicy.fallbackAllowed(LocalModelPolicy.LIGHT, pending))
    }
    @Test fun failedCancelledAndEmptyRetryDoNotClaimRecovery() {
        for (result in listOf(ModelResult.failure(ModelResult.Status.ERROR, "error", 1),
            ModelResult.failure(ModelResult.Status.CANCELLED, "cancelled", 1), ModelResult.success("   ", 1))) {
            assertFalse(LocalModelPolicy.confirmRecovery({ result }, { fail() }).isSuccess)
        }
    }
    @Test fun exceptionDuringRetryDoesNotEraseFailureEvidence() {
        assertThrows(IllegalStateException::class.java) {
            LocalModelPolicy.confirmRecovery({ throw IllegalStateException("broken native load") }, { fail() })
        }
    }
}
