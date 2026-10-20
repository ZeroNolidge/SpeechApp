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
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.zeronolidge.speechapp.data.SpeechContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.audio.VoiceTry
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.VoiceStories
import com.zeronolidge.speechapp.logic.VoicePush
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.placeKey
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.VoiceBeatScene
import com.zeronolidge.speechapp.logic.Move
import com.zeronolidge.speechapp.ui.art.VoiceListenExtras
import com.zeronolidge.speechapp.ui.art.HolePunchConfetti
import com.zeronolidge.speechapp.ui.art.voiceTarget
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableFloatStateOf
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.components.popIn
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
 * the problem, says the word three times (slower the third time), then the listening look (the
 * thing to move glows). His voice pushes the action live ([VoicePush]): louder pushes further,
 * holding the sound keeps it moving, shown as paper wind strips from Rory. Any voice finishes it
 * (the rest gently); a quiet try nudges it and Rory says "Again, together!"; the second try and
 * "I said it!" always finish it. Every success: hole-punch confetti, then Rory says it back and
 * adds one more ("Go! Go, boat!"). A calm goodbye beat, then a sticker.
 */
@Composable
fun VoiceStoryScreen(id: String, nav: Nav, firstBeat: Int = 0) {
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
    var sticker by remember { mutableStateOf<WordStat?>(null) }
    /** The current beat's action, 0 to 1, moved live by his voice (see [VoicePush]). */
    val act = remember { Animatable(0f) }
    /** How hard his voice pushes right now (0 when quiet), for the paper wind strips. */
    var pushNow by remember { mutableFloatStateOf(0f) }
    /** Hole-punch confetti: where, and its 0 to 1 run (1.6 s). */
    var confettiAt by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val confetti = remember { Animatable(1f) }
    val model = remember { arrayOf(VoicePush()) }
    /** Modelling on: the sound to model after this beat's move, and whether it was modelled already. */
    val modelSound = remember { arrayOf<String?>(null) }
    val modelledBeat = remember { arrayOf(false) }
    /** The sound whose mouth shape Rory shows while he models it. */
    var mouthCue by remember { mutableStateOf<String?>(null) }
    /** One pulse of the word chip per word Rory models (the slow third one bigger). */
    var chipBeat by remember { mutableIntStateOf(0) }
    var chipBig by remember { mutableStateOf(false) }
    val chipPulse = remember { Animatable(1f) }
    LaunchedEffect(chipBeat) {
        if (chipBeat > 0 && !still) {
            chipPulse.animateTo(if (chipBig) 1.25f else 1.12f, tween(if (chipBig) 420 else 160))
            chipPulse.animateTo(1f, tween(if (chipBig) 480 else 220))
        }
    }
    fun micOk() = p.useMic && app.voice.available && mic.granted()
    fun line(k: Int) = beats[k].line(p.speechStep)
    fun strength(k: Int) = strengths[k] ?: 0.7f

    lateinit var startBeat: (Int) -> Unit

    fun finish() {
        phase = VPhase.END
        p.finishVoiceStory(story.id)
        // The sticker for his collection (shiny).
        sticker = p.recordWord(story.sticker.first, story.sticker.second, clear = true, heard = false)
        app.celebrate(story.ending)
        app.rememberInstruction("${story.ending} Tap the big green arrow.")
    }

    /** It worked: hole-punch confetti at the thing that moved (gone in under 2 s). */
    fun cheer(k: Int) {
        confettiAt = voiceTarget(beats[k], act.value, strength(k))
        scope.launch {
            confetti.snapTo(0f)
            confetti.animateTo(1f, tween(if (still) 0 else 1600))
        }
    }

    /** After the action: Rory says it back and adds one more, tells what happened, and the story goes on. */
    fun after(k: Int, heard: Boolean, micUsed: Boolean) {
        val s = session
        // Modelling on, after a confident near-miss: the move has played; now Rory models the
        // sound once, clearly, with his mouth shape, and listens for one more try (any try is fine).
        modelSound[0]?.let { id ->
            modelSound[0] = null
            modelledBeat[0] = true
            val target = SpeechContent.sounds.first { it.id == id }
            phase = VPhase.MODEL
            app.listening = false
            mouthCue = id
            app.speaker.say("Listen. ${target.modelSound}. ${line(k)}.")
            app.speaker.then("One more time!") {
                if (s != session) return@then
                mouthCue = null
                phase = VPhase.LISTEN
                app.listening = true
                val again = app.wordCheck(line(k), beats[k].sound, modeled = true)
                app.voice.listenTry(4000, onLevel = { app.listenLevel = it }, onVoice = null, words = again?.grammar,
                    onWords = { h -> again?.let { app.wordHeard(it, h) } }) { _ ->
                    if (s != session) return@listenTry
                    app.listening = false
                    app.listenLevel = 0f
                    after(k, heard, micUsed)
                }
            }
            return
        }
        phase = VPhase.ACT
        val b = beats[k]
        p.recordWord(b.word, b.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        p.addGem()?.let { app.showGift(it) }
        app.speaker.say(b.success) {
            if (s != session) return@say
            when {
                k + 1 >= beats.size -> finish()
                // After about 12 minutes of play: straight to the calm goodbye beat.
                !beats[k + 1].goodbye && app.sessionLong() -> startBeat(beats.lastIndex)
                else -> startBeat(k + 1)
            }
        }
    }

    /** The rest of the action at a gentle speed ("I said it!", or after a try), then confetti and Rory's echo. */
    fun finishGently(k: Int, heard: Boolean, micUsed: Boolean) {
        val s = session
        app.listening = false
        pushNow = 0f
        strengths = strengths + (k to model[0].strength)
        phase = VPhase.ACT
        scope.launch {
            if (act.value < 1f) act.animateTo(1f, tween(if (still) 0 else (1400 * (1f - act.value)).toInt().coerceAtLeast(250)))
            if (s != session) return@launch
            cheer(k)
            after(k, heard, micUsed)
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
            app.listening = true
            var reached = false
            // Word listening: records what was heard (listen-only). With modelling on, the
            // answer waits (about a second at most) for the words; the action has already moved.
            val check = app.wordCheck(line(k), beats[k].sound, modeled = !model[0].waiting)
            val waitWords = check != null && app.modelingOn && !modelledBeat[0]
            var loud: VoiceTry? = null
            lateinit var handle: (VoiceTry, Pair<com.zeronolidge.speechapp.logic.Heard, Float>?) -> Unit
            app.voice.listenTry(
                4000,
                onLevel = { app.listenLevel = it },
                onVoice = null,
                // Every 50 ms: louder pushes further, holding the sound keeps it moving.
                onPush = { push ->
                    if (s == session && phase == VPhase.LISTEN) {
                        pushNow = push
                        if (push > 0f && !reached) {
                            reached = model[0].feed(push, 50)
                            strengths = strengths + (k to model[0].strength)
                            val to = model[0].progress
                            scope.launch { act.animateTo(to, tween(if (still) 0 else 70)) }
                            if (reached) cheer(k)
                        }
                    }
                },
                words = check?.grammar,
                onWords = { heard ->
                    val judged = check?.let { app.wordHeard(it, heard) }
                    if (waitWords) loud?.let { handle(it, judged) }
                },
                mode = model[0].mode,
            ) { t -> if (waitWords) loud = t else handle(t, null) }
            handle = handle@{ t, judged ->
                if (s != session || phase != VPhase.LISTEN) return@handle
                app.listenLevel = 0f
                pushNow = 0f
                if (t == VoiceTry.NO_MIC) {
                    waitForTap(k, "Say it out loud! Then tap the big green I said it button.")
                    return@handle
                }
                when (model[0].afterTry(heard = t == VoiceTry.HEARD, soft = t == VoiceTry.SOFT)) {
                    // He spoke: the rest of the action goes on by itself (gently), then confetti.
                    VoicePush.Next.FINISH_GENTLY -> {
                        if (waitWords && check != null && judged != null) {
                            val reply = com.zeronolidge.speechapp.logic.Corrections.reply(check, judged.first, judged.second, app.corrections())
                            val canModel = reply == com.zeronolidge.speechapp.logic.Reply.MODEL_SOUND && SpeechContent.sounds.any { it.id == check.sound }
                            app.corrections().noteReply(check.sound, canModel)
                            // A clear match: the full-strength move.
                            if (reply == com.zeronolidge.speechapp.logic.Reply.CELEBRATE) strengths = strengths + (k to 1f)
                            // A confident near-miss: after the move, Rory models the sound once.
                            if (canModel) modelSound[0] = check.sound
                        }
                        if (reached) {
                            phase = VPhase.ACT
                            app.listening = false
                            after(k, heard = t == VoiceTry.HEARD, micUsed = true)
                        } else {
                            finishGently(k, heard = t == VoiceTry.HEARD, micUsed = true)
                        }
                    }
                    // Quiet: it still moves a little, then "Again, together!" and one more listen.
                    VoicePush.Next.AGAIN_TOGETHER -> {
                        phase = VPhase.MODEL
                        app.listening = false
                        strengths = strengths + (k to model[0].strength)
                        val to = model[0].progress
                        scope.launch { act.animateTo(to, tween(if (still) 0 else 500)) }
                        app.speaker.say("Again, together!")
                        app.speaker.then(line(k), slow = true) { if (s == session) listen(k) }
                    }
                    // Still quiet after two tries: it waits for his voice or "I said it!" and
                    // never moves on by itself. Rory asks once (not listening while he talks),
                    // then just listens.
                    VoicePush.Next.WAIT -> if (model[0].tries > VoicePush.MAX_TRIES) {
                        listen(k)
                    } else {
                        phase = VPhase.MODEL
                        app.listening = false
                        app.speaker.say("Say it out loud! Then tap the big green I said it button.") { if (s == session) listen(k) }
                        app.rememberInstruction("${line(k)}! Say it out loud! Then tap the big green I said it button.")
                    }
                }
            }
        }
    }

    startBeat = { k ->
        session++
        val s = session
        index = k
        // A page reload carries on from this beat.
        app.notePlace(com.zeronolidge.speechapp.ui.Screen.VoiceStory(id, k).placeKey())
        model[0] = VoicePush(VoicePush.modeFor(beats[k].sound))
        modelSound[0] = null
        modelledBeat[0] = false
        mouthCue = null
        pushNow = 0f
        scope.launch { act.snapTo(0f) }
        phase = VPhase.MODEL
        app.listening = false
        val b = beats[k]
        val say = line(k)
        app.rememberInstruction("${b.problem} $say. $say. $say.")
        // Rory names the problem, then says the word three times, slower the third time; the
        // word chip pulses once as he starts each one (the slow third one bigger and longer).
        fun pulse(big: Boolean) { if (s == session) { chipBig = big; chipBeat++ } }
        app.speaker.say(b.problem) { pulse(false) }
        app.speaker.then(say) { pulse(false) }
        app.speaker.then(say) { pulse(true) }
        app.speaker.then(say, slow = true) { if (s == session && phase == VPhase.MODEL) listen(k) }
    }

    LaunchedEffect(Unit) {
        app.rememberInstruction(story.setup)
        val s = session
        // The first talk turn of a sitting: a quick quiet check of the room before Rory talks.
        val first = firstBeat.coerceIn(0, beats.lastIndex)
        val go = { if (s == session) app.speaker.say(story.setup) { if (s == session && phase == VPhase.INTRO) startBeat(first) } }
        if (micOk()) app.voice.checkRoom(go) else go()
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
            // Rory stands in the one listening spot, moved aside only if he'd cover the path of
            // the thing his voice moves.
            val beat = beats[index.coerceAtLeast(0)]
            ListeningStage(
                Modifier.fillMaxSize(),
                aboveDim = { rory ->
                    // The thing to move glows; his voice streams to it as paper wind strips.
                    if (index >= 0 && phase != VPhase.END) {
                        VoiceListenExtras(
                            target = { voiceTarget(beat, act.value, strength(index)) },
                            fromX = rory.x, fromY = rory.y,
                            // The glow shows what will move while Rory names it, and hides on his
                            // turn so it never overlaps Rory's ear.
                            glowing = phase == VPhase.MODEL && !app.listening,
                            push = { pushNow },
                        )
                    }
                    HolePunchConfetti(confettiAt, { confetti.value })
                },
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight
                    VoiceBeatScene(beat, { if (index < 0) 0f else act.value }, strength(index), { pushNow }, Modifier.fillMaxSize().clipToBounds())
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
                        // Rory beside the buddy, his mouth going as he models each word (on a phone
                        // too while he talks; the big listening Rory takes over on his turn).
                        if (listenLookGone() && (!narrow || phase == VPhase.MODEL)) Critter(Species.ROBOT, Modifier.fillMaxHeight(0.75f), Color(0xFFFFCC80), mood = Mood.HAPPY, talking = phase == VPhase.MODEL || phase == VPhase.ACT)
                    }
                }
            }
            // The word to say, big, at the top: drawn over everything (scene, dim and the glowing
            // prop), so it's never hidden. When the prop starts high (it sinks), the word sits
            // top-left, clear of it.
            val k = index
            // After the action: Rory's echo adds a word, and the new words slide in on paper
            // ("go" to "go, boat").
            if (k >= 0 && phase == VPhase.ACT) {
                val more = beats[k].line(p.speechStep + 1)
                Row(
                    Modifier.align(if (beats[k].move == Move.SINK) Alignment.TopStart else Alignment.TopCenter)
                        .padding(top = 8.dp, start = 8.dp).widthIn(max = 560.dp).zIndex(4f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(beats[k].emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(8.dp))
                    ExpandChip(line(k), more)
                }
            }
            if (k >= 0 && phase != VPhase.END && phase != VPhase.ACT) {
                Row(
                    Modifier.align(if (beats[k].move == Move.SINK) Alignment.TopStart else Alignment.TopCenter)
                        .padding(top = 8.dp, start = 8.dp)
                        .widthIn(max = 560.dp)
                        .zIndex(4f)
                        .graphicsLayer { scaleX = chipPulse.value; scaleY = chipPulse.value }
                        .paperCard(PaperStyle.chip, 22.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(beats[k].emoji, fontSize = 40.sp, modifier = Modifier.wiggleOnTap())
                    Spacer(Modifier.width(10.dp))
                    Text(line(k), fontSize = if (line(k).length > 14) 28.sp else 40.sp, fontWeight = FontWeight.ExtraBold, color = PaperStyle.ink, maxLines = 1)
                }
            }
            // Rory models a sound: his mouth shape for it, big, top-right.
            mouthCue?.let { MouthCue(it, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).zIndex(4f).popIn("mouth")) }
            // "I said it!" is always there in the same spot: it does the same action as his voice.
            ContinueAction(
                hint = phase == VPhase.WAIT_TAP || phase == VPhase.END || (phase == VPhase.LISTEN && model[0].waiting),
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
                        // The same action as his voice, at a gentle speed.
                        model[0].pressed()
                        finishGently(k, heard = false, micUsed = false)
                    }
                    VPhase.ACT -> {}
                    VPhase.END -> if (app.sessionLong()) nav.go(Screen.Rest) else nav.home()
                }
            }
        }
    }
}
