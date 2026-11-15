package com.zeronolidge.speechapp.ui

import com.zeronolidge.speechapp.audio.ClipPlayer
import com.zeronolidge.speechapp.audio.ClipVoiceEngine
import com.zeronolidge.speechapp.audio.VoiceEngine
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.ui.screens.PracticeKind
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test55 (Timothy: "the audio keeps playing even if the screen changes"): every screen change
 * goes through [navOver], which stops all of Rory's sound before the screen changes, and that
 * stop cuts the clip mid-sound and cancels the device voice.
 */
class NavStopsAudioTest {
    @Test
    fun everyScreenChangeStopsAllAudioFirst() {
        val stack = mutableListOf<Screen>(Screen.Map)
        val log = mutableListOf<String>()
        val nav = navOver(stack) { log += "stop@${stack.size}:${stack.last()}" }
        nav.go(Screen.Practice)
        nav.go(Screen.PracticeGame(PracticeKind.COUNT))
        nav.replace(Screen.PracticeGame(PracticeKind.ADD))
        nav.back()
        nav.go(Screen.LandGames(Land.MEADOW))
        nav.home()
        nav.back() // already at the map: still quiet
        // Stopped before each move (the old screen still on top), once per move.
        assertEquals(
            listOf(
                "stop@1:Map", "stop@2:Practice", "stop@3:PracticeGame(kind=COUNT)", "stop@3:PracticeGame(kind=ADD)",
                "stop@2:Practice", "stop@3:LandGames(land=MEADOW)", "stop@1:Map",
            ),
            log,
        )
        assertEquals(listOf<Screen>(Screen.Map), stack)
    }

    @Test
    fun stoppingCutsTheClipAndTheDeviceVoice() {
        var clipStops = 0
        var voiceCancels = 0
        var clipEnd: ((Boolean) -> Unit)? = null
        val clips = object : ClipPlayer {
            override val ready = true
            override fun whenReady(maxMs: Int, then: () -> Unit) = then()
            override fun has(key: String) = true
            override fun play(keys: List<String>, slow: Boolean, onEnd: (ok: Boolean) -> Unit) { clipEnd = onEnd }
            override fun stop() { clipStops++ }
        }
        val device = object : VoiceEngine {
            override fun speakOne(text: String, rate: Float, onEnd: (spoken: Boolean) -> Unit) {}
            override fun cancel() { voiceCancels++ }
            override fun shutdown() {}
        }
        val engine = ClipVoiceEngine(clips, device)
        var ended = 0
        engine.speakOne("Let's count!", 1f) { ended++ }
        engine.cancel()
        assertEquals(1, clipStops, "the clip playing is stopped")
        assertEquals(1, voiceCancels, "the device voice is cancelled (speechSynthesis.cancel())")
        // A late "ended" from the cut clip doesn't carry the old line on.
        clipEnd?.invoke(true)
        assertEquals(0, ended)
    }
}
