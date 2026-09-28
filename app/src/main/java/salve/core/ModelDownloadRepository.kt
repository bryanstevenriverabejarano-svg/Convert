package salve.core

import android.content.Context
import kotlinx.coroutines.CancellationException
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
    fun downloadAndPrepareModels(context: Context, jsonBytes: ByteArray,
                                 fallbackOnly: Boolean = false): Flow<ModelDownloadEvent> = flow {
        val jobContext = currentCoroutineContext()
        val prefs = context.getSharedPreferences("salve_prefs", Context.MODE_PRIVATE)
        try {
            if (fallbackOnly && prefs.getString(LocalModelPolicy.FAILURE_KEY, null).isNullOrBlank())
                throw LocalModelPolicy.SupersededFallbackException()
            val prepared = LocalModelPolicy.prepare(fallbackOnly, prefs.getString(LocalModelPolicy.FAILURE_KEY, null),
                attempt = { id ->
                    val fallbackFailure = if (id == LocalModelPolicy.FALLBACK)
                        prefs.getString(LocalModelPolicy.FAILURE_KEY, null) else null
                    var ready: ModelDownloadEvent.Prepared? = null
                    downloader.downloadSelected(context, jsonBytes.inputStream(), id).collect { event ->
                        when (event) {
                            is ModelDownloader.DownloadEvent.Started -> emit(ModelDownloadEvent.Status("Preparando ${event.id}", 0))
                            is ModelDownloader.DownloadEvent.Progress -> emit(ModelDownloadEvent.Status(
                                "${event.id}: ${event.bytes / 1_000_000} / ${event.totalBytes / 1_000_000} MB",
                                ((event.bytes * 100) / event.totalBytes).toInt().coerceIn(0, 99)))
                            is ModelDownloader.DownloadEvent.Verifying -> emit(ModelDownloadEvent.Status("Verificando ${event.id}…", 99))
                            is ModelDownloader.DownloadEvent.Completed -> {
                                emit(ModelDownloadEvent.Status("Cargando ${event.id} y comprobando una respuesta real…", 99))
                                val result = runInterruptible(Dispatchers.IO) {
                                    val engine = SalveLLM.getInstance(context)
                                    if (id == LocalModelPolicy.FALLBACK) engine.activateFallbackModel(
                                        event.file.absolutePath, event.supportsVision, { !jobContext.isActive }, fallbackFailure)
                                    else engine.activateDownloadedModel(event.file.absolutePath,
                                        event.supportsVision, { !jobContext.isActive })
                                }
                                jobContext.ensureActive()
                                if (result.status == ModelResult.Status.CANCELLED) throw CancellationException("Instalación pausada")
                                if (!result.isSuccess) throw IOException(result.error ?: "No se verificó la inferencia local")
                                ready = ModelDownloadEvent.Prepared(event.file, result.latencyMillis, event.name, event.supportsVision)
                            }
                            is ModelDownloader.DownloadEvent.Error -> throw event.error
                            ModelDownloader.DownloadEvent.AllDone -> Unit
                        }
                    }
                    ready ?: throw IOException("No se confirmó la activación de $id")
                }, onPrimaryFailure = { failure ->
                    prefs.edit().putString(LocalModelPolicy.FAILURE_KEY, failure.message ?: "Falló Dolphin").apply()
                    emit(ModelDownloadEvent.Status("Dolphin falló. Preparando Gemma como respaldo; se reutilizará si ya está descargado.", 0))
                })
            emit(prepared)
        } catch (superseded: LocalModelPolicy.SupersededFallbackException) {
            jobContext.ensureActive()
            emit(ModelDownloadEvent.Skipped(superseded.message ?: "Respaldo antiguo descartado"))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            jobContext.ensureActive()
            emit(ModelDownloadEvent.Error(failure))
        }
    }.flowOn(Dispatchers.IO)
}

sealed class ModelDownloadEvent {
    data class Status(val message: String, val percent: Int) : ModelDownloadEvent()
    data class Prepared(val file: File, val latencyMillis: Long, val modelName: String,
                        val supportsVision: Boolean) : ModelDownloadEvent()
    data class Skipped(val message: String) : ModelDownloadEvent()
    data class Error(val error: Exception) : ModelDownloadEvent()
}
