package com.vlcsheets.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** One example of a symbol written by the user, to teach the recognizer. Any units: only the shape counts. */
data class InkSample(val symbol: String, val strokes: List<List<Pt>>)

/** Handwritten symbols the user can teach, in the order the teaching screen shows them. */
val HANDWRITING_SYMBOLS = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "○") // ○ = natural harmonic

/**
 * Turns handwritten strokes into text annotations, with a point-cloud matcher (the $P method):
 * each symbol is compared, as a cloud of points, with the user's own samples. No network, no model files.
 */
object Handwriting {

    private const val N = 32

    // shortcut: fixed limits picked from synthetic shapes; tune them with real samples from the tablet.
    /** A match farther than this is not trusted, and the strokes stay as they are. */
    const val MAX_DISTANCE = 1.2f

    /** A group taller or wider than this (part of the page height) is a drawing, not a character. */
    private const val MAX_CHAR_SIZE = 0.06f

    /** Smaller than this on both sides is a dot (staccato…), not a character. */
    private const val MIN_CHAR_SIZE = 0.006f

    private class P(val x: Float, val y: Float, val id: Int)

    /** Best symbol for [strokes] and its distance, or null when there is nothing to compare with. */
    fun classify(strokes: List<List<Pt>>, samples: List<InkSample>): Pair<String, Float>? {
        val cloud = cloud(strokes) ?: return null
        return samples.mapNotNull { s -> cloud(s.strokes)?.let { s.symbol to match(cloud, it) } }.minByOrNull { it.second }
    }

    /**
     * Groups the page's handwriting into characters and recognizes each one.
     * Returns the ids of the strokes to remove and the text annotations that replace them.
     * [aspect] is the page's width / height, since stroke points are 0..1 of each side.
     */
    fun convert(
        pageStrokes: List<Stroke>,
        allSamples: List<InkSample>,
        aspect: Float,
        texts: List<Stroke> = emptyList(),
    ): Pair<Set<String>, List<Stroke>> {
        // Samples of symbols no longer taught (p, m, f… from older versions) are kept but not used.
        val samples = allSamples.filter { it.symbol in HANDWRITING_SYMBOLS }
        val aSamples = samples.filter { it.symbol == "a" }
        val removed = mutableSetOf<String>()
        val added = mutableListOf<Stroke>()
        val unsure = mutableListOf<List<Stroke>>()
        for (group in groupCharacters(pageStrokes, aspect)) {
            val box = bounds(group.flatMap { it.points })
            val w = (box[2] - box[0]) * aspect
            val h = box[3] - box[1]
            if (w > MAX_CHAR_SIZE || h > MAX_CHAR_SIZE || (w < MIN_CHAR_SIZE && h < MIN_CHAR_SIZE)) continue
            val real = group.map { s -> s.points.map { Pt(it.x * aspect, it.y) } }
            val (symbol, distance) = classify(real, samples) ?: continue
            if (distance > MAX_DISTANCE) {
                unsure += group
                continue
            }
            removed += group.map { it.id }
            added += textFor(group, box, symbol)
        }
        // Right after a digit an "a" is expected (4a = 4th position), so it may look less like the samples.
        val digits = (texts + added).filter { it.text.length == 1 && it.text[0].isDigit() }
        for (group in unsure) {
            val box = bounds(group.flatMap { it.points })
            val real = group.map { s -> s.points.map { Pt(it.x * aspect, it.y) } }
            val distance = classify(real, aSamples)?.second ?: continue
            val page = group.first().page
            if (distance <= MAX_DISTANCE * 1.6f && digits.any { it.page == page && followsDigit(it.points, box, aspect) }) {
                removed += group.map { it.id }
                added += textFor(group, box, "a")
            }
        }
        return removed to added
    }

    private fun textFor(group: List<Stroke>, box: FloatArray, symbol: String): Stroke {
        val first = group.first()
        return Stroke(
            id = UUID.randomUUID().toString(),
            page = first.page,
            points = listOf(Pt(box[0], box[1]), Pt(box[2], box[3])),
            color = first.color,
            width = first.width,
            text = symbol,
        )
    }

