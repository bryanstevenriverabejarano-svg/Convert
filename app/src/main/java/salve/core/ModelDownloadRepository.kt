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
        val engine = SalveLLM.getInstance(context)
        suspend fun fetch(id: String): ModelDownloader.DownloadEvent.Completed {
            var complete: ModelDownloader.DownloadEvent.Completed? = null
            downloader.downloadSelected(context, jsonBytes.inputStream(), id).collect { event ->
                when (event) {
                    is ModelDownloader.DownloadEvent.Started -> emit(ModelDownloadEvent.Status("Preparando ${event.id}", 0))
                    is ModelDownloader.DownloadEvent.Progress -> emit(ModelDownloadEvent.Status(
                        "${event.id}: ${event.bytes / 1_000_000} / ${event.totalBytes / 1_000_000} MB",
                        ((event.bytes * 100) / event.totalBytes).toInt().coerceIn(0, 99)))
                    is ModelDownloader.DownloadEvent.Verifying -> emit(ModelDownloadEvent.Status("Verificando ${event.id}…", 99))
                    is ModelDownloader.DownloadEvent.Completed -> complete = event
                    is ModelDownloader.DownloadEvent.Error -> throw event.error
                    ModelDownloader.DownloadEvent.AllDone -> Unit
                }
            }
            return complete ?: throw IOException("No se confirmó el archivo de $id")
        }
        try {
            val start = if (fallbackOnly) LocalModelPolicy.next(engine.pendingFallbackFrom)
                ?: throw LocalModelPolicy.SupersededFallbackException() else LocalModelPolicy.PRIMARY
            var selectionBeforeAttempt = engine.selectionVersion
            val prepared = LocalModelPolicy.prepare(start, attempt = { id ->
                selectionBeforeAttempt = engine.selectionVersion
                if (id != LocalModelPolicy.PRIMARY && !LocalModelPolicy.fallbackAllowed(id, engine.pendingFallbackFrom))
                    throw LocalModelPolicy.SupersededFallbackException()
                val event = fetch(id)
                emit(ModelDownloadEvent.Status("Midiendo RAM, cargando $id y comprobando una respuesta…", 99))
                val result = runInterruptible(Dispatchers.IO) {
                    if (id == LocalModelPolicy.PRIMARY)
                        engine.activateDownloadedModel(event.file.absolutePath, event.supportsVision, { !jobContext.isActive })
                    else engine.activateFallbackModel(event.file.absolutePath, event.supportsVision, { !jobContext.isActive })
                }
                jobContext.ensureActive()
                if (result.status == ModelResult.Status.CANCELLED) throw CancellationException("Instalación pausada")
                if (!result.isSuccess) throw IOException(result.error ?: "No se verificó la inferencia local")
                ModelDownloadEvent.Prepared(event.file, result.latencyMillis, event.name, event.supportsVision)
            }, onFailure = { id, failure ->
                // A successful concurrent recovery must not be replaced by an obsolete fallback.
                if (id != LocalModelPolicy.PRIMARY && !LocalModelPolicy.fallbackAllowed(id, engine.pendingFallbackFrom))
                    throw LocalModelPolicy.SupersededFallbackException()
                engine.recordFailureIfUnchanged(id, failure.message ?: "Falló $id", selectionBeforeAttempt)
                val next = LocalModelPolicy.next(id)
                if (next != null) emit(ModelDownloadEvent.Status("$id no está disponible. Preparando $next.", 0))
            })
            // A verified 3B file allows an offline downgrade; keep only the 8B loaded in RAM.
            if (prepared.modelName == LocalModelPolicy.PRIMARY) {
                try {
                    emit(ModelDownloadEvent.Status("8B activo. Guardando Dolphin 3B para cambios automáticos sin conexión…", 0))
                    fetch(LocalModelPolicy.LIGHT)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    jobContext.ensureActive()
                    emit(ModelDownloadEvent.Notice("8B preparado. El respaldo 3B aún no está guardado: ${failure.message}. Reanuda la preparación en IA y cámara."))
                }
            }
            // KEEP can coalesce a runtime downgrade while the 3B prefetch is in progress.
            if (engine.pendingFallbackFrom != null) {
                downloadAndPrepareModels(context, jsonBytes, true).collect { emit(it) }
            } else emit(prepared)
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
    data class Notice(val message: String) : ModelDownloadEvent()
    data class Skipped(val message: String) : ModelDownloadEvent()
    data class Error(val error: Exception) : ModelDownloadEvent()
}
