package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.data.Word
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission

private enum class Speak { MODELLING, LISTENING, WAITING_TAP, DONE, PARROT_RECORDING, PARROT_PLAYING }

/** Only ask for the microphone automatically once per run, so a "no" isn't asked again and again. */
private var askedForMic = false

/** Set when the microphone couldn't be opened, so later words go straight to "say it out loud". */
private var micBroken = false

/** Free practice of one speech sound, word by word. Each word moves on by itself after a try. */
@Composable
fun TalkPracticeScreen(sound: SpeechSound, nav: Nav) {
    val app = LocalApp.current
    val words = remember(sound) {
        when (app.progress.speechStep) {
            0 -> sound.words.sortedBy { it.syllableCount }
            1 -> sound.words.sortedByDescending { it.syllableCount == 2 }
            else -> sound.phrases + sound.words
        }
    }
    var index by remember { mutableIntStateOf(0) }
    val word = words[index % words.size]
    val intro = if (index == 0) "Let's practice the ${sound.label} sound. ${sound.modelSound}." else null
    KidScreen("${sound.emoji} ${sound.label}", nav.home, "Let's say ${sound.modelSound}", onBack = nav.back) {
        SpeakMoment(word, sound, Modifier.fillMaxSize(), intro = intro, showParrot = true, key = index) {
            index++
        }
    }
}

