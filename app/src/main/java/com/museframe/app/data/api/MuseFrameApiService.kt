package com.museframe.app.data.api

import com.museframe.app.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MuseFrameApiService {

    @POST("display/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): Response<DeviceRegisterResponse>

    @POST("auth/token/verify")
    suspend fun verifyToken(
        @Header("Authorization") token: String,
        @Body request: TokenVerifyRequest
    ): Response<TokenVerifyResponse>

    @GET("display/{displayId}/playlists")
    suspend fun getPlaylists(
        @Header("Authorization") token: String,
        @Path("displayId") displayId: String
    ): Response<PlaylistsResponse>

    @GET("playlists/{playlistId}/artworks")
    suspend fun getPlaylistArtworks(
        @Header("Authorization") token: String,
        @Path("playlistId") playlistId: String
    ): Response<ArtworksResponse>

    @GET("playlists/{playlistId}/artwork/{artworkId}")
    suspend fun getArtworkDetail(
        @Header("Authorization") token: String,
        @Path("playlistId") playlistId: String,
        @Path("artworkId") artworkId: String
    ): Response<ArtworkDetailResponse>

    @GET("exhibitions/current")
    suspend fun getCurrentExhibition(
        @Header("Authorization") token: String
    ): Response<com.museframe.app.data.api.dto.ExhibitionResponse>

    @GET("{display}/playlist/{playlist}/next")
    suspend fun getNextPlaylist(
        @Header("Authorization") token: String,
        @Path("display") displayId: String,
        @Path("playlist") playlistId: String
    ): Response<NextPlaylistResponse>

    @GET("versions/latest")
    suspend fun getLatestVersion(
        @Header("Authorization") token: String
    ): Response<VersionResponse>

    @GET("versions/download/{displayId}")
    suspend fun downloadVersion(
        @Header("Authorization") token: String,
        @Path("displayId") displayId: String
    ): Response<VersionDownloadResponse>

    @POST("versions/report/{displayId}")
    suspend fun reportVersion(
        @Header("Authorization") token: String,
        @Path("displayId") displayId: String,
        @Body request: VersionReportRequest
    ): Response<Unit>

    @DELETE("display/{displayId}")
    suspend fun unpairDisplay(
        @Header("Authorization") token: String,
        @Path("displayId") displayId: String
    ): Response<Unit>
}

@kotlinx.serialization.Serializable
data class NextPlaylistResponse(
    @kotlinx.serialization.SerialName("success")
    val success: Boolean,
    @kotlinx.serialization.SerialName("next_playlist_id")
    val nextPlaylistId: String? = null,
    @kotlinx.serialization.SerialName("message")
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class VersionResponse(
    @kotlinx.serialization.SerialName("success")
    val success: Boolean,
    @kotlinx.serialization.SerialName("version")
    val version: VersionInfo? = null,
    @kotlinx.serialization.SerialName("message")
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class VersionDownloadResponse(
    @kotlinx.serialization.SerialName("success")
    val success: Boolean,
    @kotlinx.serialization.SerialName("url")
    val url: String? = null,
    @kotlinx.serialization.SerialName("version")
    val version: String? = null,
    @kotlinx.serialization.SerialName("message")
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class VersionInfo(
    @kotlinx.serialization.SerialName("id")
    val id: Int,
    @kotlinx.serialization.SerialName("version")
    val version: String,
    @kotlinx.serialization.SerialName("stored_at")
    val storedAt: String? = null,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String? = null,
    @kotlinx.serialization.SerialName("updated_at")
    val updatedAt: String? = null
)

@kotlinx.serialization.Serializable
data class VersionReportRequest(
    @kotlinx.serialization.SerialName("version")
    val version: String
)