package com.museframe.app.presentation.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.museframe.app.domain.model.Artwork
import com.museframe.app.domain.model.MediaType
import com.museframe.app.presentation.components.tv.TvButton
import com.museframe.app.presentation.components.tv.TvCard

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onArtworkClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Title and artwork count
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.playlist?.name ?: "Loading...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.artworks.isNotEmpty()) {
                        Text(
                            text = if (uiState.artworks.size == 1) {
                                "Artwork (1)"
                            } else {
                                "Artworks (${uiState.artworks.size})"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons in a dedicated row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    TvButton(
                        onClick = onBackClick,
                        text = "Back",
                        icon = Icons.AutoMirrored.Default.ArrowBack,
                        requestInitialFocus = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Start slideshow button
                    if (uiState.artworks.isNotEmpty()) {
                        TvButton(
                            onClick = {
                                uiState.artworks.firstOrNull()?.let {
                                    onArtworkClick(it.id)
                                }
                            },
                            text = "Start Slideshow",
                            icon = Icons.Default.PlayArrow
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Refresh button
                    TvButton(
                        onClick = {
                            if (!uiState.isLoading) {
                                viewModel.refresh()
                            }
                        },
                        text = if (uiState.isLoading) "Loading..." else "Refresh",
                        icon = if (!uiState.isLoading) Icons.Default.Refresh else null,
                        enabled = true // Keep enabled to maintain focus
                    )
                }
            }

            // Content
            when {
                uiState.isLoading -> {
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

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error loading artworks",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.artworks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No artworks in this playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                    }
                }

                else -> {
                    // Artworks grid - 4 columns for TV
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.artworks,
                            key = { it.id }
                        ) { artwork ->
                            TvCard(
                                onClick = {
                                    viewModel.selectArtwork(artwork)
                                    onArtworkClick(artwork.id)
                                },
                                title = artwork.title,
                                imageUrl = artwork.thumbnailUrl ?: artwork.displayUrl,
                                badge = when (artwork.mediaType) {
                                    MediaType.VIDEO -> "VIDEO"
                                    MediaType.GIF -> "GIF"
                                    else -> null
                                },
                                aspectRatio = 4f / 3f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackHandler(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler {
        onBack()
    }
}