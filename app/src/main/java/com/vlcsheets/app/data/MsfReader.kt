package com.vlcsheets.app.data

import java.nio.ByteBuffer

/**
 * Reads a .msf file: big-endian records marked with 0xFFFF + a tag; embedded files are
 * [i64 size][u16 path length][UTF-8 path][bytes].
 * shortcut: only titles, pen and line annotations and files; extend with a sample that has more.
 */
object MsfReader {

    data class MsfFile(val name: String, val offset: Int, val size: Int)

    /** Points are normalised (0..1); width is already in the app's units. */
    data class MsfStroke(
        val page: Int,
        val points: List<Pt>,
        val color: Long,
        val width: Float,
        val highlighter: Boolean,
    )

    data class MsfSong(
        val title: String,
        val files: MutableList<MsfFile> = mutableListOf(),
        val strokes: MutableList<MsfStroke> = mutableListOf(),
    )

    private const val TAG_SONG = 0x0120
    private const val TAG_PAGE = 0x0121
    private const val TAG_ITEM = 0x0126
    private const val TAG_ITEM_END = 0x0135
    private const val MARK: Short = -1 // 0xFFFF

    /** [buf] must be big-endian (the ByteBuffer default). */
    fun read(buf: ByteBuffer): List<MsfSong> {
        val songs = mutableListOf<MsfSong>()
        var page = 0
        var i = 0
        while (i < buf.limit() - 12) {
            val file = fileAt(buf, i)
            if (file != null) {
                songs.lastOrNull()?.files?.add(file)
                i = file.offset + file.size
                continue
            }
            if (buf.getShort(i) == MARK) {
                when (u16(buf, i + 2)) {
                    TAG_SONG -> {
                        val len = u16(buf, i + 4)
                        songs += MsfSong(utf8(buf, i + 6, len).trim())
                        i += 6 + len
                        continue
                    }
                    TAG_PAGE -> {
                        page = buf.getInt(i + 4)
                        i += 8
                        continue
                    }
                    TAG_ITEM -> {
                        val end = readItem(buf, i + 4, page, songs.lastOrNull())
                        if (end > 0) {
                            i = end
                            continue
                        }
                    }
                }
            }
            i++
        }
        return songs
    }

    private fun fileAt(buf: ByteBuffer, i: Int): MsfFile? {
        val size = buf.getLong(i)
        val len = u16(buf, i + 8)
        val start = i + 10 + len
        if (size <= 0 || len !in 2..1024 || start + size > buf.limit()) return null
        if (buf.get(i + 10) != '/'.code.toByte()) return null
        val name = utf8(buf, i + 10, len).substringAfterLast('/')
        if (!name.contains('.') || name == "..") return null
        return MsfFile(name, start, size.toInt())
    }

    /** Returns the index after the item, or -1 if this is not an item we can read. */
    private fun readItem(buf: ByteBuffer, start: Int, page: Int, song: MsfSong?): Int = try {
        var p = start + 4 // unknown int
        p += 1 // type: 1 = pen, 6 = straight line; both share this layout
        val alpha = buf.get(p).toInt() and 0xFF
        p += 1
        p += 8 // screen pixels per page point when drawn (x and y)
        p += 4 // unknown int
        val pageW = buf.getFloat(p)
        val pageH = buf.getFloat(p + 4)
        p += 8
        val twoColors = buf.get(p).toInt() != 0
        p += 1
        val color = buf.getInt(p).toLong() and 0xFFFFFFFFL
        p += if (twoColors) 8 else 4
        val width = buf.getFloat(p) // in page points
        p += 4
        val segments = buf.getInt(p)
        val count = buf.getInt(p + 4)
        p += 8
        if (count < 0 || p + 4L * count + 4 > buf.limit() || pageW <= 0f || pageH <= 0f) {
            -1
        } else {
            val floats = FloatArray(count) { buf.getFloat(p + 4 * it) }
            p += 4 * count
            if (buf.getShort(p) != MARK || u16(buf, p + 2) != TAG_ITEM_END) {
                -1
            } else {
                if (song != null) {
                    val highlighter = alpha < 100
                    // The app draws width/1000 of the page width (x2.6 for the highlighter).
                    val appWidth = width / pageW * 800f / if (highlighter) 2.6f else 1f
                    for (raw in lines(floats, segments)) {
                        val line = if (segments > 0) bezier(raw) else raw
                        val pts = line.map { (x, y) -> Pt((x / pageW).coerceIn(0f, 1f), (y / pageH).coerceIn(0f, 1f)) }
                        song.strokes += MsfStroke(
                            page = page,
                            points = if (pts.size == 1) pts + pts else pts,
                            color = color or 0xFF000000L,
                            width = appWidth,
                            highlighter = highlighter,
                        )
                    }
                }
                p + 4
            }
        }
    } catch (_: IndexOutOfBoundsException) {
        -1
    }

    /**
     * Lines are x,y pairs. With segments > 0 each segment is [?, float count, ?, floats...] and a
     * pair of Float.MAX_VALUE lifts the pen; with 0 the floats are the points themselves.
     */
    private fun lines(f: FloatArray, segments: Int): List<List<Pair<Float, Float>>> {
        val out = mutableListOf<List<Pair<Float, Float>>>()
        val ranges = if (segments == 0) listOf(0 until f.size) else buildList {
            var k = 0
            repeat(segments) {
                if (k + 3 > f.size) return@repeat
                val cnt = f[k + 1].toInt().coerceIn(0, f.size - k - 3)
                add(k + 3 until k + 3 + cnt)
                k += 3 + cnt
            }
        }
        for (range in ranges) {
            var cur = mutableListOf<Pair<Float, Float>>()
            var j = range.first
            while (j + 1 <= range.last) {
                if (f[j] > 1e37f) {
                    if (cur.isNotEmpty()) out += cur
                    cur = mutableListOf()
                } else {
                    cur += f[j] to f[j + 1]
                }
                j += 2
            }
            if (cur.isNotEmpty()) out += cur
        }
        return out
    }

    /**
     * Pen strokes are stored as chains of cubic Bézier curves (start, then control, control, end
     * for each curve). The app draws polylines, so sample each curve.
     */
    internal fun bezier(p: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (p.size < 4 || (p.size - 1) % 3 != 0) return p
        val out = mutableListOf(p[0])
        for (c in 0 until (p.size - 1) / 3) {
            val (x0, y0) = p[3 * c]
            val (x1, y1) = p[3 * c + 1]
            val (x2, y2) = p[3 * c + 2]
            val (x3, y3) = p[3 * c + 3]
            for (s in 1..BEZIER_STEPS) {
                val t = s.toFloat() / BEZIER_STEPS
                val u = 1 - t
                val a = u * u * u
                val b = 3 * u * u * t
                val d = 3 * u * t * t
                val e = t * t * t
                out += (a * x0 + b * x1 + d * x2 + e * x3) to (a * y0 + b * y1 + d * y2 + e * y3)
            }
        }
        return out
    }

    private const val BEZIER_STEPS = 16

    private fun u16(buf: ByteBuffer, i: Int) = buf.getShort(i).toInt() and 0xFFFF

    private fun utf8(buf: ByteBuffer, i: Int, len: Int): String {
        val bytes = ByteArray(len)
        buf.duplicate().apply { position(i) }.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
