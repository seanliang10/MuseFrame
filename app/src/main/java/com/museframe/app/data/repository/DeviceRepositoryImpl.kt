package com.museframe.app.data.repository

import com.museframe.app.BuildConfig
import com.museframe.app.data.api.ApiConstants
import com.museframe.app.data.api.MuseFrameApiService
import com.museframe.app.data.api.dto.DeviceRegisterRequest
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.model.Device
import com.museframe.app.domain.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val apiService: MuseFrameApiService,
    private val preferencesManager: PreferencesManager
) : DeviceRepository {

    override fun getDeviceId(): Flow<String?> {
        return preferencesManager.deviceId
    }

    override fun getPushyToken(): Flow<String?> {
        return preferencesManager.pushyToken
    }

    override suspend fun registerDevice(pushyToken: String): Result<Device> {
        return try {
            // First get or generate device ID
            var deviceId = preferencesManager.deviceId.first()
            if (deviceId == null) {
                deviceId = generateDeviceId()
            }

            val response = apiService.registerDevice(
                request = DeviceRegisterRequest(
                    deviceId = pushyToken, // Using pushy token as device ID
                    token = ApiConstants.FRAME_TOKEN,
                    currentVersion = BuildConfig.VERSION_NAME
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val responseBody = response.body()!!
                val displayId = responseBody.displayId ?: pushyToken

                val device = Device(
                    id = displayId,
                    name = "Muse Frame Display",
                    pushyToken = pushyToken,
                    authToken = null,
                    isActivated = false
                )

                // Save device info
                preferencesManager.saveDeviceId(displayId)
                preferencesManager.savePushyToken(pushyToken)

                Result.success(device)
            } else {
                val errorMessage = response.body()?.message ?: "Failed to register device: ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error registering device")
            Result.failure(e)
        }
    }

    override suspend fun generateDeviceId(): String {
        val deviceId = UUID.randomUUID().toString()
        preferencesManager.saveDeviceId(deviceId)
        return deviceId
    }

    override suspend fun savePushyToken(token: String) {
        preferencesManager.savePushyToken(token)
    }

    override suspend fun unpairDevice(callApi: Boolean): Result<Unit> {
        return try {
            // Only call the API if requested (i.e., when we have a valid token)
            if (callApi) {
                val authToken = preferencesManager.authToken.first()
                val deviceId = preferencesManager.deviceId.first()

                if (!authToken.isNullOrEmpty() && !deviceId.isNullOrEmpty()) {
                    try {
                        val response = apiService.unpairDisplay(
                            token = "Bearer $authToken",
                            displayId = deviceId
                        )

                        if (!response.isSuccessful) {
                            Timber.w("Failed to unpair device from server: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        // Log but don't fail - we still want to clear local data
                        Timber.e(e, "Error calling unpair API, will clear local data anyway")
                    }
                }
            }

            // Clear auth data but preserve display settings
            preferencesManager.clearAuthData()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error during unpair process")
            // Even if there's an error, try to clear auth data
            try {
                preferencesManager.clearAuthData()
            } catch (clearError: Exception) {
                Timber.e(clearError, "Error clearing auth data")
            }
            Result.failure(e)
        }
    }

    override suspend fun fetchAndUpdateDisplayDetails(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authToken = preferencesManager.authToken.first()
            val deviceId = preferencesManager.deviceId.first()

            if (authToken.isNullOrEmpty() || deviceId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Missing auth token or device ID"))
            }

            val response = apiService.getDisplayDetails(
                token = "Bearer $authToken",
                displayId = deviceId
            )

            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.display?.let { display ->
                    // Update only name and pause status from API
                    // Don't update volume/brightness as user may have different system settings
                    // API is source of truth - always update pause state even if null
                    val pauseState = display.isPaused ?: false // Default to false if null
                    preferencesManager.updateDisplaySettings(
                        name = display.name,
                        isPaused = pauseState
                    )
                    Timber.d("API SYNC - Updated display settings: name=${display.name}, isPaused=$pauseState (API returned: ${display.isPaused})")
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch display details"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching display details")
            Result.failure(e)
        }
    }
}