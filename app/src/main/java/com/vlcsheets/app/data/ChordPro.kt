package com.vlcsheets.app.data

/** One printed line: chords (already spaced to sit over the right letters) and the text under them. */
data class ChordLine(val chords: String, val text: String, val style: Style = Style.NORMAL) {
    enum class Style { NORMAL, CHORUS, COMMENT }
}

data class ChordProSong(val title: String, val artist: String, val lines: List<ChordLine>)

private val DIRECTIVE = Regex("""^\{\s*([A-Za-z_]+)\s*(?::\s*(.*?))?\s*\}$""") // Android's regex needs the closing brace escaped

/**
 * Reads a ChordPro or plain-text song. Chords go in square brackets inside the lyrics
 * ("[G]Hello [D]world"); {title}, {artist}/{subtitle}, {comment} and {start_of_chorus}/
 * {end_of_chorus} are understood, other directives are skipped. Plain text comes out as is.
 */
fun parseChordPro(source: String): ChordProSong {
    var title = ""
    var artist = ""
    var chorus = false
    val out = mutableListOf<ChordLine>()
    for (raw in source.lines()) {
        val line = raw.trimEnd().replace("\t", "    ")
        if (line.startsWith("#")) continue
        val directive = DIRECTIVE.find(line.trim())
        if (directive != null) {
            val (name, value) = directive.destructured
            when (name.lowercase()) {
                "title", "t" -> title = value
                "subtitle", "st", "artist", "composer" -> if (artist.isBlank()) artist = value
                "comment", "c", "ci", "comment_italic", "cb", "comment_box" -> out += ChordLine("", value, ChordLine.Style.COMMENT)
                "start_of_chorus", "soc" -> chorus = true
                "end_of_chorus", "eoc" -> chorus = false
            }
            continue
        }
        val chords = StringBuilder()
        val text = StringBuilder()
        var i = 0
        while (i < line.length) {
            val end = if (line[i] == '[') line.indexOf(']', i) else -1
            if (end > i) {
                // Leave a space after the previous chord, moving the lyrics right if needed.
                if (chords.isNotEmpty() && chords.length >= text.length) text.append(" ".repeat(chords.length - text.length + 1))
                chords.append(" ".repeat(text.length - chords.length)).append(line, i + 1, end)
                i = end + 1
            } else {
                text.append(line[i])
                i++
            }
        }
        out += ChordLine(chords.toString(), text.toString(), if (chorus) ChordLine.Style.CHORUS else ChordLine.Style.NORMAL)
    }
    while (out.lastOrNull()?.let { it.chords.isBlank() && it.text.isBlank() } == true) out.removeAt(out.lastIndex)
    return ChordProSong(title.trim(), artist.trim(), out)
}
