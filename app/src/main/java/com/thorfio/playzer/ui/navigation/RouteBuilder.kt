package com.thorfio.playzer.ui.navigation

object RouteBuilder {
    fun addToPlaylist(trackIds: List<Long>): String =
        if (trackIds.isEmpty()) Routes.ADD_TO_PLAYLIST.replace("{trackIds}", "_")
        else "addToPlaylist/" + trackIds.joinToString(",") { it.toString() }

    fun createPlaylist(trackIds: List<Long> = emptyList()): String =
        if (trackIds.isEmpty()) Routes.CREATE_PLAYLIST.replace("{trackIds}", "_")
        else "createPlaylist/" + trackIds.joinToString(",") { it.toString() }
}
