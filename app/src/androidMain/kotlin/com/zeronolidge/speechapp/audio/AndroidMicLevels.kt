package com.zeronolidge.speechapp.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
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
    @Volatile override var band = 1f
        private set
    @Volatile override var periodic = 1f
        private set

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
                    val b = voiceBand(buf, n, rate)
                    val p = voiced(buf, n, rate)
                    main.post {
                        if (generation == token) {
                            band = b
                            periodic = p
                            onLevel(rms)
                        }
                    }
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

    /** Share of the energy between about 150 and 2000 Hz (two simple one-pole filters). */
    private fun voiceBand(buf: ShortArray, n: Int, rate: Int): Float {
        val aHp = exp(-2.0 * PI * 150 / rate)
        val aLp = 1 - exp(-2.0 * PI * 2000 / rate)
        var lpHp = 0.0
        var lp = 0.0
        var all = 0.0
        var inV = 0.0
        for (i in 0 until n) {
            val x = buf[i] / 32768.0
            lpHp = aHp * lpHp + (1 - aHp) * x
            val hp = x - lpHp
            lp += aLp * (hp - lp)
            all += x * x
            inV += lp * lp
        }
        return if (all > 0) (inV / all).toFloat().coerceIn(0f, 1f) else 0f
    }

    /** How voiced: the best normalised self-match at a pitch of 80 to 600 Hz. */
    private fun voiced(buf: ShortArray, n: Int, rate: Int): Float {
        val lo = rate / 600
        val hi = minOf(rate / 80, n - 64)
        var best = 0.0
        for (lag in lo..hi) {
            var xy = 0.0
            var xx = 0.0
            var yy = 0.0
            for (i in 0 until n - lag) {
                val x = buf[i].toDouble()
                val y = buf[i + lag].toDouble()
                xy += x * y
                xx += x * x
                yy += y * y
            }
            if (xx > 0 && yy > 0) best = maxOf(best, xy / sqrt(xx * yy))
        }
        return best.toFloat().coerceIn(0f, 1f)
    }
}
