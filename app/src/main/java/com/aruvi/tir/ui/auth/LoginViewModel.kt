package com.aruvi.tir.ui.auth

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruvi.tir.data.repository.AuthRepository
import com.aruvi.tir.data.repository.SettingsRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Login screen state.
 */
data class LoginUiState(
    val loginCode: String? = null,
    val loginUrl: String? = null,
    val qrCodeBitmap: android.graphics.Bitmap? = null,
    val expiresAt: String? = null,
    val isLoading: Boolean = true,
    val isPolling: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val debugLog: String = "",
    val serverUrl: String = "",
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
                error = null,
                qrCodeBitmap = null
            )

            try {
                val result = authRepository.generateLoginCode()
                
                result.fold(
                    onSuccess = { response ->
                        val bot = _uiState.value.botUsername.ifBlank { "Aaruvi_movie_bot" }
                        val url = "https://t.me/$bot?start=${response.code}"
                        val qrBitmap = withContext(Dispatchers.Default) {
                            generateQrCode(url, 600)
                        }
                        _uiState.value = _uiState.value.copy(
                            loginCode = response.code,
                            loginUrl = url,
                            qrCodeBitmap = qrBitmap,
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

    private fun generateQrCode(content: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    }
}

