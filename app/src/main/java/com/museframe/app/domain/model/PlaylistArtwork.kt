package com.museframe.app.domain.model

data class PlaylistArtwork(
    val id: Int,
    val playlistId: Int,
    val artwork: Artwork,
    val zoom: Float = 1.0f,
    val adjustment: Adjustment = Adjustment(0f, 0f),
    val showInfo: Boolean = false,
    val background: String = "#000000",
    val borderWidth: Float = 0f,
    val duration: Int = 30,
    val orderNumber: Int = 0
)

data class Adjustment(
    val x: Float = 0f,
    val y: Float = 0f
)