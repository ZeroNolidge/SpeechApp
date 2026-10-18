package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.logic.Echo
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.PipSpot
import com.zeronolidge.speechapp.logic.PipStory
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.art.paperCircle
import com.zeronolidge.speechapp.ui.art.paperOval
import com.zeronolidge.speechapp.ui.art.paperRoundRect
import com.zeronolidge.speechapp.ui.art.star
import com.zeronolidge.speechapp.ui.components.ContinueAction
import com.zeronolidge.speechapp.ui.components.ContinueHost
import com.zeronolidge.speechapp.ui.components.ContinueReserve
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.wiggleOnTap
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission

private enum class PipPhase { INTRO, PROMPT, LISTEN, WAIT_TAP, REACT, END }

/**
 * "Lost Puppy Pip": he calls "Pip!" (or "Here, puppy!"), and every call brings Pip closer. The
 * bush, the rock and the log rustle, show two floppy ears and fold open, until Pip jumps out of
 * the log. Then it gets dark, he says goodnight, and Pip falls asleep. Every try works: a call the
 * microphone hears gets Rory's echo ("Pip! Hi, Pip!"), a quiet one still counts, and with no
 * microphone a big "I said it!" button stands in. Finding Pip puts a new tag on Pip's collar.
 */
@Composable
fun PipStoryScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    // His own words first, then a word for each of his target sounds: the treats that tempt Pip out.
    val turns = remember {
        val own = p.customWords.map { it.first }
        val sounds = p.focusSounds.mapNotNull { id -> SpeechContent.sounds.firstOrNull { it.id == id }?.words?.firstOrNull()?.text }
        PipStory.turns(p.speechStep, own + sounds)
    }
    val mic = rememberMicPermission()
    var index by remember { mutableIntStateOf(-1) }
    var phase by remember { mutableStateOf(PipPhase.INTRO) }
    var level by remember { mutableFloatStateOf(0f) }
    var rustle by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf(setOf<PipSpot>()) }
    var pipOut by remember { mutableStateOf(false) }
    var night by remember { mutableStateOf(false) }
    var asleep by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(0) }
    var line by remember { mutableStateOf("") }
    /** How many hops closer Pip has come: one on every call (0 = hiding behind the bush). */
    var hops by remember { mutableIntStateOf(0) }
    fun micOk() = p.useMic && app.voice.available && mic.granted()

    lateinit var prompt: (Int) -> Unit

    fun finish() {
        phase = PipPhase.END
        p.addPipTag()
        // Finding Pip opens the crystal cave: a reward stop on the map.
        p.updateEchoReady(true)
        line = "🏷️"
        app.celebrate(PipStory.ending(p.heroName))
        app.rememberInstruction("You found Pip! Tap the big green arrow to go back.")
    }

    fun react(k: Int, heard: Boolean) {
        if (phase == PipPhase.REACT || phase == PipPhase.END) return
        val s = session
        val turn = turns[k]
        phase = PipPhase.REACT
        app.listening = false
        level = 0f
        app.listenLevel = 0f
        val spot = turn.spot
        if (spot != null) {
            // Every call: Pip hops a little closer (and the spot rustles, for fun).
            hops = (hops + 1).coerceAtMost(PipStory.HOPS)
            rustle++
            if (turn.stage == PipStory.CALLS_PER_SPOT - 1) {
                opened = opened + spot
                if (spot == PipSpot.LOG) {
                    pipOut = true
                    app.celebrate("")
                }
            }
        } else {
            night = true
            asleep = true
        }
        // Every try works: heard or not, Pip comes closer.
        val said = if (heard) Echo.line(turn.call.trimEnd('!'), k) else "Pip heard you!"
        line = turn.reaction
        app.speaker.say("$said ${turn.reaction}") {
            if (s != session) return@say
            when {
                k + 1 >= turns.size -> finish()
                // After about 12 minutes of play: Pip hops out and gets sleepy (the goodnight turn).
                turns[k + 1].spot != null && app.sessionLong() -> {
                    hops = PipStory.HOPS
                    opened = opened + PipSpot.entries
                    pipOut = true
                    prompt(turns.lastIndex)
                }
                else -> prompt(k + 1)
            }
        }
    }

    /** This call's voice push (pops hop Pip a little, holds keep it coming); reset each call. */
    val push = remember { arrayOf(com.zeronolidge.speechapp.logic.VoicePush()) }

    lateinit var listen: (Int) -> Unit
    listen = { k ->
        val s = session
        phase = PipPhase.LISTEN
        app.listening = true
        val call = turns[k].call
        val model = push[0]
        // Word listening (listen-only here): a small record of what was heard.
        val check = app.wordCheck(call, null, modeled = !model.waiting)
        var lastBurst = 0
        app.voice.listenTry(
            4000, onLevel = { level = it; app.listenLevel = it }, onVoice = null,
            // Each pop (or a held sound) rustles Pip's hiding spot: his voice is reaching Pip.
            onPush = { v ->
                if (s == session && v > 0f) {
                    model.feed(v, 50)
                    if (model.mode == com.zeronolidge.speechapp.logic.VoicePush.Mode.BURSTS && model.bursts > lastBurst) { lastBurst = model.bursts; rustle++ }
                }
            },
            mode = model.mode,
            words = check?.grammar, onWords = { h -> check?.let { app.wordHeard(it, h) } },
        ) { t ->
            if (s != session || phase != PipPhase.LISTEN) return@listenTry
            level = 0f
            app.listenLevel = 0f
            if (t == com.zeronolidge.speechapp.audio.VoiceTry.NO_MIC) {
                phase = PipPhase.WAIT_TAP
                app.speaker.say("Call Pip out loud! Then tap the big green I said it button.")
                return@listenTry
            }
            when (model.afterTry(heard = t == com.zeronolidge.speechapp.audio.VoiceTry.HEARD, soft = t == com.zeronolidge.speechapp.audio.VoiceTry.SOFT)) {
                com.zeronolidge.speechapp.logic.VoicePush.Next.FINISH_GENTLY -> react(k, true)
                // Quiet (or a whisper, which still rustles the spot): call together, then one more listen.
                com.zeronolidge.speechapp.logic.VoicePush.Next.AGAIN_TOGETHER -> {
                    if (t == com.zeronolidge.speechapp.audio.VoiceTry.SOFT) rustle++
                    phase = PipPhase.PROMPT
                    app.listening = false
                    app.speaker.say("Let's call together!")
                    app.speaker.then(call, slow = true) { if (s == session) listen(k) }
                }
                // Still quiet: it waits for his voice or "I said it!" and never moves on by itself.
                com.zeronolidge.speechapp.logic.VoicePush.Next.WAIT -> if (model.tries > com.zeronolidge.speechapp.logic.VoicePush.MAX_TRIES) {
                    listen(k)
                } else {
                    phase = PipPhase.PROMPT
                    app.listening = false
                    app.speaker.say("Call Pip out loud! Then tap the big green I said it button.") { if (s == session) listen(k) }
                }
            }
        }
    }

    prompt = { k ->
        val s = session
        index = k
        phase = PipPhase.PROMPT
        val turn = turns[k]
        push[0] = com.zeronolidge.speechapp.logic.VoicePush(com.zeronolidge.speechapp.logic.VoicePush.modeForLine(turn.call, null))
        line = "🗣️ ${turn.call}"
        app.rememberInstruction(turn.prompt)
        app.speaker.say(turn.prompt) {
            if (s != session || phase != PipPhase.PROMPT) return@say
            if (micOk()) listen(k) else {
                phase = PipPhase.WAIT_TAP
                // No microphone: Rory still listens the same way, beside the "I said it!" button.
                app.listening = true
                app.speaker.say("Call Pip out loud! Then tap the big green I said it button.")
            }
        }
    }

    LaunchedEffect(Unit) {
        val intro = PipStory.intro(p.heroName)
        app.rememberInstruction(intro)
        val s = session
        // The first talk turn of a sitting: a quick quiet check of the room before Rory talks.
        val go = { if (s == session) app.speaker.say(intro) { if (s == session) prompt(0) } }
        if (micOk()) app.voice.checkRoom(go) else go()
    }
    DisposableEffect(Unit) {
        onDispose {
            session++
            app.voice.cancel()
            app.speaker.stop()
            app.listening = false
        }
    }

    ScreenFrame(onHome = nav.home, onBack = nav.back, background = { Scenery(Land.FOREST, Modifier.fillMaxSize(), seed = 77) }, top = {
        PaperChip("🐶", fontSize = 26.sp, maxLines = 1)
    }) {
        ContinueHost {
            val nightness by animateFloatAsState(if (night) 1f else 0f, tween(450), label = "night")
            // His turn: the listening look, with Rory sliding in on the left, clear of Pip's path.
            ListeningStage(Modifier.fillMaxSize(), endX = 0.24f, endY = 0.62f) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                val h = maxHeight
                // Night falls for the goodnight turns: a deep blue wash, a moon and stars.
                Canvas(Modifier.fillMaxSize()) {
                    if (nightness > 0f) {
                        drawRect(Color(0xFF1A2350).copy(alpha = 0.45f * nightness))
                        paperCircle(Color(0xFFFFF3C4).copy(alpha = nightness), size.minDimension * 0.06f, Offset(size.width * 0.86f, size.height * 0.12f))
                        listOf(0.2f to 0.1f, 0.4f to 0.18f, 0.62f to 0.08f, 0.72f to 0.25f).forEach { (x, y) ->
                            star(Offset(size.width * x, size.height * y), size.minDimension * 0.02f, Color(0xFFFFE082).copy(alpha = nightness))
                        }
                    }
                }
                val current = turns.getOrNull(index)?.spot
                val stage = turns.getOrNull(index)?.stage ?: 0
                // All three stand on the ground band (the lower half), far (small) to near (big).
                val unit = minOf(w, h * 1.4f)
                val logX = w * 0.52f
                val logY = h * 0.70f
                // Where each spot stands (centre x, centre y, size): far and small to near and big.
                val spots = mapOf(
                    PipSpot.BUSH to Triple(w * 0.80f, h * 0.60f, unit * 0.22f),
                    PipSpot.ROCK to Triple(w * 0.34f, h * 0.56f, unit * 0.26f),
                    PipSpot.LOG to Triple(logX, logY, unit * 0.32f),
                )
                // Pip's place along the way: behind the bush (hop 0), by the rock (3), by the log
                // (6), then out in front (9). Each call is one visible hop, with a little jump.
                val still = LocalReduceMotion.current
                val hopAt by animateFloatAsState(hops.toFloat(), tween(if (still) 0 else 500), label = "hops")
                val pipBig = unit * 0.30f
                val anchors = listOf(
                    Triple(spots.getValue(PipSpot.BUSH).first, spots.getValue(PipSpot.BUSH).second - spots.getValue(PipSpot.BUSH).third * 0.22f, spots.getValue(PipSpot.BUSH).third * 0.8f),
                    Triple(spots.getValue(PipSpot.ROCK).first, spots.getValue(PipSpot.ROCK).second - spots.getValue(PipSpot.ROCK).third * 0.22f, spots.getValue(PipSpot.ROCK).third * 0.8f),
                    Triple(logX, logY - spots.getValue(PipSpot.LOG).third * 0.2f, spots.getValue(PipSpot.LOG).third * 0.8f),
                    Triple(logX, logY + pipBig * 0.1f, pipBig),
                )
                val per = PipStory.CALLS_PER_SPOT.toFloat()
                val seg = (hopAt / per).toInt().coerceIn(0, anchors.size - 2)
                val f = ((hopAt - seg * per) / per).coerceIn(0f, 1f)
                val a = anchors[seg]
                val b = anchors[seg + 1]
                val pipX = a.first + (b.first - a.first) * f
                val pipY = a.second + (b.second - a.second) * f
                val pipH = a.third + (b.third - a.third) * f
                val inHop = hopAt - kotlin.math.floor(hopAt)
                val jump = if (still) 0.dp else pipH * 0.45f * kotlin.math.sin(inHop * kotlin.math.PI.toFloat())
                val drawPip: @Composable () -> Unit = {
                    if (!pipOut) {
                        Critter(
                            Species.PUPPY,
                            Modifier
                                .offset(x = pipX - pipH * 0.4f, y = pipY - pipH * 0.8f - jump)
                                .size(pipH * 0.8f, pipH)
                                .wiggleOnTap(),
                            mood = Mood.CHEER,
                            mirrored = b.first < a.first,
                        )
                    }
                }
                // Pip is behind a spot while sitting at it (only ears and head peek over), and in
                // front of the spots it has already passed.
                var pipDrawn = false
                spots.entries.forEachIndexed { i, (spot, at) ->
                    if (!pipDrawn && hopAt <= i * per + 0.05f) {
                        drawPip()
                        pipDrawn = true
                    }
                    PipHidingSpot(spot, at.first, at.second, at.third, current == spot, false, spot in opened, rustle)
                }
                if (!pipDrawn) drawPip()
                if (pipOut) {
                    val pipH = unit * 0.30f
                    Box(
                        Modifier
                            .offset(x = logX - pipH * 0.4f, y = logY - pipH * 0.75f)
                            .size(pipH * 0.8f, pipH)
                            .popIn("pip"),
                    ) {
                        if (asleep) {
                            // A cozy paper blanket.
                            Canvas(Modifier.fillMaxSize()) {
                                paperRoundRect(Color(0xFF7DB8E8), Offset(-size.width * 0.15f, size.height * 0.62f), Size(size.width * 1.3f, size.height * 0.38f), CornerRadius(size.height * 0.1f))
                            }
                        }
                        Critter(Species.PUPPY, Modifier.fillMaxSize().wiggleOnTap(), mood = if (asleep) Mood.SLEEP else Mood.CHEER)
                        PipCollar(p.pipTags, Modifier.fillMaxSize())
                    }
                }
                // The buddy and Rory, bottom-left. Rory cups his ear while listening.
                // Never into the bottom-right corner the green arrow uses; Rory only where he fits.
                val narrow = w < 520.dp
                Row(Modifier.align(Alignment.BottomStart).padding(start = 4.dp).height(h * if (narrow) 0.30f else 0.36f).widthIn(max = (w - ContinueReserve).coerceAtLeast(0.dp)), verticalAlignment = Alignment.Bottom) {
                    val species = p.hero ?: Species.KID
                    HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.fillMaxHeight().wiggleOnTap(), if (pipOut) Mood.CHEER else Mood.HAPPY, talking = phase == PipPhase.REACT)
                    // While listening Rory is the big one in the listening look instead.
                    if (!narrow && !app.listening) Critter(
                        Species.ROBOT, Modifier.fillMaxHeight(0.75f), Color(0xFFFFCC80),
                        mood = Mood.HAPPY,
                        talking = true,
                    )
                }
                if (line.isNotEmpty()) PaperChip(line, Modifier.align(Alignment.TopStart).padding(8.dp).widthIn(max = w * 0.6f), fontSize = 24.sp)
            }
            }
            // The big "I said it!" button is always there, in the same spot every turn (the way
            // on without a microphone, or any time); the big green arrow back at the end.
            ContinueAction(
                hint = phase == PipPhase.WAIT_TAP,
                label = if (phase == PipPhase.END) null else "I said it!",
            ) {
                when (phase) {
                    PipPhase.INTRO -> {
                        session++
                        app.speaker.stop()
                        prompt(0)
                    }
                    // Pressed while Rory is still asking: that counts as calling too.
                    PipPhase.PROMPT, PipPhase.WAIT_TAP -> {
                        app.speaker.stop()
                        react(index, heard = false)
                    }
                    PipPhase.LISTEN -> {
                        app.voice.cancel()
                        react(index, heard = false)
                    }
                    PipPhase.REACT -> {}
                    PipPhase.END -> if (app.sessionLong()) nav.go(Screen.Rest) else nav.back()
                }
            }
        }
    }
}


