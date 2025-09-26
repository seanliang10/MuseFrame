package com.museframe.app.presentation.screens.playlists

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.model.Playlist
import com.museframe.app.domain.repository.DeviceRepository
import com.museframe.app.domain.repository.PlaylistRepository
import com.museframe.app.presentation.base.NetworkAwareViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    application: Application,
    private val playlistRepository: PlaylistRepository,
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager
) : NetworkAwareViewModel(application) {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        fetchDisplayDetails()
        loadPlaylists()
        observeFrameName()
    }

    fun loadPlaylists() {
        checkNetworkAndExecute(
            onNetworkAvailable = {
                _uiState.update { it.copy(isLoading = true, error = null) }

                try {
                    // Get device ID
                    val deviceId = deviceRepository.getDeviceId().first()
                    if (deviceId == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Device not registered"
                            )
                        }
                        return@checkNetworkAndExecute
                    }

                    // Load playlists
                    val result = playlistRepository.getPlaylists(deviceId)
                    if (result.isSuccess) {
                        val playlists = result.getOrNull() ?: emptyList()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                playlists = playlists,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to load playlists"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading playlists")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error occurred"
                        )
                    }
                }
            }
        )
    }

    fun refresh() {
        loadPlaylists()
    }

    fun selectPlaylist(playlist: Playlist) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
    }

    private fun fetchDisplayDetails() {
        viewModelScope.launch {
            // Fetch latest display details from API on startup
            val result = deviceRepository.fetchAndUpdateDisplayDetails()
            if (result.isSuccess) {
                Timber.d("Successfully fetched display details from API")
            } else {
                Timber.w("Failed to fetch display details: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun observeFrameName() {
        viewModelScope.launch {
            // Observe the display settings which includes the persisted frame name
            preferencesManager.displaySettings.collect { settings ->
                Timber.d("Frame name from preferences: ${settings.name}")
                _uiState.update { it.copy(frameName = settings.name) }
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Call API since we should have a valid token when user manually logs out
            val result = deviceRepository.unpairDevice(callApi = true)

            if (result.isSuccess) {
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to logout"
                    )
                }
            }
        }
    }
}

data class PlaylistsUiState(
    val isLoading: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val error: String? = null,
    val frameName: String? = null
)