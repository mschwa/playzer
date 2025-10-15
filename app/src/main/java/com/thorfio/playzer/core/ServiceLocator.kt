package com.thorfio.playzer.core

import android.content.Context
import com.thorfio.playzer.services.PlaybackService
import com.thorfio.playzer.services.TrackDeletionService
import com.thorfio.playzer.data.persistence.PlaylistStore
import com.thorfio.playzer.data.persistence.AppPreferencesRepository
import com.thorfio.playzer.data.queue.InternalQueue
import com.thorfio.playzer.data.persistence.MusicLibrary
import com.thorfio.playzer.data.persistence.ThemePreferencesRepository

/** Simple manual DI / service locator. */
object ServiceLocator {
    lateinit var appContext: Context
        private set

    val musicLibrary: MusicLibrary by lazy { MusicLibrary() }
    val playlistStore: PlaylistStore by lazy { PlaylistStore(appContext) }
    val internalQueue: InternalQueue by lazy { InternalQueue(appContext) }
    val playbackService: PlaybackService by lazy {
        PlaybackService(appContext, internalQueue)
    }
    val trackDeletionService: TrackDeletionService by lazy {
        TrackDeletionService(
            internalQueue,
            musicLibrary,
            playlistStore
        )
    }
    val themePreferences: ThemePreferencesRepository by lazy { ThemePreferencesRepository(appContext) }
    val appPreferencesRepository: AppPreferencesRepository by lazy { AppPreferencesRepository(appContext) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
