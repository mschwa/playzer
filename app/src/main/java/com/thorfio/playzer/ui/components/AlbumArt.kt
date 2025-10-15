package com.thorfio.playzer.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import androidx.core.net.toUri

/** Simple in-memory artwork cache keyed by fileUri */
private object ArtworkCache {
    private val map = ConcurrentHashMap<String, ImageBitmap>()
    fun get(key: String): ImageBitmap? = map[key]
    fun put(key: String, bmp: ImageBitmap) { map[key] = bmp }
}

// LRU cache for regular artwork bitmaps
private object ArtworkLruCache {
    private const val MAX_ENTRIES = 100
    private val map = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean = size > MAX_ENTRIES
    }
    @Synchronized fun get(key: String) = map[key]
    @Synchronized fun put(key: String, value: ImageBitmap) { map[key] = value }
}

// LRU cache specifically for high-quality artwork (for PlayerScreen)
private object HighQualityArtworkCache {
    private const val MAX_ENTRIES = 20  // Fewer entries since these are larger images
    private val map = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean = size > MAX_ENTRIES
    }
    @Synchronized fun get(key: String) = map[key]
    @Synchronized fun put(key: String, value: ImageBitmap) { map[key] = value }
}

private fun decodeDownscaled(bytes: ByteArray, target: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val (w, h) = opts.outWidth to opts.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        while ((w / sample) > target * 2 || (h / sample) > target * 2) sample *= 2
        val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, loadOpts)
    } catch (_: Exception) { null }
}

// Function to decode at full quality or higher quality than standard
private fun decodeHighQuality(bytes: ByteArray): Bitmap? {
    return try {
        // Use higher quality decoding options
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888  // Use high quality color configuration
            inMutable = false  // Immutable for better caching
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (_: Exception) { null }
}

@Composable
fun TrackAlbumArt(
    track: Track? = null,
    album: com.thorfio.playzer.data.model.Album? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    highQuality: Boolean = false,  // Parameter to request high quality images
    squareCorners: Boolean = false  // New parameter to control if corners should be square (90 degrees)
) {
    val context = LocalContext.current
    val repo = ServiceLocator.musicLibrary
    rememberCoroutineScope()

    // Get fileUri from track or from album's first track
    val fileUri: String? = when {
        track != null -> track.fileUri
        album != null -> {
            // Get first track ID from album
            val firstTrackId = album.trackIds.firstOrNull()
            if (firstTrackId != null) {
                // Get the track using the tracksByIds function
                val firstTrack = repo.tracksByIds(listOf(firstTrackId)).firstOrNull()
                firstTrack?.fileUri
            } else {
                null
            }
        }
        else -> null
    }

    // For high quality mode, create a state that can hold both regular and high quality images
    var art by remember(fileUri, size) { mutableStateOf<ImageBitmap?>(null) }
    var highQualityArt by remember(fileUri) { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingHighQuality by remember(fileUri) { mutableStateOf(false) }

    // Effect to load the standard quality image (fast loading)
    LaunchedEffect(fileUri, size) {
        if (fileUri == null) return@LaunchedEffect

        // Try to get from cache first
        ArtworkLruCache.get(fileUri)?.let {
            art = it
            return@LaunchedEffect
        }

        val result: ImageBitmap? = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, fileUri.toUri())
                val bytes = retriever.embeddedPicture
                retriever.release()
                if (bytes != null) {
                    decodeDownscaled(bytes, size.value.toInt().coerceAtLeast(32))?.asImageBitmap()
                } else null
            }.getOrNull()
        }

        result?.let { bmp ->
            art = bmp
            ArtworkLruCache.put(fileUri, bmp)
        }
    }

    // Effect to load high quality image when requested (for player screen)
    LaunchedEffect(fileUri, highQuality) {
        // Only proceed if high quality is requested and we have a file URI
        if (!highQuality || fileUri == null) return@LaunchedEffect

        // Check if we already have the high quality image cached
        HighQualityArtworkCache.get(fileUri)?.let {
            highQualityArt = it
            return@LaunchedEffect
        }

        isLoadingHighQuality = true

        // Load high quality image in background
        val result: ImageBitmap? = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, fileUri.toUri())
                val bytes = retriever.embeddedPicture
                retriever.release()
                if (bytes != null) {
                    decodeHighQuality(bytes)?.asImageBitmap()
                } else null
            }.getOrNull()
        }

        result?.let { bmp ->
            highQualityArt = bmp
            HighQualityArtworkCache.put(fileUri, bmp)
        }

        isLoadingHighQuality = false
    }

    // Show the highest quality image available (high quality if loaded, otherwise regular)
    val displayArt = if (highQuality && highQualityArt != null) highQualityArt else art

    if (displayArt != null) {
        Image(
            bitmap = displayArt,
            contentDescription = track?.title ?: album?.title ?: "Artwork",
            // Only apply shape clipping if squareCorners is false
            modifier = if (squareCorners) {
                modifier
            } else {
                modifier.clip(if (highQuality) MaterialTheme.shapes.large else MaterialTheme.shapes.medium)
            },
            contentScale = contentScale
        )
    } else {
        // Fallback: colored box with first letter
        val title = track?.title ?: album?.title ?: "?"
        val letter = title.firstOrNull()?.uppercaseChar() ?: '?'
        Box(
            // Only apply shape clipping if squareCorners is false
            modifier = if (squareCorners) {
                modifier.background(MaterialTheme.colorScheme.primary)
            } else {
                modifier
                    .clip(if (highQuality) MaterialTheme.shapes.large else MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary)
            },
            contentAlignment = Alignment.Center
        ) {
            Text(letter.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Clip, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
