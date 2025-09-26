package com.museframe.app.services

import android.content.Context
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _authState = MutableStateFlow(AuthState.CHECKING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    init {
        observeAuthToken()
    }

    private fun observeAuthToken() {
        serviceScope.launch {
            preferencesManager.authToken.collect { token ->
                _authToken.value = token
                updateAuthState(token)
            }
        }
    }

    private fun updateAuthState(token: String?) {
        _authState.value = when {
            token.isNullOrEmpty() -> AuthState.UNAUTHENTICATED
            else -> AuthState.AUTHENTICATED
        }
    }

    /**
     * Handle authentication from push notification
     */
    fun handleAuthTokenReceived(token: String, deviceId: String? = null) {
        serviceScope.launch {
            try {
                // Save token immediately to ensure it's available for API calls
                authRepository.saveAuthToken(token)
                _authToken.value = token

                // Save device ID if provided
                deviceId?.let {
                    preferencesManager.saveDeviceId(it)
                    Timber.d("Device ID saved: $it")
                }

                _authState.value = AuthState.AUTHENTICATED
                Timber.d("Authentication successful, token saved: ${token.take(10)}...")

                // Optionally verify token in background (non-blocking)
                launch {
                    try {
                        val result = authRepository.verifyToken(token)
                        if (result.isFailure || result.getOrNull() != true) {
                            Timber.w("Token verification failed, but keeping token for now")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error verifying token, but keeping token for now")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling auth token")
                _authState.value = AuthState.ERROR
            }
        }
    }

    /**
     * Handle logout/disconnection
     */
    fun handleLogout() {
        serviceScope.launch {
            authRepository.logout()
            _authState.value = AuthState.UNAUTHENTICATED
            Timber.d("User logged out")
        }
    }

    /**
     * Check current authentication status
     */
    suspend fun checkAuthStatus(): AuthState {
        return try {
            val isAuthenticated = authRepository.isAuthenticated().first()
            if (isAuthenticated) {
                // Verify token is still valid
                val token = _authToken.value
                if (!token.isNullOrEmpty()) {
                    val result = authRepository.verifyToken(token)
                    if (result.isSuccess && result.getOrNull() == true) {
                        AuthState.AUTHENTICATED
                    } else {
                        // Token expired or invalid
                        authRepository.clearAuthToken()
                        AuthState.UNAUTHENTICATED
                    }
                } else {
                    AuthState.UNAUTHENTICATED
                }
            } else {
                AuthState.UNAUTHENTICATED
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking auth status")
            AuthState.ERROR
        }
    }

    /**
     * Sync auth token from SharedPreferences (for BroadcastReceiver compatibility)
     */
    fun syncAuthTokenFromSharedPrefs() {
        serviceScope.launch {
            try {
                val sharedPrefs = context.getSharedPreferences("museframe_prefs", Context.MODE_PRIVATE)
                val token = sharedPrefs.getString("auth_token", null)
                val deviceId = sharedPrefs.getString("device_id", null)

                if (!token.isNullOrEmpty()) {
                    // Save to DataStore
                    preferencesManager.saveAuthToken(token)
                    _authToken.value = token
                    _authState.value = AuthState.AUTHENTICATED

                    // Clear from SharedPreferences
                    sharedPrefs.edit().remove("auth_token").apply()

                    Timber.d("Auth token synced from SharedPreferences: ${token.take(10)}...")
                }

                if (!deviceId.isNullOrEmpty()) {
                    // Save device ID to DataStore
                    preferencesManager.saveDeviceId(deviceId)

                    // Clear from SharedPreferences
                    sharedPrefs.edit().remove("device_id").apply()

                    Timber.d("Device ID synced from SharedPreferences: $deviceId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing auth token")
            }
        }
    }

    enum class AuthState {
        CHECKING,
        AUTHENTICATED,
        UNAUTHENTICATED,
        ERROR
    }
}