/** Pip's collar with a paper tag for every time he found Pip. */
@Composable
private fun PipCollar(tags: Int, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 100f
        val neckY = (88f + 15f) * u
        paperRoundRect(Color(0xFF1FA39A), Offset(32f * u, neckY - 3f * u), Size(36f * u, 6f * u), CornerRadius(3f * u))
        val colors = listOf(Color(0xFFF2B630), Color(0xFF9B7FE0), Color(0xFF5FD6CF), Color(0xFFE84A7F), Color(0xFF8CC63F))
        val n = tags.coerceIn(0, 7)
        for (i in 0 until n) {
            val x = 50f * u + (i - (n - 1) / 2f) * 7f * u
            paperCircle(colors[i % colors.size], 3.2f * u, Offset(x, neckY + 5f * u))
        }
    }
}

/**
 * A hiding spot drawn in paper. While Pip is in it, each call makes it rustle; from the second call
 * two floppy ears peek out; on the third it folds open (flat, like a pop-up page closing).
 */
@Composable
private fun PipHidingSpot(spot: PipSpot, cx: Dp, cy: Dp, size: Dp, current: Boolean, ears: Boolean, opened: Boolean, rustle: Int) {
    val still = LocalReduceMotion.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(rustle) {
        if (current && !still && rustle > 0) for (a in listOf(7f, -6f, 5f, -3f, 0f)) shake.animateTo(a, tween(60))
    }
    val fold by animateFloatAsState(if (opened) 1f else 0f, tween(if (still) 0 else 420), label = "fold")
    Box(
        Modifier
            .offset(x = cx - size / 2, y = cy - size / 2)
            .size(size)
            .wiggleOnTap()
            .graphicsLayer {
                rotationZ = shake.value
                transformOrigin = TransformOrigin(0.5f, 1f)
                cameraDistance = 10f * density
                rotationX = -78f * fold
                alpha = 1f - 0.6f * fold
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.width
            // Floppy ears peeking out (behind the spot's front).
            if (ears && !opened) {
                paperOval(Color(0xFF8B5E3C), Offset(s * 0.32f, s * 0.16f), Size(s * 0.13f, s * 0.24f))
                paperOval(Color(0xFF8B5E3C), Offset(s * 0.55f, s * 0.16f), Size(s * 0.13f, s * 0.24f))
            }
            when (spot) {
                PipSpot.BUSH -> {
                    paperCircle(Color(0xFF3F8C58), s * 0.26f, Offset(s * 0.32f, s * 0.6f))
                    paperCircle(Color(0xFF2C6B4E), s * 0.24f, Offset(s * 0.68f, s * 0.62f))
                    paperCircle(Color(0xFF5BAA5E), s * 0.28f, Offset(s * 0.5f, s * 0.46f))
                    paperCircle(Color(0xFFE2A72E), s * 0.04f, Offset(s * 0.4f, s * 0.42f))
                    paperCircle(Color(0xFFE2A72E), s * 0.04f, Offset(s * 0.63f, s * 0.55f))
                }
                PipSpot.ROCK -> {
                    paperOval(Color(0xFF8E8E9A), Offset(s * 0.08f, s * 0.36f), Size(s * 0.84f, s * 0.58f))
                    paperOval(Color(0xFFA9A9B5), Offset(s * 0.22f, s * 0.42f), Size(s * 0.4f, s * 0.22f))
                }
                PipSpot.LOG -> {
                    paperRoundRect(Color(0xFF8C5A3A), Offset(s * 0.04f, s * 0.48f), Size(s * 0.92f, s * 0.38f), CornerRadius(s * 0.19f))
                    paperCircle(Color(0xFFC9925E), s * 0.17f, Offset(s * 0.82f, s * 0.67f))
                    paperCircle(Color(0xFF8C5A3A), s * 0.08f, Offset(s * 0.82f, s * 0.67f))
                }
            }
        }
    }
}
