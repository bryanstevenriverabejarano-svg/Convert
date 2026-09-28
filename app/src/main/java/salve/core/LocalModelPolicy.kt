package salve.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** One primary attempt and, only on failure, one fallback attempt. Never a download list. */
object LocalModelPolicy {
    const val PRIMARY = "Dolphin 3.0 Llama 3.2 3B"
    const val FALLBACK = "Gemma 4 E2B"
    const val FAILURE_KEY = "dolphin_failure"

    @JvmStatic fun isDolphin(path: String?): Boolean = path != null &&
        java.io.File(path).name.startsWith("Dolphin3.0-Llama3.2-3B-") && path.endsWith(".gguf")

    suspend fun <T> prepare(fallbackOnly: Boolean, primaryFailure: String?,
                            attempt: suspend (String) -> T,
                            onPrimaryFailure: suspend (Exception) -> Unit): T {
        currentCoroutineContext().ensureActive()
        if (fallbackOnly) {
            require(!primaryFailure.isNullOrBlank()) { "Gemma requiere un fallo previo de Dolphin" }
            return attempt(FALLBACK)
        }
        try {
            return attempt(PRIMARY)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            currentCoroutineContext().ensureActive()
            onPrimaryFailure(failure)
            currentCoroutineContext().ensureActive()
            return attempt(FALLBACK)
        }
    }
}
