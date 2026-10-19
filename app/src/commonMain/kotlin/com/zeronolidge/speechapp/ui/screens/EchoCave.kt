package com.zeronolidge.speechapp.ui.screens

import androidx.compose.ui.graphics.drawscope.Stroke
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.logic.WordStat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.logic.Echo
import com.zeronolidge.speechapp.logic.EchoCave
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.state.today
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.art.paperCircle
import com.zeronolidge.speechapp.ui.art.paperOval
import com.zeronolidge.speechapp.ui.art.paperPath
import com.zeronolidge.speechapp.ui.art.paperRoundRect
import com.zeronolidge.speechapp.ui.art.star
import com.zeronolidge.speechapp.ui.components.ContinueAction
import com.zeronolidge.speechapp.ui.components.ContinueHost
import com.zeronolidge.speechapp.ui.components.ContinueReserve
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.components.wiggleOnTap
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import com.zeronolidge.speechapp.ui.art.PaperStyle

private enum class CavePhase { INTRO, PROMPT, LISTEN, WAIT_TAP, ECHO, END }

/**
 * "Echo Cave", a short reward stop after finding Pip (and the last stop of a long play): Rory
 * says a word (his own words and target sounds first), he says it into the paper cave, the echo
 * bounces back and a crystal lights up. 8 tries. Any voice counts (loudness only, never speech
 * recognition); the big "I said it!" button is always there; no score, timer or fail. Then back to
 * the story, or, at the end of a long play ([rest]), to Rory's "let's rest".
 */
