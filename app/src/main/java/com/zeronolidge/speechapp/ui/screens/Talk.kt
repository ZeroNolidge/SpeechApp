package com.zeronolidge.speechapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.data.Word
import com.zeronolidge.speechapp.logic.SpeechMatch
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.theme.Kid

private enum class Mic { IDLE, PROMPTING, LISTENING, PARROT_RECORDING, PARROT_PLAYING }

/** Only ask for the microphone automatically once per run, so a "no" isn't asked again and again. */
private var askedForMic = false

/** Free practice of one speech sound, word by word. */
@Composable
fun TalkPracticeScreen(sound: SpeechSound, back: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val word = sound.words[index]
    val intro = if (index == 0) "Let's practice the ${sound.label} sound. ${sound.modelSound}. ${word.text}." else word.text
    KidScreen("Say ${sound.label} ${sound.emoji}", Kid.talk, back, "Let's say ${sound.modelSound}") {
        SayPanel(sound, word, Modifier.fillMaxSize(), intro = intro, showParrot = true, onSaid = {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavArrow("◀️", index > 0) { index-- }
                Spacer(Modifier.size(12.dp))
                sound.words.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (i == index) 18.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (i == index) Kid.talk else Kid.soft),
                    )
                }
                Spacer(Modifier.size(12.dp))
                NavArrow("▶️", true) { index = (index + 1) % sound.words.size }
            }
        }
    }
}

/**
 * Practise saying one word. After the narrator says "Your turn!", the microphone switches on
 * by itself, because young children can't read a "talk now" button. They can also hear the word,
 * hear it slowly, learn where the mouth goes, or a grown-up can give the thumbs up.
 * [onSaid] runs after the word has been said well and praised.
 */
