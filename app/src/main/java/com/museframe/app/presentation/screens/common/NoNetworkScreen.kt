package com.museframe.app.presentation.screens.common

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.museframe.app.presentation.components.tv.TvButton
import com.museframe.app.utils.NetworkUtils
import kotlinx.coroutines.delay

@Composable
fun NoNetworkScreen(
    onRetry: () -> Unit = {},
    onNetworkRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }

    // Auto-check network every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            if (NetworkUtils.isNetworkAvailable(context)) {
                onNetworkRestored()
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // WiFi Off Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No Network",
                        modifier = Modifier.size(60.dp),
                        tint = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "No Internet Connection",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Please check your network connection and try again.\nYou can open WiFi settings to connect to a network.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Open WiFi Settings Button
                    TvButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        text = "Network Settings",
                        icon = Icons.Default.Settings,
                        requestInitialFocus = true,
                        colors = com.museframe.app.presentation.components.tv.TvButtonDefaults.colors(
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            contentColor = Color.White,
                            borderColor = MaterialTheme.colorScheme.primary,
                            focusedBackgroundColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor = Color.White,
                            focusedBorderColor = Color.White
                        )
                    )

                    // Retry Button
                    TvButton(
                        onClick = {
                            isChecking = true
                            if (NetworkUtils.isNetworkAvailable(context)) {
                                onRetry()
                            } else {
                                isChecking = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        text = if (isChecking) "Checking..." else "Retry Connection",
                        icon = if (!isChecking) Icons.Default.Refresh else null,
                        enabled = !isChecking,
                        colors = com.museframe.app.presentation.components.tv.TvButtonDefaults.colors(
                            backgroundColor = Color(0xFF424242),
                            contentColor = Color.White,
                            borderColor = Color(0xFF616161),
                            focusedBackgroundColor = Color(0xFF616161),
                            focusedContentColor = Color.White,
                            focusedBorderColor = Color.White,
                            disabledBackgroundColor = Color(0xFF303030),
                            disabledContentColor = Color.Gray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-retry indicator
                Text(
                    text = "Automatically checking connection every 3 seconds...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}