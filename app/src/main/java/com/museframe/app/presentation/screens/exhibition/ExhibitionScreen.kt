package com.museframe.app.presentation.screens.exhibition

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.museframe.app.domain.model.MediaType
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExhibitionScreen(
    exhibitionId: String,
    onBackClick: () -> Unit,
    viewModel: ExhibitionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val focusRequester = remember { FocusRequester() }

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

    // Handle back navigation
    BackHandler {
        viewModel.pauseSlideshow()
        onBackClick()
    }

    // Auto-hide controls after delay
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls) {
            delay(5000) // Hide after 5 seconds
            viewModel.toggleControls()
        }
    }

    // Request focus for TV remote handling
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
                                // Center/OK button - toggle play/pause
                                if (uiState.isPaused) {
                                    viewModel.resumeSlideshow()
                                } else {
                                    viewModel.pauseSlideshow()
                                }
                                true
                            }
                            Key.DirectionLeft -> {
                                // Left arrow - previous artwork
                                viewModel.previousArtwork()
                                true
                            }
                            Key.DirectionRight -> {
                                // Right arrow - next artwork
                                viewModel.nextArtwork()
                                true
                            }
                            Key.DirectionUp, Key.DirectionDown -> {
                                // Up/Down arrows - toggle controls
                                viewModel.toggleControls()
                                true
                            }
                            Key.Back -> {
                                // Back button
                                viewModel.pauseSlideshow()
                                onBackClick()
                                true
                            }
                            Key.MediaPlay -> {
                                viewModel.resumeSlideshow()
                                true
                            }
                            Key.MediaPause -> {
                                viewModel.pauseSlideshow()
                                true
                            }
                            Key.MediaPlayPause -> {
                                if (uiState.isPaused) {
                                    viewModel.resumeSlideshow()
                                } else {
                                    viewModel.pauseSlideshow()
                                }
                                true
                            }
                            Key.MediaNext -> {
                                viewModel.nextArtwork()
                                true
                            }
                            Key.MediaPrevious -> {
                                viewModel.previousArtwork()
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
        // Main content
        uiState.currentArtwork?.let { artwork ->
            when (artwork.mediaType) {
                MediaType.VIDEO -> {
                    VideoPlayer(
                        url = artwork.displayUrl,
                        volume = uiState.volume,
                        onEnded = { viewModel.nextArtwork() }
                    )
                }
                MediaType.IMAGE, MediaType.GIF -> {
                    AsyncImage(
                        model = artwork.displayUrl,
                        contentDescription = artwork.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Artwork info overlay (always visible in Exhibition mode)
            AnimatedVisibility(
                visible = !uiState.showControls, // Show info when controls are hidden
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(24.dp)
                    ) {
                        Text(
                            text = artwork.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (!artwork.creator.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = artwork.creator,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                        }
                        // Year field not available in current model
                        if (!artwork.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = artwork.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading exhibition...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Controls overlay
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ExhibitionControls(
                exhibition = uiState.exhibitionTitle,
                currentIndex = uiState.currentIndex,
                totalArtworks = uiState.totalArtworks,
                isPaused = uiState.isPaused,
                onBackClick = {
                    viewModel.pauseSlideshow()
                    onBackClick()
                },
                onPreviousClick = { viewModel.previousArtwork() },
                onNextClick = { viewModel.nextArtwork() },
                onPlayPauseClick = {
                    if (uiState.isPaused) {
                        viewModel.resumeSlideshow()
                    } else {
                        viewModel.pauseSlideshow()
                    }
                }
            )
        }
    }
}

@Composable
fun ExhibitionControls(
    exhibition: String?,
    currentIndex: Int,
    totalArtworks: Int,
    isPaused: Boolean,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top gradient and controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Exhibition title
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EXHIBITION",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        letterSpacing = 2.sp
                    )
                    exhibition?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Position indicator
                Text(
                    text = "${currentIndex + 1} / $totalArtworks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }

        // Bottom gradient and playback controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Play/Pause button
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Close,
                        contentDescription = if (isPaused) "Play" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Next button
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    url: String,
    volume: Float,
    onEnded: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            setVolume(volume)

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onEnded()
                    }
                }
            })
        }
    }

    // Update volume when it changes
    LaunchedEffect(volume) {
        exoPlayer.setVolume(volume)
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
                useController = false // We're using custom controls
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}