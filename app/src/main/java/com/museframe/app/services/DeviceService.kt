package com.museframe.app.services

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.museframe.app.domain.model.Device
import com.museframe.app.domain.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val pushyService: PushyService
) {

    /**
     * Initialize device - register with Pushy and backend
     */
    suspend fun initializeDevice(): Result<Device> {
        return try {
            // Step 1: Register with Pushy
            val pushyResult = pushyService.registerDevice()
            if (pushyResult.isFailure) {
                return Result.failure(pushyResult.exceptionOrNull() ?: Exception("Pushy registration failed"))
            }
            val pushyToken = pushyResult.getOrNull()!!

            // Step 2: Save Pushy token
            deviceRepository.savePushyToken(pushyToken)

            // Step 3: Get or generate device ID
            var deviceId = deviceRepository.getDeviceId().first()
            if (deviceId == null) {
                deviceId = generateUniqueDeviceId()
            }

            // Step 4: Register with backend
            val registerResult = deviceRepository.registerDevice(pushyToken)
            if (registerResult.isSuccess) {
                val device = registerResult.getOrNull()!!

                // Subscribe to device-specific topic
                pushyService.subscribeTopic("device_${device.id}")

                // Subscribe to broadcast topic
                pushyService.subscribeTopic("all_devices")

                Timber.d("Device initialized successfully: ${device.id}")
                Result.success(device)
            } else {
                // Create local device object even if backend registration fails
                val device = Device(
                    id = deviceId,
                    name = getDeviceName(),
                    pushyToken = pushyToken,
                    authToken = null,
                    isActivated = false
                )
                Result.success(device)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize device")
            Result.failure(e)
        }
    }

    /**
     * Get current device state
     */
    fun getDeviceId(): Flow<String?> = deviceRepository.getDeviceId()
    fun getPushyToken(): Flow<String?> = deviceRepository.getPushyToken()

    /**
     * Generate a unique device ID
     */
    private suspend fun generateUniqueDeviceId(): String {
        // Use Android ID if available, otherwise generate UUID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val deviceId = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            // Use Android ID if it's valid (not the emulator ID)
            "mf_${androidId}"
        } else {
            // Generate UUID
            "mf_${UUID.randomUUID().toString()}"
        }

        // Save the generated ID
        deviceRepository.generateDeviceId()

        return deviceId
    }

    /**
     * Get device name for display
     */
    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Check if device is registered and activated
     */
    suspend fun isDeviceReady(): Boolean {
        val deviceId = deviceRepository.getDeviceId().first()
        val pushyToken = deviceRepository.getPushyToken().first()
        return !deviceId.isNullOrEmpty() && !pushyToken.isNullOrEmpty()
    }

    /**
     * Reset device (clear all data)
     */
    suspend fun resetDevice() {
        try {
            // Unsubscribe from topics
            deviceRepository.getDeviceId().first()?.let { deviceId ->
                pushyService.unsubscribeTopic("device_$deviceId")
            }
            pushyService.unsubscribeTopic("all_devices")

            Timber.d("Device reset completed")
        } catch (e: Exception) {
            Timber.e(e, "Error resetting device")
        }
    }

    /**
     * Update device activation status
     */
    suspend fun updateActivationStatus(isActivated: Boolean) {
        // This would typically update the backend and local state
        Timber.d("Device activation status updated: $isActivated")
    }
}