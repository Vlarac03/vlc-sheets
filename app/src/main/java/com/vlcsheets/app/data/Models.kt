package com.vlcsheets.app.data

import org.json.JSONArray
import org.json.JSONObject

/** The kind of document stored for a song. */
enum class SongKind { PDF, IMAGE }

data class Pt(val x: Float, val y: Float)

/**
 * A single freehand stroke drawn on top of a page. Coordinates are normalised (0..1).
 * When [text] is set it is handwriting turned into text: [points] are the top-left and bottom-right of its box.
 */
data class Stroke(
    val id: String,
    val page: Int,
    val points: List<Pt>,
    val color: Long,
    val width: Float,
    val highlighter: Boolean = false,
    val text: String = "",
)

data class Bookmark(
    val id: String,
    val page: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A point of a song's audio track ([file] is the file name) where the viewer turns to [page]. */
data class AudioMark(val file: String, val ms: Int, val page: Int)

/**
 * A marker on [page] at (x, y) (0..1 of the whole page). Tapping it turns to [target] (D.S., coda…),
 * or, when [audioFile] is set, plays that audio track from [audioMs].
 */
data class Jump(
    val id: String,
    val page: Int,
    val x: Float,
    val y: Float,
    val target: Int,
    val audioFile: String = "",
    val audioMs: Int = 0,
) {
    val isAudio: Boolean get() = audioFile.isNotEmpty()
}

/** A rectangle of [page] (0..1 of the whole page) covered with paper, without touching the file. */
data class Mask(val id: String, val page: Int, val left: Float, val top: Float, val right: Float, val bottom: Float)

data class Song(
    val id: String,
    val title: String,
    val composer: String = "",
    val genre: String = "",
    val key: String = "",
    val tempo: Int = 0,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val collections: List<String> = emptyList(),
    val favorite: Boolean = false,
    val kind: SongKind = SongKind.PDF,
    /** File name relative to the song folder (e.g. score.pdf or page.jpg). */
    val fileName: String = "score.pdf",
    val pageCount: Int = 1,
    val thumbFile: String? = null,
    /** Width / height of the first page, used for placeholders and layout. */
    val pageAspect: Float = 0.7071f,
    val bookmarks: List<Bookmark> = emptyList(),
    val annotations: List<Stroke> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    val openCount: Int = 0,
    val lastPage: Int = 0,
    val audioMarks: List<AudioMark> = emptyList(),
    /** The metronome turns the page every this many bars; 0 = never. */
    val barsPerPage: Int = 0,
    /** Bars the metronome clicks before the page counting starts. */
    val countInBars: Int = 1,
    /** Time signature beats saved for the metronome; 0 = not saved. */
    val beatsPerBar: Int = 0,
    val year: Int = 0,
    /** Length in seconds, for setlist totals; 0 = unknown. */
    val durationSec: Int = 0,
    val jumps: List<Jump> = emptyList(),
    val masks: List<Mask> = emptyList(),
) {
    val hasAnnotations: Boolean get() = annotations.isNotEmpty()
    val initials: String
        get() = title.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            .ifBlank { "♪" }
}

data class Setlist(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val notes: String = "",
    /** A note shown under each piece (key, comments…), by song id. */
    val songNotes: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class Collection(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class LibraryData(
    val songs: List<Song> = emptyList(),
    val setlists: List<Setlist> = emptyList(),
    val collections: List<Collection> = emptyList(),
    /** The user's handwriting samples, to turn handwriting into text. */
    val ink: List<InkSample> = emptyList(),
    val loaded: Boolean = false,
)

data class ImportProgress(
    val current: Int,
    val total: Int,
    val fileName: String,
)

data class ImportResult(
    val imported: Int,
    val failed: Int,
    val songs: List<Song> = emptyList(),
)

// ---------------------------------------------------------------------------
// JSON mapping
// ---------------------------------------------------------------------------

fun Song.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("composer", composer)
    put("genre", genre)
    put("key", key)
    put("tempo", tempo)
    put("notes", notes)
    put("tags", JSONArray(tags))
    put("collections", JSONArray(collections))
    put("favorite", favorite)
    put("kind", kind.name)
    put("fileName", fileName)
    put("pageCount", pageCount)
    put("thumbFile", thumbFile ?: JSONObject.NULL)
    put("pageAspect", pageAspect.toDouble())
    put("bookmarks", JSONArray().apply {
        bookmarks.forEach { b ->
            put(JSONObject().apply {
                put("id", b.id)
                put("page", b.page)
                put("label", b.label)
                put("createdAt", b.createdAt)
            })
        }
    })
    // Annotations live in songs/<id>/annotations.json, not in the index.
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("lastOpenedAt", lastOpenedAt)
    put("openCount", openCount)
    put("lastPage", lastPage)
    put("audioMarks", JSONArray().apply {
        audioMarks.forEach { m ->
            put(JSONObject().apply {
                put("file", m.file)
                put("ms", m.ms)
                put("page", m.page)
            })
        }
    })
    put("barsPerPage", barsPerPage)
    put("countInBars", countInBars)
    put("beatsPerBar", beatsPerBar)
    put("year", year)
    put("durationSec", durationSec)
    put("jumps", JSONArray().apply {
        jumps.forEach { j ->
            put(JSONObject().apply {
                put("id", j.id)
                put("page", j.page)
                put("x", j.x.toDouble())
                put("y", j.y.toDouble())
                put("target", j.target)
                if (j.isAudio) {
                    put("audioFile", j.audioFile)
                    put("audioMs", j.audioMs)
                }
            })
        }
    })
    put("masks", JSONArray().apply {
        masks.forEach { m ->
            put(JSONObject().apply {
                put("id", m.id)
                put("page", m.page)
                put("left", m.left.toDouble())
                put("top", m.top.toDouble())
                put("right", m.right.toDouble())
                put("bottom", m.bottom.toDouble())
            })
        }
    })
}

fun songFromJson(o: JSONObject): Song {
    val tags = o.optJSONArray("tags")?.let { a -> List(a.length()) { a.optString(it) } } ?: emptyList()
    val collections = o.optJSONArray("collections")?.let { a -> List(a.length()) { a.optString(it) } } ?: emptyList()
    val bookmarks = o.optJSONArray("bookmarks")?.let { a ->
        List(a.length()) { i ->
            val b = a.optJSONObject(i) ?: JSONObject()
            Bookmark(
                id = b.optString("id"),
                page = b.optInt("page"),
                label = b.optString("label"),
                createdAt = b.optLong("createdAt"),
            )
        }
    } ?: emptyList()
    // Only old index files (and old backups) still carry annotations here.
    val annotations = o.optJSONArray("annotations")?.let(::strokesFromJson) ?: emptyList()
    return Song(
        id = o.optString("id"),
        title = o.optString("title"),
        composer = o.optString("composer"),
        genre = o.optString("genre"),
        key = o.optString("key"),
        tempo = o.optInt("tempo"),
        notes = o.optString("notes"),
        tags = tags,
        collections = collections,
        favorite = o.optBoolean("favorite"),
        kind = runCatching { SongKind.valueOf(o.optString("kind", "PDF")) }.getOrDefault(SongKind.PDF),
        fileName = o.optString("fileName", "score.pdf"),
        pageCount = o.optInt("pageCount", 1).coerceAtLeast(1),
        thumbFile = o.optString("thumbFile").takeIf { it.isNotBlank() && it != "null" },
        pageAspect = o.optDouble("pageAspect", 0.7071).toFloat(),
        bookmarks = bookmarks,
        annotations = annotations,
        createdAt = o.optLong("createdAt"),
        updatedAt = o.optLong("updatedAt"),
        lastOpenedAt = o.optLong("lastOpenedAt"),
        openCount = o.optInt("openCount"),
        lastPage = o.optInt("lastPage"),
        audioMarks = o.optJSONArray("audioMarks")?.let { a ->
            List(a.length()) { i ->
                val m = a.optJSONObject(i) ?: JSONObject()
                AudioMark(m.optString("file"), m.optInt("ms"), m.optInt("page"))
            }
        } ?: emptyList(),
        barsPerPage = o.optInt("barsPerPage"),
        countInBars = o.optInt("countInBars", 1),
        beatsPerBar = o.optInt("beatsPerBar"),
        year = o.optInt("year"),
        durationSec = o.optInt("durationSec"),
        jumps = o.optJSONArray("jumps")?.let { a ->
            List(a.length()) { i ->
                val j = a.optJSONObject(i) ?: JSONObject()
                Jump(
                    j.optString("id"), j.optInt("page"), j.optDouble("x").toFloat(), j.optDouble("y").toFloat(),
                    j.optInt("target"), j.optString("audioFile"), j.optInt("audioMs"),
                )
            }
        } ?: emptyList(),
        masks = o.optJSONArray("masks")?.let { a ->
            List(a.length()) { i ->
                val m = a.optJSONObject(i) ?: JSONObject()
                Mask(
                    m.optString("id"), m.optInt("page"),
                    m.optDouble("left").toFloat(), m.optDouble("top").toFloat(),
                    m.optDouble("right").toFloat(), m.optDouble("bottom").toFloat(),
                )
            }
        } ?: emptyList(),
    )
}

fun strokesToJson(strokes: List<Stroke>): JSONArray = JSONArray().apply {
    strokes.forEach { s ->
        put(JSONObject().apply {
            put("id", s.id)
            put("page", s.page)
            put("color", s.color)
            put("width", s.width.toDouble())
            put("highlighter", s.highlighter)
            if (s.text.isNotEmpty()) put("text", s.text)
            put("points", JSONArray().apply {
                s.points.forEach { p ->
                    put(JSONArray().apply {
                        put(p.x.toDouble())
                        put(p.y.toDouble())
                    })
                }
            })
        })
    }
}

fun strokesFromJson(a: JSONArray): List<Stroke> = List(a.length()) { i ->
    val s = a.optJSONObject(i) ?: JSONObject()
    val pts = s.optJSONArray("points")?.let { p ->
        List(p.length()) { j ->
            val pair = p.optJSONArray(j)
            Pt(pair?.optDouble(0)?.toFloat() ?: 0f, pair?.optDouble(1)?.toFloat() ?: 0f)
        }
    } ?: emptyList()
    Stroke(
        id = s.optString("id"),
        page = s.optInt("page"),
        color = s.optLong("color"),
        width = s.optDouble("width", 3.0).toFloat(),
        highlighter = s.optBoolean("highlighter"),
        points = pts,
        text = s.optString("text"),
    )
}

fun Setlist.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("songIds", JSONArray(songIds))
    put("notes", notes)
    put("songNotes", JSONObject(songNotes))
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
}

fun setlistFromJson(o: JSONObject): Setlist {
    val ids = o.optJSONArray("songIds")?.let { a -> List(a.length()) { a.optString(it) } } ?: emptyList()
    return Setlist(
        id = o.optString("id"),
        name = o.optString("name"),
        songIds = ids,
        notes = o.optString("notes"),
        songNotes = o.optJSONObject("songNotes")?.let { n -> n.keys().asSequence().associateWith { n.optString(it) } } ?: emptyMap(),
        createdAt = o.optLong("createdAt"),
        updatedAt = o.optLong("updatedAt"),
    )
}

/** "3:25" or "3" (minutes) to seconds; blank = 0; null if it cannot be read. */
fun parseDuration(text: String): Int? {
    val t = text.trim()
    if (t.isEmpty()) return 0
    val parts = t.split(':').map { it.trim().toIntOrNull() ?: return null }
    return when (parts.size) {
        1 -> parts[0] * 60
        2 -> if (parts[1] in 0..59) parts[0] * 60 + parts[1] else null
        3 -> if (parts[1] in 0..59 && parts[2] in 0..59) parts[0] * 3600 + parts[1] * 60 + parts[2] else null
        else -> null
    }
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun Collection.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("createdAt", createdAt)
}

fun collectionFromJson(o: JSONObject): Collection = Collection(
    id = o.optString("id"),
    name = o.optString("name"),
    createdAt = o.optLong("createdAt"),
)
