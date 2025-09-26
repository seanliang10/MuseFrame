package com.museframe.app.presentation.screens.playlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.museframe.app.domain.model.Playlist
import com.museframe.app.presentation.components.tv.TvButton
import com.museframe.app.presentation.components.tv.TvCard
import com.museframe.app.presentation.components.tv.TvIconButton

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistsScreen(
    onPlaylistClick: (String) -> Unit,
    onExhibitionClick: () -> Unit,
    onVersionsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

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
        // Top bar with frame name and action buttons
        if (isPortrait) {
            // Portrait layout - stack vertically
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Frame name and title
                Text(
                    text = uiState.frameName ?: "Muse Frame",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.playlists.size == 1) {
                        "Playlist (1)"
                    } else {
                        "Playlists (${uiState.playlists.size})"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons in single row for portrait
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.refresh() },
                        text = "Refresh",
                        icon = Icons.Default.Refresh,
                        enabled = !uiState.isLoading,
                        requestInitialFocus = true
                    )
                    TvButton(
                        modifier = Modifier.weight(1f),
                        onClick = onExhibitionClick,
                        text = "Exhibition",
                        icon = Icons.Default.PlayArrow
                    )
                    TvButton(
                        modifier = Modifier.weight(1f),
                        onClick = onVersionsClick,
                        text = "Versions",
                        icon = Icons.Default.Info
                    )
                    TvButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.logout(onSuccess = onLogoutClick)
                        },
                        text = "Logout",
                        icon = Icons.AutoMirrored.Default.ExitToApp
                    )
                }
            }
        } else {
            // Landscape layout - original horizontal layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Frame name and title row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = uiState.frameName ?: "Muse Frame",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.playlists.size == 1) {
                                "Playlist (1)"
                            } else {
                                "Playlists (${uiState.playlists.size})"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp
                        )
                    }

                    // Action buttons in a single row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Refresh button
                        TvButton(
                            onClick = { viewModel.refresh() },
                            text = "Refresh",
                            icon = Icons.Default.Refresh,
                            enabled = !uiState.isLoading,
                            requestInitialFocus = true
                        )

                        // Exhibition button
                        TvButton(
                            onClick = onExhibitionClick,
                            text = "Exhibition",
                            icon = Icons.Default.PlayArrow
                        )

                        // Versions button
                        TvButton(
                            onClick = onVersionsClick,
                            text = "Versions",
                            icon = Icons.Default.Info
                        )

                        // Logout button
                        TvButton(
                            onClick = {
                                viewModel.logout(onSuccess = onLogoutClick)
                            },
                            text = "Logout",
                            icon = Icons.AutoMirrored.Default.ExitToApp
                        )
                    }
                }
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
                            text = "Error loading playlists",
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

            uiState.playlists.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                }
            }

            else -> {
                // Playlists grid - adjust columns based on orientation
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isPortrait) 2 else 4),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.playlists,
                        key = { it.id }
                    ) { playlist ->
                        TvCard(
                            onClick = {
                                viewModel.selectPlaylist(playlist)
                                onPlaylistClick(playlist.id)
                            },
                            title = playlist.name,
                            subtitle = playlist.description,
                            imageUrl = playlist.thumbnailUrl,
                            badge = if (playlist.artworkCount > 0) "${playlist.artworkCount}" else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        }
    }
}