@Composable
fun SayPanel(
    sound: SpeechSound,
    word: Word,
    modifier: Modifier = Modifier,
    /** Said first, before "Your turn!", e.g. the story narration. */
    intro: String? = null,
    showParrot: Boolean = false,
    onSaid: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val app = LocalApp.current
    val context = LocalContext.current
    var mic by remember(word) { mutableStateOf(Mic.IDLE) }
    var level by remember { mutableFloatStateOf(0f) }
    var heard by remember(word) { mutableStateOf<String?>(null) }
    var tries by remember(word) { mutableIntStateOf(0) }
    var silent by remember(word) { mutableIntStateOf(0) }
    var done by remember(word) { mutableStateOf(false) }
    // Bumped whenever a new prompt starts, so callbacks from an interrupted one are ignored.
    var session by remember(word) { mutableIntStateOf(0) }

    fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    val canRecognize = remember { app.listener.available }
    val micOn = app.progress.useMic && canRecognize

    fun success(message: String) {
        app.listener.cancel()
        session++
        mic = Mic.IDLE
        level = 0f
        tries = 0
        done = true
        app.reward(Progress.AREA_SPEECH, message, onDone = { done = false; onSaid() })
    }

    // Declared up front because the recognizer's result handler prompts again.
    lateinit var speakThenListen: (List<Pair<String, Boolean>>, Boolean) -> Unit

    fun startRecognizer(s: Int) {
        if (s != session || mic != Mic.PROMPTING) return
        mic = Mic.LISTENING
        app.listener.listen(word.text, onLevel = { level = it }) { results ->
            if (s != session) return@listen
            mic = Mic.IDLE
            level = 0f
            heard = results.firstOrNull()
            when {
                results.isEmpty() -> {
                    silent++
                    // Don't keep listening to a quiet room; wait for a tap instead.
                    if (silent >= 2) app.speaker.say("Tap the microphone when you're ready!")
                    else speakThenListen(listOf("I didn't hear you." to false), true)
                }
                SpeechMatch.isGood(word.text, results) ->
                    success("I heard ${word.text}! ${Praise.great.random()}")
                else -> {
                    silent = 0
                    tries++
                    // Practice is what matters; reward effort, too.
                    if (tries >= 3) success("Great trying! You worked so hard. ${word.syllables}. ${word.text}.")
                    else speakThenListen(listOf(Praise.speechTry.random() to false, word.syllables to true), true)
                }
            }
        }
    }

    /** Says each line (text to slow?), then "Your turn!" and switches the microphone on if [listen]. */
    speakThenListen = { lines, listen ->
        if (!done) {
            app.listener.cancel()
            session++
            val s = session
            level = 0f
            val all = lines + if (listen) listOf("Your turn! Say ${word.text}!" to false) else emptyList()
            mic = if (listen) Mic.PROMPTING else Mic.IDLE
            all.forEachIndexed { i, (text, slow) ->
                val cb: (() -> Unit)? = if (listen && i == all.lastIndex) ({ startRecognizer(s) }) else null
                if (i == 0) app.speaker.say(text, slow, cb) else app.speaker.then(text, slow, cb)
            }
        }
    }

    /** Speaks, then listens automatically when the microphone is allowed. */
    fun speakAndMaybeListen(vararg lines: Pair<String, Boolean>) =
        speakThenListen(lines.toList(), micOn && hasMic())

    fun parrotNow() {
        app.listener.cancel()
        session++
        mic = Mic.PARROT_RECORDING
        app.speaker.say("Say ${word.text} and I'll copy you!") {
            if (mic != Mic.PARROT_RECORDING) return@say
            app.parrot.recordThenPlay(
                seconds = 3,
                onPlaying = { mic = Mic.PARROT_PLAYING },
                onDone = { mic = Mic.IDLE },
            )
        }
    }

    var afterPermission by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        afterPermission?.invoke(ok)
        afterPermission = null
    }

    fun withMic(onDenied: () -> Unit = { app.speaker.say("Ask a grown-up to turn on the microphone.") }, action: () -> Unit) {
        if (hasMic()) action() else {
            askedForMic = true
            afterPermission = { ok -> if (ok) action() else onDenied() }
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAll() {
        app.listener.cancel()
        app.parrot.stop()
        session++
        mic = Mic.IDLE
        level = 0f
    }

    // Start each word: say the intro, then "Your turn!" and listen.
    LaunchedEffect(word) {
        val lines = listOfNotNull(intro?.let { it to false })
        when {
            !micOn -> speakThenListen(lines + ("Say ${word.text}!" to false), false)
            hasMic() -> speakThenListen(lines, true)
            !askedForMic -> withMic(onDenied = { speakThenListen(lines + ("Say ${word.text}!" to false), false) }) {
                speakThenListen(lines, true)
            }
            else -> speakThenListen(lines + ("Say ${word.text}!" to false), false)
        }
    }
    DisposableEffect(word) { onDispose { stopAll() } }

    BoxWithConstraints(modifier) {
        val small = maxHeight < 320.dp
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // The word card.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .clickable(enabled = !done) { speakAndMaybeListen(word.text to false) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                val pulse by animateFloatAsState(if (mic == Mic.LISTENING) 1f + level * 0.25f else 1f, label = "pulse")
                Text(word.emoji, fontSize = if (small) 64.sp else 100.sp, modifier = Modifier.scale(pulse))
                Text(
                    highlighted(word.text, sound.id),
                    fontSize = if (small) 36.sp else 50.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Kid.ink,
                )
                if (mic == Mic.LISTENING || mic == Mic.PARROT_RECORDING) {
                    ListeningBadge(level, if (small) 56.dp else 72.dp)
                } else {
                    Text(
                        when (mic) {
                            Mic.PARROT_PLAYING -> "🦜 Here's you!"
                            Mic.PROMPTING -> "🔊"
                            else -> heard?.let { "I heard: “$it”" } ?: " "
                        },
                        fontSize = if (mic == Mic.PROMPTING) 30.sp else 18.sp,
                        color = Kid.ink.copy(alpha = 0.7f),
                    )
                }
                footer()
            }

            // The action buttons, big enough for small hands.
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Hearing the word again interrupts the microphone, then it listens again.
                val canTap = !done && mic != Mic.PARROT_RECORDING && mic != Mic.PARROT_PLAYING
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("🔊", "Hear it", Modifier.weight(1f), enabled = canTap) {
                        speakAndMaybeListen(word.text to false)
                    }
                    ActionButton("🐢", "Slowly", Modifier.weight(1f), enabled = canTap) {
                        speakAndMaybeListen(word.syllables to true, word.text to true)
                    }
                    ActionButton("👄", "How?", Modifier.weight(1f), enabled = canTap) {
                        speakAndMaybeListen(sound.tip to false, sound.modelSound to true)
                    }
                }
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (micOn) {
                        val listening = mic == Mic.LISTENING || mic == Mic.PROMPTING
                        ActionButton(
                            "🎤", if (listening) "Stop" else "My turn", Modifier.weight(1f),
                            color = Color(0xFFFFCDD2), enabled = canTap,
                        ) {
                            if (listening) stopAll() else withMic {
                                silent = 0
                                speakThenListen(emptyList(), true)
                            }
                        }
                    }
                    if (showParrot && app.progress.useMic) {
                        ActionButton("🦜", "Copy me", Modifier.weight(1f), color = Color(0xFFC8E6C9), enabled = canTap) {
                            withMic { parrotNow() }
                        }
                    }
                    // For the grown-up: tap when the child says it well.
                    ActionButton("👍", "Said it!", Modifier.weight(1f), color = Color(0xFFFFF59D), enabled = !done) {
                        stopAll()
                        success("${Praise.great.random()} You said ${word.text}!")
                    }
                }
            }
        }
    }
}

/** A big red microphone that throbs with the child's voice, so they can see it's their turn. */
@Composable
private fun ListeningBadge(level: Float, size: androidx.compose.ui.unit.Dp) {
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
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFE53935)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎤", fontSize = (size.value * 0.5f).sp)
        }
    }
}

/** Makes the practised sound stand out in the word, e.g. the "sh" in "fish". */
private fun highlighted(text: String, soundId: String) = buildAnnotatedString {
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

@Composable
private fun ActionButton(
    emoji: String,
    label: String,
    modifier: Modifier,
    color: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .fillMaxHeight()
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
    }
}

@Composable
private fun NavArrow(emoji: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (enabled) Kid.soft else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) Text(emoji, fontSize = 30.sp)
    }
}
