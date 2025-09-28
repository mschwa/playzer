package com.thorfio.playzer.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/** Simple in-memory artwork cache keyed by fileUri */
private object ArtworkCache {
    private val map = ConcurrentHashMap<String, ImageBitmap>()
    fun get(key: String): ImageBitmap? = map[key]
    fun put(key: String, bmp: ImageBitmap) { map[key] = bmp }
}

// LRU cache for artwork bitmaps (ImageBitmap converted) with max entries
private object ArtworkLruCache {
    private const val MAX_ENTRIES = 100
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

@Composable
fun TrackAlbumArt(track: Track?, size: Dp, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val fileUri = track?.fileUri
    var art by remember(fileUri, size) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(fileUri, size) {
        if (fileUri == null) return@LaunchedEffect
        ArtworkLruCache.get(fileUri)?.let { art = it; return@LaunchedEffect }
        val result: ImageBitmap? = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, android.net.Uri.parse(fileUri))
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
    if (art != null) {
        Image(
            bitmap = art!!,
            contentDescription = track?.title ?: "Artwork",
            modifier = modifier.size(size).clip(MaterialTheme.shapes.medium),
            contentScale = contentScale
        )
    } else {
        // Fallback: colored box with first letter
        val letter = (track?.title?.firstOrNull() ?: '?').uppercaseChar()
        Box(
            modifier = modifier
                .size(size)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(letter.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Clip, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
