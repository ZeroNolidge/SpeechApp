package com.zeronolidge.speechapp.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android's text-to-speech engine, one utterance at a time. [QueueSpeaker] handles the queue and
 * time limits on top. Speech is a little higher than normal, which is easier for young listeners.
 */
class AndroidVoiceEngine(context: Context) : VoiceEngine {
    private val main = Handler(Looper.getMainLooper())
    private val ids = AtomicInteger()
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private var ready = false
    private var failed = false
    private var pending: (() -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status -> main.post { onInit(status) } }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
            }
            tts.setPitch(1.1f)
            ready = true
            pending?.invoke()
            pending = null
        } else {
            // No speech engine: finish everything straight away so the app keeps moving.
            failed = true
            pending = null
            callbacks.values.forEach { main.post(it) }
            callbacks.clear()
        }
    }

    private fun finish(id: String?) {
        val cb = id?.let { callbacks.remove(it) } ?: return
        main.post(cb)
    }

    override fun speakOne(text: String, rate: Float, onEnd: () -> Unit) {
        if (failed) {
            main.post(onEnd)
            return
        }
        val id = "u${ids.incrementAndGet()}"
        callbacks[id] = onEnd
        val action = {
            tts.setSpeechRate(rate)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            Unit
        }
        if (ready) action() else pending = action
    }

    override fun cancel() {
        callbacks.clear()
        pending = null
        if (ready) tts.stop()
    }

    override fun shutdown() {
        callbacks.clear()
        tts.shutdown()
    }
}
