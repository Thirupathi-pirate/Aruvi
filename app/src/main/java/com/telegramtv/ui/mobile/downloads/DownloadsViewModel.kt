package com.telegramtv.ui.mobile.downloads

import android.os.Environment
import androidx.lifecycle.ViewModel
import com.telegramtv.download.DownloadStatus
import com.telegramtv.download.DownloadTask
import com.telegramtv.download.FileDownloader
import com.telegramtv.data.repository.AuthRepository
import com.telegramtv.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import java.io.File

/**
 * Unified download item for the UI layer.
 * Works with both the custom FileDownloader and legacy DownloadManager items.
 */
data class DownloadItem(
    val id: Long,
    val title: String,
    val status: DownloadStatus,
    val totalSize: Long,
    val downloadedSize: Long,
    val speed: Long = 0L,
    val localPath: String? = null,
    val mimeType: String? = null,
    val fileId: Int? = null
)

data class DownloadsUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDownloader: FileDownloader,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = fileDownloader.tasks.map { tasksMap ->
        val items = tasksMap.values
            .sortedByDescending { it.id }
            .map { task ->
                DownloadItem(
                    id = task.id,
                    title = task.fileName,
                    status = task.status,
                    totalSize = task.totalBytes,
                    downloadedSize = task.downloadedBytes,
                    speed = task.speed,
                    localPath = task.localPath,
                    mimeType = task.mimeType,
                    fileId = task.fileId
                )
            }
        DownloadsUiState(downloads = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadsUiState())

    /**
     * Start a new download.
     */
    fun startDownload(fileId: Int, fileName: String, mimeType: String? = null) {
        viewModelScope.launch {
            val serverUrl = settingsRepository.getServerUrl()
            val token = authRepository.getAccessToken()
            val url = if (token != null) {
                "$serverUrl/api/stream/$fileId?token=$token"
            } else {
                "$serverUrl/api/stream/$fileId"
            }
            fileDownloader.enqueue(fileId, fileName, url, mimeType)
        }
    }

    fun pauseDownload(id: Long) {
        fileDownloader.pause(id)
    }

    fun resumeDownload(id: Long) {
        fileDownloader.resume(id)
    }

    fun cancelDownload(id: Long) {
        fileDownloader.cancel(id)
    }

    fun deleteDownload(id: Long) {
        fileDownloader.deleteFile(id)
    }
}
