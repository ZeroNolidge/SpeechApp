package com.zeronolidge.speechapp.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * The "parrot": records the child for a few seconds, then plays it straight back,
 * so they can hear how they sound. Hearing yourself is a big part of speech practice.
 */
class AndroidParrot(private val context: Context) : Parrot {
    private val main = Handler(Looper.getMainLooper())
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private val file get() = File(context.cacheDir, "parrot.m4a")

    override fun recordThenPlay(seconds: Int, onPlaying: () -> Unit, onDone: () -> Unit) {
        stop()
        try {
            val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioSamplingRate(44100)
            rec.setAudioEncodingBitRate(96000)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
        } catch (e: Exception) {
            stop()
            onDone()
            return
        }
        main.postDelayed({
            if (!stopRecording()) {
                onDone(); return@postDelayed
            }
            try {
                val p = MediaPlayer()
                p.setDataSource(file.absolutePath)
                p.setOnCompletionListener { stop(); onDone() }
                p.prepare()
                p.start()
                player = p
                onPlaying()
            } catch (e: Exception) {
                stop()
                onDone()
            }
        }, seconds * 1000L)
    }

    override fun listenThenPlay(waitMs: Int, onLevel: (Float) -> Unit, onPlaying: () -> Unit, onQuiet: () -> Unit, onDone: () -> Unit) {
        stop()
        val rec = try {
            (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            stop()
            onDone()
            return
        }
        recorder = rec
        var t = 0
        var voiceMs = 0
        var quietMs = 0
        var heard = false
        val room = ArrayList<Float>()
        // Polls the recorder's loudness: plays back 0.7 s after he stops talking, or gives up quietly.
        val tick = object : Runnable {
            override fun run() {
                if (recorder !== rec) return
                val level = (rec.maxAmplitude / 32767f).coerceIn(0f, 1f)
                onLevel(level)
                t += 50
                if (t <= 300) room += level else {
                    val bar = maxOf(0.04f, 2.5f * (room.sum() / room.size.coerceAtLeast(1)))
                    if (level > bar) { voiceMs += 50; quietMs = 0; if (voiceMs >= 100) heard = true } else quietMs += 50
                }
                val end = (heard && quietMs >= 700) || voiceMs >= 6000 || (!heard && t >= waitMs)
                if (!end) { main.postDelayed(this, 50); return }
                if (!stopRecording() || !heard) {
                    if (heard) onDone() else onQuiet()
                    return
                }
                try {
                    val p = MediaPlayer()
                    p.setDataSource(file.absolutePath)
                    p.setOnCompletionListener { stop(); onDone() }
                    p.prepare()
                    p.start()
                    player = p
                    onPlaying()
                } catch (e: Exception) {
                    stop()
                    onDone()
                }
            }
        }
        main.postDelayed(tick, 50)
    }

    private fun stopRecording(): Boolean {
        val rec = recorder ?: return false
        recorder = null
        return try {
            rec.stop(); true
        } catch (e: Exception) {
            false
        } finally {
            rec.release()
        }
    }

    override fun stop() {
        main.removeCallbacksAndMessages(null)
        stopRecording()
        player?.release()
        player = null
    }
}
