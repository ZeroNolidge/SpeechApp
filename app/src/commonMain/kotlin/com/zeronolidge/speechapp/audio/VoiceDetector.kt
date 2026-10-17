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
class VoiceDetector(private val mic: MicLevels) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var session = 0
    private var guard: Job? = null

    val available: Boolean get() = mic.available

    fun listen(windowMs: Int = 4000, onLevel: (Float) -> Unit, onResult: (heard: Boolean?) -> Unit) =
        listenTry(windowMs, onLevel, onVoice = null) { t ->
            onResult(when (t) { VoiceTry.HEARD -> true; VoiceTry.NO_MIC -> null; else -> false })
        }

    /**
     * Like [listen], with more detail: [onVoice] fires the moment a clear voice is heard (about
     * 0.15 s after he starts, long before listening ends), so a story can act on his word at
     * once; the result also tells a soft try ([VoiceTry.SOFT]) from silence.
     */
    fun listenTry(windowMs: Int = 4000, onLevel: (Float) -> Unit, onVoice: (() -> Unit)?, onResult: (VoiceTry) -> Unit) {
        cancel()
        val s = ++session
        val meter = VoiceMeter(windowMs)
        var voiced = false
        fun done(result: VoiceTry) {
            if (s != session) return
            cancel()
            onResult(result)
        }
        fun outcome() = when {
            meter.heard -> VoiceTry.HEARD
            meter.soft -> VoiceTry.SOFT
            else -> VoiceTry.QUIET
        }
        // If the microphone never starts sending (a stuck permission prompt, say), give up kindly.
        guard = scope.launch {
            delay(windowMs + 3000L)
            done(VoiceTry.NO_MIC)
        }
        mic.start(
            onLevel = { rms ->
                if (s == session) {
                    onLevel(VoiceMeter.display(rms))
                    val finished = meter.feed(rms, MicLevels.STEP_MS)
                    if (meter.heard && !voiced) {
                        voiced = true
                        onVoice?.invoke()
                    }
                    if (finished) done(outcome())
                }
            },
            onFail = { done(VoiceTry.NO_MIC) },
        )
    }

    fun cancel() {
        session++
        guard?.cancel()
        mic.stop()
    }
}

/** How a listening turn went: a clear voice, a soft try, silence, or no microphone. */
enum class VoiceTry { HEARD, SOFT, QUIET, NO_MIC }
