package com.museframe.app.data.repository

import com.museframe.app.data.api.MuseFrameApiService
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.model.Artwork
import com.museframe.app.domain.model.MediaType
import com.museframe.app.domain.model.Playlist
import com.museframe.app.domain.model.PlaylistArtwork
import com.museframe.app.domain.model.Adjustment
import com.museframe.app.domain.model.Exhibition
import com.museframe.app.domain.model.ExhibitionItem
import com.museframe.app.domain.repository.PlaylistRepository
import com.museframe.app.domain.exception.HttpException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val apiService: MuseFrameApiService,
    private val preferencesManager: PreferencesManager
) : PlaylistRepository {

    override suspend fun getPlaylists(deviceId: String): Result<List<Playlist>> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getPlaylists(
                token = "Bearer $authToken",
                displayId = deviceId
            )

            if (response.isSuccessful) {
                val playlists = response.body()?.data?.map { dto ->
                    Playlist(
                        id = dto.id.toString(),
                        name = dto.name,
                        description = null, // Backend doesn't return description
                        artworkCount = dto.artworkCount,
                        thumbnailUrl = dto.artworks?.firstOrNull()?.artwork?.thumbnailUrl,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt
                    )
                } ?: emptyList()
                Result.success(playlists)
            } else {
                Result.failure(HttpException(response.code(), "Failed to get playlists"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting playlists")
            Result.failure(e)
        }
    }

    override suspend fun getPlaylistArtworks(playlistId: String): Result<List<Artwork>> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getPlaylistArtworks(
                token = "Bearer $authToken",
                playlistId = playlistId
            )

            if (response.isSuccessful) {
                val artworks = response.body()?.data?.mapNotNull { playlistArtwork ->
                    playlistArtwork.artwork?.let { dto ->
                        Artwork(
                            id = dto.id,
                            title = dto.title ?: "Untitled",
                            creator = dto.creator,
                            description = dto.description,
                            thumbnailUrl = dto.thumbnailUrl,
                            displayUrl = dto.displayUrl ?: "",
                            mediaType = when (dto.mediaType?.lowercase()) {
                                "video" -> MediaType.VIDEO
                                "gif" -> MediaType.GIF
                                else -> MediaType.IMAGE
                            },
                            marketplaceUrl = dto.marketplaceUrl,
                            duration = playlistArtwork.duration?.toInt() ?: dto.duration ?: 30,
                            volume = dto.volume,
                            brightness = dto.brightness
                        )
                    }
                } ?: emptyList()
                Result.success(artworks)
            } else {
                Result.failure(HttpException(response.code(), "Failed to get artworks"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting playlist artworks")
            Result.failure(e)
        }
    }

    override suspend fun getArtworkDetail(playlistId: String, artworkId: String): Result<Artwork> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getArtworkDetail(
                token = "Bearer $authToken",
                playlistId = playlistId,
                artworkId = artworkId
            )

            if (response.isSuccessful) {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No artwork data"))

                val artwork = Artwork(
                    id = dto.id,
                    title = dto.title ?: "Untitled",
                    creator = dto.creator,
                    description = dto.description,
                    thumbnailUrl = dto.thumbnailUrl,
                    displayUrl = dto.displayUrl ?: "",
                    mediaType = when (dto.mediaType?.lowercase()) {
                        "video" -> MediaType.VIDEO
                        "gif" -> MediaType.GIF
                        else -> MediaType.IMAGE
                    },
                    duration = dto.settings?.duration ?: 30,
                    volume = dto.settings?.volume ?: 1.0f,
                    brightness = dto.settings?.brightness ?: 1.0f
                )
                Result.success(artwork)
            } else {
                Result.failure(Exception("Failed to get artwork detail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting artwork detail")
            Result.failure(e)
        }
    }

    override suspend fun refreshPlaylists(deviceId: String) {
        // Force refresh playlists - could implement caching strategy here
        getPlaylists(deviceId)
    }

    override suspend fun refreshPlaylist(playlistId: String) {
        // Force refresh specific playlist - could implement caching strategy here
        getPlaylistArtworks(playlistId)
    }

    override suspend fun getPlaylistArtworksWithSettings(playlistId: String): Result<List<PlaylistArtwork>> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getPlaylistArtworks(
                token = "Bearer $authToken",
                playlistId = playlistId
            )

            if (response.isSuccessful) {
                val playlistArtworks = response.body()?.data?.mapNotNull { playlistArtwork ->
                    playlistArtwork.artwork?.let { dto ->
                        PlaylistArtwork(
                            id = playlistArtwork.id,
                            playlistId = playlistArtwork.playlistId ?: playlistId.toIntOrNull() ?: 0,
                            artwork = Artwork(
                                id = dto.id,
                                title = dto.title ?: "Untitled",
                                creator = dto.creator,
                                description = dto.description,
                                thumbnailUrl = dto.thumbnailUrl,
                                displayUrl = dto.displayUrl ?: "",
                                mediaType = when (dto.mediaType?.lowercase()) {
                                    "video" -> MediaType.VIDEO
                                    "gif" -> MediaType.GIF
                                    else -> MediaType.IMAGE
                                },
                                marketplaceUrl = dto.marketplaceUrl,
                                duration = playlistArtwork.duration?.toInt() ?: dto.duration ?: 30,
                                volume = dto.volume,
                                brightness = dto.brightness
                            ),
                            zoom = playlistArtwork.zoom?.toFloatOrNull() ?: 1.0f,
                            adjustment = Adjustment(
                                x = playlistArtwork.adjustment?.x ?: 0f,
                                y = playlistArtwork.adjustment?.y ?: 0f
                            ),
                            showInfo = playlistArtwork.showInfo ?: false,
                            background = playlistArtwork.background ?: "#000000",
                            borderWidth = playlistArtwork.borderWidth ?: 0f,
                            duration = playlistArtwork.duration?.toInt() ?: 30,
                            orderNumber = playlistArtwork.orderNumber ?: 0
                        )
                    }
                } ?: emptyList()
                Result.success(playlistArtworks)
            } else {
                Result.failure(HttpException(response.code(), "Failed to get artworks"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting playlist artworks with settings")
            Result.failure(e)
        }
    }

    override suspend fun getPlaylistArtworkDetail(playlistId: String, artworkId: String): Result<PlaylistArtwork?> {
        return try {
            val result = getPlaylistArtworksWithSettings(playlistId)
            if (result.isSuccess) {
                val playlistArtwork = result.getOrNull()?.find { it.artwork.id == artworkId }
                Result.success(playlistArtwork)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to get playlist artwork detail"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting playlist artwork detail")
            Result.failure(e)
        }
    }

    override suspend fun getCurrentExhibition(): Result<Exhibition?> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getCurrentExhibition(
                token = "Bearer $authToken"
            )

            when {
                response.isSuccessful && response.body()?.success == true -> {
                    val dto = response.body()?.data
                    if (dto != null) {
                        val exhibition = Exhibition(
                            id = dto.id,
                            description = dto.description,
                            startsAt = dto.startsAt,
                            endsAt = dto.endsAt,
                            items = dto.items.map { item ->
                                ExhibitionItem(
                                    id = item.id,
                                    title = item.title ?: "Untitled",
                                    artist = item.artist,
                                    type = item.type,
                                    thumbnailUrl = item.thumbnailUrl,
                                    displayUrl = item.displayUrl ?: "",
                                    duration = 60 // 60 seconds for exhibition images as requested
                                )
                            }
                        )
                        Result.success(exhibition)
                    } else {
                        Result.success(null)
                    }
                }
                response.code() == 404 -> {
                    // No current exhibition - this is not an error, just no data
                    val message = response.body()?.message ?: "No current exhibition found"
                    Result.failure(Exception(message))
                }
                else -> {
                    val message = response.body()?.message ?: "Failed to get exhibition"
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting current exhibition")
            Result.failure(e)
        }
    }

    override suspend fun getNextPlaylist(deviceId: String, currentPlaylistId: String): Result<String?> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getNextPlaylist(
                token = "Bearer $authToken",
                displayId = deviceId,
                playlistId = currentPlaylistId
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.nextPlaylistId)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get next playlist"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting next playlist")
            Result.failure(e)
        }
    }
}