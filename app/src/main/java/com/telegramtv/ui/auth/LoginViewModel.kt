package com.telegramtv.ui.auth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramtv.data.model.AuthResponse
import com.telegramtv.data.model.LoginCodeResponse
import com.telegramtv.data.repository.AuthRepository
import com.telegramtv.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login screen state.
 */
data class LoginUiState(
    val loginCode: String? = null,
    val expiresAt: String? = null,
    val isLoading: Boolean = true,
    val isPolling: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val debugLog: String = "",
    val serverUrl: String = "https://lavender7736-teleplay-backend.hf.space",
    val botUsername: String = "",
    val manualCode: String = ""
)

/**
 * ViewModel for the login screen.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        loadServerUrl()
    }

    /**
     * Load saved server URL.
     */
    private fun loadServerUrl() {
        viewModelScope.launch {
            val url = settingsRepository.serverUrl.first()
            val bot = settingsRepository.botUsername.first()
            _uiState.value = _uiState.value.copy(serverUrl = url, botUsername = bot)
            fetchBotInfo()
            generateLoginCode()
        }
    }

    /**
     * Fetch bot info from the backend.
     */
    fun fetchBotInfo() {
        viewModelScope.launch {
            authRepository.getBotInfo().onSuccess { botInfo ->
                _uiState.value = _uiState.value.copy(botUsername = botInfo.username)
                settingsRepository.setBotUsername(botInfo.username)
            }
        }
    }

    /**
     * Generate a new login code.
     */
    fun generateLoginCode() {
        // Stop any existing polling for an old code
        stopPolling()

        viewModelScope.launch {
            // Save server URL before generating code
            settingsRepository.setServerUrl(_uiState.value.serverUrl)

            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null
            )

            try {
                val result = authRepository.generateLoginCode()
                
                result.fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            loginCode = response.code,
                            expiresAt = response.expiresAt,
                            isLoading = false
                        )
                        startPolling(response.code)
                    },
                    onFailure = { e ->
                        e.printStackTrace()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Crash: ${e.message}"
                )
            }
        }
    }

    /**
     * Start polling for login confirmation.
     */
    private fun startPolling(code: String) {
        stopPolling()

        pollingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPolling = true)

            // Poll every 2 seconds for up to 5 minutes
            repeat(150) {
                val result = authRepository.verifyLoginCode(code)
                result.fold(
                    onSuccess = { _ ->
                        _uiState.value = _uiState.value.copy(
                            isPolling = false,
                            isLoggedIn = true
                        )
                        return@launch
                    },
                    onFailure = { e ->
                        // Check if code expired
                        if (e.message?.contains("expired") == true) {
                            _uiState.value = _uiState.value.copy(
                                isPolling = false,
                                error = "Code expired. Please generate a new one."
                            )
                            return@launch
                        }
                        // Otherwise continue polling
                    }
                )

                delay(2000)
            }

            // Timeout after 5 minutes
            _uiState.value = _uiState.value.copy(
                isPolling = false,
                error = "Login timeout. Please try again."
            )
        }
    }

    fun updateManualCode(code: String) {
        _uiState.value = _uiState.value.copy(manualCode = code)
    }

    fun verifyManualCode() {
        val code = _uiState.value.manualCode.trim()
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a code")
            return
        }

        stopPolling()
        _uiState.value = _uiState.value.copy(
            loginCode = code,
            manualCode = "",
            error = null
        )
        startPolling(code)
    }

    /**
     * Stop polling.
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.value = _uiState.value.copy(isPolling = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

