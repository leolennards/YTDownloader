package com.leolennards.ytdownloader.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.ui.theme.ArtShape
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionTween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// reads the cover art out of saved mp3s and a frame out of saved mp4s, and keeps them in memory
object ArtworkCache {
    private val cache = LruCache<String, ImageBitmap>(160)
    // files that have no picture, so they are not read again and again
    private val missing = HashSet<String>()

    fun peek(uri: String, sizePx: Int): ImageBitmap? = cache.get("$uri@$sizePx")

    suspend fun load(context: Context, uri: String, format: MediaFormat, sizePx: Int): ImageBitmap? {
        val key = "$uri@$sizePx"
        cache.get(key)?.let { return it }
        if (key in missing) return null
        val bitmap = withContext(Dispatchers.IO) { read(context, Uri.parse(uri), format, sizePx) }
        if (bitmap == null) {
            missing.add(key)
            return null
        }
        return bitmap.asImageBitmap().also { cache.put(key, it) }
    }

    // a picture from the internet, like a video thumbnail while it downloads
    suspend fun loadRemote(url: String, sizePx: Int): ImageBitmap? {
        val key = "$url@$sizePx"
        cache.get(key)?.let { return it }
        if (key in missing) return null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = java.net.URL(url).openStream().use { it.readBytes() }
                decodeSampled(bytes, sizePx)
            }.getOrNull()
        }
        if (bitmap == null) {
            missing.add(key)
            return null
        }
        return bitmap.asImageBitmap().also { cache.put(key, it) }
    }

    fun forget(uri: String) {
        cache.snapshot().keys.filter { it.startsWith("$uri@") }.forEach { cache.remove(it) }
        missing.removeAll { it.startsWith("$uri@") }
    }

    private fun read(context: Context, uri: Uri, format: MediaFormat, sizePx: Int): Bitmap? {
        if (format == MediaFormat.MP4 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
            runCatching { return context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null) }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            if (format == MediaFormat.MP3) {
                val bytes = retriever.embeddedPicture ?: return null
                decodeSampled(bytes, sizePx)
            } else {
                retriever.getFrameAtTime(1_000_000)
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeSampled(bytes: ByteArray, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}

// the picture for a saved file, with a soft placeholder until it loads (or when there is none)
@Composable
fun Artwork(
    uri: String?,
    format: MediaFormat,
    modifier: Modifier = Modifier,
    shape: Shape = ArtShape,
    loadSize: Dp = 160.dp,
    iconSize: Dp = 22.dp,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { loadSize.roundToPx() }
    val image by produceState(uri?.let { ArtworkCache.peek(it, px) }, uri, px) {
        if (uri != null && value == null) value = ArtworkCache.load(context, uri, format, px)
    }
    ArtworkFrame(image, format, modifier, shape, iconSize)
}

// picture from a link, used for downloads that are still running
@Composable
fun RemoteArtwork(
    url: String?,
    format: MediaFormat,
    modifier: Modifier = Modifier,
    shape: Shape = ArtShape,
    loadSize: Dp = 160.dp,
) {
    val px = with(LocalDensity.current) { loadSize.roundToPx() }
    val image by produceState(url?.let { ArtworkCache.peek(it, px) }, url, px) {
        if (url != null && value == null) value = ArtworkCache.loadRemote(url, px)
    }
    ArtworkFrame(image, format, modifier, shape)
}

// same frame for a picture that is already in memory, like the preview thumbnail
@Composable
fun ArtworkFrame(
    image: ImageBitmap?,
    format: MediaFormat,
    modifier: Modifier = Modifier,
    shape: Shape = ArtShape,
    iconSize: Dp = 22.dp,
    showIcon: Boolean = true,
) {
    val alpha by animateFloatAsState(if (image != null) 1f else 0f, motionTween(), label = "artFade")
    val colors = YtTheme.colors
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(colors.tonal, colors.placeholder)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showIcon && alpha < 1f) {
            YtIcon(format.icon, tint = colors.accentText, size = iconSize)
        }
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

// the main color of a picture, used to tint the player background
fun ImageBitmap.ambientColor(): Color? = runCatching {
    val small = Bitmap.createScaledBitmap(
        this.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false),
        12,
        12,
        true,
    )
    var r = 0L
    var g = 0L
    var b = 0L
    var weight = 0L
    for (x in 0 until 12) for (y in 0 until 12) {
        val c = small.getPixel(x, y)
        val cr = (c shr 16) and 0xFF
        val cg = (c shr 8) and 0xFF
        val cb = c and 0xFF
        // colorful pixels count more than grey ones, so the tint is not just muddy brown
        val w = 1L + (maxOf(cr, cg, cb) - minOf(cr, cg, cb))
        r += cr * w
        g += cg * w
        b += cb * w
        weight += w
    }
    Color((r / weight).toInt(), (g / weight).toInt(), (b / weight).toInt())
}.getOrNull()
