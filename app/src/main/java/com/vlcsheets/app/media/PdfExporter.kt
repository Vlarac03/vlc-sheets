package com.vlcsheets.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Writes a song as a new PDF with its covered areas and annotations drawn on each page.
 * Pages go out one by one as JPEG images: Android's PdfDocument keeps every page in memory
 * until the end, which runs out of memory on long scores.
 */
object PdfExporter {

    private const val PAGE_WIDTH_PT = 595 // A4 width; the height follows each page's shape
    private const val RENDER_WIDTH_PX = 1600

    suspend fun export(context: Context, song: Song, out: OutputStream) = withContext(Dispatchers.IO) {
        val renderer = DocumentRenderer(context, song)
        try {
            val pdf = JpegPdfWriter(out)
            for (index in 0 until song.pageCount) {
                val rendered = renderer.render(index, RENDER_WIDTH_PX)?.bitmap
                    ?: throw IllegalStateException("Cannot render page ${index + 1}")
                val bitmap = if (rendered.isMutable) rendered else rendered.copy(Bitmap.Config.ARGB_8888, true).also { rendered.recycle() }
                val canvas = Canvas(bitmap)
                val w = bitmap.width.toFloat()
                val h = bitmap.height.toFloat()
                val paper = Paint().apply { color = android.graphics.Color.WHITE }
                song.masks.filter { it.page == index }.forEach { m -> canvas.drawRect(m.left * w, m.top * h, m.right * w, m.bottom * h, paper) }
                song.annotations.filter { it.page == index }.forEach { drawStroke(canvas, it, w, h) }
                val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
                val heightPt = (PAGE_WIDTH_PT * h / w).roundToInt().coerceAtLeast(1)
                pdf.addPage(PAGE_WIDTH_PT, heightPt, bitmap.width, bitmap.height, jpeg)
                bitmap.recycle()
            }
            pdf.finish()
        } finally {
            renderer.close()
        }
    }

    // Same look as the viewer: width relative to the page, highlighter wider and see-through.
    private fun drawStroke(canvas: android.graphics.Canvas, stroke: Stroke, width: Float, height: Float) {
        if (stroke.text.isNotEmpty()) return drawTextAnnotation(canvas, stroke, width, height)
        if (stroke.points.size < 2) return
        val path = Path()
        stroke.points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x * width, p.y * height) else path.lineTo(p.x * width, p.y * height)
        }
        val base = stroke.width * (width / 1000f) * 1.25f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = stroke.color.toInt()
            if (stroke.highlighter) alpha = (0.32f * 255).roundToInt()
            strokeWidth = if (stroke.highlighter) base * 2.6f else base
        }
        canvas.drawPath(path, paint)
    }
}

/** Draws handwriting turned into text so it fills the box where it was written. Used by the viewer and the PDF export. */
fun drawTextAnnotation(canvas: android.graphics.Canvas, stroke: Stroke, width: Float, height: Float) {
    if (stroke.points.size < 2) return
    val left = stroke.points[0].x * width
    val top = stroke.points[0].y * height
    val right = stroke.points[1].x * width
    val bottom = stroke.points[1].y * height
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stroke.color.toInt()
        textSize = 100f
    }
    val ink = Rect()
    paint.getTextBounds(stroke.text, 0, stroke.text.length, ink)
    if (ink.height() <= 0) return
    // Same height as the handwriting, centred on it, sitting on its bottom edge.
    val scale = (bottom - top).coerceAtLeast(1f) / ink.height()
    paint.textSize = 100f * scale
    val x = (left + right) / 2f - (ink.left + ink.width() / 2f) * scale
    val y = bottom - ink.bottom * scale
    canvas.drawText(stroke.text, x, y, paint)
}

/** A minimal PDF file with one full-page JPEG per page, written as it goes. */
class JpegPdfWriter(private val out: OutputStream) {
    private val offsets = sortedMapOf<Int, Long>()
    private val pages = mutableListOf<Int>()
    private var pos = 0L

    init {
        write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".toByteArray(Charsets.ISO_8859_1))
    }

    fun addPage(widthPt: Int, heightPt: Int, widthPx: Int, heightPx: Int, jpeg: ByteArray) {
        val n = 3 + 3 * pages.size // 1 and 2 are the catalog and the page list, written at the end
        obj(n, "<< /Type /XObject /Subtype /Image /Width $widthPx /Height $heightPx /ColorSpace /DeviceRGB " +
            "/BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>", jpeg)
        val content = "q $widthPt 0 0 $heightPt 0 0 cm /Im0 Do Q".toByteArray()
        obj(n + 1, "<< /Length ${content.size} >>", content)
        obj(n + 2, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $widthPt $heightPt] " +
            "/Resources << /XObject << /Im0 $n 0 R >> >> /Contents ${n + 1} 0 R >>")
        pages += n + 2
    }

    fun finish() {
        obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        obj(2, "<< /Type /Pages /Kids [${pages.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>")
        val xref = pos
        val size = offsets.lastKey() + 1
        val table = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
        // Each line must be exactly 20 bytes, with ASCII digits whatever the phone's language.
        for (i in 1 until size) table.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.getValue(i)))
        table.append("trailer\n<< /Size $size /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        write(table.toString().toByteArray())
        out.flush()
    }

    private fun obj(n: Int, dict: String, stream: ByteArray? = null) {
        offsets[n] = pos
        write("$n 0 obj\n$dict\n".toByteArray())
        if (stream != null) {
            write("stream\n".toByteArray())
            write(stream)
            write("\nendstream\n".toByteArray())
        }
        write("endobj\n".toByteArray())
    }

    private fun write(bytes: ByteArray) {
        out.write(bytes)
        pos += bytes.size
    }
}
