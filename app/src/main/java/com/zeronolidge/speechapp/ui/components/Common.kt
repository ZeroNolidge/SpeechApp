package com.zeronolidge.speechapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.audio.Listener
import com.zeronolidge.speechapp.audio.Parrot
import com.zeronolidge.speechapp.audio.Speaker
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.delay

/** Everything a screen needs to talk, listen and award stars. */
class AppServices(
    val speaker: Speaker,
    val listener: Listener,
    val parrot: Parrot,
    val progress: Progress,
) {
    /** Bumped every time a star is earned; the celebration overlay watches it. */
    var celebrations by mutableIntStateOf(0)
        private set

    fun reward(area: String, message: String = Praise.great.random(), onDone: (() -> Unit)? = null) {
        progress.addStar(area)
        celebrations++
        speaker.say(message, onDone = onDone)
    }

    fun tryAgain(message: String = Praise.tryAgain.random()) = speaker.say(message)
}

val LocalApp = staticCompositionLocalOf<AppServices> { error("AppServices not provided") }

/** A screen with a home/back button, a title and the star counter. */
@Composable
fun KidScreen(
    title: String,
    color: Color,
    onBack: () -> Unit,
    titleSpoken: String = title,
    content: @Composable ColumnScope.() -> Unit,
) {
    val app = LocalApp.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Kid.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon("⬅️", color, size = 56.dp) { app.speaker.stop(); onBack() }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = Kid.ink,
                modifier = Modifier
                    .weight(1f)
                    .clickable { app.speaker.say(titleSpoken) },
            )
            StarCount(app.progress.stars)
        }
        Spacer(Modifier.size(8.dp))
        content()
    }
}

@Composable
fun StarCount(stars: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⭐", fontSize = 28.sp)
        Spacer(Modifier.width(6.dp))
        Text("$stars", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
    }
}

@Composable
fun RoundIcon(
    emoji: String,
    color: Color,
    size: Dp = 72.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

/** A big, chunky, colourful button with an emoji and an optional label. */
@Composable
fun BigTile(
    emoji: String,
    label: String?,
    color: Color,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 56.sp,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .alpha(if (enabled) 1f else 0.35f)
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .then(if (selected) Modifier.border(6.dp, Kid.highlight, RoundedCornerShape(28.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = emojiSize, textAlign = TextAlign.Center)
        if (label != null) {
            Text(
                label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Kid.ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A white answer card. Wrong answers fade out so the child can try again. */
@Composable
fun AnswerCard(
    modifier: Modifier = Modifier,
    faded: Boolean = false,
    correct: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val s by pulse.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "scale",
    )
    Box(
        modifier
            .scale(if (correct) s else 1f)
            .alpha(if (faded) 0.3f else 1f)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(if (correct) Kid.good else Kid.card)
            .clickable(enabled = !faded && !correct, onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Shows answer options and handles right/wrong taps the same way everywhere:
 * wrong answers fade with a gentle "try again", the right one glows and earns a star.
 */
@Composable
fun <T> AnswerRow(
    options: List<T>,
    answer: T,
    area: String,
    roundKey: Any,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onSolved: () -> Unit,
    /** Optional words for an option, said back to the child when they tap it. */
    describe: ((T) -> String)? = null,
    content: @Composable (T) -> Unit,
) {
    val app = LocalApp.current
    var faded by remember(roundKey) { mutableStateOf(setOf<T>()) }
    var solved by remember(roundKey) { mutableStateOf(false) }
    var advanced by remember(roundKey) { mutableStateOf(false) }
    val advance = {
        if (!advanced) {
            advanced = true
            onSolved()
        }
    }
    // Normally the next round starts when the praise finishes; this is a backstop.
    if (solved) AfterDelay(roundKey, 4000) { advance() }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { opt ->
            AnswerCard(
                modifier = cardModifier,
                faded = opt in faded,
                correct = solved && opt == answer,
                onClick = {
                    if (!solved) {
                        val said = describe?.invoke(opt)?.let { "$it! " } ?: ""
                        if (opt == answer) {
                            solved = true
                            app.reward(area, said + Praise.great.random(), onDone = advance)
                        } else {
                            faded = faded + opt
                            app.tryAgain(said + Praise.tryAgain.random())
                        }
                    }
                },
            ) { content(opt) }
        }
    }
}

@Composable
fun Emoji(text: String, size: TextUnit, modifier: Modifier = Modifier) {
    Text(text, fontSize = size, modifier = modifier, textAlign = TextAlign.Center)
}

/** Falling stars and confetti whenever [AppServices.celebrations] changes. */
@Composable
fun CelebrationOverlay() {
    val app = LocalApp.current
    val key = app.celebrations
    if (key == 0) return
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(1600, easing = LinearEasing))
    }
    if (progress.value >= 1f) return
    val pieces = remember(key) {
        val r = kotlin.random.Random(key)
        List(16) {
            Triple(r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, listOf("⭐", "🌟", "🎉", "✨", "🎈").random(r))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        pieces.forEach { (x, speed, emoji) ->
            val y = (progress.value * speed * 1.4f - 0.2f)
            Text(
                emoji,
                fontSize = 40.sp,
                modifier = Modifier.offset(x = w * x - 20.dp, y = h * y),
            )
        }
    }
}

/** Waits, then runs [action]; handy for moving to the next round after praise. */
@Composable
fun AfterDelay(key: Any, ms: Long, action: () -> Unit) {
    LaunchedEffect(key) {
        delay(ms)
        action()
    }
}

@Composable
fun Prompt(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔊", fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
    }
}
