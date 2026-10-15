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
    private val callbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()
    private val started = ConcurrentHashMap.newKeySet<String>()
    private var ready = false
    private var failed = false
    private var pending: (() -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status -> main.post { onInit(status) } }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.let { started.add(it) }
            }
            override fun onDone(utteranceId: String?) = finish(utteranceId, true)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId, false)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId, false)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId, true)
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
            callbacks.values.forEach { cb -> main.post { cb(false) } }
            callbacks.clear()
        }
    }

    private fun finish(id: String?, spoken: Boolean) {
        val cb = id?.let { callbacks.remove(it) } ?: return
        started.remove(id)
        main.post { cb(spoken) }
    }

    override fun speakOne(text: String, rate: Float, onEnd: (Boolean) -> Unit) {
        if (failed) {
            main.post { onEnd(false) }
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
        // If it hasn't started within a few seconds, it isn't going to: report it now.
        main.postDelayed({ if (id !in started && callbacks.containsKey(id)) finish(id, false) }, 3500)
    }

    override fun cancel() {
        callbacks.clear()
        started.clear()
        pending = null
        if (ready) tts.stop()
    }

    override fun shutdown() {
        callbacks.clear()
        tts.shutdown()
    }
}
