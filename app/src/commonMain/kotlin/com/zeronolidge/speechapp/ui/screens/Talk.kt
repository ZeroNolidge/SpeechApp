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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.zeronolidge.speechapp.logic.SpeechMatch
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Speak { MODELLING, LISTENING, WAITING_TAP, DONE, PARROT_RECORDING, PARROT_PLAYING }

/** Only ask for the microphone automatically once per run, so a "no" isn't asked again and again. */
private var askedForMic = false

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
    KidScreen("${sound.emoji} ${sound.label}", nav.home, "Let's say ${sound.modelSound}") {
        SpeakMoment(word, sound, Modifier.fillMaxSize(), intro = intro, showParrot = true, key = index) {
            index++
        }
    }
}

/**
 * A speaking moment, used inside the story and in free practice. In order:
 *  1. The picture and word, modelled slowly and clearly a few times (normal, slow, normal).
 *  2. "Your turn!" and the microphone turns on by itself.
 *  3. Any honest try moves on. A clear catch earns a shiny sticker and an extra sparkle.
 *     A miss gets a friendly "say it with me", one more listen, and then moves on anyway.
 *  4. The big ▶ skips at any time. Without a microphone, the child says the word and taps ▶.
 * The microphone never blocks the story and a miss is never a fail.
 * [onDone] gets true if the microphone heard the word clearly.
 */
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
    onDone: (clear: Boolean) -> Unit,
) {
    val app = LocalApp.current
    val p = app.progress
    val micPermission = rememberMicPermission()
    val scope = rememberCoroutineScope()
    var state by remember(key) { mutableStateOf(Speak.MODELLING) }
    var level by remember { mutableFloatStateOf(0f) }
    var attempt by remember(key) { mutableIntStateOf(0) }
    var silent by remember(key) { mutableIntStateOf(0) }
    var shiny by remember(key) { mutableStateOf<WordStat?>(null) }
    // Bumped whenever a new step starts, so callbacks from an interrupted one are ignored.
    var session by remember(key) { mutableIntStateOf(0) }
    val canRecognize = remember { app.listener.available }
    fun micUsable() = p.useMic && canRecognize && micPermission.granted()
    val yourTurn = "Your turn! Say ${word.text}!"

    fun stopAll() {
        app.listener.cancel()
        app.parrot.stop()
        session++
        level = 0f
    }

    /** The try is over: record it, reward it, and move the story on. */
    fun finish(clear: Boolean, heard: Boolean, skipped: Boolean = false) {
        if (state == Speak.DONE) return
        stopAll()
        state = Speak.DONE
        val next = { onDone(clear) }
        if (skipped) {
            app.speaker.say(listOfNotNull("Okay! Let's keep going.", afterLine).joinToString(" "), onDone = next)
            return
        }
        val stat = p.recordWord(word.text, word.emoji, clear, heard)
        if (heard) p.speechResult(clear)
        val praise = when {
            clear -> "I heard ${word.text}! So clear! You got a shiny sticker!"
            heard -> listOf("Great trying!", "Good talking!", "I love your trying!").random()
            else -> Praise.great.random()
        }
        if (clear) shiny = stat
        app.reward(Progress.AREA_SPEECH, listOfNotNull(praise, afterLine).joinToString(" "), onDone = next)
    }

    lateinit var listen: () -> Unit

    /** "Let's say it together": the word, slowly, then [then]. Ignored if a newer step has started. */
    fun sayItWithMe(then: () -> Unit) {
        stopAll()
        val s = session
        state = Speak.MODELLING
        app.speaker.say("Let's say it together.")
        app.speaker.then(word.syllables, slow = true)
        app.speaker.then(word.text) { if (s == session) then() }
    }

    /** Says [lines] (text to slow?), then either listens or waits for a tap on ▶. */
    fun speakThen(lines: List<Pair<String, Boolean>>, thenListen: Boolean) {
        if (state == Speak.DONE) return
        stopAll()
        val s = session
        state = Speak.MODELLING
        val all = lines + (yourTurn to false)
        all.forEachIndexed { i, (text, slow) ->
            val cb: (() -> Unit)? = if (i == all.lastIndex) ({
                if (s == session && state != Speak.DONE) {
                    if (thenListen && micUsable()) listen() else {
                        // No microphone: give time to say it, then the big ▶ to carry on.
                        state = Speak.WAITING_TAP
                        scope.launch {
                            delay(2500)
                            if (s == session && state == Speak.WAITING_TAP) {
                                app.speaker.say("When you've said it, tap the big green arrow!")
                            }
                        }
                    }
                }
            }) else null
            if (i == 0) app.speaker.say(text, slow, cb) else app.speaker.then(text, slow, cb)
        }
    }

    /** The word, modelled clearly: normal, slow and in pieces, then normal again. */
    fun model(): List<Pair<String, Boolean>> = listOf(
        "Listen. ${word.text}." to false,
        word.syllables to true,
        word.text to false,
    )

    listen = {
        val s = session
        state = Speak.LISTENING
        attempt++
        app.listener.listen(word.text, onLevel = { level = it }) { results ->
            if (s != session || state == Speak.DONE) return@listen
            level = 0f
            when {
                results.isEmpty() -> {
                    silent++
                    if (silent == 1) speakThen(listOf("I'm listening!" to false), true)
                    // Two quiet turns: say it together, then carry on anyway.
                    else sayItWithMe { finish(clear = false, heard = false) }
                }
                SpeechMatch.isGood(word.text, results) -> finish(clear = true, heard = true)
                attempt < 2 -> {
                    // A miss: say it together, and one more listen.
                    p.speechResult(false)
                    speakThen(listOf("Good try! Say it with me." to false, word.syllables to true, word.text to false), true)
                }
                else -> finish(clear = false, heard = true)
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

    // Start: narration, the word modelled a few times, "Your turn!", then listen.
    LaunchedEffect(key) {
        app.rememberInstruction("${word.text}. ${word.syllables}. ${word.text}. $yourTurn")
        val lines = listOfNotNull(intro?.let { it to false }) + model()
        when {
            !p.useMic || !canRecognize -> speakThen(lines, false)
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
        }
        Spacer(Modifier.height(10.dp))
        // One row of big buttons: hear it, slowly, mouth, microphone (and copy-me in practice), then ▶.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val count = 3 + (if (sound != null) 1 else 0) + (if (showParrot && p.useMic) 1 else 0)
            val gap = 12.dp
            val size = ((maxWidth - gap * count) / (count + 1)).coerceIn(64.dp, 88.dp)
            val active = state != Speak.DONE
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top,
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
                    withMic(onDenied = { speakThen(emptyList(), false) }) { silent = 0; speakThen(emptyList(), true) }
                }
                if (showParrot && p.useMic) {
                    RoundIcon("🦜", Color(0xFFB2EBF2), size = size, enabled = active) {
                        withMic(onDenied = { app.speaker.say("Ask a grown-up to turn on the microphone.") }) { parrotNow() }
                    }
                }
                // Always there: carry on. It pulses and is pointed at only when it's the way forward.
                GoButton(size = size, enabled = active, hint = state == Speak.WAITING_TAP) {
                    if (state == Speak.WAITING_TAP) finish(clear = false, heard = false) else finish(clear = false, heard = false, skipped = true)
                }
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
