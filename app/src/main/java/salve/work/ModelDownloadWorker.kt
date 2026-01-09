package salve.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import salve.core.ModelDownloadEvent
import salve.core.ModelDownloadRepository
import java.io.IOException

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: ModelDownloadRepository = ModelDownloadRepository()
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val jsonBytes = try {
            withContext(Dispatchers.IO) {
                applicationContext.assets.open("config/models.json").readBytes()
            }
        } catch (e: IOException) {
            setProgressAsync(workDataOf(KEY_STATUS to "error", KEY_MESSAGE to e.message))
            return Result.failure()
        }

        ensureNotificationChannel()
        setForeground(createForegroundInfo("Iniciando descarga…"))

        repository.downloadAndPrepareModels(applicationContext, jsonBytes).collect { event ->
            when (event) {
                is ModelDownloadEvent.Started -> {
                    updateProgress(event.id, 0, "Descargando ${event.id}")
                }
                is ModelDownloadEvent.Progress -> {
                    val percent = if (event.totalBytes > 0) {
                        ((event.bytes * 100) / event.totalBytes).toInt()
                    } else {
                        0
                    }
                    updateProgress(event.id, percent, "${event.id}: $percent%")
                }
                is ModelDownloadEvent.Prepared -> {
                    updateProgress(event.id, 100, "${event.id} preparado")
                }
                is ModelDownloadEvent.Error -> {
                    updateProgress(event.id, 0, "Error en ${event.id}: ${event.error.message}")
                }
                ModelDownloadEvent.AllReady -> {
                    updateProgress(null, 100, "Modelos listos")
                }
                ModelDownloadEvent.AllDone -> {
                    updateProgress(null, 100, "Descarga finalizada")
                }
            }
        }

        return Result.success()
    }

    private fun updateProgress(modelId: String?, percent: Int, message: String) {
        setProgressAsync(
            workDataOf(
                KEY_MODEL_ID to modelId,
                KEY_PROGRESS to percent,
                KEY_MESSAGE to message,
                KEY_STATUS to "running"
            )
        )
        val notification = buildNotification(message, percent)
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, buildNotification(message, 0))
    }

    private fun buildNotification(message: String, progress: Int): Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Descarga de modelos")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descargas de modelos",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "model_download_work"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_MESSAGE = "message"
        const val KEY_STATUS = "status"

        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 2001
    }
}
