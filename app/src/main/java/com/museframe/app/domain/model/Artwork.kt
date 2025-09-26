package com.museframe.app.domain.model

data class Artwork(
    val id: String,
    val title: String,
    val creator: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val displayUrl: String,
    val mediaType: MediaType,
    val marketplaceUrl: String? = null,
    val duration: Int? = null,
    val volume: Float = 1.0f,
    val brightness: Float = 1.0f
)

enum class MediaType {
    IMAGE,
    VIDEO,
    GIF
}