package com.vlcsheets.app.data

import com.vlcsheets.app.media.Crop
import com.vlcsheets.app.media.paperCrop
import com.vlcsheets.app.media.contentBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class ChordProAndCropTest {

    @Test
    fun chordsSitOverTheirLetters() {
        val song = parseChordPro(
            """
            {title: Cançó de prova}
            {artist: Victor}
            # a comment line that is not printed
            [G]Hola [D]món
            {soc}
            [Am]Tor[C]nada
            {eoc}
            {c: Lent}
            Sense acords
            """.trimIndent(),
        )
        assertEquals("Cançó de prova", song.title)
        assertEquals("Victor", song.artist)
        assertEquals(
            listOf(
                ChordLine("G    D", "Hola món"),
                ChordLine("Am C", "Tornada", ChordLine.Style.CHORUS),
                ChordLine("", "Lent", ChordLine.Style.COMMENT),
                ChordLine("", "Sense acords"),
            ),
            song.lines,
        )
    }

    @Test
    fun closeChordsPushTheLyricsRight() {
        // "Am" is wider than "T", so the lyrics move right to keep a space before "C".
        assertEquals(ChordLine("Am C", "T  ornada"), parseChordPro("[Am]T[C]ornada").lines.single())
    }

    @Test
    fun plainTextStaysAsItIs() {
        val song = parseChordPro("Primera línia\n\nTercera\n\n")
        assertEquals(listOf("Primera línia", "", "Tercera"), song.lines.map { it.text })
        assertEquals("", song.title)
    }

    @Test
    fun cropFindsTheInkAndIgnoresSpecks() {
        val w = 100
        val h = 200
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val pixels = IntArray(w * h) { white }
        for (y in 50 until 150) for (x in 20 until 80) pixels[y * w + x] = black
        pixels[5 * w + 5] = black // one speck of dust
        val crop = contentBounds(pixels, w, h)
        assertEquals(0.18f, crop.left, 0.001f)
        assertEquals(0.23f, crop.top, 0.001f)
        assertEquals(0.82f, crop.right, 0.001f)
        assertEquals(0.77f, crop.bottom, 0.001f)
        assertEquals(Crop.FULL, contentBounds(IntArray(w * h) { white }, w, h))
    }

    @Test
    fun cropDropsTheBrowserHeaderAndFooter() {
        val w = 100
        val h = 1000
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val pixels = IntArray(w * h) { white }
        fun ink(rows: IntRange, cols: IntRange) { for (y in rows) for (x in cols) pixels[y * w + x] = black }
        ink(20 until 30, 5 until 95) // date and link, alone at the top
        ink(100 until 900, 10 until 90) // the music
        ink(970 until 980, 5 until 95) // link and "1/1" at the bottom
        val crop = contentBounds(pixels, w, h)
        assertEquals(0.08f, crop.top, 0.001f)
        assertEquals(0.92f, crop.bottom, 0.001f)
        assertEquals(0.08f, crop.left, 0.001f) // the wider header lines no longer set the sides

        // The cropped page keeps the page's shape: the extra height goes below the music.
        val paper = paperCrop(crop, w, h)
        assertEquals(0.08f, paper.top, 0.001f)
        assertEquals(crop.width * w / (paper.height * h), w.toFloat() / h, 0.001f)
        // Music at the foot of the page grows upwards instead, and never leaves the page.
        val low = paperCrop(Crop(0.1f, 0.8f, 0.9f, 0.95f), 100, 141)
        assertEquals(1f, low.bottom, 0.001f)
        assertEquals(0.8f * 100 * 1.41f / 141, low.height, 0.001f)

        // A big title near the top is music, not a header: it stays.
        ink(20 until 60, 30 until 70)
        assertEquals(0.0f, contentBounds(pixels, w, h).top, 0.001f)
    }
}
