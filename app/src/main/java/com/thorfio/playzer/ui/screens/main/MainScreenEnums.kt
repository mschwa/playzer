package com.thorfio.playzer.ui.screens.main

/**
 * Enum defining the main tabs in the music library
 */
enum class MainTab { TRACKS, PLAYLISTS, ALBUMS, ARTISTS }

/**
 * Enum defining the possible sort fields for the track list
 */
enum class TrackSortField { TITLE, DATE_ADDED, ALBUM, ARTIST }

/**
 * Enum defining the general sort order (ascending or descending)
 */
enum class SortOrder { ASC, DESC }

/**
 * Enum defining the sort order for albums
 */
enum class AlbumSortOrder { ASC, DESC }

/**
 * Enum defining the sort order for artists
 */
enum class ArtistSortOrder { ASC, DESC }
