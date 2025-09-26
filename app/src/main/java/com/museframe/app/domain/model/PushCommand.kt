package com.museframe.app.domain.model

enum class PushCommand(val value: String) {
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    PAUSE("Pause"),
    RESUME("Resume"),
    UPDATE_DISPLAY_SETTING("UpdateDisplaySetting"),
    REFRESH_PLAYLISTS("RefreshPlaylists"),
    REFRESH_PLAYLIST("RefreshPlaylist"),
    UPDATE_PLAYLIST_ARTWORK_SETTING("UpdateSettings"),
    CAST("Cast"),
    CAST_EXHIBITION("CastExhibition"),
    NEXT("Next"),
    PREVIOUS("Previous")
}