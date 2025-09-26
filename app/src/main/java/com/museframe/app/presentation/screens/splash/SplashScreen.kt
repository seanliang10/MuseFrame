package com.museframe.app.presentation.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.museframe.app.BuildConfig
import com.museframe.app.R

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation
    LaunchedEffect(uiState.shouldNavigate, uiState.isAuthenticated) {
        if (uiState.shouldNavigate) {
            if (uiState.isAuthenticated) {
                onNavigateToPlaylists()
            } else {
                onNavigateToWelcome()
            }
            viewModel.onNavigationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "Muse Frame Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Muse Frame",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // Version info at bottom
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}