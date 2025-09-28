package com.thorfio.playzer.core

import android.content.Context
import com.thorfio.playzer.data.player.PlaybackController
import com.thorfio.playzer.data.playlist.PlaylistStore
import com.thorfio.playzer.data.queue.InternalQueue
import com.thorfio.playzer.data.repo.MusicRepository
import com.thorfio.playzer.data.prefs.ThemePreferencesRepository

/** Simple manual DI / service locator. */
object ServiceLocator {
    lateinit var appContext: Context
        private set

    val musicRepository: MusicRepository by lazy { MusicRepository() }
    val playlistStore: PlaylistStore by lazy { PlaylistStore(appContext) }
    val internalQueue: InternalQueue by lazy { InternalQueue() }
    val playbackController: PlaybackController by lazy { PlaybackController(appContext, musicRepository, internalQueue) }
    val themePreferences: ThemePreferencesRepository by lazy { ThemePreferencesRepository(appContext) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
