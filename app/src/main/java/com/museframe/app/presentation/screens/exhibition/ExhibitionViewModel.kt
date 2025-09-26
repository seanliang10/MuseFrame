package com.museframe.app.presentation.screens.exhibition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.domain.model.Artwork
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
class ExhibitionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val exhibitionId: String = savedStateHandle.get<String>("exhibitionId") ?: ""

    private val _uiState = MutableStateFlow(ExhibitionUiState())
    val uiState: StateFlow<ExhibitionUiState> = _uiState.asStateFlow()

    private var slideshowJob: Job? = null
    private var artworksList: List<Artwork> = emptyList()
    private var currentIndex: Int = 0

    init {
        loadExhibitionArtworks()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferencesManager.displaySettings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        slideshowDuration = settings.slideshowDuration,
                        isPaused = false, // Exhibition mode always starts playing
                        volume = settings.volume,
                        brightness = settings.brightness
                    )
                }

                // Start slideshow immediately in exhibition mode
                if (slideshowJob?.isActive != true) {
                    startSlideshow()
                }
            }
        }
    }

    private fun loadExhibitionArtworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get device ID from preferences
                val deviceId = preferencesManager.deviceId.firstOrNull() ?: ""
                if (deviceId.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Device not configured"
                        )
                    }
                    return@launch
                }

                // For now, load all playlists and combine their artworks
                // In a real implementation, you would have a separate exhibition endpoint
                val result = playlistRepository.getPlaylists(deviceId)
                if (result.isSuccess) {
                    val playlists = result.getOrNull() ?: emptyList()

                    // Load artworks from all playlists
                    val allArtworks = mutableListOf<Artwork>()
                    playlists.forEach { playlist ->
                        val artworksResult = playlistRepository.getPlaylistArtworks(playlist.id)
                        if (artworksResult.isSuccess) {
                            artworksResult.getOrNull()?.let { artworks ->
                                allArtworks.addAll(artworks)
                            }
                        }
                    }

                    artworksList = allArtworks.shuffled() // Shuffle for variety
                    currentIndex = 0

                    if (artworksList.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                exhibitionTitle = "Mixed Exhibition",
                                totalArtworks = artworksList.size
                            )
                        }
                        loadArtworkDetail(artworksList[currentIndex])
                        startSlideshow()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No artworks found for exhibition"
                            )
                        }
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
                Timber.e(e, "Error loading exhibition")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private suspend fun loadArtworkDetail(artwork: Artwork) {
        try {
            // Try to get detailed artwork info
            // Since we don't have the playlist ID in exhibition mode,
            // we'll use the basic artwork data
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    currentArtwork = artwork,
                    currentIndex = currentIndex,
                    totalArtworks = artworksList.size
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading artwork detail")
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    currentArtwork = artwork,
                    currentIndex = currentIndex,
                    totalArtworks = artworksList.size
                )
            }
        }
    }

    fun startSlideshow() {
        slideshowJob?.cancel()
        slideshowJob = viewModelScope.launch {
            while (true) {
                // Use longer duration for exhibition mode
                val duration = (_uiState.value.slideshowDuration * 1.5 * 1000).toLong()
                delay(duration)
                if (!_uiState.value.isPaused) {
                    nextArtwork()
                }
            }
        }
    }

    fun pauseSlideshow() {
        slideshowJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeSlideshow() {
        _uiState.update { it.copy(isPaused = false) }
        startSlideshow()
    }

    fun nextArtwork() {
        if (artworksList.isEmpty()) return

        currentIndex = (currentIndex + 1) % artworksList.size
        viewModelScope.launch {
            loadArtworkDetail(artworksList[currentIndex])
        }
    }

    fun previousArtwork() {
        if (artworksList.isEmpty()) return

        currentIndex = if (currentIndex > 0) currentIndex - 1 else artworksList.size - 1
        viewModelScope.launch {
            loadArtworkDetail(artworksList[currentIndex])
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    override fun onCleared() {
        super.onCleared()
        slideshowJob?.cancel()
    }
}

data class ExhibitionUiState(
    val isLoading: Boolean = false,
    val currentArtwork: Artwork? = null,
    val exhibitionTitle: String? = null,
    val currentIndex: Int = 0,
    val totalArtworks: Int = 0,
    val isPaused: Boolean = false,
    val showControls: Boolean = false,
    val slideshowDuration: Int = 30,
    val volume: Float = 1.0f,
    val brightness: Float = 1.0f,
    val error: String? = null
)