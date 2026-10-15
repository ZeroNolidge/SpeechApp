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
