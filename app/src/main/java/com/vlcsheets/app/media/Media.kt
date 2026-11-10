package com.vlcsheets.app.media

import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.SongKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads and rasterises score documents with Android's PdfRenderer (no extra library).
 */
object PdfTools {

    data class Info(val pageCount: Int, val firstPageAspect: Float)

    fun inspect(file: File): Info? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                val count = renderer.pageCount
                val aspect = if (count > 0) {
                    renderer.openPage(0).use { p -> p.width.toFloat() / p.height.toFloat() }
                } else 0.7071f
                Info(count, aspect)
            }
        }
    }.getOrNull()

    fun renderFirstPage(file: File, targetWidth: Int): Bitmap? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount == 0) return@use null
                renderer.openPage(0).use { page ->
                    renderPage(page, targetWidth)
                }
            }
        }
    }.getOrNull()

    internal fun renderPage(page: PdfRenderer.Page, targetWidth: Int): Bitmap? {
        val width = page.width.coerceAtLeast(1)
        val height = page.height.coerceAtLeast(1)
        val w = targetWidth.coerceIn(200, 2600)
        val h = (w.toFloat() * height / width).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(w, h)
        bitmap.eraseColor(Color.WHITE)
        val matrix = Matrix().apply {
            setScale(w.toFloat() / width, h.toFloat() / height)
        }
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }
}

/** Part of a page, as fractions of its width and height (0..1). */
data class Crop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val FULL = Crop(0f, 0f, 1f, 1f)
    }
}

/** A rendered page and the part of the full page it shows. */
class RenderedPage(val bitmap: Bitmap, val crop: Crop = Crop.FULL)

/**
 * Finds the inked part of a page (ARGB pixels, row by row) plus a small margin.
 * A row or column needs at least 3 dark pixels, so specks of scanner dust do not count.
 */
internal fun contentBounds(pixels: IntArray, w: Int, h: Int): Crop {
    fun dark(c: Int) = ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) < 600
    fun rowInk(y: Int): Boolean {
        var n = 0
        for (x in 0 until w) if (dark(pixels[y * w + x]) && ++n >= 3) return true
        return false
    }
    fun colInk(x: Int, top: Int, bottom: Int): Boolean {
        var n = 0
        for (y in top..bottom) if (dark(pixels[y * w + x]) && ++n >= 3) return true
        return false
    }
    val top = (0 until h).firstOrNull(::rowInk) ?: return Crop.FULL
    val bottom = (h - 1 downTo top).first(::rowInk)
    val left = (0 until w).firstOrNull { colInk(it, top, bottom) } ?: return Crop.FULL
    val right = (w - 1 downTo left).first { colInk(it, top, bottom) }
    val pad = 0.02f
    return Crop(
        (left.toFloat() / w - pad).coerceAtLeast(0f),
        (top.toFloat() / h - pad).coerceAtLeast(0f),
        ((right + 1f) / w + pad).coerceAtMost(1f),
        ((bottom + 1f) / h + pad).coerceAtMost(1f),
    )
}

/** Cuts the white margins off [src]; returns the original page if there is nothing to cut. */
internal fun cropMargins(src: Bitmap): RenderedPage {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)
    val crop = contentBounds(pixels, w, h)
    if (crop.width > 0.97f && crop.height > 0.97f) return RenderedPage(src)
    val x = (crop.left * w).toInt()
    val y = (crop.top * h).toInt()
    val cut = Bitmap.createBitmap(src, x, y, ((crop.right * w).toInt() - x).coerceAtLeast(1), ((crop.bottom * h).toInt() - y).coerceAtLeast(1))
    if (cut != src) src.recycle()
    return RenderedPage(cut, crop)
}

object ImageTools {

    fun decode(file: File, targetWidth: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0) return@runCatching null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        var bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@runCatching null
        bmp = applyExifRotation(file, bmp)
        if (bmp.width > targetWidth) {
            val ratio = targetWidth.toFloat() / bmp.width
            val scaled = bmp.scale(targetWidth, (bmp.height * ratio).toInt().coerceAtLeast(1))
            if (scaled != bmp) bmp.recycle()
            bmp = scaled
        }
        bmp
    }.getOrNull()

    fun aspect(file: File): Float = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val rotation = exifRotation(file)
        val w = if (rotation % 180 == 0) bounds.outWidth else bounds.outHeight
        val h = if (rotation % 180 == 0) bounds.outHeight else bounds.outWidth
        if (w > 0 && h > 0) w.toFloat() / h.toFloat() else 0.7071f
    }.getOrDefault(0.7071f)

    private fun exifRotation(file: File): Int = runCatching {
        when (ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
        val degrees = exifRotation(file)
        if (degrees == 0) return bitmap
        return runCatching {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        }.getOrDefault(bitmap)
    }
}

/**
 * Rasterises the pages of one song. One instance per open viewer; the caller
 * must invoke [close] when the viewer is disposed.
 */
class DocumentRenderer(private val context: Context, private val song: Song) {

    private val mutex = Mutex()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    @Volatile private var closed = false

    val pageCount: Int get() = song.pageCount

    private fun folder(): File = File(File(context.filesDir, "library/songs"), song.id)

    suspend fun aspect(page: Int): Float = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock song.pageAspect
            when (song.kind) {
                SongKind.IMAGE -> ImageTools.aspect(File(folder(), song.fileName))
                SongKind.PDF -> runCatching {
                    ensureOpen()
                    renderer?.openPage(page.coerceIn(0, pageCount - 1))?.use { p ->
                        p.width.toFloat() / p.height.toFloat()
                    }
                }.getOrNull() ?: song.pageAspect
            }
        }.also { releaseIfClosed() }
    }

    suspend fun render(page: Int, targetWidth: Int, crop: Boolean = false): RenderedPage? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock null
            runCatching {
                val bitmap = when (song.kind) {
                    SongKind.IMAGE -> ImageTools.decode(File(folder(), song.fileName), targetWidth)
                    SongKind.PDF -> {
                        ensureOpen()
                        val r = renderer ?: return@runCatching null
                        val index = page.coerceIn(0, r.pageCount - 1)
                        r.openPage(index).use { p -> PdfTools.renderPage(p, targetWidth) }
                    }
                } ?: return@runCatching null
                if (crop) cropMargins(bitmap) else RenderedPage(bitmap)
            }.getOrNull()
        }.also { releaseIfClosed() }
    }

    private fun ensureOpen() {
        if (renderer != null) return
        val file = File(folder(), song.fileName)
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor = fd
        renderer = PdfRenderer(fd)
    }

    /** Safe to call while a page is rendering: the PDF is freed when that render ends. */
    fun close() {
        closed = true
        releaseIfClosed()
    }

    private fun releaseIfClosed() {
        if (!closed || !mutex.tryLock()) return
        try {
            release()
        } finally {
            mutex.unlock()
        }
    }

    private fun release() {
        runCatching { renderer?.close() }
        runCatching { descriptor?.close() }
        renderer = null
        descriptor = null
    }
}

/** Small in-memory LRU used for list thumbnails. */
object ThumbnailCache {
    private val cache = object : LinkedHashMap<String, Bitmap>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 20
    }

    @Synchronized
    fun get(key: String): Bitmap? = cache[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    @Synchronized
    fun clear() = cache.clear()
}
