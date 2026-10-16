package salve.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import salve.core.EmbeddingsIndex
import salve.core.MindRuntime
import salve.core.VerifiedModelFile
import salve.core.embeddings.EmbeddingArtifacts
import java.util.concurrent.TimeUnit

/** Independent Wi-Fi download. A partial model/tokenizer pair never becomes runnable. */
class EmbeddingDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private var lastProgressAt = 0L
    override suspend fun doWork(): Result {
        val artifacts = EmbeddingsIndex.artifacts(applicationContext)
        val transfer = VerifiedModelFile(OkHttpClient.Builder().connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS).build())
        return try {
            foreground("Preparando memoria en español", 0)
            coroutineScope {
                val work = currentCoroutineContext()
                val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { transfer.cancel() }
                }
                try {
                    runInterruptible(Dispatchers.IO) {
                        check(artifacts.directory.exists() || artifacts.directory.mkdirs()) { "Sin almacenamiento para la memoria" }
                        transfer.download(EmbeddingArtifacts.MODEL_URL, artifacts.model, EmbeddingArtifacts.MODEL_BYTES,
                            EmbeddingArtifacts.MODEL_SHA, { bytes, _, verifying ->
                                publish(if (verifying) "Verificando modelo de memoria" else "Descargando memoria en español",
                                    (bytes * 100 / EmbeddingArtifacts.TOTAL_BYTES).toInt())
                            }, { !work.isActive || isStopped })
                        transfer.download(EmbeddingArtifacts.TOKENIZER_URL, artifacts.tokenizer, EmbeddingArtifacts.TOKENIZER_BYTES,
                            EmbeddingArtifacts.TOKENIZER_SHA, { bytes, _, verifying ->
                                publish(if (verifying) "Verificando tokenizador" else "Descargando tokenizador",
                                    ((EmbeddingArtifacts.MODEL_BYTES + bytes) * 100 / EmbeddingArtifacts.TOTAL_BYTES).toInt())
                            }, { !work.isActive || isStopped })
                        work.ensureActive()
                        artifacts.markVerified()
                    }
                } finally { cancellation.cancel(); transfer.cancel() }
            }
            currentCoroutineContext().ensureActive()
            status(applicationContext, "Archivos verificados; comprobando recuperación en español")
            MindRuntime.get(applicationContext).onEmbeddingsPrepared()
            Result.success()
        } catch (cancelled: CancellationException) {
            status(applicationContext, "Descarga pausada; reanuda en IA y cámara → Memoria en español")
            throw cancelled
        } catch (failure: Exception) {
            currentCoroutineContext().ensureActive()
            status(applicationContext, "No se completó la memoria en español: ${failure.message ?: failure.javaClass.simpleName}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally { transfer.cancel() }
    }
    private fun notification(message: String, percent: Int) = NotificationCompat.Builder(applicationContext, CHANNEL)
        .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Memoria en español")
        .setContentText(message).setOnlyAlertOnce(true).setProgress(100, percent, false)
        .addAction(android.R.drawable.ic_media_pause, "Pausar", WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
        .build()
    private suspend fun foreground(message: String, percent: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Memoria en español", NotificationManager.IMPORTANCE_LOW))
        val info = if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(NOTIFICATION, notification(message, percent), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else ForegroundInfo(NOTIFICATION, notification(message, percent))
        setForeground(info)
    }
    private fun publish(message: String, percent: Int) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastProgressAt < 500 && percent < 100) return
        lastProgressAt = now
        status(applicationContext, "$message ($percent %)")
        setProgressAsync(workDataOf("progress" to percent, "message" to message))
        try {
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION, notification(message, percent))
        } catch (_: SecurityException) { }
    }
    companion object {
        const val NAME = "salve-spanish-embeddings-v1"
        private const val PREFS = "salve_embedding_download"
        private const val CHANNEL = "salve_embedding_download"
        private const val NOTIFICATION = 2002
        /** Schedule once automatically; user cancellation persists until an explicit resume. */
        @JvmStatic @Synchronized fun enqueue(context: Context, resume: Boolean = false) {
            if (EmbeddingsIndex.artifacts(context).installed()) {
                if (resume) MindRuntime.get(context).onEmbeddingsPrepared()
                return
            }
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!resume && prefs.getBoolean(EmbeddingArtifacts.ID, false)) return
            val work = OneTimeWorkRequestBuilder<EmbeddingDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresStorageNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, work)
            prefs.edit().putBoolean(EmbeddingArtifacts.ID, true).apply()
            status(context, "Esperando Wi-Fi para preparar la memoria en español (123,42 MB)")
        }
        @JvmStatic fun describe(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("status", "Memoria en español pendiente de preparar (123,42 MB)")!!
        @JvmStatic fun status(context: Context, message: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("status", message).apply()
        }
    }
}
