package com.zeronolidge.speechapp.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Talking, with the same behaviour on every platform. It speaks one utterance at a time from its
 * own queue, and every utterance has a time limit: if the platform voice never reports that it
 * finished (Chrome sometimes drops speech that starts right after a cancel, or cuts off long
 * sentences without telling anyone), the queue moves on anyway and [onTrouble] fires. So the
 * story can never get stuck waiting for speech.
 */
class QueueSpeaker(private val engine: VoiceEngine) : Speaker {
    private class Item(val id: Int, val text: String, val slow: Boolean, val onDone: (() -> Unit)?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queue = ArrayDeque<Item>()
    // Observable, so characters can wiggle while something is being said.
    private var current: Item? by mutableStateOf(null)
    private var watchdog: Job? = null
    private var nextId = 0
    /** A say() is waiting out its short gap before starting. */
    private var starting = false

    override var rate: Float = 0.85f
    override var onTrouble: (() -> Unit)? = null
    override val speaking: Boolean get() = current != null || queue.isNotEmpty()

    override fun say(text: String, slow: Boolean, onDone: (() -> Unit)?) {
        // Interrupted speech still reports "done" (later, not in the middle of this call),
        // so flows waiting on it carry on.
        val interrupted = listOfNotNull(current?.onDone) + queue.mapNotNull { it.onDone }
        clear()
        engine.cancel()
        queue.addLast(Item(++nextId, text, slow, onDone))
        starting = true
        scope.launch {
            interrupted.forEach { it() }
            // A short gap after cancelling: some browsers silently drop speech started straight away.
            delay(90)
            starting = false
            if (current == null) pump()
        }
    }

    override fun then(text: String, slow: Boolean, onDone: (() -> Unit)?) {
        queue.addLast(Item(++nextId, text, slow, onDone))
        if (current == null && !starting && queue.size == 1) scope.launch { if (current == null && !starting) pump() }
    }

    private fun pump() {
        val item = queue.removeFirstOrNull() ?: return
        current = item
        val r = if (item.slow) rate * 0.55f else rate
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(timeLimitMs(item.text, r))
            ended(item.id, ok = false)
        }
        engine.speakOne(item.text, r) { spoken -> ended(item.id, ok = spoken) }
    }

    private fun ended(id: Int, ok: Boolean) {
        val item = current ?: return
        if (item.id != id) return
        watchdog?.cancel()
        current = null
        if (!ok) {
            engine.cancel()
            onTrouble?.invoke()
        }
        item.onDone?.invoke()
        if (current == null && !starting) pump()
    }

    private fun clear() {
        watchdog?.cancel()
        current = null
        queue.clear()
    }

    override fun mouth(): MouthNow? = if (speaking) engine.mouth() else null

    override fun stop() {
        clear()
        engine.cancel()
    }

    override fun shutdown() {
        stop()
        engine.shutdown()
    }

    companion object {
        /** Generous: a few seconds to get going, plus time for the words at this speed. */
        fun timeLimitMs(text: String, rate: Float): Long = 4000L + (text.length * 95f / rate.coerceAtLeast(0.2f)).toLong()
    }
}
