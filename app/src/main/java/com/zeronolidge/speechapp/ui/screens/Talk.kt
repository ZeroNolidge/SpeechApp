package com.zeronolidge.speechapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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

private enum class Mic { IDLE, LISTENING, PARROT_RECORDING, PARROT_PLAYING }

/** Free practice of one speech sound, word by word. */
@Composable
fun TalkPracticeScreen(sound: SpeechSound, back: () -> Unit) {
    val app = LocalApp.current
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(sound.id) {
        app.speaker.say("Let's practice the ${sound.label} sound. ${sound.modelSound}.")
    }
    LaunchedEffect(index) {
        if (index > 0) app.speaker.say(sound.words[index].text)
    }
    KidScreen("Say ${sound.label} ${sound.emoji}", Kid.talk, back, "Let's say ${sound.modelSound}") {
        SayPanel(sound, sound.words[index], Modifier.fillMaxSize(), showParrot = true, onSaid = {}) {
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
 * Practise saying one word: hear it, hear it slowly, learn where the mouth goes,
 * say it to the microphone, or let a grown-up give the thumbs up.
 * [onSaid] runs after the word has been said well and praised.
 */
@Composable
fun SayPanel(
    sound: SpeechSound,
    word: Word,
    modifier: Modifier = Modifier,
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
    var done by remember(word) { mutableStateOf(false) }

    fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    val canRecognize = remember { app.listener.available }

    fun success(message: String) {
        tries = 0
        done = true
        app.reward(Progress.AREA_SPEECH, message, onDone = { done = false; onSaid() })
    }

    fun listenNow() {
        heard = null
        mic = Mic.LISTENING
        app.speaker.say("Your turn! Say ${word.text}.") {
            if (mic != Mic.LISTENING) return@say
            app.listener.listen(word.text, onLevel = { level = it }) { results ->
                mic = Mic.IDLE
                level = 0f
                heard = results.firstOrNull()
                tries++
                when {
                    SpeechMatch.isGood(word.text, results) ->
                        success("I heard ${word.text}! ${Praise.great.random()}")
                    // Practice is what matters; reward effort, too.
                    tries >= 3 -> success("Great trying! You worked so hard. ${word.syllables}. ${word.text}.")
                    else -> {
                        app.speaker.say(Praise.speechTry.random())
                        app.speaker.then(word.syllables, slow = true)
                    }
                }
            }
        }
    }

    fun parrotNow() {
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

    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) afterPermission?.invoke() else app.speaker.say("Ask a grown-up to turn on the microphone.")
        afterPermission = null
    }

    fun withMic(action: () -> Unit) {
        if (hasMic()) action() else {
            afterPermission = action
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAll() {
        app.listener.cancel()
        app.parrot.stop()
        mic = Mic.IDLE
        level = 0f
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
                    .clickable { app.speaker.say(word.text) }
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
                Text(
                    when (mic) {
                        Mic.LISTENING -> "👂 Listening…"
                        Mic.PARROT_RECORDING -> "🦜 Say it now!"
                        Mic.PARROT_PLAYING -> "🦜 Here's you!"
                        Mic.IDLE -> heard?.let { "I heard: “$it”" } ?: " "
                    },
                    fontSize = 18.sp,
                    color = Kid.ink.copy(alpha = 0.7f),
                )
                footer()
            }

            // The action buttons, big enough for small hands.
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val busy = mic != Mic.IDLE || done
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("🔊", "Hear it", Modifier.weight(1f), enabled = !busy) {
                        app.speaker.say(word.text)
                    }
                    ActionButton("🐢", "Slowly", Modifier.weight(1f), enabled = !busy) {
                        app.speaker.say(word.syllables, slow = true)
                        app.speaker.then(word.text, slow = true)
                    }
                    ActionButton("👄", "How?", Modifier.weight(1f), enabled = !busy) {
                        app.speaker.say(sound.tip)
                        app.speaker.then(sound.modelSound, slow = true)
                    }
                }
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (app.progress.useMic && canRecognize) {
                        ActionButton(
                            if (mic == Mic.LISTENING) "👂" else "🎤", "My turn", Modifier.weight(1f),
                            color = Color(0xFFFFCDD2), enabled = !done && (mic == Mic.IDLE || mic == Mic.LISTENING),
                        ) {
                            if (mic == Mic.LISTENING) stopAll() else withMic { listenNow() }
                        }
                    }
                    if (showParrot && app.progress.useMic) {
                        ActionButton("🦜", "Copy me", Modifier.weight(1f), color = Color(0xFFC8E6C9), enabled = !busy) {
                            withMic { parrotNow() }
                        }
                    }
                    // For the grown-up: tap when the child says it well.
                    ActionButton("👍", "Said it!", Modifier.weight(1f), color = Color(0xFFFFF59D), enabled = !busy) {
                        success("${Praise.great.random()} You said ${word.text}!")
                    }
                }
            }
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