/**
 * A speaking moment, used inside the story and in free practice. In order:
 *  1. The picture and word, played slowly and clearly (slow, then normal).
 *  2. "3, 2, 1, your turn!" and about 4 seconds of listening, with a throbbing microphone.
 *     Listening only checks for a voice (loudness), not the exact word, so it works with young
 *     voices and on iPhones and iPads.
 *  3. Voice heard: a celebration and a shiny sticker. Nothing heard: "Let's say it together!",
 *     the word again, and the story carries on. No microphone (or permission denied): Rory says
 *     "Say it out loud to me!" and waits for a tap on ▶.
 *  4. The big ▶ skips at any time. Every try (not a skip) puts a gem in the backpack.
 * The microphone never blocks the story and quiet is never a fail.
 * [onDone] gets true if a voice was heard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakMoment(
    word: Word,
    sound: SpeechSound?,
    modifier: Modifier = Modifier,
    /** Story narration said first. */
    intro: String? = null,
    /** Said after the try, e.g. "Now we can open the door!". */
    afterLine: String? = null,
    showParrot: Boolean = false,
    /** What to show instead of the word's emoji, e.g. a sleeping friend. */
    picture: (@Composable (Modifier) -> Unit)? = null,
    key: Any = word,
    onDone: (heard: Boolean) -> Unit,
) {
    val app = LocalApp.current
    val p = app.progress
    val micPermission = rememberMicPermission()
    var state by remember(key) { mutableStateOf(Speak.MODELLING) }
    var level by remember { mutableFloatStateOf(0f) }
    var shiny by remember(key) { mutableStateOf<WordStat?>(null) }
    /** Rory is waiting for the child to say it out loud (no microphone). */
    var roryWaiting by remember(key) { mutableStateOf(false) }
    // Bumped whenever a new step starts, so callbacks from an interrupted one are ignored.
    var session by remember(key) { mutableIntStateOf(0) }
    fun micUsable() = p.useMic && app.voice.available && micPermission.granted() && !micBroken
    val countdown = "3, 2, 1, your turn!"

    fun stopAll() {
        app.voice.cancel()
        app.parrot.stop()
        session++
        level = 0f
    }

    /** The try is over: record it, reward it, and move the story on. */
    fun finish(heard: Boolean, micUsed: Boolean, skipped: Boolean = false) {
        if (state == Speak.DONE) return
        stopAll()
        state = Speak.DONE
        roryWaiting = false
        val next = { onDone(heard) }
        if (skipped) {
            app.speaker.say(listOfNotNull("Okay! Let's keep going.", afterLine).joinToString(" "), onDone = next)
            return
        }
        val stat = p.recordWord(word.text, word.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        if (heard) shiny = stat
        // Gems are for trying to say words, not for getting things right.
        val piece = p.addGem()
        val praise = when {
            heard -> "I heard you! Great saying ${word.text}! You got a shiny sticker!"
            micUsed -> "Great job saying it together!"
            else -> listOf("Great talking!", "Good saying!", "I love your talking!").random()
        }
        val gemLine = if (piece != null) "Your backpack is full! Your buddy got a new ${piece.name}!" else "A gem for your backpack!"
        app.reward(Progress.AREA_SPEECH, listOfNotNull(praise, gemLine, afterLine).joinToString(" "), onDone = next)
        if (piece != null) app.showGift(piece)
    }

    /** No microphone: Rory asks for it out loud and waits for a tap on ▶. */
    fun askOutLoud() {
        state = Speak.WAITING_TAP
        roryWaiting = true
        app.instruct("Say it out loud to me! Then tap the big green arrow.")
    }

    lateinit var listen: () -> Unit

    /** Says [lines] (text to slow?), then the countdown and listening, or Rory's "say it out loud". */
    fun speakThen(lines: List<Pair<String, Boolean>>, thenListen: Boolean) {
        if (state == Speak.DONE) return
        stopAll()
        val s = session
        state = Speak.MODELLING
        roryWaiting = false
        val useMic = thenListen && micUsable()
        val all = lines + if (useMic) listOf(countdown to false) else emptyList()
        if (all.isEmpty()) {
            if (useMic) listen() else askOutLoud()
            return
        }
        all.forEachIndexed { i, (text, slow) ->
            val cb: (() -> Unit)? = if (i == all.lastIndex) ({
                if (s == session && state != Speak.DONE) {
                    if (useMic) listen() else askOutLoud()
                }
            }) else null
            if (i == 0) app.speaker.say(text, slow, cb) else app.speaker.then(text, slow, cb)
        }
    }

    /** The word, played slowly and clearly, then at normal speed. */
    fun model(): List<Pair<String, Boolean>> = listOf(
        "Listen." to false,
        word.syllables to true,
        word.text to false,
    )

    listen = {
        val s = session
        state = Speak.LISTENING
        app.voice.listen(4000, onLevel = { level = it }) { heard ->
            if (s != session || state == Speak.DONE) return@listen
            level = 0f
            when (heard) {
                true -> finish(heard = true, micUsed = true)
                false -> {
                    // Quiet: say it together, then carry on anyway.
                    stopAll()
                    val s2 = session
                    state = Speak.MODELLING
                    app.speaker.say("Let's say it together!")
                    app.speaker.then(word.syllables, slow = true)
                    app.speaker.then(word.text) { if (s2 == session) finish(heard = false, micUsed = true) }
                }
                null -> {
                    // The microphone couldn't be opened: carry on without it.
                    micBroken = true
                    askOutLoud()
                }
            }
        }
    }

    fun withMic(onDenied: () -> Unit, action: () -> Unit) {
        if (micPermission.granted()) action() else {
            askedForMic = true
            micPermission.request { ok -> if (ok) action() else onDenied() }
        }
    }

    fun parrotNow() {
        stopAll()
        state = Speak.PARROT_RECORDING
        app.speaker.say("Say ${word.text} and I'll copy you!") {
            if (state != Speak.PARROT_RECORDING) return@say
            app.parrot.recordThenPlay(
                seconds = 3,
                onPlaying = { state = Speak.PARROT_PLAYING },
                onDone = { state = Speak.WAITING_TAP },
            )
        }
    }

    // Start: narration, the word played slowly, the countdown, then listen.
    LaunchedEffect(key) {
        app.rememberInstruction("${word.syllables}. ${word.text}. Your turn! Say ${word.text}!")
        val lines = listOfNotNull(intro?.let { it to false }) + model()
        when {
            !p.useMic || !app.voice.available || micBroken -> speakThen(lines, false)
            micPermission.granted() -> speakThen(lines, true)
            !askedForMic -> withMic(onDenied = { speakThen(lines, false) }) { speakThen(lines, true) }
            else -> speakThen(lines, false)
        }
    }
    DisposableEffect(key) { onDispose { stopAll() } }

    // The word card on top, then one row of big icon buttons. Works the same on a phone and a tablet.
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val t = rememberInfiniteTransition(label = "model")
        val beat by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "beat")
        val pulse by animateFloatAsState(
            when (state) {
                Speak.LISTENING -> 1f + level * 0.25f
                Speak.MODELLING -> beat
                else -> 1f
            },
            label = "pulse",
        )
        // The picture card: tap it to hear the word again.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val small = maxHeight < 240.dp
            PaperButton(
                PaperStyle.chip,
                Modifier.fillMaxSize().padding(bottom = 8.dp),
                enabled = state != Speak.DONE,
                onClick = { speakThen(model(), true) },
            ) {
                Row(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().scale(pulse), contentAlignment = Alignment.Center) {
                        if (picture != null) picture(Modifier.fillMaxHeight()) else Text(word.emoji, fontSize = if (small) 64.sp else 110.sp)
                    }
                    Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            highlighted(word.text, sound?.id),
                            fontSize = if (small) 30.sp else if (word.text.length > 12) 36.sp else 50.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaperStyle.ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.height(if (small) 64.dp else 96.dp), contentAlignment = Alignment.Center) {
                            when (state) {
                                Speak.LISTENING, Speak.PARROT_RECORDING -> ListeningBadge(level, if (small) 56.dp else 84.dp)
                                Speak.PARROT_PLAYING -> Text("🦜", fontSize = 52.sp)
                                Speak.MODELLING -> Text("👂", fontSize = 52.sp)
                                Speak.WAITING_TAP -> Text("🗣️", fontSize = 52.sp)
                                Speak.DONE -> shiny?.let { Sticker(it, if (small) 64.dp else 92.dp, Modifier.popIn(it.text)) }
                            }
                        }
                    }
                }
            }
            // No microphone: Rory pops up big and asks for the word out loud, then waits for ▶.
            if (roryWaiting) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 16.dp).popIn("rory"),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Critter(Species.ROBOT, Modifier.height(minOf(maxHeight * 0.7f, 150.dp)), Color(0xFFFFCC80), mood = Mood.CHEER)
                    PaperChip("🗣️ ${word.text}!", fontSize = 22.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Big buttons: hear it, slowly, mouth, microphone (and copy-me in practice), then ▶.
        // The ▶ always keeps its own space, so it is never pushed off a narrow phone screen;
        // the other buttons wrap onto a second line if they don't fit.
        val active = state != Speak.DONE
        val size = 68.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoundIcon("🔊", PaperColorsUi.hear, size = size, enabled = active) { speakThen(model(), true) }
                RoundIcon("🐢", Color(0xFFC8E6C9), size = size, enabled = active) {
                    speakThen(listOf(word.syllables to true, word.text to true), true)
                }
                if (sound != null) {
                    RoundIcon("👄", Color(0xFFFFCDD2), size = size, enabled = active) {
                        speakThen(listOf(sound.tip to false, sound.modelSound to true), true)
                    }
                }
                RoundIcon("🎤", Color(0xFFFFE0B2), size = size, enabled = active) {
                    micBroken = false
                    withMic(onDenied = { speakThen(emptyList(), false) }) { speakThen(emptyList(), true) }
                }
                if (showParrot && p.useMic) {
                    RoundIcon("🦜", Color(0xFFB2EBF2), size = size, enabled = active) {
                        withMic(onDenied = { app.speaker.say("Ask a grown-up to turn on the microphone.") }) { parrotNow() }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            // Always there: carry on. It pulses and is pointed at only when it's the way forward.
            GoButton(size = 76.dp, enabled = active, hint = state == Speak.WAITING_TAP) {
                if (state == Speak.WAITING_TAP) finish(heard = false, micUsed = false) else finish(heard = false, micUsed = false, skipped = true)
            }
        }
    }
}

