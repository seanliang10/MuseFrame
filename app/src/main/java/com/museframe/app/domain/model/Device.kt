package com.museframe.app.domain.model

data class Device(
    val id: String,
    val name: String,
    val pushyToken: String?,
    val authToken: String?,
    val isActivated: Boolean = false
)