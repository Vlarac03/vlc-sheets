package com.vlcsheets.app.media

import com.vlcsheets.app.data.AudioMark
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.songFromJson
import com.vlcsheets.app.data.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PracticeTest {

    @Test
    fun audioMarksPickTheLastPassedPage() {
        val marks = listOf(AudioMark("a.mp3", 30_000, 2), AudioMark("a.mp3", 10_000, 1))
        assertNull(pageForPosition(marks, 5_000))
        assertEquals(1, pageForPosition(marks, 10_000))
        assertEquals(1, pageForPosition(marks, 29_999))
        assertEquals(2, pageForPosition(marks, 90_000))
    }

    @Test
    fun loopJumpsBackOnlyAtItsEnd() {
        assertNull(loopJump(4_000, 2_000, 5_000))
        assertEquals(2_000, loopJump(5_000, 2_000, 5_000))
        assertNull(loopJump(9_000, null, 5_000))
        assertNull(loopJump(9_000, 6_000, 5_000))
    }

    @Test
    fun slowerSpeedSlowsTheMetronome() {
        assertEquals(70, effectiveBpm(100, 0.7f))
        assertEquals(120, effectiveBpm(120, 1f))
    }

    @Test
    fun metronomeTurnsAfterCountInAndEveryNBars() {
        // 4/4, one bar of count-in, a page every 2 bars.
        assertEquals(0, metronomeTurns(-1, 4, 1, 2))
        assertEquals(0, metronomeTurns(3, 4, 1, 2)) // still counting in
        assertEquals(0, metronomeTurns(11, 4, 1, 2)) // last beat of bar 2
        assertEquals(1, metronomeTurns(12, 4, 1, 2)) // first beat of bar 3
        assertEquals(2, metronomeTurns(20, 4, 1, 2))
        assertEquals(1, metronomeTurns(8, 4, 0, 2)) // no count-in
        assertEquals(0, metronomeTurns(500, 4, 1, 0)) // turning off
    }

    @Test
    fun practiceFieldsSurviveSaving() {
        val song = Song(
            id = "s",
            title = "T",
            audioMarks = listOf(AudioMark("a.mp3", 1234, 3)),
            barsPerPage = 8,
            countInBars = 2,
        )
        val back = songFromJson(song.toJson())
        assertEquals(song.audioMarks, back.audioMarks)
        assertEquals(8, back.barsPerPage)
        assertEquals(2, back.countInBars)
        // Older libraries without these fields get the defaults.
        val old = songFromJson(org.json.JSONObject().put("id", "o").put("title", "O"))
        assertEquals(emptyList<AudioMark>(), old.audioMarks)
        assertEquals(0, old.barsPerPage)
        assertEquals(1, old.countInBars)
    }
}