    /** True when the box (minX, minY, maxX, maxY) sits just right of a digit whose corners are [digit]. */
    private fun followsDigit(digit: List<Pt>, box: FloatArray, aspect: Float): Boolean {
        val (d0, d1) = digit
        val h = d1.y - d0.y
        val gap = (box[0] - d1.x) * aspect
        return gap in -0.3f * h..0.8f * h && box[1] < d1.y && box[3] > d0.y
    }

    /** A digit with an "a" right after it is one position mark ("4a"), not two texts. */
    fun joinPositions(texts: List<Stroke>, aspect: Float): List<Stroke> {
        val out = texts.toMutableList()
        for (a in texts.filter { it.text == "a" }) {
            val (a0, a1) = a.points
            val box = floatArrayOf(a0.x, a0.y, a1.x, a1.y)
            val digit = out.filter { it.page == a.page && it.text.length == 1 && it.text[0].isDigit() }
                .firstOrNull { followsDigit(it.points, box, aspect) } ?: continue
            val (d0, d1) = digit.points
            out -= a
            out[out.indexOf(digit)] = digit.copy(
                text = digit.text + "a",
                points = listOf(Pt(min(d0.x, a0.x), min(d0.y, a0.y)), Pt(max(d1.x, a1.x), max(d1.y, a1.y))),
            )
        }
        return out
    }

    /**
     * Strokes that overlap side to side and touch (or almost) up and down make one character,
     * so a 4 or a ♯ stays whole while digits written next to or above each other stay apart.
     */
    fun groupCharacters(strokes: List<Stroke>, aspect: Float): List<List<Stroke>> {
        val ink = strokes.filter { !it.highlighter && it.text.isEmpty() && it.points.isNotEmpty() }
        val boxes = ink.map { s -> bounds(s.points).let { floatArrayOf(it[0] * aspect, it[1], it[2] * aspect, it[3]) } }
        val parent = IntArray(ink.size) { it }
        fun find(i: Int): Int { var r = i; while (parent[r] != r) r = parent[r]; return r }
        for (i in ink.indices) for (j in i + 1 until ink.size) {
            val a = boxes[i]
            val b = boxes[j]
            val xOverlap = min(a[2], b[2]) - max(a[0], b[0])
            val yGap = max(a[1], b[1]) - min(a[3], b[3])
            val tolerance = 0.15f * max(a[3] - a[1], b[3] - b[1])
            // Two pages side by side share coordinates: only strokes of the same page make a character.
            if (ink[i].page == ink[j].page && xOverlap >= 0f && yGap <= tolerance) parent[find(i)] = find(j)
        }
        return ink.indices.groupBy { find(it) }.values.map { ids -> ids.map { ink[it] } }
    }

    /** Text heights to pick from in Settings, as a part of the page height; 0 = the median of what was written. */
    val TEXT_SIZES = listOf(0f, 0.015f, 0.022f, 0.03f)

    /**
     * Gives every text annotation in [texts] the same height: [height] (part of the page height),
     * or the median of their heights when it is 0. Each keeps its bottom edge and its centre.
     */
    fun sameSize(texts: List<Stroke>, height: Float): List<Stroke> {
        if (texts.isEmpty()) return texts
        val h = height.takeIf { it > 0f }
            ?: texts.map { it.points[1].y - it.points[0].y }.sorted()[texts.size / 2]
        return texts.map { t ->
            val (a, b) = t.points
            val k = h / (b.y - a.y).coerceAtLeast(1e-4f)
            val cx = (a.x + b.x) / 2
            val halfW = (b.x - a.x) / 2 * k
            t.copy(points = listOf(Pt(cx - halfW, b.y - h), Pt(cx + halfW, b.y)))
        }
    }

