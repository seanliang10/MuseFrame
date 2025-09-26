package com.museframe.app.presentation.screens.welcome

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.data.api.ApiConstants
import com.museframe.app.domain.repository.AuthRepository
import com.museframe.app.domain.repository.DeviceRepository
import com.museframe.app.presentation.utils.QRCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.pushy.sdk.Pushy
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        initializeDevice()
        observeAuthToken()
    }

    private fun initializeDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Register with Pushy in background thread
                val pushyToken = withContext(Dispatchers.IO) {
                    Pushy.register(context)
                }

                // Save Pushy token
                deviceRepository.savePushyToken(pushyToken)

                // Get or generate device ID
                var deviceId = deviceRepository.getDeviceId().first()
                if (deviceId == null) {
                    deviceId = deviceRepository.generateDeviceId()
                }

                // Register device with backend
                val result = deviceRepository.registerDevice(pushyToken)
                if (result.isSuccess) {
                    val device = result.getOrNull()
                    device?.let {
                        deviceId = it.id
                    }
                }

                // Generate QR codes for mobile app download
                deviceId?.let { generateQRCodes(it) }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        deviceId = deviceId,
                        pushyToken = pushyToken
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "Error initializing device")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to initialize device: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun generateQRCodes(deviceId: String) {
        withContext(Dispatchers.IO) {
            try {
                // iOS App Store URL
                val iosUrl = "https://apps.apple.com/app/muse-frame/id1234567890" // Replace with actual
                val iosQR = QRCodeGenerator.generateQRCode(iosUrl, 300, 300)

                // Android Play Store URL
                val androidUrl = "https://play.google.com/store/apps/details?id=com.museframe.mobile" // Replace with actual
                val androidQR = QRCodeGenerator.generateQRCode(androidUrl, 300, 300)

                // Device pairing QR (contains device ID for pairing)
                val pairingData = """
                    {
                        "deviceId": "$deviceId",
                        "pairingUrl": "${ApiConstants.BASE_URL}pair/$deviceId"
                    }
                """.trimIndent()
                val pairingQR = QRCodeGenerator.generateQRCode(pairingData, 400, 400)

                _uiState.update { state ->
                    state.copy(
                        iosQRCode = iosQR,
                        androidQRCode = androidQR,
                        pairingQRCode = pairingQR
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error generating QR codes")
            }
        }
    }

    private fun observeAuthToken() {
        viewModelScope.launch {
            authRepository.isAuthenticated()
                .collect { isAuthenticated ->
                    if (isAuthenticated) {
                        _uiState.update { it.copy(isPaired = true) }
                    }
                }
        }
    }

    fun retryInitialization() {
        initializeDevice()
    }
}

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val deviceId: String? = null,
    val pushyToken: String? = null,
    val iosQRCode: Bitmap? = null,
    val androidQRCode: Bitmap? = null,
    val pairingQRCode: Bitmap? = null,
    val isPaired: Boolean = false,
    val error: String? = null
)