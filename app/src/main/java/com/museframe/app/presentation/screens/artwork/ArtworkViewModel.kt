package com.museframe.app.presentation.screens.artwork

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.domain.model.Artwork
import com.museframe.app.domain.model.PlaylistArtwork
import com.museframe.app.domain.repository.PlaylistRepository
import com.museframe.app.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArtworkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""
    private val initialArtworkId: String = savedStateHandle.get<String>("artworkId") ?: ""

    private val _uiState = MutableStateFlow(ArtworkUiState())
    val uiState: StateFlow<ArtworkUiState> = _uiState.asStateFlow()

    private var slideshowJob: Job? = null
    private var playlistArtworksList: List<PlaylistArtwork> = emptyList()
    private var currentIndex: Int = 0
    private var nextPlaylistId: String? = null
    private var nextPlaylistArtworks: List<PlaylistArtwork> = emptyList()
    private var deviceId: String? = null

    init {
        loadPlaylistArtworks()
        observeSettings()
        // Start slideshow by default when entering artwork view
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = false)
            deviceId = preferencesManager.deviceId.first()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferencesManager.displaySettings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        slideshowDuration = settings.slideshowDuration,
                        isPaused = settings.isPaused,
                        volume = settings.volume,
                        brightness = settings.brightness
                    )
                }
            }
        }
    }

    fun loadPlaylistArtworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = playlistRepository.getPlaylistArtworksWithSettings(playlistId)
                if (result.isSuccess) {
                    playlistArtworksList = result.getOrNull() ?: emptyList()

                    // Find initial artwork index
                    currentIndex = playlistArtworksList.indexOfFirst { it.artwork.id == initialArtworkId }
                    if (currentIndex == -1) currentIndex = 0

                    if (playlistArtworksList.isNotEmpty()) {
                        loadCurrentArtwork()
                        // Preload next playlist
                        preloadNextPlaylist()
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading artworks")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun loadCurrentArtwork() {
        if (playlistArtworksList.isEmpty()) return

        val currentPlaylistArtwork = playlistArtworksList[currentIndex]
        Timber.d("loadCurrentArtwork - index: $currentIndex, artwork: ${currentPlaylistArtwork.artwork.title}, mediaType: ${currentPlaylistArtwork.artwork.mediaType}, duration: ${currentPlaylistArtwork.duration}")
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                currentPlaylistArtwork = currentPlaylistArtwork,
                currentArtwork = currentPlaylistArtwork.artwork,
                currentIndex = currentIndex,
                totalArtworks = playlistArtworksList.size,
                // Use the duration from playlist artwork for images
                slideshowDuration = if (currentPlaylistArtwork.artwork.mediaType == com.museframe.app.domain.model.MediaType.IMAGE) {
                    currentPlaylistArtwork.duration
                } else {
                    30 // Default for video, will auto-advance when finished
                }
            )
        }
        Timber.d("uiState updated - isPaused: ${_uiState.value.isPaused}")
    }

    fun startSlideshow() {
        // No longer using automatic timer here
        // The screen components handle timing based on media type
        _uiState.update { it.copy(isPaused = false) }
    }

    fun pauseSlideshow() {
        slideshowJob?.cancel()
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = true)
        }
    }

    fun resumeSlideshow() {
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = false)
        }
        _uiState.update { it.copy(isPaused = false) }
    }

    fun handlePauseCommand() {
        // Handle pause command from push notification
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = true)
            _uiState.update { it.copy(isPaused = true) }
        }
    }

    fun handleResumeCommand() {
        // Handle resume command from push notification
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = false)
            _uiState.update { it.copy(isPaused = false) }
        }
    }

    fun nextArtwork() {
        Timber.d("nextArtwork called - currentIndex: $currentIndex, total: ${playlistArtworksList.size}")
        if (playlistArtworksList.isEmpty()) return

        val nextIndex = currentIndex + 1

        // Check if we're at the end of current playlist
        if (nextIndex >= playlistArtworksList.size) {
            // Switch to next playlist if available
            if (nextPlaylistArtworks.isNotEmpty()) {
                Timber.d("Switching to next playlist: $nextPlaylistId")
                playlistArtworksList = nextPlaylistArtworks
                currentIndex = 0
                loadCurrentArtwork()
                // Preload the next playlist after this one
                preloadNextPlaylist()
            } else {
                // Loop back to beginning if no next playlist
                currentIndex = 0
                loadCurrentArtwork()
            }
        } else {
            currentIndex = nextIndex
            loadCurrentArtwork()
        }
    }

    fun previousArtwork() {
        if (playlistArtworksList.isEmpty()) return

        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlistArtworksList.size - 1
        loadCurrentArtwork()
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(volume = volume)
        }
    }

    fun updateBrightness(brightness: Float) {
        viewModelScope.launch {
            preferencesManager.updateDisplaySettings(brightness = brightness)
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    private fun preloadNextPlaylist() {
        viewModelScope.launch {
            try {
                val currentDeviceId = deviceId ?: return@launch

                // Get the next playlist ID
                val nextResult = playlistRepository.getNextPlaylist(currentDeviceId, playlistId)

                if (nextResult.isSuccess) {
                    nextPlaylistId = nextResult.getOrNull()

                    // Preload the artworks for the next playlist
                    nextPlaylistId?.let { nextId ->
                        Timber.d("Preloading next playlist: $nextId")
                        val artworksResult = playlistRepository.getPlaylistArtworksWithSettings(nextId)
                        if (artworksResult.isSuccess) {
                            nextPlaylistArtworks = artworksResult.getOrNull() ?: emptyList()
                            Timber.d("Preloaded ${nextPlaylistArtworks.size} artworks for next playlist")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error preloading next playlist")
                nextPlaylistArtworks = emptyList()
            }
        }
    }

    fun handleNextCommand() {
        // Handle next command from push notification
        if (!_uiState.value.isPaused) {
            nextArtwork()
        }
    }

    fun handlePreviousCommand() {
        // Handle previous command from push notification
        if (!_uiState.value.isPaused) {
            previousArtwork()
        }
    }

    fun handleArtworkSettingsUpdate(playlistId: String, artworkId: String) {
        // Check if this update is for the current artwork
        val currentArtwork = _uiState.value.currentArtwork
        if (currentArtwork?.id == artworkId && this.playlistId == playlistId) {
            // Reload current artwork settings
            viewModelScope.launch {
                try {
                    val result = playlistRepository.getPlaylistArtworkDetail(playlistId, artworkId)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { updatedArtwork ->
                            // Update current artwork and restart duration
                            val index = playlistArtworksList.indexOfFirst { it.artwork.id == artworkId }
                            if (index >= 0) {
                                playlistArtworksList = playlistArtworksList.toMutableList().apply {
                                    this[index] = updatedArtwork
                                }
                                currentIndex = index
                                loadCurrentArtwork()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error updating artwork settings")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        slideshowJob?.cancel()
    }
}

data class ArtworkUiState(
    val isLoading: Boolean = false,
    val currentArtwork: Artwork? = null,
    val currentPlaylistArtwork: PlaylistArtwork? = null,
    val currentIndex: Int = 0,
    val totalArtworks: Int = 0,
    val isPaused: Boolean = false,
    val showControls: Boolean = false,
    val slideshowDuration: Int = 30,
    val volume: Float = 1.0f,
    val brightness: Float = 1.0f,
    val error: String? = null
)