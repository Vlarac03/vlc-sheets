package com.vlcsheets.app.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.locks.LockSupport
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class MetronomeState(
    val running: Boolean = false,
    val bpm: Int = 100,
    val beatsPerBar: Int = 4,
    val accentFirst: Boolean = true,
    val currentBeat: Int = 0,
    val pulse: Long = 0L,
    /** Clicks played since the last start, minus one (-1 = none yet). */
    val beatIndex: Long = -1L,
    /** Practice speed (1 = normal); the clicks play at bpm × speed. */
    val speed: Float = 1f,
)

/**
 * Sample accurate-ish click generator built directly on AudioTrack. It runs on
 * a dedicated thread so the rhythm is not affected by UI recomposition.
 */
class MetronomeEngine {

    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    private val lock = Any()
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    @Volatile private var running = false
    @Volatile private var bpm = 100
    @Volatile private var beatsPerBar = 4
    @Volatile private var accentFirst = true
    @Volatile private var speed = 1f

    private var accentClick: ShortArray = ShortArray(0)
    private var normalClick: ShortArray = ShortArray(0)

    fun configure(bpm: Int, beatsPerBar: Int, accentFirst: Boolean) {
        this.bpm = bpm.coerceIn(30, 300)
        this.beatsPerBar = beatsPerBar.coerceIn(1, 16)
        this.accentFirst = accentFirst
        _state.update {
            it.copy(bpm = this.bpm, beatsPerBar = this.beatsPerBar, accentFirst = accentFirst)
        }
    }

    fun setSpeed(value: Float) {
        speed = value.coerceIn(0.25f, 1f)
        _state.update { it.copy(speed = speed) }
    }

    fun start() {
        synchronized(lock) {
            if (running) return
            running = true
            accentClick = buildClick(1760.0, 0.9f)
            normalClick = buildClick(1180.0, 0.55f)
            _state.update { it.copy(running = true, currentBeat = 0, beatIndex = -1L) }
            thread = Thread({ loop() }, "vlc-sheets-metronome").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            running = false
            val t = thread
            thread = null
            t?.interrupt()
            t?.let { runCatching { it.join(400) } }
            runCatching { track?.pause() }
            runCatching { track?.flush() }
            runCatching { track?.release() }
            track = null
            _state.update { it.copy(running = false, currentBeat = 0, beatIndex = -1L) }
        }
    }

    fun release() = stop()

    private fun loop() {
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        val audioTrack = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBuf * 2)
                .build()
        }.getOrNull() ?: run {
            running = false
            _state.update { it.copy(running = false) }
            return
        }

        track = audioTrack
        audioTrack.play()

        val silence = ShortArray(2048)
        var beat = 0
        var nextBeatAt = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            if (now >= nextBeatAt) {
                val click = if (accentFirst && beat % beatsPerBar == 0) accentClick else normalClick
                runCatching { audioTrack.write(click, 0, click.size) }
                val displayBeat = beat % beatsPerBar
                val index = beat.toLong()
                _state.update { it.copy(currentBeat = displayBeat, pulse = it.pulse + 1, beatIndex = index) }
                beat += 1
                nextBeatAt += (60_000_000_000.0 / (bpm * speed)).toLong()
            } else {
                val remainingSamples = ((nextBeatAt - now) / 1_000_000_000.0 * sampleRate).toInt()
                if (remainingSamples in 1..silence.size) {
                    runCatching { audioTrack.write(silence, 0, remainingSamples) }
                } else if (remainingSamples > silence.size) {
                    runCatching { audioTrack.write(silence, 0, silence.size) }
                } else {
                    LockSupport.parkNanos(500_000)
                }
            }
        }
    }

    private fun buildClick(freq: Double, volume: Float): ShortArray {
        val sampleRate = 44100
        val duration = 0.045
        val length = (sampleRate * duration).toInt()
        val out = ShortArray(length)
        for (i in 0 until length) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 55.0)
            val sample = sin(2 * PI * freq * t) * envelope * volume
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
