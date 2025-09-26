package com.museframe.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    @SerialName("id")
    val id: Int,
    @SerialName("user_id")
    val userId: Int? = null,
    @SerialName("name")
    val name: String,
    @SerialName("artwork_count")
    val artworkCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("artworks")
    val artworks: List<PlaylistArtworkDto>? = emptyList()
)

@Serializable
data class PlaylistArtworkDto(
    @SerialName("id")
    val id: Int,
    @SerialName("playlist_id")
    val playlistId: Int? = null,
    @SerialName("artwork_id")
    val artworkId: String? = null,
    @SerialName("order_number")
    val orderNumber: Int? = null,
    @SerialName("display_duration")
    val displayDuration: String? = null,
    @SerialName("zoom")
    val zoom: String? = null,
    @SerialName("adjustment")
    val adjustment: AdjustmentDto? = null,
    @SerialName("show_info")
    val showInfo: Boolean? = null,
    @SerialName("background")
    val background: String? = null,
    @SerialName("duration")
    val duration: Float? = null,
    @SerialName("border_width")
    val borderWidth: Float? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("artwork")
    val artwork: ArtworkDto? = null
)

@Serializable
data class AdjustmentDto(
    @SerialName("x")
    val x: Float? = 0f,
    @SerialName("y")
    val y: Float? = 0f
)

@Serializable
data class PlaylistsResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("playlists")
    val data: List<PlaylistDto> = emptyList()
)