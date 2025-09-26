package com.museframe.app.services

import android.content.Context
import com.museframe.app.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class PushCommandEvent(
    val command: String,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Instant = Instant.now()
)

@Singleton
class PushCommandHandler @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private val _commandEvents = MutableSharedFlow<PushCommandEvent>()
    val commandEvents: SharedFlow<PushCommandEvent> = _commandEvents.asSharedFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val COMMAND_EXPIRY_MINUTES = 5L
        
        // Commands that don't expire
        private val NON_EXPIRING_COMMANDS = setOf(
            "Connected",
            "Disconnected"
        )
        
        // Commands that require specific screen context
        private val ARTWORK_SCREEN_COMMANDS = setOf(
            "Pause",
            "Resume",
            "Next",
            "Previous",
            "UpdateSettings" // UpdatePlaylistArtworkSetting
        )
    }
    
    fun handleCommand(
        command: String,
        data: Map<String, Any> = emptyMap(),
        timestamp: Instant = Instant.now(),
        currentScreen: String? = null
    ) {
        scope.launch {
            try {
                // Check command expiry (5 minutes)
                if (!isCommandValid(command, timestamp)) {
                    Timber.w("Command $command expired (timestamp: $timestamp)")
                    return@launch
                }
                
                // Check screen context for specific commands
                if (!isCommandAllowedForScreen(command, currentScreen)) {
                    Timber.d("Command $command ignored for screen: $currentScreen")
                    return@launch
                }
                
                // Emit the command event
                _commandEvents.emit(PushCommandEvent(command, data, timestamp))
                Timber.d("Command emitted: $command with data: $data")
            } catch (e: Exception) {
                Timber.e(e, "Error handling command: $command")
            }
        }
    }
    
    private fun isCommandValid(command: String, timestamp: Instant): Boolean {
        // Connected and Disconnected commands don't expire
        if (command in NON_EXPIRING_COMMANDS) {
            return true
        }
        
        // Check if command is within 5 minutes
        val now = Instant.now()
        val minutesElapsed = ChronoUnit.MINUTES.between(timestamp, now)
        return minutesElapsed <= COMMAND_EXPIRY_MINUTES
    }
    
    private fun isCommandAllowedForScreen(command: String, currentScreen: String?): Boolean {
        // Commands that only work on artwork screen
        if (command in ARTWORK_SCREEN_COMMANDS) {
            return currentScreen == "artwork"
        }
        
        // Exhibition doesn't respond to pause/resume/next/previous
        if (currentScreen == "exhibition" && command in setOf("Pause", "Resume", "Next", "Previous")) {
            return false
        }
        
        // RefreshPlaylist only works if we're on that playlist
        if (command == "RefreshPlaylist") {
            // This will be handled by the specific screen
            return true // Let the screen decide
        }
        
        // UpdateSettings only works if we're on that artwork
        if (command == "UpdateSettings") {
            // This will be handled by the artwork screen
            return true // Let the screen decide
        }
        
        // All other commands are allowed
        return true
    }
}