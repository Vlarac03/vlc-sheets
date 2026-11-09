package com.vlcsheets.app.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class HandwritingTest {

    private val random = Random(7)

    private fun wobble(points: List<Pt>, amount: Float = 0.04f) =
        points.map { Pt(it.x + (random.nextFloat() - 0.5f) * amount, it.y + (random.nextFloat() - 0.5f) * amount) }

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float, n: Int = 12) =
        List(n) { i -> val t = i / (n - 1f); Pt(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t) }

    private fun one() = listOf(wobble(line(0.3f, 0.2f, 0.5f, 0f) + line(0.5f, 0f, 0.5f, 1f)))
    private fun zero() = listOf(wobble(List(30) { i -> val a = 2 * PI * i / 29; Pt(0.5f + 0.35f * sin(a).toFloat(), 0.5f - 0.5f * cos(a).toFloat()) }))
    private fun seven() = listOf(wobble(line(0f, 0f, 0.7f, 0f) + line(0.7f, 0f, 0.25f, 1f)))
    private fun four() = listOf(wobble(line(0.5f, 0f, 0f, 0.65f) + line(0f, 0.65f, 0.7f, 0.65f)), wobble(line(0.55f, 0.3f, 0.55f, 1f)))
    private fun zigzag() = listOf(line(0f, 0f, 0.25f, 1f) + line(0.25f, 1f, 0.5f, 0f) + line(0.5f, 0f, 0.75f, 1f) + line(0.75f, 1f, 1f, 0f))

    private val samples = buildList {
        repeat(3) {
            add(InkSample("1", one()))
            add(InkSample("0", zero()))
            add(InkSample("7", seven()))
            add(InkSample("4", four()))
        }
    }

    @Test
    fun recognizesTaughtSymbols() {
        for ((symbol, shape) in listOf("1" to ::one, "0" to ::zero, "7" to ::seven, "4" to ::four)) {
            val (found, distance) = Handwriting.classify(shape(), samples)!!
            assertEquals(symbol, found)
            assertTrue("$symbol at $distance", distance < Handwriting.MAX_DISTANCE)
        }
    }

    @Test
    fun leavesUnknownShapesAlone() {
        val (_, distance) = Handwriting.classify(zigzag(), samples)!!
        assertTrue("zigzag at $distance", distance > Handwriting.MAX_DISTANCE)
    }

    @Test
    fun convertsSmallCharactersAndKeepsDrawings() {
        // A "4" (two strokes) and a "1" side by side, 2 % of the page tall, plus a long slur.
        fun place(shape: List<List<Pt>>, x: Float) = shape.mapIndexed { i, pts ->
            Stroke("$x-$i", 0, pts.map { Pt(x + it.x * 0.02f, 0.3f + it.y * 0.02f) }, 0xFF000000, 2f)
        }
        val slur = Stroke("slur", 0, line(0.1f, 0.5f, 0.6f, 0.52f), 0xFF000000, 2f)
        val strokes = place(four(), 0.40f) + place(one(), 0.43f) + slur
        val (removed, added) = Handwriting.convert(strokes, samples, aspect = 1f)
        assertEquals(setOf("0.4-0", "0.4-1", "0.43-0"), removed)
        assertEquals(listOf("4", "1"), added.sortedBy { it.points[0].x }.map { it.text })
    }

    @Test
    fun strokesOnTwoPagesSideBySideStayApart() {
        // Two-page view: a "4" on page 0 and a "1" on page 1 at the same spot are two characters.
        fun place(shape: List<List<Pt>>, page: Int) = shape.mapIndexed { i, pts ->
            Stroke("$page-$i", page, pts.map { Pt(0.4f + it.x * 0.02f, 0.3f + it.y * 0.02f) }, 0xFF000000, 2f)
        }
        val (_, added) = Handwriting.convert(place(four(), 0) + place(one(), 1), samples, aspect = 1f)
        assertEquals(listOf(0 to "4", 1 to "1"), added.map { it.page to it.text }.sortedBy { it.first })
    }

    @Test
    fun textAnnotationsAndSamplesSurviveJson() {
        val text = Stroke("t", 1, listOf(Pt(0.1f, 0.2f), Pt(0.12f, 0.23f)), 0xFF112233, 2f, text = "△")
        assertEquals(listOf(text), strokesFromJson(JSONArray(strokesToJson(listOf(text)).toString())))
        val back = inkSamplesFromJson(JSONArray(inkSamplesToJson(samples).toString()))
        assertEquals(samples.map { it.symbol }, back.map { it.symbol })
        assertEquals(samples[0].strokes.size, back[0].strokes.size)
    }
}
