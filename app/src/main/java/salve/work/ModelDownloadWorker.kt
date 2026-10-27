package salve.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import salve.core.ModelDownloadEvent
import salve.core.ModelDownloadRepository

// Keep the standard two-argument constructor: WorkManager instantiates workers via reflection.
class ModelDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        var failure: String? = null
        var ready: String? = null
        var notice: String? = null
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo("Preparando la descarga…"))
            val optionalId = inputData.getString(KEY_OPTIONAL_ID)
            if (optionalId != null) return prepareOptional(optionalId)
            val jsonBytes = withContext(Dispatchers.IO) {
                applicationContext.assets.open("config/models.json").use { it.readBytes() }
            }
            ModelDownloadRepository().downloadAndPrepareModels(applicationContext, jsonBytes, inputData.getBoolean(KEY_FALLBACK, false)).collect { event ->
                when (event) {
                    is ModelDownloadEvent.Status -> updateProgress(event.percent, event.message)
                    is ModelDownloadEvent.Prepared -> {
                        failure = null
                        ready = salve.core.SalveLLM.getInstance(applicationContext).statusDescription + ". Chat en modo local." +
                            if (event.supportsVision) " Visión declarada por el catálogo; pendiente de probar con una foto." else ""
                    }
                    is ModelDownloadEvent.Notice -> notice = event.message
                    is ModelDownloadEvent.Skipped -> {
                        failure = null
                        ready = event.message
                    }
                    is ModelDownloadEvent.Error -> {
                        failure = event.error.message ?: "No se pudo preparar el modelo"
                        android.util.Log.e("ModelDownloadWorker", "Preparación fallida", event.error)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("ModelDownloadWorker", "Preparación fallida", error)
            failure = error.message ?: "No se pudo iniciar la descarga"
        }
        val errorMessage = failure ?: if (ready == null) "No se confirmó la activación del modelo" else null
        return if (errorMessage != null) Result.failure(workDataOf(KEY_STATUS to "error", KEY_MESSAGE to errorMessage))
            else {
                // Recién preparado E2B en un móvil grande: se encola la mejora a E4B (otro trabajo, solo Wi-Fi).
                salve.core.AutoModelUpgrade.maybeUpgrade(applicationContext)
                Result.success(workDataOf(KEY_STATUS to "ready", KEY_MESSAGE to listOfNotNull(ready, notice).joinToString("\n")))
            }
    }

    /**
     * Modelo opcional (fuera de la escalera automática), p. ej. Gemma 4 E4B: descarga fijada por
     * SHA-256 y activación con una respuesta real. Si no carga (RAM) o no responde, SalveLLM
     * restaura el modelo anterior.
     */
    private suspend fun prepareOptional(id: String): Result {
        return try {
            val jsonBytes = withContext(Dispatchers.IO) {
                applicationContext.assets.open("config/models_optional.json").use { it.readBytes() }
            }
            var completed: salve.core.ModelDownloader.DownloadEvent.Completed? = null
            salve.core.ModelDownloader().downloadSelected(applicationContext, jsonBytes.inputStream(), id).collect { event ->
                when (event) {
                    is salve.core.ModelDownloader.DownloadEvent.Progress -> updateProgress(
                        ((event.bytes * 100) / event.totalBytes).toInt().coerceIn(0, 98),
                        "${event.id}: ${event.bytes / 1_000_000} / ${event.totalBytes / 1_000_000} MB")
                    is salve.core.ModelDownloader.DownloadEvent.Verifying -> updateProgress(99, "Verificando ${event.id}…")
                    is salve.core.ModelDownloader.DownloadEvent.Completed -> completed = event
                    is salve.core.ModelDownloader.DownloadEvent.Error -> throw event.error
                    else -> Unit
                }
            }
            val file = completed ?: throw java.io.IOException("No se confirmó el archivo de $id")
            updateProgress(99, "Cargando $id y comprobando una respuesta…")
            val result = withContext(Dispatchers.IO) {
                salve.core.SalveLLM.getInstance(applicationContext)
                    .activateDownloadedModel(file.file.absolutePath, file.supportsVision) { isStopped }
            }
            if (result.isSuccess) Result.success(workDataOf(KEY_STATUS to "ready",
                KEY_MESSAGE to "$id activo. " + salve.core.SalveLLM.getInstance(applicationContext).statusDescription))
            else Result.failure(workDataOf(KEY_STATUS to "error",
                KEY_MESSAGE to "$id descargado pero no se pudo activar; se conserva el modelo anterior. ${result.error ?: ""}"))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("ModelDownloadWorker", "Modelo opcional fallido", error)
            Result.failure(workDataOf(KEY_STATUS to "error", KEY_MESSAGE to (error.message ?: "No se pudo preparar $id")))
        }
    }

    private suspend fun updateProgress(percent: Int, message: String) {
        setProgress(workDataOf(KEY_PROGRESS to percent, KEY_MESSAGE to message, KEY_STATUS to "running"))
        try {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(message, percent))
        } catch (error: SecurityException) {
            android.util.Log.w("ModelDownloadWorker", "Notificación de progreso no permitida")
        }
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val notification = buildNotification(message, 0)
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(message: String, progress: Int): Notification =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Modelo local de Salve").setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download).setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0 || progress == 99)
            .addAction(android.R.drawable.ic_media_pause, "Pausar",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
            .build()

    private fun ensureNotificationChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Descarga del modelo local", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        const val UNIQUE_WORK_NAME = "model_download_work"
        const val KEY_PROGRESS = "progress"
        const val KEY_MESSAGE = "message"
        const val KEY_STATUS = "status"
        private const val KEY_FALLBACK = "fallback_only"
        private const val KEY_OPTIONAL_ID = "optional_model_id"

        /**
         * Como [enqueueOptional], pero detrás de la descarga en curso si la hay (con KEEP se
         * descartaría en silencio mientras se prepara el modelo por defecto).
         */
        @JvmStatic fun enqueueOptionalAfterCurrent(context: Context, id: String) {
            val work = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(KEY_OPTIONAL_ID to id))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresStorageNotLow(true).build()).build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }

        /** Descarga y activa un modelo de config/models_optional.json (solo Wi-Fi). */
        @JvmStatic fun enqueueOptional(context: Context, id: String) {
            val work = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(KEY_OPTIONAL_ID to id))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresStorageNotLow(true).build()).build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, work)
        }
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 2001

        /** Persisted 8B pressure/failure tries 3B first; a 3B failure permits Gemma. */
        @JvmStatic fun enqueueFallback(context: Context) {
            val prefs = context.getSharedPreferences("salve_prefs", Context.MODE_PRIVATE)
            val pending = prefs.getString(salve.core.LocalModelPolicy.PENDING_KEY, null)
                ?: if (!prefs.getString(salve.core.LocalModelPolicy.FAILURE_KEY, null).isNullOrBlank()) salve.core.LocalModelPolicy.LIGHT else return
            if (salve.core.LocalModelPolicy.next(pending) == null) return
            // Cached recovery is performed synchronously by SalveLLM with no socket access.
            // A corrupt cache may require a new transfer, so background work still requires Wi-Fi.
            val work = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(KEY_FALLBACK to true))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresStorageNotLow(true).build()).build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, work)
        }

        @JvmStatic @JvmOverloads
        fun enqueue(context: Context, allowMetered: Boolean = false) {
            val builder = OneTimeWorkRequestBuilder<ModelDownloadWorker>().setConstraints(
                Constraints.Builder().setRequiredNetworkType(if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED)
                    .setRequiresStorageNotLow(true).build())
            val manager = WorkManager.getInstance(context)
            if (!allowMetered) {
                manager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, builder.build())
                return
            }
            // Update the queued Wi-Fi request rather than having KEEP ignore the user's network choice.
            val pending = manager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME)
            pending.addListener({
                try {
                    val existing = pending.get().firstOrNull { !it.state.isFinished }
                    if (existing != null) manager.updateWork(builder.setId(existing.id).build())
                    else manager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, builder.build())
                } catch (error: Exception) {
                    android.util.Log.e("ModelDownloadWorker", "No se pudo cambiar la red de descarga", error)
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        }
    }
}
