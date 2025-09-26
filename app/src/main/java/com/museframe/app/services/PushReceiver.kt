package com.museframe.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.museframe.app.MainActivity
import com.museframe.app.domain.model.PushCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.format.DateTimeFormatter

class PushReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PushReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val commandType = intent.getStringExtra("command")
            val data = intent.getStringExtra("data")

            // Log all extras for debugging
            val extras = intent.extras
            extras?.let {
                for (key in it.keySet()) {
                    Log.d(TAG, "Push extra: $key = ${it.get(key)}")
                }
            }

            Log.d(TAG, "Received push notification: command=$commandType, data=$data")

            when (commandType) {
                PushCommand.CONNECTED.value -> handleConnected(context, intent)
                PushCommand.DISCONNECTED.value -> handleDisconnected(context)
                PushCommand.PAUSE.value -> handlePause(context)
                PushCommand.RESUME.value -> handleResume(context)
                PushCommand.UPDATE_DISPLAY_SETTING.value -> handleUpdateDisplaySetting(context, intent)
                PushCommand.REFRESH_PLAYLISTS.value -> handleRefreshPlaylists(context)
                PushCommand.REFRESH_PLAYLIST.value -> handleRefreshPlaylist(context, data)
                PushCommand.UPDATE_PLAYLIST_ARTWORK_SETTING.value -> handleUpdateArtworkSetting(context, data)
                PushCommand.CAST.value -> handleCast(context, intent)
                PushCommand.CAST_EXHIBITION.value -> handleCastExhibition(context, intent)
                PushCommand.NEXT.value -> handleNext(context)
                PushCommand.PREVIOUS.value -> handlePrevious(context)
                else -> Log.w(TAG, "Unknown command: $commandType")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing push notification", e)
        }
    }

    private fun handleConnected(context: Context, intent: Intent) {
        try {
            // Try to get token and display_id directly from intent extras
            val authToken = intent.getStringExtra("token")
            val displayId = intent.getStringExtra("display_id") ?: intent.getIntExtra("display_id", 0).toString()

            // If not found, try parsing from data field
            if (authToken == null) {
                val data = intent.getStringExtra("data")
                data?.let {
                    try {
                        val json = Json.parseToJsonElement(it).jsonObject
                        val tokenFromData = json["auth_token"]?.jsonPrimitive?.content
                        val displayIdFromData = json["display_id"]?.jsonPrimitive?.content
                        tokenFromData?.let { token ->
                            saveTokenAndNavigate(context, token, displayIdFromData)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing connected data", e)
                    }
                }
            } else {
                saveTokenAndNavigate(context, authToken, displayId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling connected command", e)
        }
    }

    private fun saveTokenAndNavigate(context: Context, token: String, displayId: String? = null) {
        // Save auth token and device ID using SharedPreferences for synchronous access
        val sharedPrefs = context.getSharedPreferences("museframe_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("auth_token", token)
            displayId?.let { putString("device_id", it) }
            apply()
        }

        Log.d(TAG, "Device connected with auth token: ${token.take(10)}... and displayId: $displayId")

        // Send broadcast to navigate to playlists
        val broadcastIntent = Intent("com.museframe.DEVICE_CONNECTED")
        broadcastIntent.putExtra("auth_token", token)
        displayId?.let { broadcastIntent.putExtra("device_id", it) }
        broadcastIntent.setPackage(context.packageName) // Ensure it's sent to our app only
        context.sendBroadcast(broadcastIntent)

        // Also try to start MainActivity if it's not running
        val mainActivityIntent = Intent(context, MainActivity::class.java)
        mainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        mainActivityIntent.putExtra("auth_token", token)
        displayId?.let { mainActivityIntent.putExtra("device_id", it) }
        mainActivityIntent.putExtra("navigate_to", "playlists")
        context.startActivity(mainActivityIntent)

        Log.d(TAG, "Attempting to start MainActivity with navigation intent")
    }

    private fun handleDisconnected(context: Context) {
        // Clear auth token
        val sharedPrefs = context.getSharedPreferences("museframe_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("auth_token").apply()

        // Send broadcast to navigate to welcome screen
        val intent = Intent("com.museframe.DEVICE_DISCONNECTED")
        context.sendBroadcast(intent)
    }

    private fun handlePause(context: Context) {
        val intent = Intent("com.museframe.PAUSE_SLIDESHOW")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun handleResume(context: Context) {
        val intent = Intent("com.museframe.RESUME_SLIDESHOW")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun handleUpdateDisplaySetting(context: Context, pushIntent: Intent) {
        Log.d(TAG, "handleUpdateDisplaySetting called")

        // UpdateDisplaySetting comes with individual fields from the push
        val intent = Intent("com.museframe.UPDATE_DISPLAY_SETTING")

        // Extract all the display settings from the push
        // Note: Pushy sends these as different types based on the JSON
        val name = pushIntent.getStringExtra("name")
        val volume = pushIntent.getIntExtra("volume", -1).takeIf { it != -1 }
        val brightness = pushIntent.getIntExtra("brightness", -1).takeIf { it != -1 }
        val pause = pushIntent.getBooleanExtra("pause", false)
        val hasPauseExtra = pushIntent.hasExtra("pause")

        Log.d(TAG, "Extracted values - name: $name, volume: $volume, brightness: $brightness, pause: $pause (hasPause: $hasPauseExtra)")

        // Create a JSON string with the settings
        val settingsMap = mutableMapOf<String, String>()
        name?.let { settingsMap["name"] = it }
        volume?.let { settingsMap["volume"] = it.toString() }
        brightness?.let { settingsMap["brightness"] = it.toString() }
        if (hasPauseExtra) { settingsMap["pause"] = pause.toString() }

        // Convert to JSON format for the data field
        val dataString = settingsMap.entries.joinToString(",") { "${it.key}:${it.value}" }

        Log.d(TAG, "Created data string: $dataString")

        if (dataString.isNotEmpty()) {
            intent.putExtra("data", dataString)
        }

        // Add timestamp
        intent.putExtra("timestamp", System.currentTimeMillis())

        Log.d(TAG, "Broadcasting UPDATE_DISPLAY_SETTING intent with data: $dataString")
        // Ensure the broadcast is sent to our app
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun handleRefreshPlaylists(context: Context) {
        val intent = Intent("com.museframe.REFRESH_PLAYLISTS")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun handleRefreshPlaylist(context: Context, data: String?) {
        data?.let {
            val json = Json.parseToJsonElement(it).jsonObject
            val playlistId = json["playlist_id"]?.jsonPrimitive?.content
            playlistId?.let { id ->
                val intent = Intent("com.museframe.REFRESH_PLAYLIST")
                intent.putExtra("playlist_id", id)
                intent.setPackage(context.packageName)
                context.sendBroadcast(intent)
            }
        }
    }

    private fun handleUpdateArtworkSetting(context: Context, data: String?) {
        data?.let {
            val intent = Intent("com.museframe.UPDATE_ARTWORK_SETTING")
            intent.putExtra("data", it)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }

    private fun handleCast(context: Context, pushIntent: Intent) {
        Log.d(TAG, "handleCast called")

        val broadcastIntent = Intent("com.museframe.CAST_PLAYLIST")

        // Try to get playlist_id directly from intent extras (similar to exhibition_id)
        // It could come as string or int
        var playlistId = pushIntent.getStringExtra("playlist_id")
        if (playlistId == null) {
            val intId = pushIntent.getIntExtra("playlist_id", 0)
            if (intId != 0) {
                playlistId = intId.toString()
            }
        }

        // Also try from data field if not found in extras
        if (playlistId == null) {
            val data = pushIntent.getStringExtra("data")
            data?.let {
                try {
                    val json = Json.parseToJsonElement(it).jsonObject
                    playlistId = json["playlist_id"]?.jsonPrimitive?.content
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing cast data", e)
                }
            }
        }

        if (playlistId != null) {
            Log.d(TAG, "Playlist ID: $playlistId")
            broadcastIntent.putExtra("playlist_id", playlistId)
            broadcastIntent.putExtra("start_slideshow", true) // Add flag to start slideshow

            // Add timestamp for validation
            broadcastIntent.putExtra("timestamp", System.currentTimeMillis())

            broadcastIntent.setPackage(context.packageName)
            context.sendBroadcast(broadcastIntent)
            Log.d(TAG, "Broadcasting CAST_PLAYLIST intent with playlist_id: $playlistId")
        } else {
            Log.w(TAG, "No playlist_id found in Cast command")
        }
    }

    private fun handleCastExhibition(context: Context, pushIntent: Intent) {
        Log.d(TAG, "handleCastExhibition called")

        val broadcastIntent = Intent("com.museframe.CAST_EXHIBITION")

        // Try to get exhibition_id directly from intent extras (as per log: exhibition_id=1)
        val exhibitionId = pushIntent.getIntExtra("exhibition_id", 0).takeIf { it != 0 }?.toString()
            ?: pushIntent.getStringExtra("exhibition_id")
            ?: "default"

        Log.d(TAG, "Exhibition ID: $exhibitionId")

        broadcastIntent.putExtra("exhibition_id", exhibitionId)

        // Add timestamp for validation
        broadcastIntent.putExtra("timestamp", System.currentTimeMillis())

        broadcastIntent.setPackage(context.packageName)
        context.sendBroadcast(broadcastIntent)
        Log.d(TAG, "Broadcasting CAST_EXHIBITION intent with exhibition_id: $exhibitionId")
    }

    private fun handleNext(context: Context) {
        val intent = Intent("com.museframe.NEXT_ARTWORK")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun handlePrevious(context: Context) {
        val intent = Intent("com.museframe.PREVIOUS_ARTWORK")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
}