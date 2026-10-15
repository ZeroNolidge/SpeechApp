package com.zeronolidge.speechapp.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Microphone loudness on Android: reads short chunks from AudioRecord on a background thread and
 * reports their loudness on the main thread. The app asks for the microphone permission first.
 */
class AndroidMicLevels : MicLevels {
    private val main = Handler(Looper.getMainLooper())
    /** Bumped by every start and stop, so an old reading loop stops by itself. */
    @Volatile private var generation = 0

    override val available: Boolean = true

    @SuppressLint("MissingPermission")
    override fun start(onLevel: (Float) -> Unit, onFail: () -> Unit) {
        stop()
        val rate = 16000
        val chunk = rate * MicLevels.STEP_MS / 1000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, chunk * 4))
        } catch (e: Exception) {
            null
        }
        if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
            rec?.release()
            main.post(onFail)
            return
        }
        val token = ++generation
        thread(name = "mic-levels") {
            val buf = ShortArray(chunk)
            try {
                rec.startRecording()
                while (generation == token) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    var sum = 0.0
                    for (i in 0 until n) {
                        val v = buf[i] / 32768.0
                        sum += v * v
                    }
                    val rms = sqrt(sum / n).toFloat()
                    main.post { if (generation == token) onLevel(rms) }
                }
            } catch (e: Exception) {
                main.post { if (generation == token) onFail() }
            } finally {
                try { rec.stop() } catch (e: Exception) {}
                rec.release()
            }
        }
    }

    override fun stop() {
        generation++
    }
}
