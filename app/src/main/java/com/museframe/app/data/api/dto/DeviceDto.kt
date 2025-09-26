package com.museframe.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("token")
    val token: String,
    @SerialName("current_version")
    val currentVersion: String = "3.0.0"
)

@Serializable
data class DeviceRegisterResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("message")
    val message: String,
    @SerialName("display_id")
    val displayId: String? = null
)

@Serializable
data class TokenVerifyRequest(
    @SerialName("token")
    val token: String
)

@Serializable
data class TokenVerifyResponse(
    @SerialName("success")
    val valid: Boolean,
    @SerialName("message")
    val message: String? = null,
    @SerialName("user")
    val user: UserDto? = null
)

@Serializable
data class UserDto(
    @SerialName("id")
    val id: Int,
    @SerialName("username")
    val username: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("is_admin")
    val isAdmin: Boolean? = null,
    @SerialName("email_verified_at")
    val emailVerifiedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)