    /** minX, minY, maxX, maxY. */
    private fun bounds(points: List<Pt>): FloatArray = floatArrayOf(
        points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y },
    )

    private fun cloud(strokes: List<List<Pt>>): List<P>? {
        val points = strokes.flatMapIndexed { i, s -> s.map { P(it.x, it.y, i) } }
        if (points.isEmpty()) return null
        return normalize(resample(points))
    }

    private fun resample(points: List<P>): List<P> {
        var length = 0f
        for (i in 1 until points.size) if (points[i].id == points[i - 1].id) length += dist(points[i - 1], points[i])
        if (length == 0f) return List(N) { points[0] }
        val interval = length / (N - 1)
        val src = points.toMutableList()
        val out = mutableListOf(src[0])
        var acc = 0f
        var i = 1
        while (i < src.size) {
            val a = src[i - 1]
            val b = src[i]
            if (a.id == b.id) {
                val d = dist(a, b)
                if (acc + d >= interval && d > 0f) {
                    val t = (interval - acc) / d
                    val q = P(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y), a.id)
                    out += q
                    src.add(i, q)
                    acc = 0f
                } else {
                    acc += d
                }
            }
            i++
        }
        while (out.size < N) out += src.last()
        return out.take(N)
    }

    private fun normalize(points: List<P>): List<P> {
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val size = max(points.maxOf { it.x } - minX, points.maxOf { it.y } - minY).takeIf { it > 0f } ?: 1f
        val scaled = points.map { P((it.x - minX) / size, (it.y - minY) / size, it.id) }
        val cx = scaled.sumOf { it.x.toDouble() }.toFloat() / scaled.size
        val cy = scaled.sumOf { it.y.toDouble() }.toFloat() / scaled.size
        return scaled.map { P(it.x - cx, it.y - cy, it.id) }
    }

    private fun match(a: List<P>, b: List<P>): Float {
        val step = floor(sqrt(N.toFloat())).toInt()
        var best = Float.MAX_VALUE
        for (start in 0 until N step step) {
            best = min(best, min(cloudDistance(a, b, start), cloudDistance(b, a, start)))
        }
        return best
    }

    private fun cloudDistance(a: List<P>, b: List<P>, start: Int): Float {
        val used = BooleanArray(N)
        var sum = 0f
        var i = start
        do {
            var index = -1
            var nearest = Float.MAX_VALUE
            for (j in 0 until N) {
                if (used[j]) continue
                val d = dist(a[i], b[j])
                if (d < nearest) { nearest = d; index = j }
            }
            used[index] = true
            val weight = 1f - ((i - start + N) % N).toFloat() / N
            sum += weight * nearest
            i = (i + 1) % N
        } while (i != start)
        return sum
    }

    private fun dist(a: P, b: P) = hypot(a.x - b.x, a.y - b.y)
}

fun inkSamplesToJson(samples: List<InkSample>): JSONArray = JSONArray().apply {
    samples.forEach { s ->
        put(JSONObject().apply {
            put("symbol", s.symbol)
            put("strokes", JSONArray().apply {
                s.strokes.forEach { stroke ->
                    put(JSONArray().apply { stroke.forEach { put(JSONArray().put(it.x.toDouble()).put(it.y.toDouble())) } })
                }
            })
        })
    }
}

fun inkSamplesFromJson(a: JSONArray): List<InkSample> = List(a.length()) { i ->
    val o = a.optJSONObject(i) ?: JSONObject()
    val strokes = o.optJSONArray("strokes")?.let { ss ->
        List(ss.length()) { j ->
            val pts = ss.optJSONArray(j) ?: JSONArray()
            List(pts.length()) { k ->
                val pair = pts.optJSONArray(k)
                Pt(pair?.optDouble(0)?.toFloat() ?: 0f, pair?.optDouble(1)?.toFloat() ?: 0f)
            }
        }
    } ?: emptyList()
    InkSample(o.optString("symbol"), strokes)
}.filter { it.symbol.isNotEmpty() && it.strokes.isNotEmpty() }
