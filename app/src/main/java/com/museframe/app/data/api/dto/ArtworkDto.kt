package com.museframe.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtworkDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String? = null,
    @SerialName("creator")
    val creator: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("display_url")
    val displayUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("marketplace_url")
    val marketplaceUrl: String? = null,
    @SerialName("customisation")
    val customisation: CustomisationDto? = null,
    @SerialName("duration")
    val duration: Int? = null,
    @SerialName("volume")
    val volume: Float = 1.0f,
    @SerialName("brightness")
    val brightness: Float = 1.0f
)

@Serializable
data class CustomisationDto(
    @SerialName("title")
    val title: String? = null,
    @SerialName("creator")
    val creator: String? = null,
    @SerialName("url")
    val url: String? = null
)

@Serializable
data class ArtworkDetailResponse(
    @SerialName("data")
    val data: ArtworkDetailDto
)

@Serializable
data class ArtworkDetailDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("creator")
    val creator: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("display_url")
    val displayUrl: String,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("settings")
    val settings: ArtworkSettingsDto? = null
)

@Serializable
data class ArtworkSettingsDto(
    @SerialName("duration")
    val duration: Int? = null,
    @SerialName("volume")
    val volume: Float = 1.0f,
    @SerialName("brightness")
    val brightness: Float = 1.0f
)

@Serializable
data class ArtworksResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val data: List<com.museframe.app.data.api.dto.PlaylistArtworkDto>
)