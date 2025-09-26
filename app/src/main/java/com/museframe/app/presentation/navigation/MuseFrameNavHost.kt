package com.museframe.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.museframe.app.presentation.screens.artwork.ArtworkScreen
import com.museframe.app.presentation.screens.exhibition.ExhibitionScreenNew
import com.museframe.app.presentation.screens.playlists.PlaylistDetailScreen
import com.museframe.app.presentation.screens.playlists.PlaylistsScreen
import com.museframe.app.presentation.screens.splash.SplashScreen
import com.museframe.app.presentation.screens.versions.VersionsScreen
import com.museframe.app.presentation.screens.welcome.WelcomeScreen

@Composable
fun MuseFrameNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToPlaylists = {
                    navController.navigate(Screen.Playlists.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onDevicePaired = {
                    navController.navigate(Screen.Playlists.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onExhibitionClick = {
                    navController.navigate(Screen.Exhibition.route)
                },
                onVersionsClick = {
                    navController.navigate(Screen.Versions.route)
                },
                onLogoutClick = {
                    // Clear auth and navigate to welcome
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PlaylistDetail.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                playlistId = playlistId,
                onArtworkClick = { artworkId ->
                    navController.navigate(Screen.Artwork.createRoute(playlistId, artworkId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Artwork.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            val artworkId = backStackEntry.arguments?.getString("artworkId") ?: ""
            ArtworkScreen(
                playlistId = playlistId,
                artworkId = artworkId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Exhibition.route) {
            ExhibitionScreenNew(
                exhibitionId = "default", // Default exhibition
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NoNetwork.route) {
            // TODO: Implement NoNetworkScreen
        }

        composable(Screen.Versions.route) {
            VersionsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}