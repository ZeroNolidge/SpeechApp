package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.zeronolidge.speechapp.logic.Echo
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.PipSpot
import com.zeronolidge.speechapp.logic.PipStory
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.Nav
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
import com.zeronolidge.speechapp.ui.components.LocalApp
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
 * microphone a big "I called!" button stands in. Finding Pip puts a new tag on Pip's collar.
 */
@Composable
fun PipStoryScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val turns = remember { PipStory.turns(p.speechStep) }
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
    fun micOk() = p.useMic && app.voice.available && mic.granted()

    lateinit var prompt: (Int) -> Unit

    fun finish() {
        phase = PipPhase.END
        p.addPipTag()
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
        val spot = turn.spot
        if (spot != null) {
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
            if (turn.stage >= 1) asleep = true
        }
        // Every try works: heard or not, Pip comes closer.
        val said = if (heard) Echo.line(turn.call.trimEnd('!'), k) else "Pip heard you!"
        line = turn.reaction
        app.speaker.say("$said ${turn.reaction}") {
            if (s != session) return@say
            if (k + 1 < turns.size) prompt(k + 1) else finish()
        }
    }

    fun listen(k: Int) {
        val s = session
        phase = PipPhase.LISTEN
        app.listening = true
        app.voice.listen(4000, onLevel = { level = it }) { heard ->
            if (s != session || phase != PipPhase.LISTEN) return@listen
            react(k, heard == true)
        }
    }

    prompt = { k ->
        val s = session
        index = k
        phase = PipPhase.PROMPT
        val turn = turns[k]
        line = "🗣️ ${turn.call}"
        app.rememberInstruction(turn.prompt)
        app.speaker.say(turn.prompt) {
            if (s != session || phase != PipPhase.PROMPT) return@say
            if (micOk()) listen(k) else {
                phase = PipPhase.WAIT_TAP
                app.speaker.say("Call Pip out loud! Then tap the big green I called button.")
            }
        }
    }

    LaunchedEffect(Unit) {
        val intro = PipStory.intro(p.heroName)
        app.rememberInstruction(intro)
        val s = session
        app.speaker.say(intro) { if (s == session) prompt(0) }
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
        PaperChip("🐶 Pip", fontSize = 22.sp, maxLines = 1)
    }) {
        ContinueHost {
            val nightness by animateFloatAsState(if (night) 1f else 0f, tween(450), label = "night")
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
                // Hiding spots, far (small, high) to near (big, low).
                PipHidingSpot(PipSpot.BUSH, w * 0.70f, h * 0.30f, minOf(w, h) * 0.26f, current == PipSpot.BUSH, stage, PipSpot.BUSH in opened, rustle)
                PipHidingSpot(PipSpot.ROCK, w * 0.48f, h * 0.40f, minOf(w, h) * 0.32f, current == PipSpot.ROCK, stage, PipSpot.ROCK in opened, rustle)
                PipHidingSpot(PipSpot.LOG, w * 0.26f, h * 0.52f, minOf(w, h) * 0.40f, current == PipSpot.LOG, stage, PipSpot.LOG in opened, rustle)
                if (pipOut) {
                    val pipH = minOf(w, h) * 0.34f
                    Box(
                        Modifier
                            .offset(x = w * 0.36f, y = h * 0.62f - pipH * 0.4f)
                            .size(pipH * 0.8f, pipH)
                            .popIn("pip"),
                    ) {
                        if (asleep) {
                            // A cozy paper blanket.
                            Canvas(Modifier.fillMaxSize()) {
                                paperRoundRect(Color(0xFF7DB8E8), Offset(-size.width * 0.15f, size.height * 0.62f), Size(size.width * 1.3f, size.height * 0.38f), CornerRadius(size.height * 0.1f))
                            }
                        }
                        Critter(Species.PUPPY, Modifier.fillMaxSize(), mood = if (asleep) Mood.SLEEP else Mood.CHEER)
                        PipCollar(p.pipTags, Modifier.fillMaxSize())
                    }
                }
                // The buddy and Rory, bottom-left. Rory cups his ear while listening.
                Row(Modifier.align(Alignment.BottomStart).padding(start = 4.dp).height(h * 0.42f), verticalAlignment = Alignment.Bottom) {
                    val species = p.hero ?: Species.KID
                    HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.height(h * 0.40f), if (pipOut) Mood.CHEER else Mood.HAPPY, talking = phase == PipPhase.REACT)
                    Critter(
                        Species.ROBOT, Modifier.height(h * 0.30f), Color(0xFFFFCC80),
                        mood = if (phase == PipPhase.LISTEN) Mood.LISTEN else Mood.HAPPY,
                        talking = phase != PipPhase.LISTEN,
                    )
                }
                if (phase == PipPhase.LISTEN) VoiceRing(level, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                if (line.isNotEmpty()) PaperChip(line, Modifier.align(Alignment.TopStart).padding(8.dp).fillMaxWidth(0.55f), fontSize = 18.sp)
            }
            // Always a way on: "I called!" without a microphone, a skip while listening, and
            // the big green arrow back at the end.
            ContinueAction(
                enabled = phase == PipPhase.WAIT_TAP || phase == PipPhase.LISTEN || phase == PipPhase.END,
                hint = phase != PipPhase.LISTEN,
                label = if (phase == PipPhase.WAIT_TAP) "I called!" else null,
            ) {
                when (phase) {
                    PipPhase.WAIT_TAP -> react(index, heard = false)
                    PipPhase.LISTEN -> {
                        app.voice.cancel()
                        react(index, heard = false)
                    }
                    else -> nav.back()
                }
            }
        }
    }
}

/** A paper ring that grows with his voice while Pip is listening for the call. */
@Composable
private fun VoiceRing(level: Float, modifier: Modifier) {
    val voice by animateFloatAsState(level, tween(90), label = "voice")
    Canvas(modifier.size(96.dp)) {
        val r = size.minDimension * 0.3f * (1f + 0.6f * voice)
        paperCircle(Color(0xFFFFF3C4), r, center)
        drawCircle(Color.White, r, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
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
 * A hiding spot drawn in paper. While Pip is in it, each call makes it rustle; on the third call
 * two floppy ears peek out; on the last it folds open (flat, like a pop-up page closing).
 */
@Composable
private fun PipHidingSpot(spot: PipSpot, cx: Dp, cy: Dp, size: Dp, current: Boolean, stage: Int, opened: Boolean, rustle: Int) {
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
            if (current && stage >= 2 && !opened) {
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
