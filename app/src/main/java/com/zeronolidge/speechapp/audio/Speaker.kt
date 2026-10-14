package com.zeronolidge.speechapp.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Small wrapper around Android's text-to-speech engine.
 * Speech is a little slower and higher than normal, which is easier for young listeners.
 */
class Speaker(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val ids = AtomicInteger()
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private var ready = false
    private var failed = false
    private val pending = mutableListOf<() -> Unit>()

    /** True while something is being said. */
    val speaking: Boolean get() = ready && tts.isSpeaking

    /** 1.0 is normal speed. Grown-ups can change this in settings. */
    var rate: Float = 0.85f

    private val tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status -> main.post { onInit(status) } }
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
            pending.forEach { it() }
            pending.clear()
        } else {
            // No speech engine: keep the app moving by finishing every request straight away.
            failed = true
            pending.clear()
            callbacks.values.forEach { main.post(it) }
            callbacks.clear()
        }
    }

    private fun finish(id: String?) {
        val cb = id?.let { callbacks.remove(it) } ?: return
        main.post(cb)
    }

    /**
     * Speaks [text], interrupting anything already being said.
     * [slow] uses half speed, for modelling a word carefully.
     * [onDone] runs on the main thread when speaking ends.
     */
    fun say(text: String, slow: Boolean = false, onDone: (() -> Unit)? = null) {
        speak(text, slow, TextToSpeech.QUEUE_FLUSH, onDone)
    }

    /** Like [say] but waits for anything already queued to finish first. */
    fun then(text: String, slow: Boolean = false, onDone: (() -> Unit)? = null) {
        speak(text, slow, TextToSpeech.QUEUE_ADD, onDone)
    }

    /** Adds a short silence to the queue. */
    fun pause(ms: Long) {
        val action = { tts.playSilentUtterance(ms, TextToSpeech.QUEUE_ADD, "p${ids.incrementAndGet()}"); Unit }
        if (ready) action() else pending += action
    }

    private fun speak(text: String, slow: Boolean, mode: Int, onDone: (() -> Unit)?) {
        if (failed) {
            onDone?.let { main.post(it) }
            return
        }
        val id = "u${ids.incrementAndGet()}"
        if (onDone != null) callbacks[id] = onDone
        val action = {
            // Speech rate is per-engine, so set it right before each utterance.
            tts.setSpeechRate(if (slow) rate * 0.55f else rate)
            tts.speak(text, mode, null, id)
            Unit
        }
        if (ready) action() else {
            if (mode == TextToSpeech.QUEUE_FLUSH) pending.clear()
            pending += action
        }
    }

    /** Stops talking. Unlike being interrupted by [say], this drops any pending onDone callbacks. */
    fun stop() {
        callbacks.clear()
        pending.clear()
        if (ready) tts.stop()
    }

    fun shutdown() {
        callbacks.clear()
        tts.shutdown()
    }
}
