package com.museframe.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    object Artwork : Screen("artwork/{playlistId}/{artworkId}") {
        fun createRoute(playlistId: String, artworkId: String) = "artwork/$playlistId/$artworkId"
    }
    object Exhibition : Screen("exhibition")
    object NoNetwork : Screen("no_network")
    object Versions : Screen("versions")
}