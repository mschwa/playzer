package com.thorfio.playzer.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore-based audio file scanner - replaces DocumentFile approach
 */
object MediaStoreAudioClient {
    private const val TAG = "MediaStoreAudioClient"

    /**
     * Helper data class for tracking album information during scanning
     */
    data class AlbumInfo(
        val id: String,
        val title: String,
        val artistName: String,
        val trackIds: MutableList<Long>
    )

    data class ArtistInfo(
        val id: String,
        val name: String,
        val trackIds: MutableList<Long>,
        val albumIds: MutableList<String>
    )

    /**
     * Scans for audio files using MediaStore API
     */
    suspend fun getObjectsFromMediaStore(context: Context): Triple<List<Track>, List<Album>, List<Artist>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val artistMap = mutableMapOf<String, ArtistInfo>()
        val albumMap = mutableMapOf<String, AlbumInfo>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                Log.d(TAG, "Found ${cursor.count} audio files in MediaStore")
                while (cursor.moveToNext()) {
                    processMediaStoreCursor(cursor, tracks, artistMap, albumMap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning with MediaStore", e)
        }

        // Create albums and artists from the collected data
        val albums = createAlbumsFromMap(albumMap)
        val artists = createArtistsFromMap(artistMap)

        Log.d(TAG, "Scan complete: ${tracks.size} tracks, ${albums.size} albums, ${artists.size} artists")
        return@withContext Triple(tracks, albums, artists)
    }

    /**
     * Processes a MediaStore cursor row to create Track, Artist, and Album data
     */
    private fun processMediaStoreCursor(
        cursor: Cursor,
        tracks: MutableList<Track>,
        artistMap: MutableMap<String, ArtistInfo>,
        albumMap: MutableMap<String, AlbumInfo>
    ) {
        try {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
            val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown Artist"
            val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)) ?: "Unknown Album"
            val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
            val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
            val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)) * 1000 // Convert to milliseconds

            // Create track with MediaStore content URI
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            val trackId = id
            val artistId = artist.toArtistId()
            val albumId = "$album:$artist".toAlbumId()

            val track = Track(
                id = trackId,
                title = title,
                artistId = artistId,
                artistName = artist,
                albumId = albumId,
                albumTitle = album,
                durationMs = duration,
                fileUri = contentUri.toString(),
                trackNumber = trackNumber,
                dateAdded = dateAdded
            )

            tracks.add(track)

            val artistInfo = artistMap.getOrPut(artist) {
               ArtistInfo(artistId, artist, mutableListOf(), mutableListOf())
            }
            if(!artistInfo.trackIds.contains(trackId)) artistInfo.trackIds.add(trackId)
            if (!artistInfo.albumIds.contains(albumId)) artistInfo.albumIds.add(albumId)

            val albumKey = "$album:$artist"
            val albumInfo = albumMap.getOrPut(albumKey) {
                AlbumInfo(albumId, album, artist, mutableListOf())
            }
            if(!albumInfo.trackIds.contains(trackId)) albumInfo.trackIds.add(trackId)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing MediaStore cursor", e)
        }
    }

    /**
     * Creates albums from the album map
     */
    private fun createAlbumsFromMap(albumMap: Map<String, AlbumInfo>): List<Album> {
        return albumMap.values.map { albumInfo ->
            Album(
                id = albumInfo.id,
                title = albumInfo.title,
                artistId = albumInfo.artistName.toArtistId(),
                artistName = albumInfo.artistName,
                trackIds = albumInfo.trackIds.toList(),
                coverTrackId = albumInfo.trackIds.firstOrNull()
            )
        }
    }

    /**
     * Creates artists from the artist map and links them to albums
     */
    private fun createArtistsFromMap(artistMap: Map<String, ArtistInfo>): List<Artist> {
        return artistMap.values.map { artistInfo ->
            Artist(
                id = artistInfo.id,
                name = artistInfo.name,
                albumIds = artistInfo.albumIds.toList(),
                trackIds = artistInfo.trackIds.toList(),
            )
        }
    }

    /**
     * Queries MediaStore for a specific audio file by URI
     */
    suspend fun getTrackByUri(context: Context, uri: Uri): Track? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED
        )

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown Artist"
                    val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)) ?: "Unknown Album"
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)) * 1000

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    return@withContext Track(
                        id = id,
                        title = title,
                        artistId = artist.toArtistId(),
                        artistName = artist,
                        albumId = "$album:$artist".toAlbumId(),
                        albumTitle = album,
                        durationMs = duration,
                        fileUri = contentUri.toString(),
                        trackNumber = trackNumber,
                        dateAdded = dateAdded
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying track by URI: $uri", e)
        }
        return@withContext null
    }
}

// Extension functions for creating consistent IDs
fun String.toArtistId(): String = "artist_${this.hashCode()}"
fun String.toAlbumId(): String = "album_${this.hashCode()}"
