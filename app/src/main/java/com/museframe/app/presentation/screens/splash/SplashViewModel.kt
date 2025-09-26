package com.museframe.app.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.repository.AuthRepository
import com.museframe.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Add a minimum splash delay for branding
            delay(1500)

            try {
                // Get the actual auth token from preferences
                val token = preferencesManager.authToken.first()

                if (!token.isNullOrEmpty()) {
                    Timber.d("Found auth token, verifying: ${token.take(10)}...")
                    // Verify token is still valid with backend
                    val result = authRepository.verifyToken(token)
                    if (result.isSuccess && result.getOrNull() == true) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                shouldNavigate = true
                            )
                        }
                    } else {
                        // Token invalid, clear local data without calling API
                        Timber.w("Token verification failed, clearing local data")
                        try {
                            // Don't call API since token is invalid
                            deviceRepository.unpairDevice(callApi = false)
                        } catch (e: Exception) {
                            Timber.e(e, "Error clearing data during token validation")
                        }
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isAuthenticated = false,
                                shouldNavigate = true
                            )
                        }
                    }
                } else {
                    Timber.d("No auth token found")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            shouldNavigate = true
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking authentication")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        shouldNavigate = true,
                        error = e.message
                    )
                }
            }
        }
    }

    fun onNavigationComplete() {
        _uiState.update { it.copy(shouldNavigate = false) }
    }
}

data class SplashUiState(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = false,
    val shouldNavigate: Boolean = false,
    val error: String? = null
)