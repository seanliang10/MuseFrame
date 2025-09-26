package com.museframe.app.presentation.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.museframe.app.R
import android.content.res.Configuration
import timber.log.Timber

@Composable
fun WelcomeScreen(
    onDevicePaired: () -> Unit,
    onNavigateToNoNetwork: () -> Unit = {},
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shouldNavigateToNoNetwork by viewModel.shouldNavigateToNoNetwork.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Navigate when device is paired
    LaunchedEffect(uiState.isPaired) {
        if (uiState.isPaired) {
            onDevicePaired()
        }
    }

    // Navigate to no network screen when needed
    LaunchedEffect(shouldNavigateToNoNetwork) {
        Timber.d("WelcomeScreen: shouldNavigateToNoNetwork = $shouldNavigateToNoNetwork")
        if (shouldNavigateToNoNetwork) {
            Timber.d("WelcomeScreen: Navigating to NoNetworkScreen")
            onNavigateToNoNetwork()
            viewModel.clearNoNetworkNavigation()
        }
    }

    // Re-initialize when returning from NoNetworkScreen
    LaunchedEffect(shouldNavigateToNoNetwork) {
        // When shouldNavigateToNoNetwork changes from true to false,
        // it means we're returning from NoNetworkScreen
        if (!shouldNavigateToNoNetwork && uiState.pairingQRCode == null && !uiState.isLoading) {
            viewModel.onReturnFromNoNetwork()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(
                id = if (isPortrait) R.drawable.welcome_vertical else R.drawable.welcome_horizontal
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "Welcome to Muse Frame",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scan the QR code with the Muse Frame mobile app to pair this display",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                // Spacer to push QR codes to bottom
                Spacer(modifier = Modifier.weight(1f))

                // Bottom section - QR codes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left side - App download QR codes
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "1. Download App",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // iOS QR
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        uiState.iosQRCode?.let { bitmap ->
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "iOS App QR Code",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } ?: Text(
                                            text = "iOS QR",
                                            color = Color.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "iOS",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }

                            // Android QR
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        uiState.androidQRCode?.let { bitmap ->
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Android App QR Code",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } ?: Text(
                                            text = "Android QR",
                                            color = Color.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Android",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(160.dp)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )

                    // Right side - Pairing QR code
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "2. Scan to Pair",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                uiState.pairingQRCode?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Device Pairing QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: if (uiState.pushyToken == null && !uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Loading...",
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Show Pushy token as ID if available, otherwise show device ID
                        (uiState.pushyToken ?: uiState.deviceId)?.let { id ->
                            Text(
                                text = "ID: $id",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Error handling at the bottom
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        // Show network settings button if needed
                        if (uiState.showNetworkSettings) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    // Open WiFi settings
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Network Settings")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.retryInitialization() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}