package com.thorfio.playzer.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Track(
    val id: Long = 0,
    val title: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumTitle: String,
    val durationMs: Long,
    val fileUri: String,
    val trackNumber: Int = 0,
    val dateAdded: Long = Instant.now().toEpochMilli()
)

@Serializable
data class Album(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artistId: String,
    val artistName: String,
    val trackIds: List<Long>,
    val coverTrackId: Long?
)

@Serializable
data class Artist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val albumIds: List<String>,
    val trackIds: List<Long>
)

@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val creationDate: Long = Instant.now().toEpochMilli(),
    val lastUpdated: Long = creationDate,
    val coverTrackMediaStoreId: Long? = null,
    val mediaStoreIds: List<Long> = emptyList()
)

@Serializable
data class PlaylistsDocument(
    val playlists: List<Playlist> = emptyList()
)
