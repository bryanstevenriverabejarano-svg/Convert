package salve.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Ordered, bounded attempts. Cancellation and superseded work never move down the ladder. */
object LocalModelPolicy {
    const val PRIMARY = "Dolphin 3.0 Llama 3.1 8B"
    const val LIGHT = "Dolphin 3.0 Llama 3.2 3B"
    const val FALLBACK = "Gemma 4 E2B"
    /** Opcional, fuera de la escalera automática: mejor razonamiento y visión, más RAM (3,66 GB). */
    const val GEMMA_E4B = "Gemma 4 E4B"
    /**
     * Opcional (Bryan): Gemma 4 E4B sin censura en GGUF Q4_K_M (5,34 GB), motor llama.cpp (gráfica si
     * puede). Sustituye a los Qwen 3.5. Gran parte del archivo son embeddings por capa que solo se leen
     * a trozos, así que en RAM ocupa bastante menos que el archivo.
     */
    const val UNCENSORED = "Gemma 4 E4B sin censura"
    const val UNCENSORED_FILE = "Gemma-4-E4B-Uncensored-HauhauCS-Aggressive-Q4_K_M-45b6a33.gguf"
    const val FAILURE_KEY = "dolphin_failure" // preserved for upgrades from the 3B-only app
    const val LARGE_FAILURE_KEY = "dolphin_8b_failure"
    const val PENDING_KEY = "local_fallback_from"
    const val REASON_KEY = "local_model_selection_reason"
    const val DOWNLOAD_VERSION_KEY = "dolphin_8b_download_requested"
    class SupersededFallbackException : IllegalStateException("El respaldo pendiente ya no es necesario.")

    @JvmStatic fun idForPath(path: String?): String? {
        val name = path?.let { java.io.File(it).name } ?: return null
        return when (name) {
            "Dolphin3.0-Llama3.1-8B-Q4_K_M-fd2736a.gguf" -> PRIMARY
            "Dolphin3.0-Llama3.2-3B-Q4_K_M-ac6b1ee.gguf" -> LIGHT
            "gemma-4-E2B-it-6e5c4f1.litertlm" -> FALLBACK
            "gemma-4-E4B-it-2eee7ac.litertlm" -> GEMMA_E4B
            UNCENSORED_FILE -> UNCENSORED
            else -> null
        }
    }
    @JvmStatic fun isDolphin(path: String?): Boolean = idForPath(path) in listOf(PRIMARY, LIGHT)
    /** Los Qwen 3.5 que se descargaban antes: sustituidos por Gemma 4 E4B sin censura, se borran. */
    @JvmStatic fun isRetiredQwen(name: String?): Boolean =
        name != null && name.startsWith("Qwen3.5-") && name.contains("Uncensored-HauhauCS")
    @JvmStatic fun isLarge(path: String?): Boolean = idForPath(path) == PRIMARY
    @JvmStatic fun failureKey(id: String): String = if (id == PRIMARY) LARGE_FAILURE_KEY else FAILURE_KEY
    @JvmStatic fun next(id: String?): String? = when (id) { PRIMARY -> LIGHT; LIGHT -> FALLBACK; else -> null }
    @JvmStatic fun fallbackAllowed(candidate: String?, pending: String?): Boolean =
        candidate != null && pending != null && next(pending) == candidate

    @JvmStatic fun confirmRecovery(probe: java.util.function.Supplier<ModelResult>, onRecovered: Runnable): ModelResult {
        val result = probe.get()
        if (!result.isSuccess) return result
        if (result.text.isNullOrBlank()) return ModelResult.failure(ModelResult.Status.ERROR,
            "El modelo recargado no devolvió texto", result.latencyMillis)
        onRecovered.run()
        return result
    }

    suspend fun <T> prepare(startId: String = PRIMARY, attempt: suspend (String) -> T,
                            onFailure: suspend (String, Exception) -> Unit): T {
        val ladder = listOf(PRIMARY, LIGHT, FALLBACK)
        require(startId in ladder)
        for (id in ladder.dropWhile { it != startId }) {
            currentCoroutineContext().ensureActive()
            try { return attempt(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (stale: SupersededFallbackException) { throw stale }
            catch (failure: Exception) {
                currentCoroutineContext().ensureActive()
                onFailure(id, failure)
                if (id == FALLBACK) throw failure
            }
        }
        error("No hay candidato local")
    }
}