@Composable
fun EchoCaveScreen(nav: Nav, rest: Boolean) {
    val app = LocalApp.current
    val p = app.progress
    val words = remember { EchoCave.words(p.customWords, p.focusSounds, p.speechStep, kotlin.random.Random(kotlin.random.Random.nextLong())) }
    val mic = rememberMicPermission()
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(CavePhase.INTRO) }
    var lit by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    /** How much the next crystal glimmers from his voice this try (0 to 1). */
    var glimmer by remember { mutableFloatStateOf(0f) }
    var session by remember { mutableIntStateOf(0) }
    val echoAnim = remember { Animatable(1f) }
    val still = LocalReduceMotion.current
    fun micOk() = p.useMic && app.voice.available && mic.granted()
    LaunchedEffect(lit) { if (lit > 0 && !still) { echoAnim.snapTo(0f); echoAnim.animateTo(1f, tween(1200)) } }

    lateinit var ask: (Int) -> Unit

    fun done() {
        phase = CavePhase.END
        // Only now, with every try done, does the cave stop waiting on the map (opening it and
        // leaving early keeps it there).
        p.updateEchoReady(false)
        app.celebrate("All the crystals are shining! What a great talker you are!")
        app.rememberInstruction("All the crystals are shining! Tap the big green arrow.")
    }

    fun echo(i: Int, heard: Boolean, micUsed: Boolean) {
        if (phase == CavePhase.ECHO || phase == CavePhase.END) return
        val s = session
        phase = CavePhase.ECHO
        app.listening = false
        level = 0f
        app.listenLevel = 0f
        val w = words[i]
        p.recordWord(w.text, w.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        p.addGem()?.let { app.showGift(it) }
        lit = i + 1
        // The echo bounces back from the cave, softer and softer.
        app.speaker.say(EchoCave.echo(w.text)) {
            if (s != session) return@say
            when {
                i + 1 >= words.size -> done()
                // After about 12 minutes of play: rest now (the cave stays on the map for next time).
                app.sessionLong() -> nav.go(Screen.Rest)
                else -> ask(i + 1)
            }
        }
    }

    /** This word's voice push (pops and holds make the next crystal glimmer); reset each word. */
    val push = remember { arrayOf(com.zeronolidge.speechapp.logic.VoicePush()) }

    lateinit var listen: (Int) -> Unit
    listen = { i ->
        val s = session
        val w = words[i]
        val model = push[0]
        phase = CavePhase.LISTEN
        app.listening = true
        // Word listening (listen-only here): a small record of what was heard.
        val check = app.wordCheck(w.text, null, modeled = !model.waiting)
        app.voice.listenTry(
            4000, onLevel = { level = it; app.listenLevel = it }, onVoice = null,
            // His voice makes the next crystal glimmer: a flicker per pop, a glow while a sound is held.
            onPush = { v -> if (s == session && v > 0f) { model.feed(v, 50); glimmer = model.progress } },
            mode = model.mode,
            // Only his voice calls (a tap on the glass or a cough doesn't), or "I said it!".
            voiceOnly = true,
            words = check?.grammar, onWords = { h -> check?.let { app.wordHeard(it, h) } },
        ) { t ->
            if (s != session || phase != CavePhase.LISTEN) return@listenTry
            level = 0f
            app.listenLevel = 0f
            if (t == com.zeronolidge.speechapp.audio.VoiceTry.NO_MIC) {
                phase = CavePhase.WAIT_TAP
                app.speaker.say("Say it out loud into the cave! Then tap the big green I said it button.")
                return@listenTry
            }
            when (model.afterTry(heard = t == com.zeronolidge.speechapp.audio.VoiceTry.HEARD, soft = t == com.zeronolidge.speechapp.audio.VoiceTry.SOFT)) {
                com.zeronolidge.speechapp.logic.VoicePush.Next.FINISH_GENTLY -> echo(i, t == com.zeronolidge.speechapp.audio.VoiceTry.HEARD, micUsed = true)
                // Quiet (a whisper still makes the crystal glimmer a little): say it together, one more listen.
                com.zeronolidge.speechapp.logic.VoicePush.Next.AGAIN_TOGETHER -> {
                    glimmer = maxOf(glimmer, model.progress)
                    phase = CavePhase.PROMPT
                    app.listening = false
                    app.speaker.say("Let's say it together!")
                    app.speaker.then(w.text, slow = true) { if (s == session) listen(i) }
                }
                // Still quiet: it waits for his voice or "I said it!" and never moves on by itself.
                com.zeronolidge.speechapp.logic.VoicePush.Next.WAIT -> if (model.tries > com.zeronolidge.speechapp.logic.VoicePush.MAX_TRIES) {
                    listen(i)
                } else {
                    phase = CavePhase.PROMPT
                    app.listening = false
                    app.speaker.say("Say it out loud into the cave! Then tap the big green I said it button.") { if (s == session) listen(i) }
                }
            }
        }
    }

    ask = { i ->
        val s = session
        index = i
        phase = CavePhase.PROMPT
        glimmer = 0f
        val w = words[i]
        push[0] = com.zeronolidge.speechapp.logic.VoicePush(com.zeronolidge.speechapp.logic.VoicePush.modeForLine(w.text, null))
        val line = "${w.text}! Say ${w.text} into the cave!"
        app.rememberInstruction(line)
        // The word slowly first, and only when it has finished, the invitation.
        app.speaker.say(w.text, slow = true)
        app.speaker.then(line) {
            if (s != session || phase != CavePhase.PROMPT) return@then
            if (micOk()) {
                listen(i)
            } else {
                phase = CavePhase.WAIT_TAP
                app.listening = true
                app.speaker.say("Say it out loud into the cave! Then tap the big green I said it button.")
            }
        }
    }

    LaunchedEffect(Unit) {
        val intro = "A secret crystal cave! When you talk into the cave, it talks back, and a crystal lights up. Let's try!"
        app.rememberInstruction(intro)
        val s = session
        // The first talk turn of a sitting: a quick quiet check of the room before Rory talks.
        val go = { if (s == session) app.speaker.say(intro) { if (s == session) ask(0) } }
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

    ScreenFrame(onHome = nav.home, background = { CaveBackdrop(lit, words.size, echoAnim.value, glimmer) }, top = {
        PaperChip("💎", fontSize = 26.sp, maxLines = 1)
    }) {
        ContinueHost {
            // His turn: the listening look (Rory in the one listening spot, clear of the crystal
            // his voice makes glimmer).
            ListeningStage(Modifier.fillMaxSize(), avoid = listOf(crystalArea(lit, words.size))) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                val h = maxHeight
                val word = words.getOrNull(index)
                // The word to say, big, at the top in the middle.
                if (word != null && phase != CavePhase.END) {
                    Row(
                        Modifier.align(Alignment.TopCenter).padding(top = 8.dp).paperCardLite(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(word.emoji, fontSize = 56.sp, modifier = Modifier.wiggleOnTap())
                        Spacer(Modifier.width(12.dp))
                        Text(word.text, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = PaperStyle.ink, maxLines = 1)
                    }
                }
                // Rory and the buddy bottom-left; Rory cups his ear while listening.
                val narrow = w < 520.dp
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 4.dp).height(h * if (narrow) 0.28f else 0.34f).widthIn(max = (w - ContinueReserve).coerceAtLeast(0.dp)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val species = p.hero ?: Species.KID
                    HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.fillMaxHeight().wiggleOnTap(), if (phase == CavePhase.END) Mood.CHEER else Mood.HAPPY)
                    // While listening Rory is the big one in the listening look instead.
                    if (!narrow && !app.listening) Critter(Species.ROBOT, Modifier.fillMaxHeight(0.75f), Color(0xFFFFCC80), mood = Mood.CHEER, talking = true)
                }
            }
            }
            ContinueAction(
                hint = phase == CavePhase.WAIT_TAP || phase == CavePhase.END,
                label = if (phase == CavePhase.END) null else "I said it!",
            ) {
                when (phase) {
                    CavePhase.INTRO -> {
                        session++
                        app.speaker.stop()
                        ask(0)
                    }
                    CavePhase.PROMPT, CavePhase.WAIT_TAP -> {
                        app.speaker.stop()
                        echo(index, heard = false, micUsed = false)
                    }
                    CavePhase.LISTEN -> {
                        app.voice.cancel()
                        echo(index, heard = false, micUsed = false)
                    }
                    CavePhase.ECHO -> {}
                    // Back to the story, or Rory's "let's rest" at the end of a long play.
                    CavePhase.END -> if (rest || app.sessionLong()) nav.go(Screen.Rest) else nav.go(Screen.Chapter())
                }
            }
        }
    }
}

