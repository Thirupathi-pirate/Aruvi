package com.telegramtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramtv.data.model.FileItem
import com.telegramtv.data.model.Folder
import com.telegramtv.data.model.TVBrowseResponse
import com.telegramtv.data.repository.FilesRepository
import com.telegramtv.data.repository.FoldersRepository
import com.telegramtv.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home screen UI state.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<FileItem> = emptyList(),
    val recentFiles: List<FileItem> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val serverUrl: String = "",
    val error: String? = null
)

/**
 * ViewModel for the home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val filesRepository: FilesRepository,
    private val foldersRepository: FoldersRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    /**
     * Load all data for the home screen.
     */
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val serverUrl = settingsRepository.getServerUrl()
            _uiState.value = _uiState.value.copy(serverUrl = serverUrl)

            // Try to load TV browse data (combined endpoint)
            val browseResult = filesRepository.getTVBrowse()
            
            browseResult.fold(
                onSuccess = { browse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        continueWatching = browse.continueWatching,
                        recentFiles = browse.recentFiles,
                        folders = browse.folders
                    )
                },
                onFailure = { _ ->
                    // Fallback to individual calls if TV browse fails
                    loadDataFallback()
                }
            )
        }
    }

    /**
     * Fallback when TV browse endpoint is not available.
     */
    private suspend fun loadDataFallback() {
        // Load continue watching
        val continueResult = filesRepository.getContinueWatching()
        val continueWatching = continueResult.getOrDefault(emptyList())

        // Load recent files
        val recentResult = filesRepository.getRecentFiles(20)
        val recentFiles = recentResult.getOrDefault(emptyList())

        // Load folders
        val foldersResult = foldersRepository.getFolders()
        val folders = foldersResult.getOrDefault(emptyList())

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            continueWatching = continueWatching,
            recentFiles = recentFiles,
            folders = folders,
            error = if (recentFiles.isEmpty() && folders.isEmpty()) {
                "Failed to load content"
            } else null
        )
    }

    /**
     * Refresh home data.
     */
    fun refresh() {
        loadHomeData()
    }
}
