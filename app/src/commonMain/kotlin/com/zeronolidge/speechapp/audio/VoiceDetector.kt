package com.zeronolidge.speechapp.audio

import com.zeronolidge.speechapp.logic.VoiceMeter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Did the child say something?" Listens to microphone loudness for a few seconds (see
 * [VoiceMeter]) and answers true (voice heard), false (quiet), or null (no microphone, or it
 * couldn't be opened). Never answers more than once per [listen], and never hangs.
 */
class VoiceDetector(private val mic: MicLevels, val words: WordListener = WordListener.None) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var session = 0
    private var guard: Job? = null

    val available: Boolean get() = mic.available

    /** True while Rory is talking: a turn's microphone waits until he's done (it would hear him). */
    var isSpeaking: () -> Boolean = { false }

    /** The last try's voice: its loudest moment (0 to 1) and how long there was voice (ms). */
    var lastPeak = 0f
        private set
    var lastVoicedMs = 0
        private set
    private var wordsOn = false

    /**
     * The room's loudness, saved from the last talk turn and reused by the next one, so the room
     * is never measured while (or right after) Rory talks. Only the first turn of a sitting
     * needs [checkRoom].
     */
    var room: Float? = null
        private set

    /**
     * The first talk turn of a sitting: a short quiet check of the room, done in the pause
     * before Rory's prompt (never while he talks). Calls [then] at once if the room is already
     * known, or without a microphone; never hangs.
     */
    fun checkRoom(then: () -> Unit) {
        if (room != null || !mic.available) {
            then()
            return
        }
        cancel()
        val s = ++session
        val levels = ArrayList<Float>()
        fun done() {
            if (s != session) return
            cancel()
            if (levels.size >= 3) {
                val low = levels.sorted().take(maxOf(1, levels.size * 3 / 4))
                room = low.sum() / low.size
            }
            then()
        }
        guard = scope.launch {
            delay(VoiceMeter.ROOM_CHECK_MS + 1500L)
            done()
        }
        mic.start(
            onLevel = { rms ->
                if (s == session) {
                    levels += rms
                    if (levels.size * MicLevels.STEP_MS >= VoiceMeter.ROOM_CHECK_MS) done()
                }
            },
            onFail = { done() },
        )
    }

    fun listen(
        windowMs: Int = 4000,
        onLevel: (Float) -> Unit,
        words: List<String>? = null,
        onWords: ((List<Pair<String, Float>>?) -> Unit)? = null,
        onResult: (heard: Boolean?) -> Unit,
    ) =
        listenTry(windowMs, onLevel, onVoice = null, words = words, onWords = onWords) { t ->
            onResult(when (t) { VoiceTry.HEARD -> true; VoiceTry.NO_MIC -> null; else -> false })
        }

    /**
     * Like [listen], with more detail: [onVoice] fires the moment a clear voice is heard (about
     * 0.15 s after he starts, long before listening ends), so a story can act on his word at
     * once; the result also tells a soft try ([VoiceTry.SOFT]) from silence.
     */
    fun listenTry(
        windowMs: Int = 4000,
        onLevel: (Float) -> Unit,
        onVoice: (() -> Unit)?,
        /** Every reading (each 50 ms): how hard his voice pushes right now, 0 when quiet (see [VoiceMeter.push]). */
        onPush: ((Float) -> Unit)? = null,
        /** Word listening: this turn's word list ([com.zeronolidge.speechapp.logic.WordCheck.grammar]). */
        words: List<String>? = null,
        /** What the recogniser heard (null: nothing, not ready or failed), shortly after [onResult]. */
        onWords: ((List<Pair<String, Float>>?) -> Unit)? = null,
        onResult: (VoiceTry) -> Unit,
    ) {
        cancel()
        val s = ++session
        val meter = VoiceMeter(windowMs, knownRoom = room)
        var voiced = false
        val listenWords = words != null && this.words.ready
        fun done(result: VoiceTry) {
            if (s != session) return
            stopMic()
            if (result != VoiceTry.NO_MIC) meter.roomAfter()?.let { room = it }
            lastPeak = meter.peak
            lastVoicedMs = meter.voicedMs
            onResult(result)
            // The words come a moment later (the story has already moved on loudness).
            if (wordsOn) {
                wordsOn = false
                if (result == VoiceTry.NO_MIC) {
                    this.words.cancel()
                    onWords?.invoke(null)
                } else {
                    this.words.finish { onWords?.invoke(it) }
                }
            } else {
                onWords?.invoke(null)
            }
        }
        fun outcome() = when {
            meter.heard -> VoiceTry.HEARD
            meter.soft -> VoiceTry.SOFT
            else -> VoiceTry.QUIET
        }
        guard = scope.launch {
            // Never listen while Rory is still talking: the microphone would hear him.
            var waited = 0
            while (isSpeaking() && waited < 4000) {
                delay(100)
                waited += 100
            }
            if (s != session) return@launch
            if (listenWords) {
                this@VoiceDetector.words.begin(words!!)
                wordsOn = true
            }
            mic.start(
                onLevel = { rms ->
                    if (s == session) {
                        onLevel(VoiceMeter.display(rms))
                        val finished = meter.feed(rms, MicLevels.STEP_MS)
                        onPush?.invoke(meter.push(rms))
                        if (meter.heard && !voiced) {
                            voiced = true
                            onVoice?.invoke()
                        }
                        if (finished) done(outcome())
                    }
                },
                onFail = { done(VoiceTry.NO_MIC) },
            )
            // If the microphone never starts sending (a stuck permission prompt, say), give up kindly.
            delay(windowMs + 3000L)
            done(VoiceTry.NO_MIC)
        }
    }

    private fun stopMic() {
        session++
        guard?.cancel()
        mic.stop()
    }

    fun cancel() {
        stopMic()
        if (wordsOn) {
            wordsOn = false
            words.cancel()
        }
    }
}

/** How a listening turn went: a clear voice, a soft try, silence, or no microphone. */
enum class VoiceTry { HEARD, SOFT, QUIET, NO_MIC }
