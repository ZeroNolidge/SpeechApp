package com.zeronolidge.speechapp.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.File

/**
 * Listens for one short phrase with Android's speech recognizer.
 * Results always arrive on the main thread.
 */
class Listener(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * [onResult] gets every guess the recognizer made (best first), or an empty list
     * if it heard nothing it could understand.
     */
    fun listen(hint: String, onLevel: (Float) -> Unit = {}, onResult: (List<String>) -> Unit) {
        cancel()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        var delivered = false
        fun deliver(list: List<String>) {
            if (delivered) return
            delivered = true
            onResult(list)
            r.destroy()
            if (recognizer === r) recognizer = null
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) = onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) = deliver(emptyList())
            override fun onResults(results: Bundle?) =
                deliver(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Biasing towards the target word helps with young voices on newer devices.
            if (Build.VERSION.SDK_INT >= 33) {
                putExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, arrayListOf(hint))
            }
        }
        r.startListening(intent)
    }

    fun cancel() {
        recognizer?.let {
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }
}

/**
 * The "parrot": records the child for a few seconds, then plays it straight back,
 * so they can hear how they sound. Hearing yourself is a big part of speech practice.
 */
class Parrot(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private val file get() = File(context.cacheDir, "parrot.m4a")

    fun recordThenPlay(seconds: Int = 3, onPlaying: () -> Unit, onDone: () -> Unit) {
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

    fun stop() {
        main.removeCallbacksAndMessages(null)
        stopRecording()
        player?.release()
        player = null
    }
}
