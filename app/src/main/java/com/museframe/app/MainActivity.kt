package com.museframe.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.museframe.app.presentation.navigation.MuseFrameNavHost
import com.museframe.app.presentation.navigation.Screen
import com.museframe.app.services.AuthService
import com.museframe.app.services.NavigationHandler
import com.museframe.app.services.PushCommandProcessor
import com.museframe.app.ui.theme.MuseFrameTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.pushy.sdk.Pushy
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), NavigationHandler {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var pushCommandProcessor: PushCommandProcessor

    private var navController: androidx.navigation.NavController? = null

    private val pushCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("Broadcast received: action=${intent?.action}")

            // Special handling for auth-related commands
            when (intent?.action) {
                "com.museframe.DEVICE_CONNECTED" -> {
                    val authToken = intent.getStringExtra("auth_token")
                    val deviceId = intent.getStringExtra("device_id")
                    authToken?.let {
                        authService.handleAuthTokenReceived(it, deviceId)
                    }
                }
                "com.museframe.DEVICE_DISCONNECTED" -> {
                    authService.handleLogout()
                }
            }

            // Delegate command processing
            pushCommandProcessor.processCommand(intent, this@MainActivity)
        }
    }

    // NavigationHandler implementation
    override fun navigateToPlaylists() {
        Timber.d("navigateToPlaylists called, navController is ${if (navController != null) "set" else "null"}")
        navController?.navigate(Screen.Playlists.route) {
            popUpTo(0) { inclusive = true }
        } ?: Timber.e("NavController is null, cannot navigate to playlists")
    }

    override fun navigateToWelcome() {
        navController?.navigate(Screen.Welcome.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    override fun navigateToPlaylist(playlistId: String) {
        navController?.navigate(Screen.PlaylistDetail.createRoute(playlistId))
    }

    override fun navigateToArtwork(playlistId: String, artworkId: String) {
        navController?.navigate(Screen.Artwork.createRoute(playlistId, artworkId))
    }

    override fun navigateToExhibition(exhibitionId: String) {
        navController?.navigate(Screen.Exhibition.route)
    }

    override fun sendCommandToScreen(command: String) {
        commandFlow.tryEmit(command)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Pushy
        Pushy.listen(this)

        // Sync auth token from SharedPreferences (if set by BroadcastReceiver)
        authService.syncAuthTokenFromSharedPrefs()

        // Register broadcast receivers early
        registerPushReceivers()

        enableEdgeToEdge()
        setContent {
            MuseFrameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navigationController = rememberNavController()

                    // Store nav controller reference for push commands
                    LaunchedEffect(navigationController) {
                        navController = navigationController
                    }

                    MuseFrameNavHost(
                        navController = navigationController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent called")

        // Handle navigation from push notification
        intent?.let {
            val authToken = it.getStringExtra("auth_token")
            val navigateTo = it.getStringExtra("navigate_to")

            Timber.d("onNewIntent: navigateTo=$navigateTo, authToken=${authToken?.take(10)}...")

            when (navigateTo) {
                "playlists" -> {
                    val deviceId = it.getStringExtra("device_id")
                    authToken?.let { token ->
                        authService.handleAuthTokenReceived(token, deviceId)
                        navigateToPlaylists()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerPushReceivers()
    }

    override fun onPause() {
        super.onPause()
        unregisterPushReceivers()
    }

    private fun registerPushReceivers() {
        Timber.d("Registering push receivers")
        val filter = IntentFilter().apply {
            addAction("com.museframe.DEVICE_CONNECTED")
            addAction("com.museframe.DEVICE_DISCONNECTED")
            addAction("com.museframe.PAUSE_SLIDESHOW")
            addAction("com.museframe.RESUME_SLIDESHOW")
            addAction("com.museframe.UPDATE_DISPLAY_SETTING")
            addAction("com.museframe.REFRESH_PLAYLISTS")
            addAction("com.museframe.REFRESH_PLAYLIST")
            addAction("com.museframe.UPDATE_ARTWORK_SETTING")
            addAction("com.museframe.CAST_PLAYLIST")
            addAction("com.museframe.CAST_EXHIBITION")
            addAction("com.museframe.NEXT_ARTWORK")
            addAction("com.museframe.PREVIOUS_ARTWORK")
        }

        // For Android 14+ (API 34+), we need to specify RECEIVER_NOT_EXPORTED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pushCommandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pushCommandReceiver, filter)
        }
        Timber.d("Push receivers registered successfully")
    }

    private fun unregisterPushReceivers() {
        try {
            unregisterReceiver(pushCommandReceiver)
        } catch (e: Exception) {
            Timber.e(e, "Error unregistering receiver")
        }
    }

    companion object {
        val commandFlow = MutableSharedFlow<String>()
    }
}