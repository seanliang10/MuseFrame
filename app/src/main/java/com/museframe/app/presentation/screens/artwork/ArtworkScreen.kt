package com.museframe.app.presentation.screens.artwork

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.museframe.app.domain.model.MediaType
import com.museframe.app.domain.model.PlaylistArtwork
import com.museframe.app.util.QrCodeGenerator
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ArtworkScreen(
    playlistId: String,
    artworkId: String,
    onBackClick: () -> Unit,
    onNavigateToPlaylistDetail: (String) -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    viewModel: ArtworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    // Handle 404 errors - navigate to playlists when playlist/artwork is deleted
    LaunchedEffect(uiState.shouldNavigateToPlaylists) {
        if (uiState.shouldNavigateToPlaylists) {
            Timber.d("Playlist/Artwork deleted (404), navigating to playlists")
            viewModel.clearNavigationFlag()
            onNavigateToPlaylists()
        }
    }

    // Observe push commands from MainActivity
    LaunchedEffect(Unit) {
        Timber.d("ArtworkScreen: Starting command flow collection")
        com.museframe.app.MainActivity.commandFlow.collect { command ->
            Timber.d("ArtworkScreen received command: $command")
            when {
                command == "PAUSE" -> viewModel.handlePauseCommand()
                command == "RESUME" -> viewModel.handleResumeCommand()
                command == "NEXT" -> {
                    Timber.d("Handling NEXT command")
                    viewModel.handleNextCommand()
                }
                command == "PREVIOUS" -> {
                    Timber.d("Handling PREVIOUS command")
                    viewModel.handlePreviousCommand()
                }
                command.startsWith("UPDATE_ARTWORK:") -> {
                    // Parse playlist_id and artwork_id from command
                    val parts = command.substringAfter("UPDATE_ARTWORK:").split(":")
                    if (parts.size >= 2) {
                        val updatePlaylistId = parts[0]
                        val updateArtworkId = parts[1]
                        Timber.d("UPDATE_ARTWORK command - Playlist: $updatePlaylistId, Artwork: $updateArtworkId")
                        viewModel.handleArtworkSettingsUpdate(updatePlaylistId, updateArtworkId)
                    } else {
                        Timber.e("Invalid UPDATE_ARTWORK command format: $command")
                    }
                }
                command.startsWith("REFRESH_PLAYLIST:") -> {
                    // Do nothing - ignore playlist refresh to avoid conflicts with transition system
                    Timber.d("Ignoring REFRESH_PLAYLIST command to maintain playlist transition stability")
                }
                command.startsWith("UPDATE_PLAYLISTS") -> {
                    // Do nothing - ignore playlists update to avoid conflicts with transition system
                    Timber.d("Ignoring UPDATE_PLAYLISTS command to maintain playlist transition stability")
                }
            }
        }
    }

    // Set full screen
    DisposableEffect(Unit) {
        activity?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        onDispose {
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
    }

    // Handle back navigation - go to current playlist detail
    BackHandler {
        viewModel.pauseSlideshow()
        val currentPlaylistId = viewModel.getCurrentPlaylistId()
        if (currentPlaylistId != playlistId) {
            // If we've switched playlists, navigate to the current playlist detail
            onNavigateToPlaylistDetail(currentPlaylistId)
        } else {
            // Same playlist, just go back normally
            onBackClick()
        }
    }

    // Auto-hide controls
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls) {
            delay(5000)
            viewModel.toggleControls()
        }
    }

    // Handle auto-advance for images based on duration
    // Track remaining time when paused
    var remainingTime by remember { mutableStateOf<Long?>(null) }
    var timerStartTime by remember { mutableStateOf(0L) }

    LaunchedEffect(uiState.currentPlaylistArtwork, uiState.isPaused) {
        val playlistArtwork = uiState.currentPlaylistArtwork
        Timber.d("LaunchedEffect triggered - artwork: ${playlistArtwork?.artwork?.title}, mediaType: ${playlistArtwork?.artwork?.mediaType}, duration: ${playlistArtwork?.duration}, isPaused: ${uiState.isPaused}")

        if (playlistArtwork != null && playlistArtwork.artwork.mediaType == MediaType.IMAGE) {
            if (!uiState.isPaused) {
                // Calculate delay time - use remaining time if resuming, otherwise full duration
                val delayTime = if (remainingTime != null && remainingTime!! > 0) {
                    Timber.d("Resuming timer with ${remainingTime}ms remaining")
                    val timeToWait = remainingTime!!
                    remainingTime = null // Clear remaining time after using it
                    timeToWait
                } else {
                    Timber.d("Starting new timer for ${playlistArtwork.duration} seconds")
                    (playlistArtwork.duration * 1000).toLong()
                }

                timerStartTime = System.currentTimeMillis()
                delay(delayTime)

                // Check pause state again after delay before advancing
                if (!uiState.isPaused) {
                    Timber.d("Timer finished and not paused, calling nextArtwork")
                    remainingTime = null
                    viewModel.nextArtwork()
                } else {
                    Timber.d("Timer finished but slideshow is paused, staying on current image")
                    // Don't advance, and reset remaining time for next resume
                    remainingTime = 0L
                }
            } else {
                // If paused and timer was running, calculate remaining time
                if (timerStartTime > 0 && playlistArtwork.artwork.mediaType == MediaType.IMAGE) {
                    val elapsed = System.currentTimeMillis() - timerStartTime
                    val totalDuration = (playlistArtwork.duration * 1000).toLong()
                    val remaining = totalDuration - elapsed
                    if (remaining > 0) {
                        remainingTime = remaining
                        Timber.d("Paused with ${remaining}ms remaining")
                    }
                }
            }
        } else if (playlistArtwork == null) {
            // Reset timer tracking when no artwork
            remainingTime = null
            timerStartTime = 0L
        }
    }

    // Request focus for TV remote
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown -> {
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter -> {
                                viewModel.toggleControls()
                                true
                            }
                            Key.DirectionLeft -> {
                                viewModel.previousArtwork()
                                true
                            }
                            Key.DirectionRight -> {
                                viewModel.nextArtwork()
                                true
                            }
                            Key.Escape, Key.Back -> {
                                viewModel.pauseSlideshow()
                                val currentPlaylistId = viewModel.getCurrentPlaylistId()
                                if (currentPlaylistId != playlistId) {
                                    onNavigateToPlaylistDetail(currentPlaylistId)
                                } else {
                                    onBackClick()
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.toggleControls()
            }
    ) {
        // Smooth crossfade transition between artworks
        Crossfade(
            targetState = uiState.currentPlaylistArtwork,
            animationSpec = tween(durationMillis = 800), // 800ms fade transition
            label = "artwork_transition"
        ) { playlistArtwork ->
            if (playlistArtwork != null) {
                ArtworkDisplay(
                    playlistArtwork = playlistArtwork,
                    isPaused = uiState.isPaused,
                    onVideoEnded = {
                        if (!uiState.isPaused) {
                            Timber.d("Video ended and not paused, advancing to next artwork")
                            viewModel.nextArtwork()
                        } else {
                            Timber.d("Video ended but paused, will loop current video")
                            // Video will loop automatically when isPaused is true
                        }
                    }
                )
            }
        }

        // Controls overlay
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ArtworkControls(
                isPaused = uiState.isPaused,
                currentIndex = uiState.currentIndex,
                totalArtworks = uiState.totalArtworks,
                onPlayPause = {
                    if (uiState.isPaused) {
                        viewModel.resumeSlideshow()
                    } else {
                        viewModel.pauseSlideshow()
                    }
                },
                onPrevious = viewModel::previousArtwork,
                onNext = viewModel::nextArtwork,
                onClose = {
                    viewModel.pauseSlideshow()
                    val currentPlaylistId = viewModel.getCurrentPlaylistId()
                    if (currentPlaylistId != playlistId) {
                        onNavigateToPlaylistDetail(currentPlaylistId)
                    } else {
                        onBackClick()
                    }
                }
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Error display
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ArtworkDisplay(
    playlistArtwork: PlaylistArtwork,
    isPaused: Boolean = false,
    onVideoEnded: () -> Unit
) {
    val backgroundColor = try {
        Color(playlistArtwork.background.toColorInt())
    } catch (e: Exception) {
        Color.Black
    }

    // Apply 5x multiplier to border width for better visibility
    val effectiveBorderWidth = playlistArtwork.borderWidth * 5f

    if (playlistArtwork.showInfo) {
        // Layout with dedicated info section
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // Artwork section with border as frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = effectiveBorderWidth.dp,
                        top = effectiveBorderWidth.dp,
                        end = effectiveBorderWidth.dp,
                        bottom = 0.dp
                    )
                    .background(Color.Black)
                    .clipToBounds(), // Ensure artwork stays within the frame
                contentAlignment = Alignment.Center
            ) {
                when (playlistArtwork.artwork.mediaType) {
                        MediaType.IMAGE, MediaType.GIF -> {
                            AsyncImage(
                                model = playlistArtwork.artwork.displayUrl,
                                contentDescription = playlistArtwork.artwork.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = playlistArtwork.zoom
                                        scaleY = playlistArtwork.zoom
                                        translationX = playlistArtwork.adjustment.x * density
                                        translationY = playlistArtwork.adjustment.y * density
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                        MediaType.VIDEO -> {
                            var isBuffering by remember { mutableStateOf(false) }

                            Box(modifier = Modifier.fillMaxSize()) {
                                VideoPlayer(
                                    videoUrl = playlistArtwork.artwork.displayUrl,
                                    volume = playlistArtwork.artwork.volume,
                                    zoom = playlistArtwork.zoom,
                                    offsetX = playlistArtwork.adjustment.x,
                                    offsetY = playlistArtwork.adjustment.y,
                                    isPaused = isPaused,
                                    onEnded = onVideoEnded,
                                    onBufferingChanged = { isBuffering = it }
                                )

                                if (isBuffering) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            // Info section with background color
            ArtworkInfoSection(
                title = playlistArtwork.artwork.title,
                creator = playlistArtwork.artwork.creator,
                marketplaceUrl = playlistArtwork.artwork.marketplaceUrl,
                backgroundColor = backgroundColor,
                borderWidth = effectiveBorderWidth
            )
        }
    } else {
        // Layout without info section (original layout)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(effectiveBorderWidth.dp)
                    .background(Color.Black)
                    .clipToBounds(), // Ensure artwork stays within the frame
                contentAlignment = Alignment.Center
            ) {
                when (playlistArtwork.artwork.mediaType) {
                        MediaType.IMAGE, MediaType.GIF -> {
                            AsyncImage(
                                model = playlistArtwork.artwork.displayUrl,
                                contentDescription = playlistArtwork.artwork.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = playlistArtwork.zoom
                                        scaleY = playlistArtwork.zoom
                                        translationX = playlistArtwork.adjustment.x * density
                                        translationY = playlistArtwork.adjustment.y * density
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                        MediaType.VIDEO -> {
                            var isBuffering by remember { mutableStateOf(false) }

                            Box(modifier = Modifier.fillMaxSize()) {
                                VideoPlayer(
                                    videoUrl = playlistArtwork.artwork.displayUrl,
                                    volume = playlistArtwork.artwork.volume,
                                    zoom = playlistArtwork.zoom,
                                    offsetX = playlistArtwork.adjustment.x,
                                    offsetY = playlistArtwork.adjustment.y,
                                    isPaused = isPaused,
                                    onEnded = onVideoEnded,
                                    onBufferingChanged = { isBuffering = it }
                                )

                                if (isBuffering) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtworkInfoSection(
    title: String,
    creator: String?,
    marketplaceUrl: String? = null,
    backgroundColor: Color,
    borderWidth: Float
) {
    // Info section as a dedicated area
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(
                start = borderWidth.dp,
                end = borderWidth.dp,
                bottom = borderWidth.dp,
                top = 0.dp
            )
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Title and creator
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            creator?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // QR Code for marketplace URL
        marketplaceUrl?.let { url ->
            val qrCodeBitmap = remember(url) {
                QrCodeGenerator.generateQrCode(url, 120)
            }

            qrCodeBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(80.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun ArtworkInfoOverlay(
    title: String,
    creator: String?,
    marketplaceUrl: String? = null,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Bottom info bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.9f))
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title and creator
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                creator?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // QR Code for marketplace URL
            marketplaceUrl?.let { url ->
                val qrCodeBitmap = remember(url) {
                    QrCodeGenerator.generateQrCode(url, 120)
                }

                qrCodeBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(120.dp)
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    volume: Float,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    isPaused: Boolean = false,
    onEnded: () -> Unit,
    onBufferingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        // Configure aggressive buffering for 4K video over WiFi
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000,  // Min buffer: 50 seconds
                60000,  // Max buffer: 60 seconds
                2500,   // Buffer for playback: 2.5 seconds
                5000    // Buffer for playback after rebuffer: 5 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                // Optimize for low-end devices
                setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                playWhenReady = true
                // Loop video when paused, otherwise play once
                repeatMode = if (isPaused) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                setVolume(volume)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_ENDED -> onEnded()
                            Player.STATE_BUFFERING -> {
                                Timber.d("Video buffering...")
                                onBufferingChanged(true)
                            }
                            Player.STATE_READY -> {
                                Timber.d("Video ready")
                                onBufferingChanged(false)
                            }
                            else -> {}
                        }
                    }
                })
            }
    }

    // Update video when URL changes
    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Update repeat mode when pause state changes (without reloading video)
    LaunchedEffect(isPaused) {
        exoPlayer.repeatMode = if (isPaused) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        Timber.d("Video repeat mode updated - looping: $isPaused")
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
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
        modifier = Modifier
            .fillMaxSize()
            .scale(zoom)
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
            }
    )
}

@Composable
fun ArtworkControls(
    isPaused: Boolean,
    currentIndex: Int,
    totalArtworks: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar with close button and progress
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "${currentIndex + 1} / $totalArtworks",
                color = Color.White,
                fontSize = 18.sp
            )
        }

        // Center controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isPaused) "Play" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}