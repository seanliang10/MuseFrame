package com.museframe.app.domain.model

data class Exhibition(
    val id: Int,
    val description: String?,
    val startsAt: String?,
    val endsAt: String?,
    val items: List<ExhibitionItem>
)

data class ExhibitionItem(
    val id: Int,
    val title: String,
    val artist: String?,
    val type: String?,
    val thumbnailUrl: String?,
    val displayUrl: String,
    val duration: Int = 30 // Default duration for exhibition items
)
