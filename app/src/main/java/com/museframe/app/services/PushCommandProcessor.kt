package com.museframe.app.services

import android.content.Intent
import com.museframe.app.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
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
    fun navigateToPlaylistAndStartSlideshow(playlistId: String)
}

@Singleton
class PushCommandProcessor @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val playlistRepository: com.museframe.app.domain.repository.PlaylistRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
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

        Timber.d("PushCommandProcessor: Processing command: ${intent.action}")

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
                "com.museframe.PAUSE_SLIDESHOW" -> handlePause(navigationHandler)
                "com.museframe.RESUME_SLIDESHOW" -> handleResume(navigationHandler)
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
        Timber.d("handleDeviceDisconnected called - navigating to welcome screen")
        // Note: Auth clearing should be done separately in MainActivity
        navigationHandler.navigateToWelcome()
    }
    
    private fun handleCastPlaylist(intent: Intent, navigationHandler: NavigationHandler) {
        Timber.d("handleCastPlaylist called")
        val playlistId = intent.getStringExtra("playlist_id")
        val startSlideshow = intent.getBooleanExtra("start_slideshow", false)

        Timber.d("Cast Playlist - ID: $playlistId, Start slideshow: $startSlideshow")

        playlistId?.let { id ->
            // Validate playlist exists locally before navigating
            coroutineScope.launch {
                try {
                    // Get device ID to fetch playlists
                    val deviceId = preferencesManager.deviceId.first()
                    if (deviceId != null) {
                        // Check if playlist exists
                        val result = playlistRepository.getPlaylists(deviceId)
                        if (result.isSuccess) {
                            val playlists = result.getOrNull() ?: emptyList()
                            val playlistExists = playlists.any { it.id == id }

                            if (playlistExists) {
                                Timber.d("Playlist $id found locally, navigating...")
                                // Navigate on main thread
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    if (startSlideshow) {
                                        // Navigate to playlist and automatically start slideshow
                                        navigationHandler.navigateToPlaylistAndStartSlideshow(id)
                                    } else {
                                        // Just navigate to playlist detail
                                        navigationHandler.navigateToPlaylist(id)
                                    }
                                }
                            } else {
                                Timber.w("Playlist $id not found locally, ignoring Cast command")
                            }
                        } else {
                            Timber.e("Failed to fetch playlists for validation")
                        }
                    } else {
                        Timber.e("No device ID available")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error validating playlist")
                }
            }
        } ?: Timber.w("No playlist_id in CAST_PLAYLIST intent")
    }
    
    private fun handleCastExhibition(intent: Intent, navigationHandler: NavigationHandler) {
        Timber.d("handleCastExhibition called")
        val exhibitionId = intent.getStringExtra("exhibition_id") ?: "default"
        Timber.d("Navigating to exhibition with id: $exhibitionId")
        navigationHandler.navigateToExhibition(exhibitionId)
        Timber.d("Navigation to exhibition requested")
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
        Timber.d("handleUpdateDisplaySettings called")
        val data = intent.getStringExtra("data")
        Timber.d("Received data: $data")

        data?.let {
            // Parse the data string (format: "key1:value1,key2:value2,...")
            val settings = it.split(",").associate { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
            }

            Timber.d("Parsed settings: $settings")

            // Update frame name if provided
            settings["name"]?.let { frameName ->
                Timber.d("PushCommandProcessor: Updating frame name to: $frameName")
                coroutineScope.launch {
                    preferencesManager.updateFrameName(frameName)
                    Timber.d("PushCommandProcessor: Frame name updated in preferences to: $frameName")
                }
            }

            // Update display settings if provided
            settings["volume"]?.toFloatOrNull()?.let { volume ->
                Timber.d("Updating volume to: $volume")
                coroutineScope.launch {
                    preferencesManager.updateDisplaySettings(volume = volume / 100f)
                }
            }

            settings["brightness"]?.toFloatOrNull()?.let { brightness ->
                Timber.d("Updating brightness to: $brightness")
                coroutineScope.launch {
                    preferencesManager.updateDisplaySettings(brightness = brightness / 100f)
                }
            }

            settings["pause"]?.toBooleanStrictOrNull()?.let { pause ->
                Timber.d("Updating pause to: $pause")
                coroutineScope.launch {
                    preferencesManager.updateDisplaySettings(isPaused = pause)
                }
            }

            // Send command to screen for immediate UI updates
            navigationHandler.sendCommandToScreen("UPDATE_DISPLAY:$data")
        } ?: Timber.w("No data received in UPDATE_DISPLAY_SETTING intent")
    }
    
    private fun handleUpdateArtworkSettings(intent: Intent, navigationHandler: NavigationHandler) {
        Timber.d("handleUpdateArtworkSettings called")
        val playlistId = intent.getStringExtra("playlist_id")
        val artworkId = intent.getStringExtra("artwork_id")

        if (playlistId != null && artworkId != null) {
            Timber.d("UpdateArtworkSettings - Playlist: $playlistId, Artwork: $artworkId")
            // Send command with both IDs to the screen
            navigationHandler.sendCommandToScreen("UPDATE_ARTWORK:$playlistId:$artworkId")
        } else {
            Timber.w("Missing playlist_id or artwork_id in UPDATE_ARTWORK_SETTING intent")
        }
    }

    private fun handlePause(navigationHandler: NavigationHandler) {
        Timber.d("handlePause called - updating global pause state")
        // Update global pause state immediately
        coroutineScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = true)
            Timber.d("Global pause state updated to true")
        }
        // Also send to screen if it's listening
        navigationHandler.sendCommandToScreen("PAUSE")
    }

    private fun handleResume(navigationHandler: NavigationHandler) {
        Timber.d("handleResume called - updating global pause state")
        // Update global pause state immediately
        coroutineScope.launch {
            preferencesManager.updateDisplaySettings(isPaused = false)
            Timber.d("Global pause state updated to false")
        }
        // Also send to screen if it's listening
        navigationHandler.sendCommandToScreen("RESUME")
    }
}