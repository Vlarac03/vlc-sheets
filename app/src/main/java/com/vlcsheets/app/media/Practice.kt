package com.vlcsheets.app.media

import com.vlcsheets.app.data.AudioMark
import kotlin.math.roundToInt

/** Speeds offered for practice; 1 = normal. */
val practiceSpeeds = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f)

/** The page the audio should be showing at [positionMs], or null before the first mark. */
fun pageForPosition(marks: List<AudioMark>, positionMs: Int): Int? =
    marks.filter { it.ms <= positionMs }.maxByOrNull { it.ms }?.page

/** Where to jump back to when the loop end has been reached, or null to keep playing. */
fun loopJump(positionMs: Int, loopStart: Int?, loopEnd: Int?): Int? =
    if (loopStart != null && loopEnd != null && loopEnd > loopStart && positionMs >= loopEnd) loopStart else null

/** The tempo the metronome really plays at when practising at [speed]. */
fun effectiveBpm(bpm: Int, speed: Float): Int = (bpm * speed).roundToInt().coerceAtLeast(1)

/**
 * How many automatic page turns are due after the click number [beatIndex] (0 = first click).
 * The first [countInBars] bars are the count-in and never turn the page; after that the page
 * turns on the first beat after every [barsPerTurn] bars.
 */
fun metronomeTurns(beatIndex: Long, beatsPerBar: Int, countInBars: Int, barsPerTurn: Int): Int {
    if (barsPerTurn <= 0 || beatsPerBar <= 0) return 0
    val played = beatIndex - countInBars.toLong() * beatsPerBar
    if (played < 0) return 0
    return (played / (beatsPerBar.toLong() * barsPerTurn)).toInt()
}
