package com.zeronolidge.speechapp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** The paper tap sounds, made once in memory (no sound files) and replayed. */
class AndroidSfx : Sfx {
    private val rate = 22050
    private val crinkleTrack by lazy { track(crinkleSamples()) }
    private val popTrack by lazy { track(popSamples()) }
    private val chopTrack by lazy { track(chopSamples()) }

    private fun crinkleSamples(): ShortArray {
        val n = (rate * 0.09).toInt()
        val r = Random(3)
        var last = 0f
        return ShortArray(n) { i ->
            val t = i / n.toFloat()
            val raw = (r.nextFloat() * 2 - 1) * (1 - t) * (1 - t) * (if (r.nextFloat() < 0.3f) 1f else 0.35f)
            // A simple high-pass, so it rustles rather than hisses.
            val v = raw - last * 0.6f
            last = raw
            (v * 0.16f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun popSamples(): ShortArray {
        val n = (rate * 0.14).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i / rate.toDouble()
            val f = 700 * exp(-t * 8)
            phase += 2 * PI * f / rate
            (sin(phase) * exp(-t * 30) * 0.12 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    // Rory's Kitchen's butter-cutting swipe (K7): a short wooden "tok" -- a brief noise burst and
    // a quick low thump, shorter and lower than the paper crinkle/pop.
    private fun chopSamples(): ShortArray {
        val n = (rate * 0.12).toInt()
        val r = Random(5)
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i / rate.toDouble()
            val noise = if (t < 0.04) (r.nextFloat() * 2 - 1) * (1 - t / 0.04).toFloat() * 0.14f else 0f
            val f = 180
            phase += 2 * PI * f / rate
            val thump = (sin(phase) * exp(-t * 20) * 0.18).toFloat()
            ((noise + thump) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun track(samples: ShortArray): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
            .also { it.write(samples, 0, samples.size) }
    }.getOrNull()

    private fun play(t: AudioTrack?) {
        t ?: return
        runCatching {
            t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    override fun crinkle() = play(crinkleTrack)
    override fun pop() = play(popTrack)
    override fun chop() = play(chopTrack)
}
