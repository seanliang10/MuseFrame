package com.museframe.app.domain.repository

import com.museframe.app.domain.model.Artwork
import com.museframe.app.domain.model.Exhibition
import com.museframe.app.domain.model.Playlist
import com.museframe.app.domain.model.PlaylistArtwork
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun getPlaylists(deviceId: String): Result<List<Playlist>>
    suspend fun getPlaylistArtworks(playlistId: String): Result<List<Artwork>>
    suspend fun getPlaylistArtworksWithSettings(playlistId: String): Result<List<PlaylistArtwork>>
    suspend fun getArtworkDetail(playlistId: String, artworkId: String): Result<Artwork>
    suspend fun getPlaylistArtworkDetail(playlistId: String, artworkId: String): Result<PlaylistArtwork?>
    suspend fun getCurrentExhibition(): Result<Exhibition?>
    suspend fun getNextPlaylist(deviceId: String, currentPlaylistId: String): Result<String?>
    suspend fun refreshPlaylists(deviceId: String)
    suspend fun refreshPlaylist(playlistId: String)
}