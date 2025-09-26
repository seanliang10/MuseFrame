package com.museframe.app.presentation.screens.exhibition

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.museframe.app.domain.model.ExhibitionItem
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun ExhibitionScreenNew(
    exhibitionId: String,
    onBackClick: () -> Unit,
    onNavigateToNoNetwork: () -> Unit = {},
    viewModel: ExhibitionViewModelNew = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shouldNavigateToNoNetwork by viewModel.shouldNavigateToNoNetwork.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Navigate to no network screen when needed
    LaunchedEffect(shouldNavigateToNoNetwork) {
        if (shouldNavigateToNoNetwork) {
            onNavigateToNoNetwork()
            viewModel.clearNoNetworkNavigation()
        }
    }

    // Set full screen mode
    DisposableEffect(Unit) {
        activity?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
        onDispose {
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
    }

    // Handle back navigation
    BackHandler {
        onBackClick()
    }

    // Handle exhibition ended state
    LaunchedEffect(uiState.exhibitionEnded) {
        if (uiState.exhibitionEnded) {
            onBackClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading exhibition...",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: ""
                val isNoExhibition = errorMessage.contains("No current exhibition", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icon or illustration
                        Icon(
                            imageVector = if (isNoExhibition)
                                Icons.Default.Info
                            else
                                Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (isNoExhibition)
                                "No Exhibition Available"
                            else
                                "Exhibition Error",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 400.dp)
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = onBackClick,
                            modifier = Modifier
                                .height(56.dp)
                                .widthIn(min = 200.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = "Back to Playlists",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            uiState.currentItem != null -> {
                uiState.currentItem?.let { item ->
                    ExhibitionItemDisplay(
                        item = item,
                        onVideoEnded = {
                            viewModel.nextItem()
                        }
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exhibition content available",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }

    // Handle auto-advance for images (60 seconds duration)
    LaunchedEffect(uiState.currentItem) {
        val item = uiState.currentItem
        if (item != null && item.type != "video") {
            delay(60000L) // 60 seconds for images
            viewModel.nextItem()
        }
    }

    // Periodically check if exhibition has ended (every minute)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L) // Check every minute
            viewModel.checkExhibitionEndTime()
        }
    }
}

@Composable
fun ExhibitionItemDisplay(
    item: ExhibitionItem,
    onVideoEnded: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (item.type?.lowercase()) {
            "video" -> {
                ExhibitionVideoPlayer(
                    videoUrl = item.displayUrl,
                    onEnded = onVideoEnded
                )
            }
            else -> {
                // Display image
                AsyncImage(
                    model = item.displayUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ExhibitionVideoPlayer(
    videoUrl: String,
    onEnded: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                // Optimize for performance on limited hardware
                setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 1.0f

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            onEnded()
                        }
                    }

                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        Timber.e(error, "Exhibition video playback error")
                        // Skip to next item on error
                        onEnded()
                    }
                })
            }
    }

    // Load video
    LaunchedEffect(videoUrl) {
        try {
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            Timber.e(e, "Error loading exhibition video")
            onEnded()
        }
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_STOP -> exoPlayer.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            StyledPlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                setShowBuffering(StyledPlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}