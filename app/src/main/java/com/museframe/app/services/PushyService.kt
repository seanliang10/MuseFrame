package com.museframe.app.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.pushy.sdk.Pushy
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushyService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Register device with Pushy and get token
     * Must be called from a background thread
     */
    suspend fun registerDevice(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Register with Pushy (blocking call)
                val deviceToken = Pushy.register(context)

                // Start listening for notifications
                Pushy.listen(context)

                Timber.d("Pushy registration successful: $deviceToken")
                Result.success(deviceToken)
            } catch (e: Exception) {
                Timber.e(e, "Failed to register with Pushy")
                Result.failure(e)
            }
        }
    }

    /**
     * Check if Pushy is registered
     */
    fun isRegistered(): Boolean {
        return try {
            Pushy.isRegistered(context)
        } catch (e: Exception) {
            Timber.e(e, "Error checking Pushy registration")
            false
        }
    }

    /**
     * Subscribe to a topic for grouped notifications
     */
    suspend fun subscribeTopic(topic: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Pushy.subscribe(topic, context)
                Timber.d("Subscribed to topic: $topic")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to subscribe to topic: $topic")
                Result.failure(e)
            }
        }
    }

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribeTopic(topic: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Pushy.unsubscribe(topic, context)
                Timber.d("Unsubscribed from topic: $topic")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unsubscribe from topic: $topic")
                Result.failure(e)
            }
        }
    }

    /**
     * Toggle notifications on/off
     */
    fun toggleNotifications(enabled: Boolean) {
        try {
            Pushy.toggleNotifications(enabled, context)
            Timber.d("Notifications ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle notifications")
        }
    }

    /**
     * Get the device token if registered
     */
    suspend fun getDeviceToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (isRegistered()) {
                    // Re-register to get the token (Pushy doesn't store it)
                    Pushy.register(context)
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get device token")
                null
            }
        }
    }

    /**
     * Start the Pushy service (call on app start)
     */
    fun startService() {
        try {
            Pushy.listen(context)
            Timber.d("Pushy service started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start Pushy service")
        }
    }
}