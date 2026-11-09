package com.vlcsheets.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class TunerTest {

    private fun tone(hz: Double, rate: Int = 44100, n: Int = 4096) =
        FloatArray(n) { i -> (0.5 * sin(2 * PI * hz * i / rate) + 0.2 * sin(4 * PI * hz * i / rate)).toFloat() }

    @Test
    fun findsThePitchOfATone() {
        listOf(82.41, 196.0, 440.0, 1046.5).forEach { hz ->
            assertEquals(hz, detectPitch(tone(hz), 44100)!!.toDouble(), hz * 0.003)
        }
        assertNull(detectPitch(FloatArray(4096), 44100))
    }

    @Test
    fun namesTheNearestNote() {
        assertEquals(TunerNote(9, 4, 0f), noteFor(440f))
        val c4 = noteFor(261.63f)
        assertEquals(0, c4.index)
        assertEquals(4, c4.octave)
        assertEquals(0f, c4.cents, 1f)
        assertEquals(9, noteFor(442f, a4 = 442f).index)
        assertEquals(7.85f, noteFor(442f).cents, 0.1f)
    }
}
