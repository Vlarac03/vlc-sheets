package com.vlcsheets.app.data

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer

class MsfReaderTest {

    @Test
    fun readsTitleStrokesAndFiles() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            write(ByteArray(16)) // header we do not read
            val title = "Swan - Saint-Saëns".toByteArray()
            writeShort(0xFFFF); writeShort(0x0120); writeShort(title.size); write(title)
            writeShort(0xFFFF); writeShort(0x0121); writeInt(1); writeInt(1) // page 2
            // Pen stroke: one segment, two points, pen up.
            writeShort(0xFFFF); writeShort(0x0126)
            writeInt(0); writeByte(1); writeByte(100); writeFloat(2.5f); writeFloat(2.5f); writeInt(3)
            writeFloat(600f); writeFloat(800f); writeByte(0); writeInt(0xFF102030.toInt()); writeFloat(0.8f)
            writeInt(1); writeInt(9)
            listOf(0f, 6f, 0.8f, 60f, 80f, 300f, 400f, Float.MAX_VALUE, Float.MAX_VALUE).forEach(::writeFloat)
            writeShort(0xFFFF); writeShort(0x0135)
            // Embedded file.
            val path = "/storage/emulated/0/a.pdf".toByteArray()
            writeLong(4); writeShort(path.size); write(path); write("%PDF".toByteArray())
            write(ByteArray(16))
        }
        val songs = MsfReader.read(ByteBuffer.wrap(bytes.toByteArray()))

        assertEquals(1, songs.size)
        val song = songs[0]
        assertEquals("Swan - Saint-Saëns", song.title)
        assertEquals(listOf("a.pdf"), song.files.map { it.name })
        assertEquals(4, song.files[0].size)
        assertEquals(1, song.strokes.size)
        val stroke = song.strokes[0]
        assertEquals(1, stroke.page)
        assertEquals(listOf(Pt(0.1f, 0.1f), Pt(0.5f, 0.5f)), stroke.points)
        assertEquals(0xFF102030L, stroke.color)
        assertEquals(0.8f / 600f * 800f, stroke.width, 1e-4f)
    }

    @Test
    fun samplesBezierCurves() {
        val curve = MsfReader.bezier(listOf(0f to 0f, 0f to 10f, 10f to 10f, 10f to 0f))
        assertEquals(17, curve.size)
        assertEquals(0f to 0f, curve.first())
        assertEquals(10f to 0f, curve.last())
        assertEquals(5f, curve[8].first, 1e-4f) // t = 0.5 lies in the middle
        assertEquals(7.5f, curve[8].second, 1e-4f)
    }

    /** Run with -DmsfSample=<path to a real .msf> to check a MobileSheets export. */
    @Test
    fun readsRealExportIfGiven() {
        val path = System.getProperty("msfSample")
        assumeTrue(path != null && File(path).exists())
        val songs = MsfReader.read(ByteBuffer.wrap(File(path!!).readBytes()))
        songs.forEach { s ->
            println("${s.title}: ${s.files.map { "${it.name} (${it.size})" }}, ${s.strokes.size} strokes, pages ${s.strokes.map { it.page }.toSet()}")
        }
        assumeTrue(songs.isNotEmpty())
    }
}
