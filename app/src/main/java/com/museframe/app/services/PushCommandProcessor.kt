package com.museframe.app.services

import android.content.Intent
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

interface CommandHandler {
    fun canHandle(action: String): Boolean
    fun handle(intent: Intent, onNavigate: NavigationHandler): Boolean
    fun requiresTimeValidation(): Boolean = true
}

interface NavigationHandler {
    fun navigateToPlaylists()
    fun navigateToWelcome()
    fun navigateToPlaylist(playlistId: String)
    fun navigateToArtwork(playlistId: String, artworkId: String)
    fun navigateToExhibition(exhibitionId: String)
    fun sendCommandToScreen(command: String)
}

@Singleton
class PushCommandProcessor @Inject constructor() {
    
    companion object {
        private const val COMMAND_EXPIRY_MINUTES = 5L
        private val NON_EXPIRING_ACTIONS = setOf(
            "com.museframe.DEVICE_CONNECTED",
            "com.museframe.DEVICE_DISCONNECTED"
        )
    }
    
    fun processCommand(
        intent: Intent?,
        navigationHandler: NavigationHandler
    ) {
        if (intent?.action == null) return
        
        try {
            // Validate timestamp if required
            if (!isCommandValid(intent)) {
                Timber.w("Command ${intent.action} expired")
                return
            }
            
            // Route to appropriate handler
            when (intent.action) {
                "com.museframe.DEVICE_CONNECTED" -> handleDeviceConnected(intent, navigationHandler)
                "com.museframe.DEVICE_DISCONNECTED" -> handleDeviceDisconnected(navigationHandler)
                "com.museframe.PAUSE_SLIDESHOW" -> navigationHandler.sendCommandToScreen("PAUSE")
                "com.museframe.RESUME_SLIDESHOW" -> navigationHandler.sendCommandToScreen("RESUME")
                "com.museframe.NEXT_ARTWORK" -> navigationHandler.sendCommandToScreen("NEXT")
                "com.museframe.PREVIOUS_ARTWORK" -> navigationHandler.sendCommandToScreen("PREVIOUS")
                "com.museframe.CAST_PLAYLIST" -> handleCastPlaylist(intent, navigationHandler)
                "com.museframe.CAST_EXHIBITION" -> handleCastExhibition(intent, navigationHandler)
                "com.museframe.REFRESH_PLAYLISTS" -> handleRefreshPlaylists(navigationHandler)
                "com.museframe.REFRESH_PLAYLIST" -> handleRefreshPlaylist(intent, navigationHandler)
                "com.museframe.UPDATE_DISPLAY_SETTING" -> handleUpdateDisplaySettings(intent, navigationHandler)
                "com.museframe.UPDATE_ARTWORK_SETTING" -> handleUpdateArtworkSettings(intent, navigationHandler)
                else -> Timber.w("Unknown command: ${intent.action}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing command: ${intent.action}")
        }
    }
    
    private fun isCommandValid(intent: Intent): Boolean {
        if (intent.action in NON_EXPIRING_ACTIONS) return true
        
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        val commandTime = Instant.ofEpochMilli(timestamp)
        val minutesElapsed = ChronoUnit.MINUTES.between(commandTime, Instant.now())
        
        return minutesElapsed <= COMMAND_EXPIRY_MINUTES
    }
    
    private fun handleDeviceConnected(intent: Intent, navigationHandler: NavigationHandler) {
        val authToken = intent.getStringExtra("auth_token")
        val deviceId = intent.getStringExtra("device_id")
        Timber.d("Device connected: token=${authToken?.take(10)}..., deviceId=$deviceId")
        // Note: Auth handling should be done separately
        navigationHandler.navigateToPlaylists()
    }
    
    private fun handleDeviceDisconnected(navigationHandler: NavigationHandler) {
        // Note: Auth clearing should be done separately
        navigationHandler.navigateToWelcome()
    }
    
    private fun handleCastPlaylist(intent: Intent, navigationHandler: NavigationHandler) {
        intent.getStringExtra("playlist_id")?.let { playlistId ->
            navigationHandler.navigateToPlaylist(playlistId)
        }
    }
    
    private fun handleCastExhibition(intent: Intent, navigationHandler: NavigationHandler) {
        val exhibitionId = intent.getStringExtra("exhibition_id") ?: "default"
        navigationHandler.navigateToExhibition(exhibitionId)
    }
    
    private fun handleRefreshPlaylists(navigationHandler: NavigationHandler) {
        navigationHandler.navigateToPlaylists()
    }
    
    private fun handleRefreshPlaylist(intent: Intent, navigationHandler: NavigationHandler) {
        intent.getStringExtra("playlist_id")?.let { playlistId ->
            navigationHandler.sendCommandToScreen("REFRESH_PLAYLIST:$playlistId")
        }
    }
    
    private fun handleUpdateDisplaySettings(intent: Intent, navigationHandler: NavigationHandler) {
        intent.getStringExtra("data")?.let { data ->
            navigationHandler.sendCommandToScreen("UPDATE_DISPLAY:$data")
        }
    }
    
    private fun handleUpdateArtworkSettings(intent: Intent, navigationHandler: NavigationHandler) {
        intent.getStringExtra("data")?.let { data ->
            navigationHandler.sendCommandToScreen("UPDATE_ARTWORK:$data")
        }
    }
}