package salve.core

import android.content.Context
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

    /** Compatibility entry point: a larger catalog must not silently download every artifact. */
    fun downloadAll(context: Context, jsonStream: InputStream): Flow<DownloadEvent> =
        downloadSelected(context, jsonStream)

    fun downloadSelected(context: Context, jsonStream: InputStream, preferredId: String? = null,
                         required: Set<ModelCatalog.Capability> = setOf(ModelCatalog.Capability.TEXT)): Flow<DownloadEvent> = channelFlow {
        val catalog = ModelCatalog.read(jsonStream)
        val item = catalog.selectDownload(required, preferredId)
            ?: throw IllegalArgumentException("No hay un modelo compatible con las capacidades solicitadas")
        val jobContext = currentCoroutineContext()
        jobContext.ensureActive()
        send(DownloadEvent.Started(item.id, 1, 1))
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
                                else DownloadEvent.Progress(item.id, 1, 1, bytes, total))
                        }, { !jobContext.isActive })
                }
            } finally {
                cancellation.cancel()
                transfer.cancel()
            }
            jobContext.ensureActive()
            send(DownloadEvent.Completed(item.id, file, item.supportsVision, item.name))
        } catch (e: Exception) {
            jobContext.ensureActive() // Cancellation must not be converted to a successful worker.
            send(DownloadEvent.Error(item.id, e))
            return@channelFlow
        }
        send(DownloadEvent.AllDone)
    }.flowOn(Dispatchers.IO)

    companion object {
        @JvmStatic
        fun loadItems(stream: InputStream): List<ModelCatalog.Entry> = ModelCatalog.read(stream).entries
    }

    sealed class DownloadEvent {
        data class Started(val id: String, val index: Int, val total: Int) : DownloadEvent()
        data class Progress(val id: String, val index: Int, val total: Int,
                            val bytes: Long, val totalBytes: Long) : DownloadEvent()
        data class Verifying(val id: String) : DownloadEvent()
        data class Completed(val id: String, val file: File, val supportsVision: Boolean,
                             val name: String = id) : DownloadEvent()
        data class Error(val id: String, val error: Exception) : DownloadEvent()
        data object AllDone : DownloadEvent()
    }
}