/** A plain paper chip around the word to say. */
private fun Modifier.paperCardLite() = this.paperCard(PaperStyle.chip, 22.dp).padding(horizontal = 18.dp, vertical = 8.dp)

/**
 * The cave in paper: a deep purple wall, a darker cave mouth in the middle with echo rings coming
 * out of it, and a crystal round the arch for every try (lit ones shine in rainbow colours).
 */
/** Where crystal [i] of [total] sits round the arch (parts of the screen), for the listening Rory to stand clear of. */
private fun crystalArea(i: Int, total: Int): androidx.compose.ui.geometry.Rect {
    val a = kotlin.math.PI.toFloat() * (0.08f + 0.84f * i.coerceAtMost(total - 1) / (total - 1).coerceAtLeast(1))
    val cx = 0.5f - kotlin.math.cos(a) * 0.3f
    val cy = 0.62f - kotlin.math.sin(a) * 0.42f
    return androidx.compose.ui.geometry.Rect(cx - 0.07f, cy - 0.1f, cx + 0.07f, cy + 0.1f)
}

@Composable
private fun CaveBackdrop(lit: Int, total: Int, echo: Float, glimmer: Float = 0f) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        paperRoundRect(Color(0xFF4A3F78), Offset(0f, 0f), Size(w, h))
        paperRoundRect(Color(0xFF5C4F91), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
        // The cave mouth: an arch.
        val mouth = Path().apply {
            moveTo(w * 0.3f, h * 0.86f)
            cubicTo(w * 0.3f, h * 0.3f, w * 0.7f, h * 0.3f, w * 0.7f, h * 0.86f)
            close()
        }
        paperPath(mouth, Color(0xFF231C3D))
        // Echo rings bouncing out of the cave.
        if (echo < 1f) {
            for (k in 0 until 3) {
                val e = (echo - k * 0.18f).coerceIn(0f, 1f)
                if (e > 0f && e < 1f) {
                    drawCircle(Color(0xFFFFF59D).copy(alpha = (1f - e) * 0.8f), radius = h * (0.08f + 0.35f * e), center = Offset(w * 0.5f, h * 0.62f), style = Stroke(6f))
                }
            }
        }
        // Crystals round the arch.
        val colours = listOf(0xFFFF8A80, 0xFFFFD180, 0xFFFFFF8D, 0xFFB9F6CA, 0xFF80D8FF, 0xFFB388FF, 0xFFFF80AB, 0xFFA7FFEB)
        for (i in 0 until total) {
            val a = kotlin.math.PI.toFloat() * (0.08f + 0.84f * i / (total - 1).coerceAtLeast(1))
            val cx = w * 0.5f - kotlin.math.cos(a) * w * 0.3f
            val cy = h * 0.62f - kotlin.math.sin(a) * h * 0.42f
            val r = minOf(w, h) * 0.05f
            val on = i < lit
            val c = if (on) Color(colours[i % colours.size]) else Color(0xFF7E78A8)
            if (on) drawCircle(c.copy(alpha = 0.35f), r * 2f, Offset(cx, cy))
            // The next crystal glimmers while his voice reaches it (pops flicker it, holds glow).
            if (i == lit && glimmer > 0f) drawCircle(Color(0xFFFFF59D).copy(alpha = 0.25f + 0.45f * glimmer.coerceIn(0f, 1f)), r * (1.4f + glimmer), Offset(cx, cy))
            val gem = Path().apply {
                moveTo(cx, cy - r * 1.4f); lineTo(cx + r * 0.8f, cy); lineTo(cx, cy + r * 1.1f); lineTo(cx - r * 0.8f, cy); close()
            }
            paperPath(gem, c)
        }
    }
}

