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
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo("Preparando la descarga…"))
            val jsonBytes = withContext(Dispatchers.IO) {
                applicationContext.assets.open("config/models.json").use { it.readBytes() }
            }
            ModelDownloadRepository().downloadAndPrepareModels(applicationContext, jsonBytes).collect { event ->
                when (event) {
                    is ModelDownloadEvent.Status -> updateProgress(event.percent, event.message)
                    is ModelDownloadEvent.Prepared -> {
                        ready = "${event.modelName} activo. Prueba de texto: ${event.latencyMillis} ms. Chat en modo local." +
                            if (event.supportsVision) " Visión declarada por el catálogo; pendiente de probar con una foto." else ""
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
            else Result.success(workDataOf(KEY_STATUS to "ready", KEY_MESSAGE to ready))
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
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 2001

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
