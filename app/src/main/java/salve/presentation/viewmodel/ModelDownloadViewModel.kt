package salve.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkInfo
import androidx.work.WorkManager
import salve.work.ModelDownloadWorker

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)
    val uiState: LiveData<DownloadUiState> = workManager
        .getWorkInfosForUniqueWorkLiveData(ModelDownloadWorker.UNIQUE_WORK_NAME).map { infos ->
            val info = infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
            if (info == null) DownloadUiState.Idle
            else {
                val data = if (info.state.isFinished) info.outputData else info.progress
                val message = data.getString(ModelDownloadWorker.KEY_MESSAGE)
                when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadUiState.Running(0,
                        "Esperando conexión y espacio disponible. Por defecto se usa Wi-Fi sin límite de datos.")
                    WorkInfo.State.RUNNING -> DownloadUiState.Running(data.getInt(ModelDownloadWorker.KEY_PROGRESS, 0), message)
                    WorkInfo.State.SUCCEEDED -> DownloadUiState.Success(message)
                    WorkInfo.State.FAILED -> DownloadUiState.Error(message ?: "No se pudo preparar el modelo")
                    WorkInfo.State.CANCELLED -> DownloadUiState.Error("Descarga pausada. Puedes reanudarla sin perder lo descargado.")
                }
            }
        }

    @JvmOverloads fun startDownload(allowMetered: Boolean = false) {
        ModelDownloadWorker.enqueue(getApplication(), allowMetered)
    }

    fun pauseDownload() { workManager.cancelUniqueWork(ModelDownloadWorker.UNIQUE_WORK_NAME) }
}

sealed class DownloadUiState {
    data object Idle : DownloadUiState()
    data class Running(val percent: Int, val message: String?) : DownloadUiState()
    data class Success(val message: String?) : DownloadUiState()
    data class Error(val message: String) : DownloadUiState()
}