/** A big red microphone that throbs with the child's voice, so they can see it's their turn. */
@Composable
private fun ListeningBadge(level: Float, size: Dp) {
    val t = rememberInfiniteTransition(label = "listen")
    val ring by t.animateFloat(1f, 1.35f, infiniteRepeatable(tween(700), RepeatMode.Restart), label = "ring")
    val ringAlpha by t.animateFloat(0.6f, 0f, infiniteRepeatable(tween(700), RepeatMode.Restart), label = "ringAlpha")
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .scale(ring)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = ringAlpha)),
        )
        Box(
            Modifier
                .size(size)
                .scale(1f + level * 0.3f)
                .clip(CircleShape)
                .background(Color(0xFFE53935)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎤", fontSize = (size.value * 0.5f).sp)
        }
    }
}

/** Makes the practised sound stand out in the word, e.g. the "sh" in "fish". */
private fun highlighted(text: String, soundId: String?) = buildAnnotatedString {
    if (soundId == null) {
        append(text)
        return@buildAnnotatedString
    }
    // "c" makes the K sound in words like "cat" and "cookie".
    val at = text.indexOf(soundId).takeIf { it >= 0 }
        ?: if (soundId == "k") text.indexOf('c') else -1
    if (at < 0) {
        append(text)
        return@buildAnnotatedString
    }
    val len = if (soundId == "k" && text[at] == 'c') 1 else soundId.length
    append(text.substring(0, at))
    withStyle(SpanStyle(color = Color(0xFFE53935))) { append(text.substring(at, at + len)) }
    append(text.substring(at + len))
}
