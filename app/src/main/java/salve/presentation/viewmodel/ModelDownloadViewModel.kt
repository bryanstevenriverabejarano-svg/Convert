package salve.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import salve.work.ModelDownloadWorker

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)

    private val workInfos: LiveData<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkLiveData(ModelDownloadWorker.UNIQUE_WORK_NAME)

    val uiState: LiveData<DownloadUiState> = Transformations.map(workInfos) { infos ->
        val info = infos.firstOrNull()
        if (info == null) {
            DownloadUiState.Idle
        } else {
            val progress = info.progress
            val percent = progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
            val message = progress.getString(ModelDownloadWorker.KEY_MESSAGE)
            val modelId = progress.getString(ModelDownloadWorker.KEY_MODEL_ID)
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

    private val _trigger = MutableLiveData(Unit)
    val trigger: LiveData<Unit> = _trigger

    fun startDownload() {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>().build()
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