/**
 * The end of a long play (about 12 minutes: the chapter, then the Echo Cave): Pip's sleepy, Rory
 * says let's rest, and today's sticker is shown. No timer, nothing taken away; the map is one tap.
 */
@Composable
fun RestScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val sticker = remember {
        p.words.words.values.filter { it.lastDay == today() }.sortedWith(compareByDescending<WordStat> { it.clear > 0 }.thenByDescending { it.tries }).firstOrNull()
    }
    LaunchedEffect(Unit) {
        app.sessionClosed = true
        val line = "Pip's sleepy. Let's rest! " + (sticker?.let { "Look, your sticker from today: ${it.text}! " } ?: "") + "Great playing, ${p.heroName}. See you next time!"
        app.rememberInstruction(line)
        app.instruct(line)
    }
    // Night has fallen over the whole forest.
    ScreenFrame(onHome = nav.home, background = {
        Scenery(Land.FOREST, Modifier.fillMaxSize(), seed = 31)
        Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFF1A2350).copy(alpha = 0.5f)) }
    }) {
        ContinueHost {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                val h = maxHeight
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sticker != null) Sticker(sticker, minOf(w, h) * 0.32f, Modifier.popIn("sticker"))
                }
                Row(
                    Modifier.align(Alignment.BottomStart).height(h * 0.32f).widthIn(max = (w - ContinueReserve).coerceAtLeast(0.dp)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val species = p.hero ?: Species.KID
                    HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.fillMaxHeight().wiggleOnTap(), Mood.SLEEP)
                    Critter(Species.PUPPY, Modifier.fillMaxHeight(0.55f).wiggleOnTap(), mood = Mood.SLEEP)
                    if (w >= 520.dp) Critter(Species.ROBOT, Modifier.fillMaxHeight(0.7f).wiggleOnTap(), Color(0xFFFFCC80), mood = Mood.HAPPY, talking = true)
                }
            }
            ContinueAction(delayMs = 2500) { nav.home() }
        }
    }
}
