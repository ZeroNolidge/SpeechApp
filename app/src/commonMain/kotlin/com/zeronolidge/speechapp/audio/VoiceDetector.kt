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

    /** When the last listening turn's microphone opened (taps right after are ignored). */
    var turnMark: kotlin.time.TimeMark? = null
        private set
    private var wordsOn = false

    /**
     * The room's loudness, saved from the last talk turn and reused by the next one, so the room
     * is never measured while (or right after) Rory talks. Only the first turn of a sitting
     * needs [checkRoom].
     */
    var room: Float? = null
        private set

    /** Called with each newly saved room level, so it's remembered across page loads. */
    var onRoom: (Float) -> Unit = {}

    /** A room level remembered from an earlier visit (capped, see [VoiceMeter.SAVED_ROOM_MAX]). */
    fun restoreRoom(saved: Float?) {
        if (room == null && saved != null && saved > 0f) room = minOf(saved, VoiceMeter.SAVED_ROOM_MAX)
    }

    /** What counts as voice right now (see [VoiceMeter.threshold]), for the test-only VOICETURN log. */
    val threshold: Float get() = maxOf(VoiceMeter.MIN_VOICE, (room ?: 0f) * VoiceMeter.ROOM_FACTOR)

    private fun saveRoom(v: Float) {
        room = v
        onRoom(v)
    }

    /**
     * The first talk turn of a sitting: a short quiet check of the room, done in the pause
     * before Rory's prompt, once anything still being said (the story's title) has finished. Calls [then] at once if the room is already
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
                saveRoom(low.sum() / low.size)
            }
            then()
        }
        guard = scope.launch {
            // Only once Rory (or the story title) has finished: the microphone would hear him.
            var waited = 0
            while (isSpeaking() && waited < 8000) {
                delay(100)
                waited += 100
            }
            delay(200)
            if (s != session) return@launch
            mic.start(
                onLevel = { rms ->
                    if (s == session) {
                        levels += rms
                        if (levels.size * MicLevels.STEP_MS >= VoiceMeter.ROOM_CHECK_MS) done()
                    }
                },
                onFail = { done() },
            )
            delay(VoiceMeter.ROOM_CHECK_MS + 1500L)
            done()
        }
    }

    fun listen(
        windowMs: Int = 4000,
        onLevel: (Float) -> Unit,
        words: List<String>? = null,
        onWords: ((List<Pair<String, Float>>?) -> Unit)? = null,
        onResult: (heard: Boolean?) -> Unit,
    ) =
        listenTry(windowMs, onLevel, onVoice = null, words = words, onWords = onWords, onResult = { t ->
            onResult(when (t) { VoiceTry.HEARD -> true; VoiceTry.NO_MIC -> null; else -> false })
        })

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
        /** Pop sounds (p b t d g k): one short burst is a clear try; others need a little longer. */
        mode: com.zeronolidge.speechapp.logic.VoicePush.Mode = com.zeronolidge.speechapp.logic.VoicePush.Mode.LOUDNESS,
        onResult: (VoiceTry) -> Unit,
    ) {
        cancel()
        val s = ++session
        val pops = mode == com.zeronolidge.speechapp.logic.VoicePush.Mode.BURSTS
        val meter = VoiceMeter(windowMs, needMs = if (pops) 50 else 90, knownRoom = room, pops = pops)
        var voiced = false
        val listenWords = words != null && this.words.ready
        fun done(result: VoiceTry) {
            if (s != session) return
            stopMic()
            if (result != VoiceTry.NO_MIC) meter.roomAfter()?.let { saveRoom(it) }
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
            turnMark = kotlin.time.TimeSource.Monotonic.markNow()
            if (listenWords) {
                this@VoiceDetector.words.begin(words!!)
                wordsOn = true
            }
            mic.start(
                onLevel = { rms ->
                    if (s == session) {
                        onLevel(VoiceMeter.display(rms))
                        val finished = meter.feed(rms, MicLevels.STEP_MS, mic.band, mic.periodic)
                        onPush?.invoke(meter.lastPush)
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
            // (A long held sound can keep listening past the window, up to HOLD_MAX_MS.)
            delay(windowMs + VoiceMeter.HOLD_MAX_MS + 2000L)
            done(VoiceTry.NO_MIC)
        }
    }

    /**
     * Watches the mic while Rory is still talking a model or prompt line (never while he's
     * waiting for a turn to finish; see [listenTry]'s own wait): a voice clearly over the room,
     * held [BARGE_MS], calls [onBargeIn] once and stops. The caller then stops Rory mid-line and
     * treats the turn as his (barge-in): "if he's clearly already talking, don't make him wait
     * for Rory to finish before it counts."
     */
    fun watchBargeIn(onBargeIn: () -> Unit) {
        if (!mic.available) return
        cancel()
        val s = ++session
        var overMs = 0
        val bar = maxOf(VoiceMeter.MIN_VOICE, (room ?: 0f) * BARGE_FACTOR)
        mic.start(
            onLevel = { rms ->
                if (s == session) {
                    if (rms > bar) {
                        overMs += MicLevels.STEP_MS
                        if (overMs >= BARGE_MS) {
                            overMs = 0
                            stopMic()
                            onBargeIn()
                        }
                    } else {
                        overMs = 0
                    }
                }
            },
            onFail = {},
        )
    }

    /** Stops a [watchBargeIn] in progress; harmless if there isn't one. */
    fun stopBargeIn() = cancel()

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

    /** Fully releases the microphone (leaving a voice story, or the app going to the background).
     * Between turns [cancel]/[stop] alone is enough: the web keeps the stream open so the next
     * turn starts at once (see [com.zeronolidge.speechapp.audio.MicLevels.release]). */
    fun release() {
        cancel()
        mic.release()
    }

    companion object {
        /** A voice clearly his own (not an echo of Rory's own, echo-cancelled, playback) while
         * Rory is still talking: louder than a plain try's bar, so his own faint playback leak
         * never triggers it. */
        private const val BARGE_FACTOR = 3f

        /** How long a loud-enough sound must hold while Rory's talking to count as barge-in
         * (one or two readings is as likely a click or cough as a word). */
        private const val BARGE_MS = 150
    }
}

/** How a listening turn went: a clear voice, a soft try, silence, or no microphone. */
enum class VoiceTry { HEARD, SOFT, QUIET, NO_MIC }
