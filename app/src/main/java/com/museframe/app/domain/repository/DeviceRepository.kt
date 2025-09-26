package com.museframe.app.domain.repository

import com.museframe.app.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDeviceId(): Flow<String?>
    fun getPushyToken(): Flow<String?>
    suspend fun registerDevice(pushyToken: String): Result<Device>
    suspend fun generateDeviceId(): String
    suspend fun savePushyToken(token: String)
    suspend fun unpairDevice(callApi: Boolean = true): Result<Unit>
    suspend fun fetchAndUpdateDisplayDetails(): Result<Unit>
}