package com.vlcsheets.app.media

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vlcsheets.app.data.ChordLine
import com.vlcsheets.app.data.ChordProSong
import java.io.File
import java.io.FileOutputStream

/**
 * Lays out a text or ChordPro song as an A4 PDF, so it opens in the normal viewer
 * and can be annotated like any score. Monospace keeps each chord over its letter.
 */
object ChordProPdf {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 42f
    private const val CHORUS_INDENT = 18f

    fun write(song: ChordProSong, fallbackTitle: String, dest: File) {
        val mono = Typeface.MONOSPACE
        val longest = song.lines.maxOfOrNull { maxOf(it.chords.length, it.text.length) } ?: 0
        // Shrink the text for long lines so nothing is cut off at the right edge (0.6 = monospace glyph width).
        val usable = PAGE_W - 2 * MARGIN - CHORUS_INDENT
        val size = if (longest == 0) 12f else (usable / (longest * 0.6f)).coerceIn(7f, 12f)
        val lineH = size * 1.3f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = size; color = 0xFF111111.toInt() }
        val chordPaint = Paint(textPaint).apply { typeface = Typeface.create(mono, Typeface.BOLD); color = 0xFF1F4E9E.toInt() }
        val commentPaint = Paint(textPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC); color = 0xFF555555.toInt() }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 20f }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = 0xFF555555.toInt() }
        val barPaint = Paint().apply { color = 0xFFBBBBBB.toInt(); strokeWidth = 1.5f }

        val doc = PdfDocument()
        try {
            var pageNo = 0
            var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create())
            var y = MARGIN + 20f
            page.canvas.drawText(song.title.ifBlank { fallbackTitle }, MARGIN, y, titlePaint)
            if (song.artist.isNotBlank()) {
                y += 18f
                page.canvas.drawText(song.artist, MARGIN, y, artistPaint)
            }
            y += lineH * 1.5f

            for (line in song.lines) {
                val needed = if (line.chords.isNotBlank()) 2 * lineH else lineH
                if (y + needed > PAGE_H - MARGIN) {
                    doc.finishPage(page)
                    page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create())
                    y = MARGIN + lineH
                }
                val x = if (line.style == ChordLine.Style.CHORUS) MARGIN + CHORUS_INDENT else MARGIN
                if (line.style == ChordLine.Style.CHORUS) {
                    page.canvas.drawLine(MARGIN + 4f, y - lineH * 0.9f, MARGIN + 4f, y + needed - lineH * 0.9f, barPaint)
                }
                if (line.chords.isNotBlank()) {
                    page.canvas.drawText(line.chords, x, y, chordPaint)
                    y += lineH
                }
                page.canvas.drawText(line.text, x, y, if (line.style == ChordLine.Style.COMMENT) commentPaint else textPaint)
                y += lineH
            }
            doc.finishPage(page)
            FileOutputStream(dest).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}
