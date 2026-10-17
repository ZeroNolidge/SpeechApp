package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.audio.VoiceTry
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.VoiceStories
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.BeatProgress
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.VoiceStoryScene
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.components.ContinueAction
import com.zeronolidge.speechapp.ui.components.ContinueHost
import com.zeronolidge.speechapp.ui.components.ContinueReserve
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.peelIn
import com.zeronolidge.speechapp.ui.components.wiggleOnTap
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import kotlinx.coroutines.launch

private enum class VPhase { INTRO, MODEL, LISTEN, WAIT_TAP, ACT, END }

/**
 * A voice story (logic/VoiceStories.kt): his word makes each thing happen. Every beat: Rory names
 * the problem, says the word three times (slower the third time), then the listening look; the
 * moment the microphone hears a voice the action starts (a soft try: a smaller version), and once
 * he stops Rory says it back and adds one more ("Go! Go, boat!"). Quiet: "Let's say it together!"
 * and one more listen, then the "I said it!" button (always there) does the same action. A calm
 * goodbye beat, then a sticker.
 */
@Composable
fun VoiceStoryScreen(id: String, nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val story = remember(id) { VoiceStories.ready(VoiceStories.byId(id) ?: VoiceStories.boat, p.customWords) }
    val beats = story.beats
    val mic = rememberMicPermission()
    val scope = rememberCoroutineScope()
    val still = LocalReduceMotion.current
    var index by remember { mutableIntStateOf(-1) }
    var phase by remember { mutableStateOf(VPhase.INTRO) }
    var strengths by remember { mutableStateOf(mapOf<Int, Float>()) }
    var session by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(0) }
    var sticker by remember { mutableStateOf<WordStat?>(null) }
    val act = remember { Animatable(0f) }
    fun micOk() = p.useMic && app.voice.available && mic.granted()
    fun line(k: Int) = beats[k].line(p.speechStep)

    lateinit var startBeat: (Int) -> Unit

    /** His word (or the button) makes it happen, at once: [strength] 1 for a clear voice, less for a soft try. */
    fun action(k: Int, strength: Float) {
        if (strengths.containsKey(k)) return
        strengths = strengths + (k to strength)
        app.listening = false
        app.listenLevel = 0f
        scope.launch {
            act.snapTo(0f)
            act.animateTo(1f, tween(if (still) 0 else 1100))
        }
    }

    fun finish() {
        phase = VPhase.END
        p.finishVoiceStory(story.id)
        // The sticker for his collection (shiny).
        sticker = p.recordWord(story.sticker.first, story.sticker.second, clear = true, heard = false)
        app.celebrate(story.ending)
        app.rememberInstruction("${story.ending} Tap the big green arrow.")
    }

    /** After the action: Rory says it back and adds one more, tells what happened, and the story goes on. */
    fun after(k: Int, heard: Boolean, micUsed: Boolean) {
        val s = session
        phase = VPhase.ACT
        val b = beats[k]
        p.recordWord(b.word, b.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        p.addGem()?.let { app.showGift(it) }
        app.speaker.say("${b.echo} ${b.result}") {
            if (s != session) return@say
            when {
                k + 1 >= beats.size -> finish()
                // After about 12 minutes of play: straight to the calm goodbye beat.
                !beats[k + 1].goodbye && app.sessionLong() -> startBeat(beats.lastIndex)
                else -> startBeat(k + 1)
            }
        }
    }

    fun waitForTap(k: Int, ask: String) {
        phase = VPhase.WAIT_TAP
        // Rory still listens the same way, beside the "I said it!" button.
        app.listening = true
        app.speaker.say(ask)
        app.rememberInstruction("${line(k)}! $ask")
    }

    lateinit var listen: (Int) -> Unit
    listen = { k ->
        val s = session
        if (!micOk()) {
            waitForTap(k, "Say it out loud! Then tap the big green I said it button.")
        } else {
            phase = VPhase.LISTEN
            tries++
            app.listening = true
            app.voice.listenTry(
                4000,
                onLevel = { app.listenLevel = it },
                // The moment a voice is heard, the action starts (no waiting for the end).
                onVoice = { if (s == session) action(k, 1f) },
            ) { t ->
                if (s != session || phase != VPhase.LISTEN) return@listenTry
                app.listenLevel = 0f
                when (t) {
                    VoiceTry.HEARD -> {
                        action(k, 1f)
                        after(k, heard = true, micUsed = true)
                    }
                    // A soft try still works, just a little smaller.
                    VoiceTry.SOFT -> {
                        action(k, 0.55f)
                        after(k, heard = false, micUsed = true)
                    }
                    VoiceTry.QUIET -> if (tries < 2) {
                        phase = VPhase.MODEL
                        app.listening = false
                        app.speaker.say("Let's say it together!")
                        app.speaker.then(line(k), slow = true) { if (s == session) listen(k) }
                    } else {
                        waitForTap(k, "Say it with me, then tap I said it!")
                    }
                    VoiceTry.NO_MIC -> waitForTap(k, "Say it out loud! Then tap the big green I said it button.")
                }
            }
        }
    }

    startBeat = { k ->
        session++
        val s = session
        index = k
        tries = 0
        phase = VPhase.MODEL
        app.listening = false
        val b = beats[k]
        val say = line(k)
        app.rememberInstruction("${b.problem} $say. $say. $say.")
        // Rory names the problem, then says the word three times, slower the third time.
        app.speaker.say(b.problem)
        app.speaker.then(say)
        app.speaker.then(say)
        app.speaker.then(say, slow = true) { if (s == session && phase == VPhase.MODEL) listen(k) }
    }

    LaunchedEffect(Unit) {
        app.rememberInstruction(story.setup)
        val s = session
        app.speaker.say(story.setup) { if (s == session && phase == VPhase.INTRO) startBeat(0) }
    }
    DisposableEffect(Unit) {
        onDispose {
            session++
            app.voice.cancel()
            app.speaker.stop()
            app.listening = false
            app.listenLevel = 0f
        }
    }

    ScreenFrame(onHome = nav.home, background = {}, top = { PaperChip("${story.emoji} ${story.title}", fontSize = 20.sp, maxLines = 1) }) {
        ContinueHost {
            ListeningStage(Modifier.fillMaxSize(), endX = 0.5f, endY = 0.55f) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight
                    VoiceStoryScene(story, BeatProgress(index, strengths) { act.value }, Modifier.fillMaxSize())
                    // The word to say, big, at the top in the middle.
                    val k = index
                    if (k >= 0 && phase != VPhase.END && phase != VPhase.ACT) {
                        Row(
                            Modifier.align(Alignment.TopCenter).padding(top = 8.dp).widthIn(max = w * 0.9f).paperCard(PaperStyle.chip, 22.dp).padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(beats[k].emoji, fontSize = 40.sp, modifier = Modifier.wiggleOnTap())
                            Spacer(Modifier.width(10.dp))
                            Text(line(k), fontSize = if (line(k).length > 14) 28.sp else 40.sp, fontWeight = FontWeight.ExtraBold, color = PaperStyle.ink, maxLines = 1)
                        }
                    }
                    // The sticker he earned, peeling on in the middle at the end.
                    sticker?.let { st ->
                        Box(Modifier.align(Alignment.Center)) { Sticker(st, minOf(h * 0.35f, 180.dp), Modifier.peelIn(st.text)) }
                    }
                    // The buddy and Rory, bottom-left, clear of the green button's corner.
                    val narrow = w < 520.dp
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(start = 4.dp).height(h * if (narrow) 0.24f else 0.3f).widthIn(max = (w - ContinueReserve).coerceAtLeast(0.dp)),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val species = p.hero ?: Species.KID
                        HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.fillMaxHeight().wiggleOnTap(), if (phase == VPhase.END) Mood.CHEER else Mood.HAPPY)
                        if (!narrow && !app.listening) Critter(Species.ROBOT, Modifier.fillMaxHeight(0.75f), Color(0xFFFFCC80), mood = Mood.HAPPY, talking = phase == VPhase.MODEL || phase == VPhase.ACT)
                    }
                }
            }
            // "I said it!" is always there in the same spot: it does the same action as his voice.
            ContinueAction(
                hint = phase == VPhase.WAIT_TAP || phase == VPhase.END,
                label = if (phase == VPhase.END || phase == VPhase.INTRO) null else "I said it!",
            ) {
                when (phase) {
                    VPhase.INTRO -> {
                        app.speaker.stop()
                        startBeat(0)
                    }
                    VPhase.MODEL, VPhase.LISTEN, VPhase.WAIT_TAP -> {
                        val k = index
                        session++
                        app.voice.cancel()
                        app.speaker.stop()
                        action(k, 1f)
                        after(k, heard = false, micUsed = false)
                    }
                    VPhase.ACT -> {}
                    VPhase.END -> if (app.sessionLong()) nav.go(Screen.Rest) else nav.home()
                }
            }
        }
    }
}
