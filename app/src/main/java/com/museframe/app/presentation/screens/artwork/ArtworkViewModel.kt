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

    private val initialPlaylistId: String = savedStateHandle.get<String>("playlistId") ?: ""
    private val initialArtworkId: String = savedStateHandle.get<String>("artworkId") ?: ""
    private var currentPlaylistId: String = initialPlaylistId
    private val shouldStartFirst: Boolean = initialArtworkId == "first"

    private val _uiState = MutableStateFlow(ArtworkUiState())
    val uiState: StateFlow<ArtworkUiState> = _uiState.asStateFlow()

    private var slideshowJob: Job? = null
    private var playlistArtworksList: List<PlaylistArtwork> = emptyList()
    private var currentIndex: Int = 0
    private var allPlaylists: List<String> = emptyList() // Store all playlist IDs
    private var currentPlaylistIndex: Int = 0 // Track current playlist in the list
    private var nextPlaylistArtworks: List<PlaylistArtwork> = emptyList()
    private var deviceId: String? = null

    fun getCurrentPlaylistId(): String = currentPlaylistId

    init {
        loadPlaylistArtworks()
        observeSettings()
        loadAllPlaylists()
        // Load the current pause state from preferences (don't force it to false)
        viewModelScope.launch {
            deviceId = preferencesManager.deviceId.first()
            // Read current display settings to get the stored pause state
            val currentSettings = preferencesManager.displaySettings.first()
            _uiState.update {
                it.copy(
                    isPaused = currentSettings.isPaused,
                    slideshowDuration = currentSettings.slideshowDuration,
                    volume = currentSettings.volume,
                    brightness = currentSettings.brightness
                )
            }
            Timber.d("ArtworkViewModel initialized with isPaused: ${currentSettings.isPaused}")
        }
    }

    private fun loadAllPlaylists() {
        viewModelScope.launch {
            try {
                val currentDeviceId = deviceId ?: preferencesManager.deviceId.first()
                currentDeviceId?.let { devId ->
                    // Get all playlists for this device
                    val result = playlistRepository.getPlaylists(devId)
                    if (result.isSuccess) {
                        val playlists = result.getOrNull() ?: emptyList()
                        allPlaylists = playlists.map { it.id }
                        // Find current playlist index
                        currentPlaylistIndex = allPlaylists.indexOf(currentPlaylistId)
                        if (currentPlaylistIndex == -1) currentPlaylistIndex = 0

                        Timber.d("Loaded ${allPlaylists.size} playlists. Current playlist index: $currentPlaylistIndex")

                        // Preload next playlist if available
                        preloadNextPlaylistInternal()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading all playlists")
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferencesManager.displaySettings.collect { settings ->
                // Simply update all settings without reloading
                // The pause state will be checked when video/image timer ends
                _uiState.update { state ->
                    state.copy(
                        slideshowDuration = settings.slideshowDuration,
                        isPaused = settings.isPaused,
                        volume = settings.volume,
                        brightness = settings.brightness
                    )
                }

                Timber.d("Settings updated - isPaused: ${settings.isPaused}, volume: ${settings.volume}, brightness: ${settings.brightness}")
            }
        }
    }

    fun loadPlaylistArtworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = playlistRepository.getPlaylistArtworksWithSettings(currentPlaylistId)
                if (result.isSuccess) {
                    playlistArtworksList = result.getOrNull() ?: emptyList()

                    // Find initial artwork index
                    if (shouldStartFirst) {
                        // Cast command wants to start from the first artwork
                        currentIndex = 0
                        Timber.d("Starting slideshow from first artwork (Cast command)")
                    } else {
                        currentIndex = playlistArtworksList.indexOfFirst { it.artwork.id == initialArtworkId }
                        if (currentIndex == -1) currentIndex = 0
                    }

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

        // Update the current playlist ID based on the artwork's actual playlist
        currentPlaylistId = currentPlaylistArtwork.playlistId.toString()

        Timber.d("loadCurrentArtwork - index: $currentIndex, artwork: ${currentPlaylistArtwork.artwork.title}, playlistId: $currentPlaylistId, mediaType: ${currentPlaylistArtwork.artwork.mediaType}, duration: ${currentPlaylistArtwork.duration}")
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
        Timber.d("handlePauseCommand called")
        viewModelScope.launch {
            // Update global pause state in preferences
            preferencesManager.updateDisplaySettings(isPaused = true)

            // Just update the state without reloading
            _uiState.update { state ->
                state.copy(isPaused = true)
            }

            Timber.d("Pause state updated - will be checked when video/image timer ends")
        }
    }

    fun handleResumeCommand() {
        // Handle resume command from push notification
        Timber.d("handleResumeCommand called")
        viewModelScope.launch {
            // Update global pause state in preferences
            preferencesManager.updateDisplaySettings(isPaused = false)

            // Just update the state without reloading
            _uiState.update { state ->
                state.copy(isPaused = false)
            }

            Timber.d("Resume state updated - will be checked when video/image timer ends")
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
                Timber.d("Switching to next playlist")
                playlistArtworksList = nextPlaylistArtworks
                currentIndex = 0
                // Update current playlist index
                currentPlaylistIndex = (currentPlaylistIndex + 1) % allPlaylists.size
                loadCurrentArtwork() // This will update currentPlaylistId from the artwork
                // Preload the next playlist after this one
                preloadNextPlaylistInternal()
            } else {
                // No next playlist loaded, try to load it now
                Timber.d("No next playlist preloaded, attempting to load")
                if (allPlaylists.isNotEmpty()) {
                    // Move to next playlist
                    currentPlaylistIndex = (currentPlaylistIndex + 1) % allPlaylists.size
                    val nextPlaylistId = allPlaylists[currentPlaylistIndex]
                    viewModelScope.launch {
                        val result = playlistRepository.getPlaylistArtworksWithSettings(nextPlaylistId)
                        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                            playlistArtworksList = result.getOrNull()!!
                            currentIndex = 0
                            loadCurrentArtwork()
                            // Preload the next one
                            preloadNextPlaylistInternal()
                        } else {
                            // If loading fails, loop back to beginning of current playlist
                            Timber.e("Failed to load next playlist, looping current")
                            currentIndex = 0
                            loadCurrentArtwork()
                        }
                    }
                } else {
                    // No playlists available, loop back to beginning
                    currentIndex = 0
                    loadCurrentArtwork()
                }
            }
        } else {
            currentIndex = nextIndex
            loadCurrentArtwork()
        }
    }

    fun previousArtwork() {
        if (playlistArtworksList.isEmpty()) return

        // If we're at the first artwork, don't go to the last one - just stay at first
        if (currentIndex > 0) {
            currentIndex -= 1
            loadCurrentArtwork() // This will update currentPlaylistId from the artwork
        } else {
            Timber.d("Already at first artwork, ignoring previous command")
        }
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
        preloadNextPlaylistInternal()
    }

    private fun preloadNextPlaylistInternal() {
        viewModelScope.launch {
            try {
                if (allPlaylists.isEmpty()) {
                    Timber.d("No playlists available to preload")
                    return@launch
                }

                // Calculate next playlist index (cycle through playlists)
                val nextIndex = (currentPlaylistIndex + 1) % allPlaylists.size
                val nextPlaylistId = allPlaylists[nextIndex]

                Timber.d("Preloading next playlist: $nextPlaylistId (index: $nextIndex)")

                // Preload the artworks for the next playlist
                val artworksResult = playlistRepository.getPlaylistArtworksWithSettings(nextPlaylistId)
                if (artworksResult.isSuccess) {
                    nextPlaylistArtworks = artworksResult.getOrNull() ?: emptyList()
                    Timber.d("Preloaded ${nextPlaylistArtworks.size} artworks for playlist $nextPlaylistId")
                } else {
                    Timber.e("Failed to preload playlist $nextPlaylistId")
                    nextPlaylistArtworks = emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error preloading next playlist")
                nextPlaylistArtworks = emptyList()
            }
        }
    }

    fun handleNextCommand() {
        // Handle next command from push notification
        // Allow manual navigation even when paused
        Timber.d("handleNextCommand called, isPaused: ${_uiState.value.isPaused}")
        nextArtwork()
    }

    fun handlePreviousCommand() {
        // Handle previous command from push notification
        // Allow manual navigation even when paused
        Timber.d("handlePreviousCommand called, isPaused: ${_uiState.value.isPaused}")
        previousArtwork()
    }

    fun handleArtworkSettingsUpdate(updatePlaylistId: String, updateArtworkId: String) {
        // Check if this update is for the current artwork
        val currentArtwork = _uiState.value.currentArtwork
        Timber.d("handleArtworkSettingsUpdate - Current: $currentPlaylistId/$currentArtwork?.id, Update: $updatePlaylistId/$updateArtworkId")

        if (currentArtwork?.id == updateArtworkId && currentPlaylistId == updatePlaylistId) {
            Timber.d("Update matches current artwork, reloading settings...")
            // Reload current artwork settings
            viewModelScope.launch {
                try {
                    // Show loading state briefly
                    _uiState.update { it.copy(isLoading = true) }

                    val result = playlistRepository.getPlaylistArtworkDetail(updatePlaylistId, updateArtworkId)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { updatedArtwork ->
                            Timber.d("Successfully fetched updated artwork settings")
                            // Update current artwork in the list
                            val index = playlistArtworksList.indexOfFirst { it.artwork.id == updateArtworkId }
                            if (index >= 0) {
                                playlistArtworksList = playlistArtworksList.toMutableList().apply {
                                    this[index] = updatedArtwork
                                }
                                currentIndex = index

                                // Force UI update by clearing then setting the artwork
                                // This will restart video or reset image timer
                                _uiState.update { state ->
                                    state.copy(
                                        currentPlaylistArtwork = null,
                                        currentArtwork = null
                                    )
                                }

                                // Small delay to ensure UI updates
                                delay(100)

                                // Load the updated artwork
                                loadCurrentArtwork()
                                Timber.d("Artwork settings updated and reloaded")
                            } else {
                                Timber.w("Artwork not found in current playlist")
                                _uiState.update { it.copy(isLoading = false) }
                            }
                        }
                    } else {
                        Timber.e("Failed to fetch updated artwork settings")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error updating artwork settings")
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        } else {
            Timber.d("Update doesn't match current artwork, ignoring")
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