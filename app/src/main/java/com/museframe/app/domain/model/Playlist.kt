package com.museframe.app.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val artworkCount: Int,
    val thumbnailUrl: String?,
    val createdAt: String? = null,
    val updatedAt: String? = null
)