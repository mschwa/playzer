package com.thorfio.playzer.ui.navigation

object RouteBuilder {
    fun addToPlaylist(trackIds: List<String>): String =
        if (trackIds.isEmpty()) Routes.ADD_TO_PLAYLIST.replace("{trackIds}", "_")
        else "addToPlaylist/" + trackIds.joinToString(",") { it }

    fun createPlaylist(trackIds: List<String> = emptyList()): String =
        if (trackIds.isEmpty()) Routes.CREATE_PLAYLIST.replace("{trackIds}", "_")
        else "createPlaylist/" + trackIds.joinToString(",") { it }
}
