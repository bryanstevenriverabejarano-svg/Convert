package salve.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import salve.work.ModelDownloadWorker

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val workInfos: LiveData<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkLiveData(ModelDownloadWorker.UNIQUE_WORK_NAME)

    val uiState: LiveData<DownloadUiState> =
        workInfos.map { infos: List<WorkInfo> ->
            val info = infos.firstOrNull()

            if (info == null) {
                DownloadUiState.Idle
            } else {
                val progressData = info.progress
                val percent = progressData.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                val message = progressData.getString(ModelDownloadWorker.KEY_MESSAGE)
                val modelId = progressData.getString(ModelDownloadWorker.KEY_MODEL_ID)

                when (info.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING -> DownloadUiState.Running(modelId, percent, message)

                    WorkInfo.State.SUCCEEDED -> DownloadUiState.Success(message)

                    WorkInfo.State.FAILED -> DownloadUiState.Error(message ?: "Error en descarga")

                    WorkInfo.State.CANCELLED -> DownloadUiState.Error("Descarga cancelada")

                    else -> DownloadUiState.Idle
                }
            }
        }

    // Si quieres mantenerlo, ok, pero realmente no es necesario.
    private val _trigger = MutableLiveData(Unit)
    val trigger: LiveData<Unit> = _trigger

    fun startDownload() {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            // Opcional: poner tag para buscarlo por tag si quieres
            //.addTag("MODEL_DOWNLOAD")
            .build()

        workManager.enqueueUniqueWork(
            ModelDownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )

        _trigger.value = Unit
    }
}

sealed class DownloadUiState {
    data object Idle : DownloadUiState()
    data class Running(val modelId: String?, val percent: Int, val message: String?) : DownloadUiState()
    data class Success(val message: String?) : DownloadUiState()
    data class Error(val message: String) : DownloadUiState()
}
