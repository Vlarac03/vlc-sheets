package com.vlcsheets.app.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log2
import kotlin.math.roundToInt

/** The nearest note to a pitch: [index] 0 = C … 11 = B, and how far off it is in cents (-50..50). */
data class TunerNote(val index: Int, val octave: Int, val cents: Float)

fun noteFor(hz: Float, a4: Float = 440f): TunerNote {
    val midi = 69 + 12 * log2(hz / a4)
    val nearest = midi.roundToInt()
    return TunerNote(Math.floorMod(nearest, 12), Math.floorDiv(nearest, 12) - 1, (midi - nearest) * 100)
}

/**
 * Pitch of [samples] in Hz with the YIN method, or null when it is too quiet or there is no clear note.
 * Needs at least two periods of the lowest note ([minHz]) in the buffer.
 */
fun detectPitch(samples: FloatArray, sampleRate: Int, minHz: Float = 40f, maxHz: Float = 1600f): Float? {
    val maxLag = (sampleRate / minHz).toInt().coerceAtMost(samples.size / 2)
    val minLag = (sampleRate / maxHz).toInt().coerceAtLeast(2)
    if (maxLag <= minLag + 1) return null
    var energy = 0.0
    for (v in samples) energy += v * v
    if (energy / samples.size < 1e-5) return null // silence: RMS below about -50 dB

    val window = samples.size - maxLag
    val cmnd = FloatArray(maxLag + 1)
    cmnd[0] = 1f
    var running = 0f
    for (tau in 1..maxLag) {
        var sum = 0f
        for (i in 0 until window) {
            val diff = samples[i] - samples[i + tau]
            sum += diff * diff
        }
        running += sum
        cmnd[tau] = if (running == 0f) 1f else sum * tau / running
    }
    var tau = minLag
    while (tau < maxLag && cmnd[tau] >= 0.15f) tau++
    if (tau >= maxLag) return null
    while (tau + 1 < maxLag && cmnd[tau + 1] < cmnd[tau]) tau++
    // Parabolic interpolation for a period between two samples.
    val s0 = cmnd[tau - 1]
    val s1 = cmnd[tau]
    val s2 = cmnd[tau + 1]
    val den = s0 + s2 - 2 * s1
    val period = if (den != 0f) tau + (s0 - s2) / (2 * den) else tau.toFloat()
    return sampleRate / period
}

/** Listens to the microphone on its own thread and publishes the pitch it hears (null = none). */
class TunerEngine {
    private val _hz = MutableStateFlow<Float?>(null)
    val hz: StateFlow<Float?> = _hz.asStateFlow()
    private val _failed = MutableStateFlow(false)

    /** True when the microphone could not be opened or stopped delivering sound. */
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    @Volatile private var running = false
    private var thread: Thread? = null

    /** The caller must already hold RECORD_AUDIO. */
    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        running = true
        _failed.value = false
        thread = Thread({
            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val record = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, WINDOW * 2),
                ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            }.getOrNull()
            if (record == null) {
                running = false
                _failed.value = true
                return@Thread
            }
            val chunk = ShortArray(HOP)
            val window = FloatArray(WINDOW)
            var errors = 0
            try {
                record.startRecording()
                while (running) {
                    var read = 0
                    while (read < HOP && running) {
                        val n = record.read(chunk, read, HOP - read)
                        if (n <= 0) {
                            // Repeated read errors: give up instead of spinning.
                            if (++errors >= MAX_READ_ERRORS) {
                                running = false
                                _failed.value = true
                            } else {
                                Thread.sleep(20)
                            }
                            break
                        }
                        errors = 0
                        read += n
                    }
                    if (read < HOP) continue
                    // Slide the window and append the new samples.
                    System.arraycopy(window, HOP, window, 0, WINDOW - HOP)
                    for (i in 0 until HOP) window[WINDOW - HOP + i] = chunk[i] / 32768f
                    _hz.value = detectPitch(window, SAMPLE_RATE)
                }
            } catch (e: IllegalStateException) {
                // startRecording fails when another app holds the microphone.
                running = false
                _failed.value = true
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }, "tuner").apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        _hz.value = null
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val WINDOW = 4096
        const val HOP = 2048
        const val MAX_READ_ERRORS = 10
    }
}
