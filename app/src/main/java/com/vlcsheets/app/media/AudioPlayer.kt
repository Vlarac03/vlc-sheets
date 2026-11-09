package com.vlcsheets.app.media

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Plays one audio track of a song (the backing track) with the platform MediaPlayer. */
class AudioPlayer {
    private var player: MediaPlayer? = null

    var file by mutableStateOf<File?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var durationMs by mutableIntStateOf(0)
        private set

    /** Playback speed; the pitch stays the same. */
    var speed by mutableFloatStateOf(1f)
        private set

    val positionMs: Int get() = runCatching { player?.currentPosition }.getOrNull() ?: 0

    /** Loads [f] without starting it. Returns false if the file cannot be played. */
    fun load(f: File): Boolean {
        release()
        val p = runCatching {
            MediaPlayer().apply {
                setDataSource(f.absolutePath)
                prepare()
                setOnCompletionListener { playing = false }
            }
        }.getOrNull() ?: return false
        player = p
        file = f
        durationMs = p.duration.coerceAtLeast(0)
        return true
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            p.start()
            applySpeed(p)
        }
        playing = p.isPlaying
    }

    fun changeSpeed(value: Float) {
        speed = value
        player?.takeIf { it.isPlaying }?.let(::applySpeed)
    }

    // Only while playing: setting a speed on a paused MediaPlayer starts it.
    private fun applySpeed(p: MediaPlayer) {
        runCatching { p.playbackParams = p.playbackParams.setSpeed(speed).setPitch(1f) }
    }

    fun seekTo(ms: Int) {
        player?.seekTo(ms.coerceIn(0, durationMs))
    }

    /** Seeks to [ms] and starts playing if paused. */
    fun playFrom(ms: Int) {
        seekTo(ms)
        if (!playing) toggle()
    }

    fun release() {
        player?.release()
        player = null
        file = null
        playing = false
        durationMs = 0
    }
}
