package com.museframe.app.domain.model

data class DisplaySettings(
    val name: String,
    val volume: Float = 1.0f,
    val brightness: Float = 1.0f,
    val duration: Int = 30,
    val isPaused: Boolean = false,
    val hasUpdate: Boolean = false
)