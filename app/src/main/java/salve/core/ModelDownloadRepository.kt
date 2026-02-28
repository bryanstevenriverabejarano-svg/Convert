package salve.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
class ModelDownloadRepository(
    private val downloader: ModelDownloader = ModelDownloader()
) {
    fun downloadAndPrepareModels(context: Context, jsonBytes: ByteArray): Flow<ModelDownloadEvent> {
        return channelFlow {
            val needsDownload = withContext(Dispatchers.IO) {
                downloader.precheckAll(context, jsonBytes.inputStream())
            }
            if (!needsDownload) {
                trySend(ModelDownloadEvent.AllReady)
                return@channelFlow
            }

            downloader.downloadAll(context, jsonBytes.inputStream()).collect { event ->
                when (event) {
                    is ModelDownloader.DownloadEvent.Progress -> {
                        trySend(
                            ModelDownloadEvent.Progress(
                                event.id,
                                event.index,
                                event.total,
                                event.bytes,
                                event.totalBytes,
                                event.mbPerSec
                            )
                        )
                    }
                    is ModelDownloader.DownloadEvent.Completed -> {
                        // ensureModelFolder throws Exception — catch it so a zip-extraction
                        // failure doesn't propagate out of the flow and crash the Worker.
                        val prepared = try {
                            withContext(Dispatchers.IO) {
                                ModelStore.ensureModelFolder(context, event.file, event.id)
                            }
                        } catch (e: Exception) {
                            trySend(ModelDownloadEvent.Error(event.id, e))
                            null
                        }
                        if (prepared != null) trySend(ModelDownloadEvent.Prepared(event.id, prepared))
                    }
                    is ModelDownloader.DownloadEvent.Error -> {
                        trySend(ModelDownloadEvent.Error(event.id, event.error))
                    }
                    is ModelDownloader.DownloadEvent.Started -> {
                        trySend(ModelDownloadEvent.Started(event.id, event.index, event.total))
                    }
                    ModelDownloader.DownloadEvent.AllDone -> {
                        trySend(ModelDownloadEvent.AllDone)
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }
}

sealed class ModelDownloadEvent {
    data class Started(val id: String, val index: Int, val total: Int) : ModelDownloadEvent()
    data class Progress(
        val id: String,
        val index: Int,
        val total: Int,
        val bytes: Long,
        val totalBytes: Long,
        val mbPerSec: Double
    ) : ModelDownloadEvent()
    data class Prepared(val id: String, val directory: java.io.File) : ModelDownloadEvent()
    data class Error(val id: String, val error: Exception) : ModelDownloadEvent()
    data object AllReady : ModelDownloadEvent()
    data object AllDone : ModelDownloadEvent()
}
