package com.museframe.app.presentation.screens.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.domain.model.Artwork
import com.museframe.app.domain.model.Playlist
import com.museframe.app.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val preferencesManager: com.museframe.app.data.local.PreferencesManager
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        if (playlistId.isNotEmpty()) {
            loadPlaylistAndArtworks()
        }
    }

    private fun loadPlaylistAndArtworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // First load playlists to get the playlist info
                val deviceId = preferencesManager.deviceId.first() ?: ""
                val playlistsResult = playlistRepository.getPlaylists(deviceId)

                val playlist = if (playlistsResult.isSuccess) {
                    playlistsResult.getOrNull()?.find { it.id == playlistId }
                } else null

                // Then load artworks
                val artworksResult = playlistRepository.getPlaylistArtworks(playlistId)
                if (artworksResult.isSuccess) {
                    val artworks = artworksResult.getOrNull() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            artworks = artworks,
                            playlistId = playlistId,
                            playlist = playlist
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = artworksResult.exceptionOrNull()?.message ?: "Failed to load artworks"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading artworks")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadPlaylistAndArtworks()
    }

    fun selectArtwork(artwork: Artwork) {
        _uiState.update { it.copy(selectedArtwork = artwork) }
    }
}

data class PlaylistDetailUiState(
    val isLoading: Boolean = false,
    val playlistId: String = "",
    val playlist: Playlist? = null,
    val artworks: List<Artwork> = emptyList(),
    val selectedArtwork: Artwork? = null,
    val error: String? = null
)