package com.museframe.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExhibitionResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val data: ExhibitionDto? = null,
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)

@Serializable
data class ExhibitionDto(
    @SerialName("id")
    val id: Int,
    @SerialName("description")
    val description: String? = null,
    @SerialName("starts_at")
    val startsAt: String? = null,
    @SerialName("ends_at")
    val endsAt: String? = null,
    @SerialName("items")
    val items: List<ExhibitionItemDto> = emptyList(),
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class ExhibitionItemDto(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String? = null,
    @SerialName("artist")
    val artist: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("display_url")
    val displayUrl: String? = null,
    @SerialName("exhibition_id")
    val exhibitionId: Int,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)