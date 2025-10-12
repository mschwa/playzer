package com.thorfio.playzer.ui.navigation

object Routes {
    const val MAIN = "main"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val PLAYLIST = "playlist/{playlistId}"
    const val ARTIST = "artist/{artistId}"
    const val ALBUM = "album/{albumId}"
    const val ADD_TO_PLAYLIST = "addToPlaylist/{trackIds}" // trackIds comma separated
    const val CREATE_PLAYLIST = "createPlaylist/{trackIds}" // optional trackIds comma separated (may be empty)
    const val SETTINGS = "settings"
    const val EQUALIZER = "equalizer"
    const val EVENT_LOG = "eventLog"
}
