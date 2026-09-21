package salve.core

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** Downloads only complete, pinned model files from the bundled catalog, never model weights in APK assets. */
class ModelDownloader(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(25, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()) {

    fun downloadAll(context: Context, jsonStream: InputStream): Flow<DownloadEvent> = channelFlow {
        val items = loadItems(jsonStream)
        val jobContext = currentCoroutineContext()
        for ((index, item) in items.withIndex()) {
            jobContext.ensureActive()
            send(DownloadEvent.Started(item.id, index + 1, items.size))
            try {
                val target = File(ModelStore.dir(context), item.filename)
                val transfer = VerifiedModelFile(client)
                val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { transfer.cancel() }
                }
                val file = try {
                    runInterruptible(Dispatchers.IO) {
                        transfer.download(item.url, target, item.sizeBytes, item.sha256,
                            { bytes, total, verifying ->
                                trySend(if (verifying) DownloadEvent.Verifying(item.id)
                                    else DownloadEvent.Progress(item.id, index + 1, items.size, bytes, total))
                            }, { !jobContext.isActive })
                    }
                } finally {
                    cancellation.cancel()
                    transfer.cancel()
                }
                jobContext.ensureActive()
                send(DownloadEvent.Completed(item.id, file, item.supportsVision))
            } catch (e: Exception) {
                jobContext.ensureActive() // Cancellation must not be converted to a successful worker.
                send(DownloadEvent.Error(item.id, e))
                return@channelFlow
            }
        }
        send(DownloadEvent.AllDone)
    }.flowOn(Dispatchers.IO)

    companion object {
        @JvmStatic
        fun loadItems(stream: InputStream): List<ModelItem> {
            val root = stream.bufferedReader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
            val items = root.getAsJsonArray("items") ?: error("Catálogo sin modelos")
            require(items.size() > 0) { "Catálogo vacío" }
            return items.map { element ->
                val item = element.asJsonObject
                val filename = item.get("filename").asString
                require(filename.matches(Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,140}\\.litertlm"))) {
                    "Nombre o formato de modelo inválido"
                }
                val url = item.get("url").asString
                require(NetworkResourcePolicy.validateModelUrl(url).allowed) { "Fuente de modelo no autorizada" }
                // Pin both the revision and the bytes. Never silently follow /resolve/main/ updates.
                require(Regex("https://huggingface\\.co/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/resolve/[a-f0-9]{40}/[^?]+(?:\\?download=true)?").matches(url)) {
                    "El enlace debe fijar una revisión de Hugging Face"
                }
                val size = item.get("sizeBytes").asLong
                val checksum = item.get("sha256").asString
                require(size in 1..16L * 1024 * 1024 * 1024 && checksum.matches(Regex("[a-f0-9]{64}"))) {
                    "Tamaño o SHA-256 de modelo inválido"
                }
                ModelItem(item.get("id").asString, url, filename, size, checksum,
                    item.get("supportsVision")?.asBoolean ?: false)
            }
        }
    }

    data class ModelItem(val id: String, val url: String, val filename: String,
                         val sizeBytes: Long, val sha256: String, val supportsVision: Boolean)

    sealed class DownloadEvent {
        data class Started(val id: String, val index: Int, val total: Int) : DownloadEvent()
        data class Progress(val id: String, val index: Int, val total: Int,
                            val bytes: Long, val totalBytes: Long) : DownloadEvent()
        data class Verifying(val id: String) : DownloadEvent()
        data class Completed(val id: String, val file: File, val supportsVision: Boolean) : DownloadEvent()
        data class Error(val id: String, val error: Exception) : DownloadEvent()
        data object AllDone : DownloadEvent()
    }
}
