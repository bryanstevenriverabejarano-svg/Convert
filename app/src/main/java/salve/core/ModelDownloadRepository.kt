package salve.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.io.IOException

class ModelDownloadRepository(private val downloader: ModelDownloader = ModelDownloader()) {
    fun downloadAndPrepareModels(context: Context, jsonBytes: ByteArray): Flow<ModelDownloadEvent> = flow {
        val jobContext = currentCoroutineContext()
        downloader.downloadAll(context, jsonBytes.inputStream()).collect { event ->
            when (event) {
                is ModelDownloader.DownloadEvent.Started -> emit(ModelDownloadEvent.Status("Descargando ${event.id}", 0))
                is ModelDownloader.DownloadEvent.Progress -> emit(ModelDownloadEvent.Status(
                    "${event.id}: ${event.bytes / 1_000_000} / ${event.totalBytes / 1_000_000} MB",
                    ((event.bytes * 100) / event.totalBytes).toInt().coerceIn(0, 99)))
                is ModelDownloader.DownloadEvent.Verifying -> emit(ModelDownloadEvent.Status("Verificando la integridad del modelo…", 99))
                is ModelDownloader.DownloadEvent.Completed -> {
                    emit(ModelDownloadEvent.Status("Cargando ${event.id} y comprobando una respuesta real…", 99))
                    val result = runInterruptible(Dispatchers.IO) {
                        SalveLLM.getInstance(context).activateDownloadedModel(event.file.absolutePath,
                            event.supportsVision, { !jobContext.isActive })
                    }
                    jobContext.ensureActive()
                    if (result.isSuccess) emit(ModelDownloadEvent.Prepared(event.file, result.latencyMillis))
                    else emit(ModelDownloadEvent.Error(IOException(result.error ?: "No se verificó la inferencia local")))
                }
                is ModelDownloader.DownloadEvent.Error -> emit(ModelDownloadEvent.Error(event.error))
                ModelDownloader.DownloadEvent.AllDone -> Unit
            }
        }
    }.flowOn(Dispatchers.IO)
}

sealed class ModelDownloadEvent {
    data class Status(val message: String, val percent: Int) : ModelDownloadEvent()
    data class Prepared(val file: File, val latencyMillis: Long) : ModelDownloadEvent()
    data class Error(val error: Exception) : ModelDownloadEvent()
}
